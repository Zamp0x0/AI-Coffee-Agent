package cn.sg.intelligentcustomerservice.infrastructure.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Created on 2025/11/7.
 *
 */
@FeignClient(name = "order-service", url = "http://localhost:8086")
public interface OrderClient {

    @PostMapping("/order/create")
    String create(@RequestBody CreateOrderCmd cmd);

    @PostMapping("/order/list")
    List<OrderDTO> orderList();

    @PostMapping("/order/complete")
    void complete(@RequestBody OrderCompleteCmd cmd);

    @PostMapping("/order/cancel")
    String cancel(@RequestBody OrderCancelCmd cmd);
}
