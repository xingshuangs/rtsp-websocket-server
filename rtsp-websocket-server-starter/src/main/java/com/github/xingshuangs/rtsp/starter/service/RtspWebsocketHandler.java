/*
 * MIT License
 *
 * Copyright (c) 2021-2099 Oscura (xingshuang) <xingshuang_cool@163.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.xingshuangs.rtsp.starter.service;


import com.github.xingshuangs.rtsp.starter.model.WebsocketConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
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
        log.info("有新连接加入！当前在线人数为[{}]，sessionId[{}]", this.connectionMap.size(), session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebsocketConnection websocketConnection = this.connectionMap.get(session.getId());
        this.rtspManager.handleMessage(websocketConnection, message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        WebsocketConnection websocketConnection = this.connectionMap.get(session.getId());
        this.rtspManager.remove(websocketConnection);
        this.connectionMap.remove(session.getId());
        log.error("发生错误，sessionId[{}]，错误信息={}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebsocketConnection websocketConnection = this.connectionMap.get(session.getId());
        this.rtspManager.remove(websocketConnection);
        this.connectionMap.remove(session.getId());
        log.info("有一连接关闭！当前在线人数为[{}]，sessionId[{}]", this.connectionMap.size(), session.getId());
    }
}
