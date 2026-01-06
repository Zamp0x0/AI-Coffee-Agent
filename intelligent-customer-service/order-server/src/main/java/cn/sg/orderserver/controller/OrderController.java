package cn.sg.orderserver.controller;

import cn.sg.orderserver.dto.CreateOrderCmd;
import cn.sg.orderserver.entity.Order;
import cn.sg.orderserver.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("order")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("create")
    public String create(@RequestBody CreateOrderCmd cmd) {
        log.info("create");
        log.info("[CREATE] cmd={}", cmd);
        return orderService.createOrder(cmd);
    }

    @PostMapping("list")
    public List<Order> list() {
        log.info("list");
        return orderService.orders();
    }

    @PostMapping("complete")
    public String complete(@RequestBody CompleteOrderCmd cmd) {
        log.info("complete");
        orderService.complete(cmd.orderNumber());
        return "SUCCESS";
    }

    record CompleteOrderCmd(String orderNumber) {}

    @PostMapping("cancel")
    public String cancel(@RequestBody CancelOrderCmd cmd) {
        log.info("cancel");
        return orderService.cancel(cmd.orderNumber());
    }

    record CancelOrderCmd(String orderNumber) {}
}
