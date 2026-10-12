package com.dfw.config;

import com.dfw.interfaces.GameWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

/** 与 HTTP 共用端口的原生 WebSocket 配置。 */
@Configuration
@EnableWebSocket
public class WebConfig implements WebSocketConfigurer {
    private final GameWebSocketHandler handler;
    private final String origin;
    /** 输入联机 Handler 与公开访问域名，配置同源限制。 */
    public WebConfig(GameWebSocketHandler handler, @Value("${game.app-origin}") String origin) { this.handler = handler; this.origin = origin; }
    /** 输入路由注册器，绑定 /ws；未配置公开域名时仅允许同源。 */
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        var registration = registry.addHandler(handler, "/ws");
        if (!origin.isBlank()) registration.setAllowedOrigins(origin);
    }
}
