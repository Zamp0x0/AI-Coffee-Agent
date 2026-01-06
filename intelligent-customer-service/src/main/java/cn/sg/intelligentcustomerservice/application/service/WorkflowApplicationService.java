package cn.sg.intelligentcustomerservice.application.service;

import cn.sg.intelligentcustomerservice.application.dto.WorkflowRunCMD;
import cn.sg.intelligentcustomerservice.domain.entity.User;
import cn.sg.intelligentcustomerservice.domain.service.MessageDomainService;
import cn.sg.intelligentcustomerservice.domain.service.UserDomainService;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author thread
 */
@Slf4j
@Service
@AllArgsConstructor
public class WorkflowApplicationService {

    private final CompiledGraph graph;
    private final MessageDomainService messageDomainService;
    private final UserDomainService userDomainService;

    public String run(WorkflowRunCMD cmd) {
        // 你原来用 JSON 来转 Map 的方式保留也行
        Map<String, Object> inputParam = JSONObject.parseObject(JSONObject.toJSONString(cmd));

        try {
            // 1) history
            String history = messageDomainService.history(cmd.userId());
            inputParam.put("history", history);

            // 2) Graph 默认输入键：input
            inputParam.put(OverAllState.DEFAULT_INPUT_KEY, cmd.userInput()); // 等价于 inputParam.put("input", cmd.userInput());

            // 3) 记录用户消息
            messageDomainService.addUserMsg(cmd.userId(), cmd.userInput());

            // 4) 用户信息（从用户服务查出来最可靠）
            User user = userDomainService.get(cmd.userId());
            if (user != null) {
                // ✅ 给 workflow 用的整串（你之前已有）
                inputParam.put("user", user.toStr());

                // ✅ 关键：单独塞入字段，给下单/业务逻辑用
                inputParam.put("userId", user.getId());
                inputParam.put("userName", user.getName());
                inputParam.put("userPhone", user.getPhone());
            } else {
                inputParam.put("user", "");

                // ✅ 如果查不到用户，就退回用 cmd 里传来的字段（demo 方案C）
                inputParam.put("userId", cmd.userId());
                inputParam.put("userName", cmd.userName());
                inputParam.put("userPhone", cmd.userPhone());
            }

            // ✅（可选增强）如果 user 查到了，但 name/phone 为空，用 cmd 兜底
            if (isBlank((String) inputParam.get("userName")) && !isBlank(cmd.userName())) {
                inputParam.put("userName", cmd.userName());
            }
            if (isBlank((String) inputParam.get("userPhone")) && !isBlank(cmd.userPhone())) {
                inputParam.put("userPhone", cmd.userPhone());
            }

            // 5) 调用工作流
            Optional<OverAllState> stateOpt = graph.call(inputParam);
            Map<String, Object> result = stateOpt.map(OverAllState::data).orElseGet(HashMap::new);

            Object outObj = result.get("output");
            String output = outObj == null ? "" : outObj.toString();

            // 6) 存储模型输出
            messageDomainService.addAssistantMsg(cmd.userId(), output);
            return output;

        } catch (Exception e) {
            log.error("执行工作流失败 fail,", e);
            throw new RuntimeException("系统内部错误");
        } finally {
            log.info("WorkflowController[]execute 结束工作流执行");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
