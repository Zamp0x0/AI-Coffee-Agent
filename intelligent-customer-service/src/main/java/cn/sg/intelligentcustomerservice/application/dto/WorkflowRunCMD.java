package cn.sg.intelligentcustomerservice.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 工作流执行请求
 * 前端每次发消息都会带上当前登录用户信息
 */
public record WorkflowRunCMD(

        @NotBlank(message = "userId is not blank")
        String userId,

        String userName,   // 登录用户姓名（可空，后端兜底）

        String userPhone,  // 登录用户手机号（可空）

        @NotBlank(message = "userInput is not blank")
        String userInput

) {}
