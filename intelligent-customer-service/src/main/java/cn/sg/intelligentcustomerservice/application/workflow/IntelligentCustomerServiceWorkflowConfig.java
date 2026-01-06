package cn.sg.intelligentcustomerservice.application.workflow;

import cn.sg.intelligentcustomerservice.infrastructure.client.CreateOrderCmd;
import cn.sg.intelligentcustomerservice.infrastructure.client.OrderClient;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class IntelligentCustomerServiceWorkflowConfig {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    // ✅ 新增：下单用的 Feign Client
    private final OrderClient orderClient;

    private static final double DISTANCE_THRESHOLD = 0.30;
    private static final double SCORE_THRESHOLD = 0.30;

    @Bean("intelligentCustomerService")
    public CompiledGraph intelligentCustomerServiceWorkflow() throws GraphStateException {

        KeyStrategyFactory overAllStateFactory = () -> {
            OverAllState s = new OverAllState();

            s.registerKeyAndStrategy("history", KeyStrategy.REPLACE);
            s.registerKeyAndStrategy("user", KeyStrategy.REPLACE);

            // ✅ 下单 & 用户态必须的
            s.registerKeyAndStrategy("userId", KeyStrategy.REPLACE);
            s.registerKeyAndStrategy("userName", KeyStrategy.REPLACE);
            s.registerKeyAndStrategy("userPhone", KeyStrategy.REPLACE);

            return s.keyStrategies();
        };

        StateGraph stateGraph = new StateGraph("intelligentCustomerService", overAllStateFactory);

        stateGraph.addNode("rag", AsyncNodeAction.node_async((OverAllState state) -> {

            String question = state.value(OverAllState.DEFAULT_INPUT_KEY, "");
            if (question == null || question.isBlank()) {
                return Map.of("output", "你还没输入问题哦～");
            }

            // ✅ 0) 下单意图：优先处理，不走知识库
            if (isOrderIntent(question)) {
                return Map.of("output", handleCreateOrder(state, question));
            }

            // ✅ 闲聊/能力类：固定回复，不走知识库
            if (isSmallTalk(question)) {
                return Map.of("output",
                        "你好～我是AI咖啡Agent☕\n" +
                                "你可以问我：菜单/口味/推荐/咖啡区别/价格等店内信息。");
            }
            if (isCapabilityAsk(question)) {
                return Map.of("output",
                        "我只回答【咖啡店内知识库】里有的内容，比如：\n" +
                                "1) 菜单与口味介绍\n" +
                                "2) 咖啡区别与推荐\n" +
                                "3) 各类咖啡的价格\n" +
                                "如果你问的是店外泛知识（非本店咖啡相关内容），我会提示找不到。\n" +
                                "你现在想问哪一项？");
            }

            String history = state.value("history", "");
            String userInfo = state.value("user", "");

            // 1) 向量检索（topK=3）
            List<Document> docs = similaritySearchTopK(question, 3);

            log.info("[RAG] question={}", question);
            log.info("[RAG] docs.size={}", docs == null ? 0 : docs.size());
            if (docs != null) {
                for (int i = 0; i < docs.size(); i++) {
                    Document d = docs.get(i);
                    log.info("[RAG] doc[{}].id={}", i, safeDocId(d));
                    log.info("[RAG] doc[{}].metadata={}", i, d.getMetadata());
                    log.info("[RAG] doc[{}].textOrContent={}", i, safeDocTextOrContent(d));
                }
            }

            if (docs == null || docs.isEmpty()) {
                return Map.of("output", notFoundReply());
            }

            List<Document> acceptedDocs = docs.stream()
                    .filter(this::isHitAcceptable)
                    .collect(Collectors.toList());

            if (acceptedDocs.isEmpty()) {
                log.info("[RAG] docs returned but all rejected by threshold. return notFound.");
                return Map.of("output", notFoundReply());
            }

            String contextAnswers = acceptedDocs.stream()
                    .map(this::extractAnswerText)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .collect(Collectors.joining("\n---\n"));

            if (contextAnswers.isBlank()) {
                log.warn("[RAG] hit docs but cannot extract answer from metadata.answer nor doc text/content.");
                return Map.of("output", notFoundReply());
            }

            String prompt = buildStrictKbPrompt(userInfo, history, question, contextAnswers);

            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (answer == null || answer.isBlank()) {
                return Map.of("output", notFoundReply());
            }

            return Map.of("output", answer);
        }));

        stateGraph.addEdge(StateGraph.START, "rag");
        stateGraph.addEdge("rag", StateGraph.END);

        return stateGraph.compile();
    }

    // -------------------- 下单：核心逻辑 --------------------

    // ======================= 下单：只允许咖啡白名单 =======================

    // 允许下单的“店内咖啡菜单”
    private static final Set<String> ALLOWED_COFFEE_ITEMS = Set.of(
            "美式", "冰美式",
            "拿铁", "冰拿铁",
            "卡布奇诺",
            "摩卡",
            "焦糖玛奇朵",
            "香草拿铁"
    );

    // 同义词映射：把各种说法归一成菜单标准名
    private static final Map<String, String> COFFEE_ALIAS_MAP = Map.ofEntries(
            Map.entry("美式咖啡", "美式"),
            Map.entry("冰美式咖啡", "冰美式"),
            Map.entry("冰美式", "冰美式"),

            Map.entry("拿铁咖啡", "拿铁"),
            Map.entry("冰拿铁咖啡", "冰拿铁"),
            Map.entry("冰拿铁", "冰拿铁"),

            Map.entry("卡布", "卡布奇诺"),
            Map.entry("卡布奇诺咖啡", "卡布奇诺"),

            Map.entry("摩卡咖啡", "摩卡"),

            Map.entry("焦糖玛奇朵咖啡", "焦糖玛奇朵"),
            Map.entry("香草拿铁咖啡", "香草拿铁")
    );

    // ✅ 口感/定制选项归一
    private static final Map<String, String> OPTION_ALIAS_MAP = new LinkedHashMap<>();
    static {
        // ---------- 冰量 ----------
        OPTION_ALIAS_MAP.put("少冰", "少冰");
        OPTION_ALIAS_MAP.put("去冰", "去冰");
        OPTION_ALIAS_MAP.put("免冰", "去冰");
        OPTION_ALIAS_MAP.put("不要冰", "去冰");
        OPTION_ALIAS_MAP.put("无冰", "去冰");
        OPTION_ALIAS_MAP.put("飞冰", "去冰");

        // ---------- 奶泡 ----------
        OPTION_ALIAS_MAP.put("去除奶泡", "去奶泡");
        OPTION_ALIAS_MAP.put("去奶泡", "去奶泡");
        OPTION_ALIAS_MAP.put("不要奶泡", "去奶泡");
        OPTION_ALIAS_MAP.put("不加奶泡", "去奶泡");

        // ---------- 糖度 ----------
        OPTION_ALIAS_MAP.put("无糖", "无糖");
        OPTION_ALIAS_MAP.put("不加糖", "无糖");
        OPTION_ALIAS_MAP.put("不要糖", "无糖");
        OPTION_ALIAS_MAP.put("少糖", "少糖");
        OPTION_ALIAS_MAP.put("半糖", "半糖");
        OPTION_ALIAS_MAP.put("正常糖", "正常糖");
        OPTION_ALIAS_MAP.put("全糖", "全糖");
        OPTION_ALIAS_MAP.put("加糖", "加糖");

        // ---------- 奶类 ----------
        OPTION_ALIAS_MAP.put("加燕麦奶", "燕麦奶");
        OPTION_ALIAS_MAP.put("换燕麦奶", "燕麦奶");
        OPTION_ALIAS_MAP.put("用燕麦奶", "燕麦奶");
        OPTION_ALIAS_MAP.put("燕麦奶", "燕麦奶");

        OPTION_ALIAS_MAP.put("加脱脂奶", "脱脂奶");
        OPTION_ALIAS_MAP.put("换脱脂奶", "脱脂奶");
        OPTION_ALIAS_MAP.put("脱脂奶", "脱脂奶");

        OPTION_ALIAS_MAP.put("加全脂奶", "全脂奶");
        OPTION_ALIAS_MAP.put("换全脂奶", "全脂奶");
        OPTION_ALIAS_MAP.put("全脂奶", "全脂奶");
    }


    private boolean isCoffeeItemAllowed(String itemName) {
        return itemName != null && ALLOWED_COFFEE_ITEMS.contains(itemName);
    }

    /**
     * 规范化商品名：去掉“下单/来/要/给我/我要/一杯/杯/大杯/中杯/小杯/热/冰…”等，
     * 并做同义词归一，最后得到菜单标准名
     */
    private String normalizeCoffeeName(String raw) {
        if (raw == null) return "";

        String s = raw.trim();
        if (s.isEmpty()) return "";

        // 去空格
        s = s.replaceAll("\\s+", "");

        // 去掉前缀废话（可反复出现）
        s = s.replaceAll("^(下单|帮我下单|帮我|给我|我要|我想要|我想|来|要)+", "");

        // 去掉数量/量词（前后都可能出现）
        s = s.replaceAll("^[0-9一二两三四五六七八九十]+(杯|份|个)", "");
        s = s.replaceAll("([0-9一二两三四五六七八九十]+)(杯|份|个)$", "");
        s = s.replaceAll("(一杯|两杯|三杯|四杯|五杯|杯|份|个)", "");

        // 去掉规格词（前后）
        s = s.replaceAll("^(大杯|中杯|小杯|热|冰)+", "");
        s = s.replaceAll("(大杯|中杯|小杯|热|冰)+$", "");

        // 去掉“咖啡”后缀
        s = s.replaceAll("咖啡$", "");

        // 同义词归一
        s = COFFEE_ALIAS_MAP.getOrDefault(s, s);

        return s.trim();
    }

    /**
     * 严格价格：只对允许商品返回价格，否则直接抛异常（防“飞机=19.9”这种漏网）
     */
    private String priceByItemNameStrict(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("itemName blank");
        }

        // 你的价格规则：美式 9.9、拿铁 8.8、其他咖啡 19.9
        if ("美式".equals(itemName) || "冰美式".equals(itemName)) {
            return "9.9";
        }
        if ("拿铁".equals(itemName) || "冰拿铁".equals(itemName)) {
            return "8.8";
        }

        if (ALLOWED_COFFEE_ITEMS.contains(itemName)) {
            return "19.9";
        }

        throw new IllegalArgumentException("item not allowed: " + itemName);
    }

    // ✅ 提取口感/定制选项（返回：去奶泡/少冰/去冰...）
    private static List<String> extractOptions(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String s = text.replaceAll("\\s+", "");

        LinkedHashSet<String> opts = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : OPTION_ALIAS_MAP.entrySet()) {
            if (s.contains(e.getKey())) {
                opts.add(e.getValue());
            }
        }

        // ---------- 互斥：冰量 ----------
        if (opts.contains("去冰") && opts.contains("少冰")) {
            opts.remove("少冰");
        }

        // ---------- 互斥：糖度（只保留一个，优先级从强到弱） ----------
        List<String> sugarPriority = List.of("无糖", "少糖", "半糖", "正常糖", "全糖", "加糖");
        keepOnlyHighestPriority(opts, sugarPriority);

        // ---------- 互斥：奶类（只保留一个） ----------
        List<String> milkPriority = List.of("燕麦奶", "脱脂奶", "全脂奶");
        keepOnlyHighestPriority(opts, milkPriority);

        return new ArrayList<>(opts);
    }
    // 优先级保留
    private static void keepOnlyHighestPriority(LinkedHashSet<String> opts, List<String> priority) {
        String keep = null;
        for (String p : priority) {
            if (opts.contains(p)) {
                keep = p;
                break;
            }
        }
        if (keep == null) return;

        for (String p : priority) {
            if (!p.equals(keep)) opts.remove(p);
        }
    }

    // ✅ 把口感词从原句里剔除，避免污染商品解析
    private static String removeOptionsFromText(String text) {
        if (text == null) return "";
        String s = text;
        for (String k : OPTION_ALIAS_MAP.keySet()) {
            s = s.replace(k, "");
        }
        return s;
    }

    // ✅ 最终展示名：摩卡x2【少冰/去奶泡】
    private static String buildDisplayItemName(String baseItemName, int qty, List<String> options) {
        int q = Math.max(1, qty);
        StringBuilder sb = new StringBuilder();
        sb.append(baseItemName).append("x").append(q);
        if (options != null && !options.isEmpty()) {
            sb.append("【").append(String.join("/", options)).append("】");
        }
        return sb.toString();
    }

    private String handleCreateOrder(OverAllState state, String question) {
        try {
            String userId = state.value("userId", "");
            if (userId == null || userId.isBlank()) {
                return "下单失败：我没有拿到你的 userId（会话ID）。请刷新页面或重新进入再试一次。";
            }

            // ✅ 先提取口感/定制项（少冰/去冰/去奶泡等）
            List<String> options = extractOptions(question);

            // ✅ 去掉口感词再解析订单（避免“摩卡少冰”被当作商品名）
            String cleaned = removeOptionsFromText(question);

            ParsedOrder parsed = parseOrderFromText(cleaned);
            if (parsed == null || parsed.itemName == null || parsed.itemName.isBlank()) {
                return "我识别到你想下单，但没看懂你要点什么。\n你可以这样说：下单一杯卡布奇诺 / 来2杯拿铁";
            }

            // ✅ 0) 规范化商品名（标准菜单名）
            String baseItemName = normalizeCoffeeName(parsed.itemName);
            if (baseItemName.isBlank()) {
                return "我识别到你想下单，但没看懂你要点什么。\n你可以这样说：下单一杯卡布奇诺 / 来2杯拿铁";
            }

            // ✅ 1) 白名单：只允许咖啡菜单内商品
            if (!isCoffeeItemAllowed(baseItemName)) {
                return "我只能帮你下单【店内咖啡】哦～\n" +
                        "可下单：美式 / 拿铁 / 卡布奇诺 / 摩卡 / 焦糖玛奇朵 / 香草拿铁 / 冰美式 / 冰拿铁。\n" +
                        "你想来哪一杯？";
            }

            int qty = Math.max(1, parsed.qty);

            // ✅ 2) 单价（严格）
            String unitPriceStr = priceByItemNameStrict(baseItemName);

            // ✅ 2.1) 总价 = 单价 * 数量
            double unitPrice = Double.parseDouble(unitPriceStr);
            double totalPrice = unitPrice * qty;

            // 统一保留 1 位小数（9.9 / 8.8 / 19.9 这种刚好）
            String totalPriceStr = String.format(Locale.US, "%.1f", totalPrice);
            String price = totalPriceStr;

            // ✅ 3) 用户信息（优先 state）
            String userName = state.value("userName", "");
            String userPhone = state.value("userPhone", "");

            // 兜底：从 state 的 "user" 字符串里解析
            if ((userName == null || userName.isBlank()) || (userPhone == null || userPhone.isBlank())) {
                String user = state.value("user", "");
                if (user != null && !user.isBlank()) {
                    if (userName == null || userName.isBlank()) {
                        userName = extractByRegex(user, "(?i)(?:用户名|userName|name)\\s*[:=]\\s*([^\\n,;\\s]+)");
                    }
                    if (userPhone == null || userPhone.isBlank()) {
                        userPhone = extractByRegex(user, "(?i)(?:手机号|userPhone|phone|mobile)\\s*[:=]\\s*([^\\n,;\\s]+)");
                    }
                }
            }

            if (userName == null || userName.isBlank()) userName = "匿名用户";
            if (userPhone == null) userPhone = "";

            // ✅ 4) 最终展示商品名（摩卡x2【少冰】）
            String displayItemName = buildDisplayItemName(baseItemName, qty, options);

            // ✅ 5) 创建订单（itemName 传展示名）
            CreateOrderCmd cmd = new CreateOrderCmd();
            cmd.setUserId(userId);
            cmd.setUserName(userName);
            cmd.setUserPhone(userPhone);
            cmd.setItemName(displayItemName);
            cmd.setPrice(price);

            String resultText = orderClient.create(cmd);

            String orderNo = extractOrderNumber(resultText);
            if (orderNo != null) {
                return "已为你下单 ✅\n订单号：" + orderNo +
                        "\n商品：" + displayItemName +
                        "\n价格：" + price +
                        "\n下单人：" + userName +
                        "\n可在【订单后台】查看。";
            }
            return "已为你下单 ✅\n" + resultText;

        } catch (IllegalArgumentException badItem) {
            return "我只能帮你下单【店内咖啡】哦～\n" +
                    "可下单：美式 / 拿铁 / 卡布奇诺 / 摩卡 / 焦糖玛奇朵 / 香草拿铁 / 冰美式 / 冰拿铁。\n" +
                    "你想来哪一杯？";
        } catch (Exception e) {
            log.error("[ORDER] create order error. question={}", question, e);
            return "下单失败：系统调用订单服务时出错了。请稍后再试。";
        }
    }

    private static String extractByRegex(String text, String regex) {
        if (text == null) return "";
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private static boolean isOrderIntent(String q) {
        if (q == null) return false;
        String s = q.trim();
        // 你可以继续加关键词
        return s.contains("下单") || s.contains("点单") || s.contains("来一杯") || s.contains("来") || s.contains("我要") || s.contains("帮我买");
    }

    // ✅ 支持：下单一杯卡布奇诺 / 来2杯拿铁 / 来两杯摩卡 / 我要十杯美式
    // group2 = 数量（数字或中文数字）
    private static final Pattern ORDER_PATTERN =
            Pattern.compile("(下单|点单|来|我要|帮我买|我需要下单)\\s*([0-9]+|[一二两三四五六七八九十]+)?\\s*(杯|个|份)?\\s*([\\u4e00-\\u9fa5A-Za-z0-9]+)");

    private static ParsedOrder parseOrderFromText(String text) {
        String compact = (text == null ? "" : text).replaceAll("\\s+", "");
        Matcher m = ORDER_PATTERN.matcher(compact);
        if (!m.find()) return null;

        String numStr = m.group(2);
        int qty = 1;
        if (numStr != null && !numStr.isBlank()) {
            qty = parseQty(numStr);
        }

        String item = m.group(4);
        item = item.replace("咖啡", "");
        return new ParsedOrder(item, qty);
    }

    // ✅ 数量解析：2 / 10 / 一 / 二 / 两 / 三 / 十 / 十一 / 二十 / 二十五
    private static int parseQty(String s) {
        if (s == null || s.isBlank()) return 1;
        s = s.trim();

        // 纯数字
        if (s.matches("\\d+")) {
            try {
                int n = Integer.parseInt(s);
                return Math.max(1, n);
            } catch (Exception ignore) {
                return 1;
            }
        }

        // 中文数字（简版：支持到 99）
        return Math.max(1, chineseNumberToInt(s));
    }

    private static int chineseNumberToInt(String s) {
        Map<Character, Integer> map = Map.ofEntries(
                Map.entry('一', 1),
                Map.entry('二', 2),
                Map.entry('两', 2),
                Map.entry('三', 3),
                Map.entry('四', 4),
                Map.entry('五', 5),
                Map.entry('六', 6),
                Map.entry('七', 7),
                Map.entry('八', 8),
                Map.entry('九', 9),
                Map.entry('十', 10)
        );

        if (s == null || s.isBlank()) return 1;
        s = s.trim();

        if (s.length() == 1) {
            return map.getOrDefault(s.charAt(0), 1);
        }

        int tenIdx = s.indexOf('十');
        if (tenIdx >= 0) {
            int tens = (tenIdx == 0) ? 1 : map.getOrDefault(s.charAt(0), 1);
            int ones = (tenIdx < s.length() - 1) ? map.getOrDefault(s.charAt(s.length() - 1), 0) : 0;
            return tens * 10 + ones;
        }

        int sum = 0;
        for (char c : s.toCharArray()) {
            sum += map.getOrDefault(c, 0);
        }
        return sum <= 0 ? 1 : sum;
    }


    private static String extractOrderNumber(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("订单号：\\s*(\\S+)").matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private record ParsedOrder(String itemName, int qty) {}

    // -------------------- RAG 保持原样 --------------------

    private List<Document> similaritySearchTopK(String query, int topK) {
        List<Document> docs = vectorStore.similaritySearch(query);
        if (docs == null || docs.isEmpty()) return Collections.emptyList();
        return docs.stream().limit(topK).collect(Collectors.toList());
    }

    private String extractAnswerText(Document d) {
        if (d == null) return "";
        Object ans = (d.getMetadata() == null) ? null : d.getMetadata().get("answer");
        if (ans != null && !ans.toString().isBlank()) return ans.toString();
        String body = safeDocTextOrContent(d);
        return body == null ? "" : body.trim();
    }

    private boolean isHitAcceptable(Document d) {
        if (d == null) return false;
        Map<String, Object> md = d.getMetadata();
        if (md == null || md.isEmpty()) return true;

        Double distance = toDouble(md.get("distance"));
        Double score = toDouble(md.get("vector_score"));
        if (distance == null && score == null) return true;

        boolean okByDistance = (distance != null) && (distance <= DISTANCE_THRESHOLD);
        boolean okByScore = (score != null) && (score <= SCORE_THRESHOLD);
        return okByDistance || okByScore;
    }

    private static Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return null; }
    }

    private static boolean isSmallTalk(String q) {
        if (q == null) return false;
        String s = q.trim().toLowerCase();
        return s.matches("^(你好|哈喽|嗨|hi|hello|在吗|在不在|忙吗|在嘛).*$");
    }

    private static boolean isCapabilityAsk(String q) {
        if (q == null) return false;
        String s = q.trim();
        return s.contains("你可以做什么") || s.contains("你能做什么") || s.contains("你会什么")
                || s.contains("能干嘛") || s.contains("有什么功能") || s.contains("你是谁");
    }

    private static String notFoundReply() {
        return "我在店内知识库里没找到相关信息，所以暂时无法回答这个问题。\n" +
                "你可以换个问法，或问我店内相关内容，比如：\n" +
                "1) 你们有哪些咖啡？\n" +
                "2) 拿铁和卡布奇诺有什么区别？\n" +
                "3) 咖啡价格是多少？\n" +
                "4) 推荐哪款咖啡？";
    }

    private static String buildStrictKbPrompt(String userInfo, String history, String question, String kbAnswers) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是AI咖啡的店内智能客服。\n");
        sb.append("你只能使用【店内知识库内容】回答，禁止添加任何知识库里没有的信息。\n");
        sb.append("如果知识库无法支持回答，必须原封不动输出这一句：\n");
        sb.append("我在店内知识库里没找到相关信息，所以暂时无法回答这个问题。\n\n");

        if (userInfo != null && !userInfo.isBlank()) sb.append("用户信息：").append(userInfo).append("\n");
        if (history != null && !history.isBlank()) sb.append("历史对话：\n").append(history).append("\n");

        sb.append("【店内知识库内容】\n").append(kbAnswers).append("\n\n");
        sb.append("用户问题：").append(question).append("\n");
        sb.append("输出要求：中文、简洁、直接；不要提到“知识库内容”这几个字。");
        return sb.toString();
    }

    private static String safeDocId(Document d) {
        if (d == null) return "null";
        try {
            Method m = d.getClass().getMethod("getId");
            Object v = m.invoke(d);
            return String.valueOf(v);
        } catch (Exception ignore) {
            return "unknown";
        }
    }

    private static String safeDocTextOrContent(Document d) {
        if (d == null) return "null";
        Object v = invokeIfExists(d, "getText");
        if (v == null) v = invokeIfExists(d, "getContent");
        if (v == null) v = invokeIfExists(d, "content");
        return v == null ? "" : String.valueOf(v);
    }

    private static Object invokeIfExists(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            return m.invoke(obj);
        } catch (Exception ignore) {
            return null;
        }
    }
}
