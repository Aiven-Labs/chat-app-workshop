package org.acme.websockets;


import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Message extends PanacheEntity {

    public String username;
    public String content;
    
}
