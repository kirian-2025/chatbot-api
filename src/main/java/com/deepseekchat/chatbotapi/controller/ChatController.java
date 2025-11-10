package com.deepseekchat.chatbotapi.controller;

import com.deepseekchat.chatbotapi.model.Conversation;
import com.deepseekchat.chatbotapi.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request,
                             @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        // 生成或使用传入的sessionId
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        String aiReply = chatService.processMessage(request.getMessage(), sessionId);

        ChatResponse response = new ChatResponse();
        response.setReply(aiReply);
        response.setSessionId(sessionId);
        return response;
    }

    @GetMapping("/history")
    public List<Conversation> getHistory() {
        return chatService.getConversationHistory();
    }

    // 内部类：请求体
    public static class ChatRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    // 内部类：响应体
    public static class ChatResponse {
        private String reply;
        private String sessionId;
        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
}