package tech.justjava.process_manager.chat;

import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    @Getter
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public OnlineEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        String userId = getUserId(event);
        if (userId != null) {
            onlineUsers.add(userId);
            System.out.println("User connected to Web Socket: " + getUserId(event));
            messagingTemplate.convertAndSend("/topic/online", getStatusPayload(userId, true));
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String userId = getUserId(event);
        if (userId != null) {
            onlineUsers.remove(userId);
            System.out.println("User disconnected from Web Socket: " + getUserFullName(event));
            messagingTemplate.convertAndSend("/topic/online", getStatusPayload(userId, false));
        }
    }

    private String getUserId(AbstractSubProtocolEvent event) {
        return Optional.ofNullable(event.getUser()) //This gets the userId...same as "auth.get("sub")"
                .map(Principal::getName).orElse(null);
    }

    private String getUserFullName(AbstractSubProtocolEvent event) {
        OAuth2AuthenticationToken auth = (OAuth2AuthenticationToken) event.getUser();
        return auth.getPrincipal().getAttributes().get("name").toString();
    }
    private String getUserEmail(AbstractSubProtocolEvent event) {
        OAuth2AuthenticationToken auth = (OAuth2AuthenticationToken) event.getUser();
        return auth.getPrincipal().getAttributes().get("email").toString();
    }

    private Map<String, Object> getStatusPayload(String username, boolean online) {
        return Map.of("userId", username, "online", online);
    }

}
