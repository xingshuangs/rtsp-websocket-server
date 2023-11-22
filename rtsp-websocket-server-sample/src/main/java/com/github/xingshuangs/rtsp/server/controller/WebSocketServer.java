package com.github.xingshuangs.rtsp.server.controller;

import com.github.xingshuangs.iot.protocol.rtsp.authentication.DigestAuthenticator;
import com.github.xingshuangs.iot.protocol.rtsp.authentication.UsernamePasswordCredential;
import com.github.xingshuangs.iot.protocol.rtsp.enums.ERtspTransportProtocol;
import com.github.xingshuangs.iot.protocol.rtsp.service.RtspClient;
import com.github.xingshuangs.iot.protocol.rtsp.service.RtspFMp4Proxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WS代理
 *
 * @author xingshuang
 */
@Slf4j
@Component
@ServerEndpoint("/rtsp")
public class WebSocketServer {

    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
     */
    private static final ConcurrentHashMap<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * RTSP+FMP4的代理服务器
     */
    private RtspFMp4Proxy rtspFMp4Proxy;

    /**
     * 连接建立成功调用的方法
     *
     * @param session 可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen
    public void onOpen(Session session) {
        SESSION_MAP.put(session.getId(), session);
        log.info("有新连接加入！，当前在线人数为{}，sessionId={}", SESSION_MAP.size(), session.getId());
    }

    /**
     * 连接关闭调用的方法
     *
     * @param session 可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnClose
    public void onClose(Session session) {
        //从map中删除
        SESSION_MAP.remove(session.getId());
        this.closeRtspFmp4Proxy();
        log.info("有一连接关闭！，当前在线人数为{}，sessionId={}", SESSION_MAP.size(), session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if (!message.startsWith("rtsp://")) {
            log.error("只支持rtsp://开头的消息，来自客户端的消息:{}，sessionId={}", message, session.getId());
            try {
                session.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            return;
        }
        log.info("来自客户端的消息:{}，sessionId={}", message, session.getId());
        this.openRtspFmp4Proxy(message, session);
    }

    /**
     * 打开FMP4代理
     *
     * @param message RTSP地址
     * @param session session
     */
    private void openRtspFmp4Proxy(String message, Session session) {
        // 关闭之前的代理
        this.closeRtspFmp4Proxy();
        try {
            URI srcUri = URI.create(message);
            URI uri = URI.create("rtsp://" + srcUri.getHost() + ":" + srcUri.getPort() + srcUri.getPath());
            DigestAuthenticator authenticator = null;
            if (srcUri.getUserInfo() != null) {
                UsernamePasswordCredential credential = UsernamePasswordCredential.createBy(srcUri.getUserInfo());
                authenticator = new DigestAuthenticator(credential);
            }
            RtspClient client = new RtspClient(uri, authenticator, ERtspTransportProtocol.TCP);
            this.rtspFMp4Proxy = new RtspFMp4Proxy(client);
            this.rtspFMp4Proxy.onFmp4DataHandle(x -> {
                ByteBuffer wrap = ByteBuffer.wrap(x);
                try {
                    if (session.isOpen()) {
                        session.getBasicRemote().sendBinary(wrap);
                    }
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
            this.rtspFMp4Proxy.onDestroyHandle(() -> this.closeSession(session));
            this.rtspFMp4Proxy.start();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            this.closeRtspFmp4Proxy();
            this.closeSession(session);
        }
    }

    /**
     * 关闭代理
     */
    private void closeRtspFmp4Proxy() {
        if (this.rtspFMp4Proxy != null) {
            this.rtspFMp4Proxy.stop();
            this.rtspFMp4Proxy = null;
        }
    }

    /**
     * 关闭session
     *
     * @param session 会话
     * @return true
     */
    private boolean closeSession(Session session) {
        if (session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return true;
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
}
