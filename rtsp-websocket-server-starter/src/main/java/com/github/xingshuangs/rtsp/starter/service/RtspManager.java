package com.github.xingshuangs.rtsp.starter.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xingshuangs.iot.exceptions.RtspCommException;
import com.github.xingshuangs.iot.protocol.rtsp.authentication.DigestAuthenticator;
import com.github.xingshuangs.iot.protocol.rtsp.authentication.UsernamePasswordCredential;
import com.github.xingshuangs.iot.protocol.rtsp.enums.ERtspTransportProtocol;
import com.github.xingshuangs.iot.protocol.rtsp.service.RtspClient;
import com.github.xingshuangs.iot.protocol.rtsp.service.RtspFMp4Proxy;
import com.github.xingshuangs.rtsp.starter.model.RtspAddress;
import com.github.xingshuangs.rtsp.starter.model.RtspConnection;
import com.github.xingshuangs.rtsp.starter.model.RtspMessage;
import com.github.xingshuangs.rtsp.starter.model.WebsocketConnection;
import com.github.xingshuangs.rtsp.starter.properties.RtspProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * RTSP的管理器
 *
 * @author xingshuang
 */
@Slf4j
@Component
public class RtspManager {

    private final Object objLock = new Object();

    /**
     * key：视频通道名称
     * value：rtsp连接对象
     */
    private final ConcurrentHashMap<String, RtspConnection> connectionMap = new ConcurrentHashMap<>();

    /**
     * rtsp的地址
     */
    private final List<RtspAddress> rtspAddresses;

    private final ObjectMapper objectMapper;

    public RtspManager(RtspProperties rtspProperties, ObjectMapper objectMapper) {
        this.rtspAddresses = rtspProperties.getAddresses();
        this.objectMapper = objectMapper;
    }

    public void handleMessage(WebsocketConnection connection, TextMessage message) {
        // 是订阅还是查询
        try {
            RtspMessage<String> rtspMessage = this.objectMapper.readValue(message.getPayload(), new TypeReference<RtspMessage<String>>() {
            });
            switch (rtspMessage.getType()) {
                case SUBSCRIBE:
                    this.handleSubscribe(connection, rtspMessage);
                    break;
                case QUERY:
                    this.handleQuery(connection, rtspMessage);
                    break;
                default:
                    throw new RtspCommException("无法识别指定消息指令");

            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleSubscribe(WebsocketConnection websocketConnection, RtspMessage<String> rtspMessage) throws IOException {
        // 更新下订阅视频通道的信息
        websocketConnection.setLastSubscribeTime(LocalDateTime.now());
        websocketConnection.setSubscribeChannelName(rtspMessage.getContent());

        // 1. 判定视频通道名称有没有，若没有返回错误消息
        boolean existSubscribeName = this.rtspAddresses.stream().allMatch(x -> x.getName().equals(rtspMessage.getContent()));
        if (!existSubscribeName) {
            RtspMessage<?> message = RtspMessage.createError("不存在该视频通道");
            this.sendTextMessage(message, websocketConnection);
            return;
        }

        // 2. 判定视频通道名称之前是否已经订阅过
        String channelName = this.getSubscribeChannelName(websocketConnection);
        if (channelName != null && channelName.equals(rtspMessage.getContent())) {
            // 已经订阅过，且同名，直接返回，无需重复订阅
            RtspMessage<?> message = RtspMessage.createError("已经订阅该通道，无需重复订阅");
            this.sendTextMessage(message, websocketConnection);
            return;
        } else if (channelName != null) {
            // 已经订阅过，但不同名，关闭之前的视频通道，保证一个客户端同时只能订阅一个通道
            this.remove(websocketConnection);
        }

        // 3. 开始订阅视频通道，connectionMap有没有该通道名称，有则添加websocket的连接，没有则重新创建新代理连接
        RtspConnection rtspConnection = this.connectionMap.get(rtspMessage.getContent());
        if (rtspConnection != null) {
            // TODO:需要返回codec
            rtspConnection.getConnections().add(websocketConnection);
            return;
        }

        this.openRtspFmp4Proxy(websocketConnection, rtspMessage);
    }

    private void sendTextMessage(RtspMessage<?> message, WebsocketConnection connection) throws IOException {
        String res = this.objectMapper.writeValueAsString(message);
        TextMessage textMessage = new TextMessage(res);
        connection.getSession().sendMessage(textMessage);
    }

    private void handleQuery(WebsocketConnection connection, RtspMessage<String> rtspMessage) throws IOException {
        RtspMessage<?> message;
        if (!rtspMessage.getContent().equals("channel")) {
            message = RtspMessage.createError("查询的content参数目前只能是channel");
        } else {
            List<String> names = this.rtspAddresses.stream().map(RtspAddress::getName).collect(Collectors.toList());
            message = RtspMessage.createQuery(names);
        }
        this.sendTextMessage(message, connection);
    }

    /**
     * 获取订阅的视频通道名称
     *
     * @param connection session对象
     * @return 视频通道名称
     */
    private String getSubscribeChannelName(WebsocketConnection connection) {
        for (Map.Entry<String, RtspConnection> entry : connectionMap.entrySet()) {
            for (WebsocketConnection websocketConnection : entry.getValue().getConnections()) {
                if (websocketConnection.getSession().getId().equals(connection.getSession().getId())) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public void remove(WebsocketConnection websocketConnection) {
        String name = this.getSubscribeChannelName(websocketConnection);
        if (name == null) {
            return;
        }
        RtspConnection connection = this.connectionMap.get(name);
        connection.getConnections().remove(websocketConnection);
    }

    private void openRtspFmp4Proxy(WebsocketConnection websocketConnection, RtspMessage<String> rtspMessage) {
        RtspFMp4Proxy rtspFMp4Proxy = null;
        try {
            Optional<RtspAddress> optRtspAddress = this.rtspAddresses.stream().filter(x -> x.getName().equals(rtspMessage.getContent())).findFirst();
            if (!optRtspAddress.isPresent()) {
                return;
            }
            RtspAddress rtspAddress = optRtspAddress.get();
            RtspConnection rtspConnection = new RtspConnection();

            URI srcUri = URI.create(rtspAddress.getUrl());
            URI uri = URI.create("rtsp://" + srcUri.getHost() + ":" + srcUri.getPort() + srcUri.getPath());
            DigestAuthenticator authenticator = null;
            UsernamePasswordCredential credential = null;
            if (srcUri.getUserInfo() != null) {
                credential = UsernamePasswordCredential.createBy(srcUri.getUserInfo());
                authenticator = new DigestAuthenticator(credential);
            }
            RtspClient client = new RtspClient(uri, authenticator, ERtspTransportProtocol.UDP);
            rtspFMp4Proxy = new RtspFMp4Proxy(client, true);
            // 初始统一更新rtsp连接信息
            rtspConnection.setRawUri(srcUri);
            rtspConnection.setRipUri(uri);
            rtspConnection.setCredential(credential);
            rtspConnection.setClient(client);
            rtspConnection.setRtspFMp4Proxy(rtspFMp4Proxy);
            rtspConnection.getConnections().add(websocketConnection);
            // 绑定事件
            rtspFMp4Proxy.onFmp4DataHandle(x -> this.fmp4DataHandle(rtspConnection, x));
            rtspFMp4Proxy.onCodecHandle(x -> this.codecHandle(rtspConnection, x));
            rtspFMp4Proxy.onDestroyHandle(() -> this.connectionMap.remove(rtspAddress.getName()));
            rtspFMp4Proxy.start();
            this.connectionMap.put(rtspAddress.getName(), rtspConnection);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (rtspFMp4Proxy != null) {
                rtspFMp4Proxy.stop();
            }
        }
    }

    private void fmp4DataHandle(RtspConnection rtspConnection, byte[] fmp4Data) {
        ByteBuffer wrap = ByteBuffer.wrap(fmp4Data);
        try {
            synchronized (this.objLock) {
                for (WebsocketConnection connection : rtspConnection.getConnections()) {
                    BinaryMessage message = new BinaryMessage(wrap);
                    connection.getSession().sendMessage(message);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void codecHandle(RtspConnection rtspConnection, String codec) {
        try {
            rtspConnection.setStartTime(LocalDateTime.now());
            rtspConnection.setCodec(codec);
            RtspMessage<String> codecMessage = RtspMessage.createSubscribe(codec);
            synchronized (this.objLock) {
                for (WebsocketConnection connection : rtspConnection.getConnections()) {
                    this.sendTextMessage(codecMessage, connection);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
