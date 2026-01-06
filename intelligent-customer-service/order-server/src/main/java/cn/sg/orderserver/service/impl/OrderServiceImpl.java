package cn.sg.orderserver.service.impl;

import cn.sg.orderserver.dto.CreateOrderCmd;
import cn.sg.orderserver.entity.Order;
import cn.sg.orderserver.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 订单服务实现类
 * 实现订单创建等核心业务逻辑
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    /**
     * 内存存储订单信息（实际项目中应使用数据库）
     */
    private final List<Order> orderStorage = new ArrayList<>();

    /**
     * 创建订单
     */
    @Override
    public String createOrder(CreateOrderCmd cmd) {
        // ✅ record 用 cmd.userId() 而不是 cmd.getUserId()
        log.info("开始创建订单，会话ID: {}", cmd.userId());

        Order order = Order.create(cmd);
        orderStorage.add(order);
        return order.toStr();
    }

    @Override
    public String orderDetail(String orderNumber) {
        return orderStorage.stream()
                .filter(i -> i.getOrderNumber().equals(orderNumber))
                .findFirst()
                .map(Order::toStr)
                .orElse("未查询到订单信息");
    }

    @Override
    public String cancel(String orderNumber) {
        Order order = orderStorage.stream()
                .filter(i -> i.getOrderNumber().equals(orderNumber))
                .findFirst()
                .orElse(null);
        if (Objects.isNull(order)) {
            return "未查询到订单信息";
        }
        if (order.isRefund()) {
            return "订单已退款";
        }
        if (order.isCompleted()) {
            return "订单已完成，不允许退款";
        }
        order.cancel();
        return "SUCCESS";
    }

    @Override
    public List<Order> orders() {
        return orderStorage.stream()
                .sorted((o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime()))
                .toList();
    }

    @Override
    public void complete(String orderNumber) {
        orderStorage.stream()
                .filter(i -> i.getOrderNumber().equals(orderNumber))
                .findFirst()
                .ifPresent(Order::complete);
    }

    @Override
    public List<Order> userOrder(String userId) {
        return orderStorage.stream()
                .filter(order -> Objects.equals(order.getUserId(), userId))
                .toList();
    }
}
