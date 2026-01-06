package cn.sg.intelligentcustomerservice.domain.repository;

import cn.sg.intelligentcustomerservice.domain.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface MessageRepository extends JpaRepository<Message, Long> {


    List<Message> findAllByUserId(String userId);
}