package cn.sg.orderserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单服务应用程序启动类
 * 基于Spring Boot框架，提供订单管理功能
 * 通过MCP协议暴露服务接口，运行在8081端口
 */
@SpringBootApplication
public class OrderServerApplication {

    /**
     * 应用程序入口点
     * 启动订单服务，监听 8081 端口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderServerApplication.class, args);
    }
}