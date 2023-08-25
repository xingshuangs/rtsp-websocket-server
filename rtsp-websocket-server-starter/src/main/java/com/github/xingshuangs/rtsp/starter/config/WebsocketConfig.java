package com.github.xingshuangs.rtsp.starter.config;


import com.github.xingshuangs.rtsp.starter.service.RtspWebsocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * websocket的配置
 *
 * @author xingshuang
 */
@Configuration
@EnableWebSocket
public class WebsocketConfig implements WebSocketConfigurer {

    private final RtspWebsocketHandler rtspWebsocketHandler;

    public WebsocketConfig(RtspWebsocketHandler rtspWebsocketHandler) {
        this.rtspWebsocketHandler = rtspWebsocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(rtspWebsocketHandler, "/rtsp");
    }
}
