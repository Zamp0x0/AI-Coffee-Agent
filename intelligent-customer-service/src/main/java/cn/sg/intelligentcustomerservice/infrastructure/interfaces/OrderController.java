package cn.sg.intelligentcustomerservice.infrastructure.interfaces;


import cn.sg.intelligentcustomerservice.infrastructure.client.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created on 2025/11/7.
 *
 */
@Slf4j
@RestController
@RequestMapping("order")
@AllArgsConstructor
public class OrderController {

    private final OrderClient orderService;


    @PostMapping("create")
    public String create(@RequestBody CreateOrderCmd cmd) {
        return orderService.create(cmd);
    }


    /**
     * 订单列表
     *
     * @return 订单列表
     */
    @PostMapping("list")
    public List<OrderDTO> list() {
        return orderService.orderList();
    }

    /**
     * 订单完成
     *
     * @param cmd 订单完成请求
     * @return 订单完成响应
     */
    @PostMapping("complete")
    public String complete(@RequestBody OrderCompleteCmd cmd) {
        orderService.complete(cmd);
        return "SUCCESS";
    }

    /**
     * 订单取消
     *
     * @param cmd 订单取消请求
     */
    @PostMapping("cancel")
    public String cancel(@RequestBody OrderCancelCmd cmd) {
        return orderService.cancel(cmd);
    }

}
