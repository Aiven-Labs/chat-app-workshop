package org.acme.websockets;

import java.util.function.Consumer;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

@ApplicationScoped
public class RedisChatSession implements Consumer<Message>{
    
    private Session session;
    private ReactivePubSubCommands<Message> pub;
    private Uni<ReactivePubSubCommands.ReactiveRedisSubscriber> subscriber;

    public RedisChatSession(ReactiveRedisDataSource ds){

       pub = ds.pubsub(Message.class);
       pub.subscribe("notifications", this).subscribe().with(item -> Log.info("subscribed"));
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Session getSession(){
      return this.session;
    }

    @Override
    public void accept(Message t) {
      
      //Panache.withSession(()->t.persist());  
      session.getAsyncRemote().sendObject(">> " + t.username + ": " + t.content);
    }
}
