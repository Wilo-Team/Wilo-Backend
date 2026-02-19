package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatbotType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatbotTypeRepository extends JpaRepository<ChatbotType, Long> {

    List<ChatbotType> findAllByIsActiveTrueOrderByIdAsc();
}
