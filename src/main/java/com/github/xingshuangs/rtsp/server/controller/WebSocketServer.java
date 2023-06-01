package com.github.xingshuangs.rtsp.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 〈一句话功能简述〉<br>
 *
 * @author xingshuang
 * @ ServerEndpoint 注解是一个类层次的注解，它的功能主要是将目前的类定义成一个websocket服务器端,
 * 注解的值将被用于监听用户连接的终端访问URL地址,客户端可以通过这个URL来连接到WebSocket服务器端
 * topicName:主题的名称，topicParam:主题的参数（设备单元号“1,2,3”或“1”）
 * topic = topicName + topicParam
 */
@Slf4j
@Component
@ServerEndpoint("/websocket")
public class WebSocketServer {

    /**
     * 用于synchronized锁参数
     */
    private final Object lockObj = new Object();

    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的
     */
    private static int onlineCount = 0;

    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
     */
    private static ConcurrentHashMap<String, WebSocketServer> webSocketMap = new ConcurrentHashMap<String, WebSocketServer>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 连接建立成功调用的方法
     *
     * @param session 可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        //加入map中
        webSocketMap.put(this.session.getId(), this);
        addOnlineCount();
        log.info("有新连接加入！，当前在线人数为{}，sessionId={}", getOnlineCount(), this.session.getId());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        //从map中删除
        webSocketMap.remove(this.session.getId());
        subOnlineCount();
        log.info("有一连接关闭！，当前在线人数为{}，sessionId={}", getOnlineCount(), this.session.getId());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(byte[] message, Session session) {
        String msg = new String(message, StandardCharsets.UTF_8);
        log.info("来自客户端的消息:{}，sessionId={}", msg, session.getId());
        try {
            ByteBuffer wrap = ByteBuffer.wrap(message);
            session.getBasicRemote().sendBinary(wrap);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 发生错误时调用
     *
     * @param session session对话
     * @param error   错误消息
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误，sessionId={}，错误信息={}", session.getId(), error.getMessage());
    }

    /**
     * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
     * 同步发送消息的方式：this.session.getBasicRemote().sendText(message);
     * 异步发送消息的方式：this.session.getAsyncRemote().sendText(message);
     *
     * @param message 待发送的消息
     */
    private void sendMessage(String message) {
        synchronized (this.lockObj) {
            try {
                this.session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 获取在线连接数
     *
     * @return 连接个数
     */
    private static synchronized int getOnlineCount() {
        return onlineCount;
    }

    /**
     * 在线数加1
     */
    private static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    /**
     * 在线数减1
     */
    private static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }
}
