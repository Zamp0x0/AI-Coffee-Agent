package cn.sg.intelligentcustomerservice.infrastructure.interfaces;


import cn.sg.intelligentcustomerservice.application.service.UserApplicationService;
import cn.sg.intelligentcustomerservice.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("user")
@AllArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;

    @PostMapping("register")
    public User register(@RequestBody RegisterCMD  cmd) {
        return userApplicationService.register(cmd.name(), cmd.phone());
    }

    public record RegisterCMD(String name, String phone) {
    }

}
