package com.github.xingshuangs.rtsp.starter.model;


import com.github.xingshuangs.iot.protocol.rtsp.authentication.UsernamePasswordCredential;
import com.github.xingshuangs.iot.protocol.rtsp.service.RtspClient;
import com.github.xingshuangs.iot.protocol.rtsp.service.RtspFMp4Proxy;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 对应的session连接
     */
    private final Set<WebsocketConnection> connections = new HashSet<>();

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

    public byte[] getMp4Header() {
        return this.rtspFMp4Proxy.getMp4Header().toByteArray();
    }

    public String getCodec() {
        return this.rtspFMp4Proxy.getMp4TrackInfo().getCodec();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * 是否有websocket连接
     *
     * @return true：有，false：没有
     */
    public boolean hasWebsocketConnection() {
        synchronized (this.objLock) {
            return !this.connections.isEmpty();
        }
    }

    /**
     * 添加websocket连接
     *
     * @param connection websocket连接
     */
    public void addWebsocketConnection(WebsocketConnection connection) {
        synchronized (this.objLock) {
            this.connections.add(connection);
        }
    }

    /**
     * 移除websocket连接
     *
     * @param connection websocket连接
     */
    public void removeWebsocketConnection(WebsocketConnection connection) {
        synchronized (this.objLock) {
            this.connections.remove(connection);
        }
    }

    /**
     * 是否包含指定websocket连接
     *
     * @param connection websocket连接
     * @return true：包含，false：不包含
     */
    public boolean containWebsocketConnection(WebsocketConnection connection) {
        synchronized (this.objLock) {
            return this.connections.stream().anyMatch(x -> x.getSession().getId().equals(connection.getSession().getId()));
        }
    }

    /**
     * 遍历连接执行动作
     *
     * @param consumer 动作
     */
    public void foreachConnections(Consumer<WebsocketConnection> consumer) {
        synchronized (this.objLock) {
            for (WebsocketConnection connection : this.connections) {
                consumer.accept(connection);
            }
        }
    }

    /**
     * 移除所有的websocket连接
     */
    public void removeAllWebsocketConnection() {
        synchronized (this.objLock) {
            this.connections.clear();
        }
    }

    /**
     * 获取websocket连接数量
     *
     * @return 数量
     */
    public int getWebsocketConnectionCount() {
        synchronized (this.objLock) {
            return this.connections.size();
        }
    }
}
