package cn.sg.intelligentcustomerservice.infrastructure.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author thread
 */
@Slf4j
@Configuration
public class LLMConfiguration {

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        DashScopeResponseFormat jsonFormat = DashScopeResponseFormat
                .builder()
                .type(DashScopeResponseFormat.Type.TEXT)
                .build();
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withTemperature(0.0)
                .withResponseFormat(jsonFormat)
                .build();
        return chatClientBuilder.defaultOptions(options).build();
    }



    @Bean
    @ConditionalOnMissingBean
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        List<Class<? extends RuntimeException>> exceptions = new ArrayList<>();
        return DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false)
                .rethrowExceptions(exceptions)
                .build();
    }
}
