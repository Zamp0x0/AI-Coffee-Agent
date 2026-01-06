package cn.sg.orderserver.config;


import cn.sg.orderserver.mcp.OrderMcpController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created on 2025/10/10.
 *
 */

@Slf4j
@Configuration
public class McpConfiguration {
    @Bean
    public ToolCallbackProvider mcpTools(OrderMcpController orderMcpController) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(orderMcpController)
                .build();
    }
}
