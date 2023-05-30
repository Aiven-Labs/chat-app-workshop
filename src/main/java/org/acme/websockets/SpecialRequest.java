package org.acme.websockets;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SpecialRequest {
    
    private String username;
    private String content;
    
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    
}
