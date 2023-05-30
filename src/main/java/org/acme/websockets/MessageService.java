package org.acme.websockets;



import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MessageService {
    private final ReactivePubSubCommands<Message> pub;

    private final Vertx vertx;

    @Inject                             
    public MessageService(Vertx vertx, ReactiveRedisDataSource ds) { 
        this.vertx = vertx;             
        pub = ds.pubsub(Message.class);
    }
    
   
     public Uni<Message> persistMessage(Message message) {
        //persist in DB
        Context context = vertx.getOrCreateContext();
        Context duplicatedContext = VertxContext.getOrCreateDuplicatedContext(context);
        VertxContextSafetyToggle.setContextSafe(duplicatedContext, Boolean.TRUE);
        duplicatedContext.runOnContext(x -> {
            Panache.withTransaction(message::persist).subscribe().with(panacheEntityBase -> Log.info("Persisted "));
        });
        return Uni.createFrom().item(message);
    } 

  
}
