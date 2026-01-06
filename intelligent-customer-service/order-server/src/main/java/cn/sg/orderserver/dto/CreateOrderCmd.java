package cn.sg.orderserver.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderCmd(
        @NotBlank(message = "用户ID") String userId,
        String userName,
        String userPhone,
        @NotBlank(message = "商品价格") String price,
        @NotBlank(message = "商品名称") String itemName
) {}
