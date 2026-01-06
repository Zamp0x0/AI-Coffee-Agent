package cn.sg.intelligentcustomerservice.application.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.excel.EasyExcel;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 文档加载服务
 * 负责在应用启动时异步加载CSV文档到Redis向量数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentLoaderService implements ApplicationRunner {
    private final VectorStore vectorStore;

    @Value("${app.knowledge.csv-path:data/QA.csv}")
    private String csvPath;

    /**
     * 应用启动后自动执行文档加载
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始异步加载知识库文档...");
        loadDocumentsAsync();
    }

    /**
     * 异步加载文档
     */
    public void loadDocumentsAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                loadDocuments();
            } catch (Exception e) {
                log.error("异步加载文档失败", e);
            }
        });
    }

    /**
     * 加载CSV文档到向量数据库
     */
    public void loadDocuments() {
        File csvFile = new File(csvPath);
        if (!csvFile.exists()) {
            log.warn("CSV文件不存在: {}", csvPath);
            return;
        }
        log.info("开始从CSV文件加载文档: {}", csvPath);
        try {
            // 使用EasyExcel读取CSV文件
            List<Document> docs = EasyExcel.read(csvFile)
                    .head(QAData.class)
                    .sheet()
                    .doReadSync()
                    .stream()
                    .map(item -> toDocument(item))
                    .collect(Collectors.toList());
            Lists.partition(docs,10).forEach(vectorStore::add);
            log.info("从CSV文件加载文档完成 : {}", csvPath);
        } catch (Exception e) {
            log.error("加载文档过程中发生错误", e);
        }
    }

    private Document toDocument(Object raw) {
        QAData document = (QAData) raw;
        String id = DigestUtil.md5Hex(document.getQuestion());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", id);
        metadata.put("question", document.getQuestion());
        metadata.put("answer", document.getAnswer());

        String content = document.getQuestion() + " " + document.getAnswer();
        return new Document(id, content, metadata);
    }

    /**
     * CSV数据模型
     */
    @Data
    public static class QAData {
        private String question;
        private String answer;
    }
}