package cn.sg.intelligentcustomerservice.infrastructure.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建订单请求（发给 order-server）
 * 字段名必须与 order-server 的 cn.sg.orderserver.dto.CreateOrderCmd 一致
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCmd {
    private String userId;
    private String userName;
    private String userPhone;
    private String price;
    private String itemName;
}
