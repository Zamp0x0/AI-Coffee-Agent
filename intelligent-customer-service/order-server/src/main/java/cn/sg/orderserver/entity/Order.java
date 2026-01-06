package cn.sg.orderserver.entity;

import cn.sg.orderserver.dto.CreateOrderCmd;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单实体类
 * 表示系统中的订单信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    /**
     * 订单号，唯一标识
     */
    private String orderNumber;

    /**
     * 用户ID
     */
    private String userId;

    private String userName;

    private String userPhone;

    /**
     * 订单价格
     */
    private String price;

    /**
     * 商品名称
     */
    private String itemName;

    /**
     * 订单状态
     * 0 制作中
     * 1 已完成
     * 2 已退款
     */
    private Integer status;

    /**
     * 订单创建时间
     */
    private LocalDateTime createTime;

    public static Order create(CreateOrderCmd cmd) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUserId(cmd.userId());
        order.setUserName(cmd.userName());
        order.setUserPhone(cmd.userPhone());
        order.setPrice(cmd.price());
        order.setItemName(cmd.itemName());
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    /**
     * 生成订单号
     * 格式：ORDER + 当前时间戳
     *
     * @return 订单号
     */
    private static String generateOrderNumber() {
        return "ORDER" + System.currentTimeMillis();
    }

    public String toStr() {
        String statusStr = status == 0 ? "制作中" : status == 1 ? "已完成" : "已退款";
        return "订单号：" + orderNumber + "\n" +
                "用户姓名：" + userName + "\n" +
                "用户手机号：" + userPhone + "\n" +
                "商品名称：" + itemName + "\n" +
                "价格：" + price + "\n" +
                "订单状态：" + statusStr + "\n" +
                "创建时间：" + createTime;
    }

    public boolean isCompleted() {
        return status == 1;
    }

    public boolean isRefund() {
        return status == 2;
    }

    public void cancel() {
        status = 2;
    }

    public void complete() {
        status = 1;
    }
}