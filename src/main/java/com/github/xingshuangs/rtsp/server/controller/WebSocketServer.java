package com.github.xingshuangs.rtsp.server.controller;

import com.github.xingshuangs.iot.protocol.rtsp.authentication.DigestAuthenticator;
import com.github.xingshuangs.iot.protocol.rtsp.authentication.UsernamePasswordCredential;
import com.github.xingshuangs.iot.protocol.rtsp.enums.ERtspTransportProtocol;
import com.github.xingshuangs.iot.protocol.rtsp.service.RtspFMp4Proxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WS代理
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
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
     */
    private static final ConcurrentHashMap<String, WebSocketServer> WebSocketMap = new ConcurrentHashMap<>();

    private RtspFMp4Proxy rtspFMp4Proxy;

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
        WebSocketMap.put(this.session.getId(), this);
        log.info("有新连接加入！，当前在线人数为{}，sessionId={}", WebSocketMap.size(), this.session.getId());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        //从map中删除
        WebSocketMap.remove(this.session.getId());
        log.info("有一连接关闭！，当前在线人数为{}，sessionId={}", WebSocketMap.size(), this.session.getId());
        if (this.rtspFMp4Proxy != null) {
            this.rtspFMp4Proxy.stop();
            this.rtspFMp4Proxy = null;
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(byte[] message, Session session) {
        String msg = new String(message, StandardCharsets.US_ASCII);
        log.info("来自客户端的消息:{}，sessionId={}", msg, session.getId());
        try {
            ByteBuffer wrap = ByteBuffer.wrap(message);
            session.getBasicRemote().sendBinary(wrap);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("来自客户端的消息:{}，sessionId={}", message, session.getId());
        if ("start".equals(message)) {
            URI uri = URI.create("rtsp://192.168.3.142:554/h264/ch1/main/av_stream");
            UsernamePasswordCredential credential = new UsernamePasswordCredential("admin", "kilox1234");
            DigestAuthenticator authenticator = new DigestAuthenticator(credential);
            this.rtspFMp4Proxy = new RtspFMp4Proxy(uri, authenticator, ERtspTransportProtocol.TCP, false);
            this.rtspFMp4Proxy.onFmp4DataHandle(x -> {
                ByteBuffer wrap = ByteBuffer.wrap(x);
                try {
                    session.getBasicRemote().sendBinary(wrap);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            });
            this.rtspFMp4Proxy.onCodecHandle(x -> {
                try {
                    session.getBasicRemote().sendText(x);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            });
            this.rtspFMp4Proxy.start();
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
}
