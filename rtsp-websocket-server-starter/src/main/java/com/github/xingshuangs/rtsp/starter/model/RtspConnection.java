package com.github.xingshuangs.rtsp.starter.model;


import com.github.xingshuangs.iot.protocol.rtsp.authentication.UsernamePasswordCredential;
import com.github.xingshuangs.iot.protocol.rtsp.service.RtspClient;
import com.github.xingshuangs.iot.protocol.rtsp.service.RtspFMp4Proxy;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * rtsp的连接器
 *
 * @author xingshuang
 */
public class RtspConnection {

    private final Object objLock = new Object();

    /**
     * 最完整的RTSP连接地址
     */
    private URI rawUri;

    /**
     * 处理过的RTSP连接地址，没有账号和密码
     */
    private URI ripUri;

    /**
     * 用户名和密码的凭证，可能没有
     */
    private UsernamePasswordCredential credential;

    /**
     * rtsp连接的客户端
     */
    private RtspClient client;

    /**
     * rtsp-fmp4转换的代理器
     */
    private RtspFMp4Proxy rtspFMp4Proxy;

    /**
     * 视频编码格式
     */
    private String codec;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 对应的session连接
     */
    private final List<WebsocketConnection> connections = new ArrayList<>();

    public URI getRawUri() {
        return rawUri;
    }

    public void setRawUri(URI rawUri) {
        this.rawUri = rawUri;
    }

    public URI getRipUri() {
        return ripUri;
    }

    public void setRipUri(URI ripUri) {
        this.ripUri = ripUri;
    }

    public UsernamePasswordCredential getCredential() {
        return credential;
    }

    public void setCredential(UsernamePasswordCredential credential) {
        this.credential = credential;
    }

    public RtspClient getClient() {
        return client;
    }

    public void setClient(RtspClient client) {
        this.client = client;
    }

    public RtspFMp4Proxy getRtspFMp4Proxy() {
        return rtspFMp4Proxy;
    }

    public void setRtspFMp4Proxy(RtspFMp4Proxy rtspFMp4Proxy) {
        this.rtspFMp4Proxy = rtspFMp4Proxy;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public boolean hasWebsocketConnection() {
        synchronized (this.objLock) {
            return !this.connections.isEmpty();
        }
    }

    public void addWebsocketConnection(WebsocketConnection connection) {
        synchronized (this.objLock) {
            this.connections.add(connection);
        }
    }

    public void removeWebsocketConnection(WebsocketConnection connection) {
        synchronized (this.objLock) {
            this.connections.remove(connection);
        }
    }

    public boolean containWebsocketConnection(WebsocketConnection connection) {
        synchronized (this.objLock) {
            return this.connections.stream().anyMatch(x -> x.getSession().getId().equals(connection.getSession().getId()));
        }
    }

    public void foreachConnections(Consumer<WebsocketConnection> consumer) {
        synchronized (this.objLock) {
            for (WebsocketConnection connection : this.connections) {
                consumer.accept(connection);
            }
        }
    }

    public void removeAllWebsocketConnection() {
        synchronized (this.objLock) {
            this.connections.clear();
        }
    }
}
