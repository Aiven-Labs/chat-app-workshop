package org.acme.websockets;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.vertx.VertxContextSupport;
import jakarta.enterprise.context.ApplicationScoped;


public class MessagePersister implements Runnable {
    private final Message message;

    public MessagePersister(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        

        
    }
}
