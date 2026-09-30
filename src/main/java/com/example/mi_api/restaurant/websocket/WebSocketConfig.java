package com.example.mi_api.restaurant.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderUpdatesWebSocketHandler orderUpdatesHandler;

    public WebSocketConfig(OrderUpdatesWebSocketHandler orderUpdatesHandler) {
        this.orderUpdatesHandler = orderUpdatesHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderUpdatesHandler, "/ws/orders")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:4173",
                        "https://ghelbert.github.io");
    }
}