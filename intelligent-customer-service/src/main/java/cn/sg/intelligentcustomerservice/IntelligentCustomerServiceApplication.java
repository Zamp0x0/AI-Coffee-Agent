package cn.sg.intelligentcustomerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 负责启动Spring Boot应用并初始化应用上下文
 */
@EnableFeignClients
@SpringBootApplication
public class IntelligentCustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelligentCustomerServiceApplication.class, args);
    }

}