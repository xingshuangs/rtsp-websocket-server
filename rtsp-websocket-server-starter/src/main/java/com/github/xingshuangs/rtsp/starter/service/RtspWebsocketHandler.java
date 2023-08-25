package com.github.xingshuangs.rtsp.starter.service;


import com.github.xingshuangs.rtsp.starter.model.WebsocketConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RTSP的Websocket处理器
 *
 * @author xingshuang
 */
@Slf4j
@Component
public class RtspWebsocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebsocketConnection> connectionMap = new ConcurrentHashMap<>();

    private final RtspManager rtspManager;

    public RtspWebsocketHandler(RtspManager rtspManager) {
        this.rtspManager = rtspManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebsocketConnection websocketConnection = new WebsocketConnection();
        websocketConnection.setSession(session);
        websocketConnection.setLastConnectionTime(LocalDateTime.now());
        this.connectionMap.putIfAbsent(session.getId(), websocketConnection);
        log.info("有新连接加入！，当前在线人数为{}，sessionId={}", this.connectionMap.size(), session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WebsocketConnection websocketConnection = this.connectionMap.get(session.getId());
        this.rtspManager.handleMessage(websocketConnection, message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("发生错误，sessionId={}，错误信息={}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebsocketConnection websocketConnection = this.connectionMap.get(session.getId());
        this.rtspManager.remove(websocketConnection);
        this.connectionMap.remove(session.getId());
        log.info("有一连接关闭！，当前在线人数为{}，sessionId={}", this.connectionMap.size(), session.getId());
    }
}
