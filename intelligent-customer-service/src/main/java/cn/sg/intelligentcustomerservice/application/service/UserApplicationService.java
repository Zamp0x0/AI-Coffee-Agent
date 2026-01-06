package cn.sg.intelligentcustomerservice.application.service;


import cn.sg.intelligentcustomerservice.domain.entity.User;
import cn.sg.intelligentcustomerservice.domain.service.UserDomainService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;


/**
 * Created on 2025/11/8.
 *
 */
@Service
@AllArgsConstructor
public class UserApplicationService {
    private final UserDomainService userDomainService;

    public User register(String name, String phone) {
        return userDomainService.register(name, phone);
    }
}
