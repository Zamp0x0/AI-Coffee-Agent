package cn.sg.intelligentcustomerservice.domain.service;


import cn.sg.intelligentcustomerservice.domain.entity.Message;
import cn.sg.intelligentcustomerservice.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 */
@Service
@RequiredArgsConstructor
public class MessageDomainService {

    private final MessageRepository messageRepository;

    public void addUserMsg(String userId, String content) {
        messageRepository.saveAndFlush(Message.ofUser(userId, content));
    }

    public void addAssistantMsg(String userId, String content) {
        messageRepository.saveAndFlush(Message.ofAssistant(userId, content));
    }

    public String history(String userId) {
        List<Message> message = messageRepository.findAllByUserId(userId);
        return Optional.ofNullable(message).orElse(Collections.emptyList()).stream()
                .map(item -> item.getRole() + ": " + item.getContent())
                .collect(Collectors.joining("\n"));
    }
}