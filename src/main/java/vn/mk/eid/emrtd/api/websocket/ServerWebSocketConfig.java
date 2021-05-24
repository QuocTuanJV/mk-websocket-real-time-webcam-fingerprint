package vn.mk.eid.emrtd.api.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ServerWebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webFaceSocketHandler(), "/face-websocket").setAllowedOrigins("*");
        registry.addHandler(webFingerSocketHandler(), "/finger-websocket").setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler webFaceSocketHandler() {
        return new ServerFaceWebSocketHandler();
    }

    @Bean
    public WebSocketHandler webFingerSocketHandler() {
        return new ServerFingerWebsocketHandler();
    }
}
