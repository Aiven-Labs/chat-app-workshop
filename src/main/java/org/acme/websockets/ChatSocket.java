package org.acme.websockets;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@ServerEndpoint("/chat/{username}")
@ApplicationScoped
public class ChatSocket {

    @Inject
    MessageService messageService;

    private Multi<Message> messages;

    ReactiveRedisDataSource defaultReactiveRedisDataSource;

    private final ReactivePubSubCommands<Message> pub;

    public ChatSocket(ReactiveRedisDataSource ds) {
        defaultReactiveRedisDataSource = ds;
        pub = defaultReactiveRedisDataSource.pubsub(Message.class);
        messages = pub.subscribe("notifications");
        messages.subscribe().with(item -> Log.info("wowowo"));
    }

    private static final Logger LOG = Logger.getLogger(ChatSocket.class);

    Map<String, RedisChatSession> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        RedisChatSession redisSession = new RedisChatSession(defaultReactiveRedisDataSource);
        sessions.put(username, redisSession);
        redisSession.setSession(session);

    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        sessions.remove(username);
        // broadcast("User " + username + " left");
    }

    @OnError
    public void onError(Session session, @PathParam("username") String username, Throwable throwable) {
        sessions.remove(username);
        LOG.error("onError", throwable);
        // broadcast("User " + username + " left on error: " + throwable);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("username") String username) {
        if (message.equalsIgnoreCase("_ready_")) {
            // broadcast("User " + username + " joined");
        } else {

            Message msg = new Message();
            msg.content = message;
            msg.username = username;
            pub.publish("notifications", msg).subscribe().with(item -> Log.info("published"));
            messageService.persistMessage(msg);
        }

    }

    @Incoming("special_command_out")
    public Uni<Void> sendSpecialRequestResponse(org.eclipse.microprofile.reactive.messaging.Message<SpecialRequest> request){
        RedisChatSession chatSession = sessions.get(request.getPayload().getUsername());
        chatSession.getSession().getAsyncRemote().sendObject(">> ChatGPT : " + ": " + request.getPayload().getContent());
        return Uni.createFrom().voidItem();
    }

}