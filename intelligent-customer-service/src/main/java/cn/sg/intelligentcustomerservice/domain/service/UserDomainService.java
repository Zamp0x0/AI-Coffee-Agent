package cn.sg.intelligentcustomerservice.domain.service;


import cn.sg.intelligentcustomerservice.domain.entity.User;
import com.google.common.collect.Maps;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created on 2025/11/8.
 */
@Service
public class UserDomainService {

    private final Map<String, User> userMap = Maps.newHashMap();

    public User register(String name, String phone) {
        User user = User.register(name, phone);
        userMap.put(user.getId(), user);
        return user;
    }

    public User get(String id) {
        return userMap.get(id);
    }

}
