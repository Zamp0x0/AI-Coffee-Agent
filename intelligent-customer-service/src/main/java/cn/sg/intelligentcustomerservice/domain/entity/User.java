package cn.sg.intelligentcustomerservice.domain.entity;


import lombok.Data;

import java.util.UUID;

/**
 * Created on 2025/11/8.
 *
 */
@Data
public class User {

    private String id;

    private String name;

    private String phone;

    public static User register(String name, String phone) {
        User user = new User();
        user.setId(uid());
        user.setName(name);
        user.setPhone(phone);
        return user;
    }

    public static String uid() {
        String ts = String.valueOf(System.currentTimeMillis());   // 13位
        int r = new java.util.Random().nextInt(900000) + 100000;  // 6位
        return "U" + ts.substring(ts.length() - 6) + r;           // U123456789012 这种
    }


    public String toStr() {
        return "用户ID: " + id + "\n" +
                "用户名：" + name + "\n" +
                "手机号：" + phone;
    }
}
