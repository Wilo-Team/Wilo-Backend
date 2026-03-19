package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatMessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageAttachmentRepository extends JpaRepository<ChatMessageAttachment, Long> {
    List<ChatMessageAttachment> findAllByMessageId(Long messageId);
    void deleteAllByMessageId(Long messageId);
}
