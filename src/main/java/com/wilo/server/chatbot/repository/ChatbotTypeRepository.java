package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatbotType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatbotTypeRepository extends JpaRepository<ChatbotType, Long> {

    List<ChatbotType> findAllByIsActiveTrueOrderByIdAsc();
    boolean existsByCode(String code);
    Optional<ChatbotType> findByCode(String code);
}
