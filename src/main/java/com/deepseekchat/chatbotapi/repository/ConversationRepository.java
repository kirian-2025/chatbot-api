package com.deepseekchat.chatbotapi.repository;

import com.deepseekchat.chatbotapi.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findBySessionId(String sessionId);
    List<Conversation> findAllByOrderByCreatedAtDesc();
}