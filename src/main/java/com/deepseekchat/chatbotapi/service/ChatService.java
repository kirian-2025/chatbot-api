package com.deepseekchat.chatbotapi.service;

import com.deepseekchat.chatbotapi.model.Conversation;
import com.deepseekchat.chatbotapi.model.Message;
import com.deepseekchat.chatbotapi.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private DeepSeekService deepSeekService;

    public String processMessage(String userMessage, String sessionId) {
        // 查找或创建会话
        Conversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElse(new Conversation(sessionId));

        // 如果是新会话，设置标题
        if (conversation.getTitle() == null) {
            conversation.setTitle(userMessage.substring(0, Math.min(userMessage.length(), 20)) + "...");
        }

        // 添加用户消息
        conversation.getMessages().add(new Message("user", userMessage));

        // 准备发送给DeepSeek的消息
        List<Map<String, String>> deepSeekMessages = new ArrayList<>();
        for (Message msg : conversation.getMessages()) {
            Map<String, String> messageMap = new HashMap<>();
            messageMap.put("role", msg.getRole());
            messageMap.put("content", msg.getContent());
            deepSeekMessages.add(messageMap);
        }

        // 调用DeepSeek API
        String aiReply = deepSeekService.getChatResponse(deepSeekMessages);

        // 在这里添加格式优化处理
        String formattedReply = formatAIResponse(aiReply);

        // 添加AI回复
        conversation.getMessages().add(new Message("assistant", aiReply));

        // 保存到MongoDB
        conversationRepository.save(conversation);

        return formattedReply;
    }

    //格式化方法
    private String formatAIResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return "抱歉，我暂时无法回复。";
        }

        // 专门处理DeepSeek回复中的特殊格式
        String formatted = rawResponse;

        // 首先处理转义换行符（核心修复）
        formatted = formatted.replace("\\n", "\n");

        // 清理多余的实际换行符
        formatted = formatted.replaceAll("\n{3,}", "\n\n");

        // 深度清理转义字符和错误格式
        formatted = formatted
                .replace("\\underline{\\s/\\n}", " ")
                .replace("\\s/\\n}", " ")
                .replace("\\underline{", "")
                // 移除下面这行，因为已经在上面统一处理了
                // .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\t", "    ")
                .replace("\\r", "");

        // 修复DeepSeek特有的格式问题
        formatted = formatted
                .replaceAll("-#\\s*", "\n## ")          // 修复"-#基本信息" -> "## 基本信息"
                .replaceAll("-\\*\\*", "\n• ")           // 修复"-**身份**" -> "• 身份"
                .replaceAll("\\*\\*", "")               // 移除所有粗体
                .replaceAll("-\\s+", "\n• ")            // 标准化列表
                .replaceAll("(?m)^\\s*\\d+\\.\\s*", "\n$0\n"); // 数字列表格式化

        // 智能分段
        String[] sections = formatted.split("\n\n+");
        StringBuilder result = new StringBuilder();

        for (String section : sections) {
            String trimmed = section.trim();
            if (!trimmed.isEmpty()) {
                // 为每个段落添加适当格式
                if (trimmed.startsWith("##") || trimmed.matches("^[#]+ .*")) {
                    result.append("\n").append(trimmed).append("\n");
                } else if (trimmed.startsWith("•") || trimmed.matches("^\\d+\\. .*")) {
                    result.append(trimmed).append("\n");
                } else {
                    result.append(trimmed).append("\n\n");
                }
            }
        }

        return result.toString().trim();
    }

    public List<Conversation> getConversationHistory() {
        return conversationRepository.findAllByOrderByCreatedAtDesc();
    }
}