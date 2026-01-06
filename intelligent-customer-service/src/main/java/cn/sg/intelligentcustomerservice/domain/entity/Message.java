package cn.sg.intelligentcustomerservice.domain.entity;


import cn.sg.intelligentcustomerservice.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Created on 2025/11/8.
 *
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "t_message")
public class Message extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private String userId;

    // user/ assistant
    @Column(name = "role")
    private String role;

    @Column(name = "content")
    private String content;

    public static Message ofUser(String userId, String content) {
        return Message.builder()
                .userId(userId)
                .role("user")
                .content(content)
                .build();
    }

    public static Message ofAssistant(String userId, String content) {
        return Message.builder()
                .userId(userId)
                .role("assistant")
                .content(content)
                .build();
    }
}
