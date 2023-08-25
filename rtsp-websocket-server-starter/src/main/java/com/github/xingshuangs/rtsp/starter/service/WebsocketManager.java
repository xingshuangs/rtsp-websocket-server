package com.github.xingshuangs.rtsp.starter.service;


import com.github.xingshuangs.rtsp.starter.model.WebsocketConnection;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * websocket管理器，管理所有连接信息
 *
 * @author xingshuang
 */
//@Component
public class WebsocketManager {

    private final ConcurrentHashMap<String, WebsocketConnection> connectionMap = new ConcurrentHashMap<>();

    private final RtspManager rtspManager;

    public WebsocketManager(RtspManager rtspManager) {
        this.rtspManager = rtspManager;
    }

    /**
     * 添加会话
     *
     * @param session 会话
     */
    public void addSession(WebSocketSession session) {
        WebsocketConnection websocketConnection = new WebsocketConnection();
        websocketConnection.setSession(session);
        websocketConnection.setLastConnectionTime(LocalDateTime.now());
        this.connectionMap.putIfAbsent(session.getId(), websocketConnection);
    }

    /**
     * 查询会话
     *
     * @param session 会话
     * @return 连接
     */
    public WebsocketConnection querySession(WebSocketSession session) {
        return this.connectionMap.get(session.getId());
    }

    /**
     * 移除会话
     *
     * @param session 会话
     */
    public void removeSession(WebSocketSession session) {
        this.connectionMap.remove(session.getId());
    }

    /**
     * 查询连接数
     *
     * @return 连接数量
     */
    public int getConnectionCount() {
        return this.connectionMap.size();
    }

    /**
     * 处理消息
     *
     * @param session 会话
     * @param message 消息内容
     */
    public void handleMessage(WebSocketSession session, TextMessage message) {

    }
}
