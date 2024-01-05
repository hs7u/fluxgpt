package com.openai;

import com.openai.completion.ChatMessage;

import lombok.Getter;

/**
 * when function call is not triggered then throw this exception
 */
public class NullCallException extends NullPointerException {
    
    @Getter
    private ChatMessage chatMessage;

    public NullCallException(ChatMessage chatMessage) {
        super(chatMessage.getContent());
        this.chatMessage = chatMessage;
    }
}
