package com.github.xingshuangs.rtsp.starter.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    /**
     * key：视频通道名称
     * value：rtsp连接对象
     */
    private final ConcurrentHashMap<String, RtspConnection> connectionMap = new ConcurrentHashMap<>();

    /**
     * rtsp的地址
     */
    private final List<RtspAddress> rtspAddresses;

    /**
     * json转换对象
     */
    private final ObjectMapper objectMapper;

    public RtspManager(RtspProperties rtspProperties, ObjectMapper objectMapper) {
        this.rtspAddresses = rtspProperties.getAddresses();
        this.objectMapper = objectMapper;
    }

    /**
     * 移除某个websocket连接
     *
     * @param websocketConnection websocket连接
     */
    public void remove(WebsocketConnection websocketConnection) {
        String channelName = this.getSubscribeChannelName(websocketConnection);
        if (channelName == null) {
            return;
        }
        // 存在该websocket连接，先移除该连接，再查看rtsp是否还有订阅连接，若没有则断开rtsp
        RtspConnection rtspConnection = this.connectionMap.get(channelName);
        rtspConnection.removeWebsocketConnection(websocketConnection);
        if (!rtspConnection.hasWebsocketConnection()) {
            rtspConnection.getRtspFMp4Proxy().stop();
            this.connectionMap.remove(channelName);
        }
    }

    /**
     * 处理消息，有订阅和查询公恩那个
     *
     * @param websocketConnection 连接
     * @param message             消息
     */
    public void handleMessage(WebsocketConnection websocketConnection, TextMessage message) {
        try {
            RtspMessage<String> rtspMessage = this.objectMapper.readValue(message.getPayload(), new TypeReference<RtspMessage<String>>() {
            });
            switch (rtspMessage.getType()) {
                case SUBSCRIBE:
                    log.info("订阅视频通道名称：{}", rtspMessage.getContent());
//                    this.handleSubscribe(websocketConnection, rtspMessage);
                    break;
                case QUERY:
                    this.handleQuery(websocketConnection, rtspMessage);
                    break;
                default:
                    this.sendTextMessage(websocketConnection, RtspMessage.createError("无法识别指定消息指令"));
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleSubscribe(WebsocketConnection websocketConnection, RtspMessage<String> rtspMessage) {
        String srcChannelName = rtspMessage.getContent();
        // 更新下订阅视频通道的信息
        websocketConnection.setLastSubscribeTime(LocalDateTime.now());
        websocketConnection.setSubscribeChannelName(srcChannelName);

        // 1. 判定视频通道名称有没有，若没有返回错误消息
        boolean existSubscribeName = this.rtspAddresses.stream().allMatch(x -> x.getName().equals(srcChannelName));
        if (!existSubscribeName) {
            this.sendTextMessage(websocketConnection, RtspMessage.createError("不存在该视频通道名称：" + srcChannelName));
            return;
        }

        // 2. 判定该websocket连接之前是否已经订阅过视频通道名称
        String channelName = this.getSubscribeChannelName(websocketConnection);
        if (channelName != null && channelName.equals(srcChannelName)) {
            // 已经订阅过，且同名，直接返回，无需重复订阅
            this.sendTextMessage(websocketConnection, RtspMessage.createError("已经订阅该通道，无需重复订阅"));
            return;
        } else if (channelName != null) {
            // 已经订阅过，但不同名，关闭之前的视频通道，保证一个客户端同时只能订阅一个通道
            this.remove(websocketConnection);
        }

        // 3. connectionMap有没有该通道名称，有则添加websocket的连接，没有则重新创建新代理连接
        RtspConnection rtspConnection = this.connectionMap.get(srcChannelName);
        if (rtspConnection != null) {
            // 已有rtsp连接，第一步需要立即返回codec，第二步需要接收视频流
            RtspMessage<String> codecMessage = RtspMessage.createSubscribe(rtspConnection.getCodec());
            this.sendTextMessage(websocketConnection, codecMessage);
            rtspConnection.addWebsocketConnection(websocketConnection);
            return;
        }

        // 4. 开始订阅新视频通道
        RtspConnection newRtspConnection = this.openRtspFmp4Proxy(websocketConnection, srcChannelName);
        if (newRtspConnection != null) {
            this.connectionMap.put(srcChannelName, newRtspConnection);
        }
    }

    /**
     * 处理查询消息
     *
     * @param websocketConnection websocket连接
     * @param rtspMessage         rtsp消息
     */
    private void handleQuery(WebsocketConnection websocketConnection, RtspMessage<String> rtspMessage) {
        RtspMessage<?> message;
        if (!rtspMessage.getContent().equals("channel")) {
            message = RtspMessage.createError("查询的content参数目前只能是channel");
        } else {
            List<String> names = this.rtspAddresses.stream().map(RtspAddress::getName).collect(Collectors.toList());
            message = RtspMessage.createQuery(names);
        }
        this.sendTextMessage(websocketConnection, message);
    }

    /**
     * 发送文本消息
     *
     * @param websocketConnection websocket连接
     * @param message             消息对象
     */
    private void sendTextMessage(WebsocketConnection websocketConnection, RtspMessage<?> message) {
        try {
            String res = this.objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(res);
            if (websocketConnection.getSession().isOpen()) {
                websocketConnection.getSession().sendMessage(textMessage);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 发送二进制消息
     *
     * @param websocketConnection websocket连接
     * @param wrap                消息内容，主要是字节内容
     */
    private void sendBinaryMessage(WebsocketConnection websocketConnection, ByteBuffer wrap) {
        try {
            BinaryMessage message = new BinaryMessage(wrap);
            if (websocketConnection.getSession().isOpen()) {
                websocketConnection.getSession().sendMessage(message);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 获取订阅的视频通道名称
     *
     * @param websocketConnection websocket连接
     * @return 视频通道名称
     */
    private String getSubscribeChannelName(WebsocketConnection websocketConnection) {
        for (Map.Entry<String, RtspConnection> entry : connectionMap.entrySet()) {
            if (entry.getValue().containWebsocketConnection(websocketConnection)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 开启rtsp-fmp4的代理
     *
     * @param websocketConnection websocket的连接
     * @param channelName         通道名称
     * @return RtspConnection
     */
    private RtspConnection openRtspFmp4Proxy(WebsocketConnection websocketConnection, String channelName) {
        RtspFMp4Proxy rtspFMp4Proxy = null;
        try {
            Optional<RtspAddress> optRtspAddress = this.rtspAddresses.stream().filter(x -> x.getName().equals(channelName)).findFirst();
            if (!optRtspAddress.isPresent()) {
                return null;
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
            rtspConnection.addWebsocketConnection(websocketConnection);
            // 绑定事件
            rtspFMp4Proxy.onFmp4DataHandle(x -> this.fmp4DataHandle(rtspConnection, x));
            rtspFMp4Proxy.onCodecHandle(x -> this.codecHandle(rtspConnection, x));
            rtspFMp4Proxy.onDestroyHandle(() -> this.destroyHandle(rtspAddress.getName()));
            rtspFMp4Proxy.start();
            return rtspConnection;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (rtspFMp4Proxy != null) {
                rtspFMp4Proxy.stop();
            }
            this.sendTextMessage(websocketConnection, RtspMessage.createError(e.getMessage()));
            return null;
        }
    }

    /**
     * 处理FMP4的数据帧
     *
     * @param rtspConnection rtsp连接
     * @param fmp4Data       FMP4的数据帧
     */
    private void fmp4DataHandle(RtspConnection rtspConnection, byte[] fmp4Data) {
        ByteBuffer wrap = ByteBuffer.wrap(fmp4Data);
        rtspConnection.foreachConnections(x -> this.sendBinaryMessage(x, wrap));
    }

    /**
     * 处理codec
     *
     * @param rtspConnection rtsp连接
     * @param codec          codec编码
     */
    private void codecHandle(RtspConnection rtspConnection, String codec) {
        rtspConnection.setStartTime(LocalDateTime.now());
        rtspConnection.setCodec(codec);
        RtspMessage<String> codecMessage = RtspMessage.createSubscribe(codec);
        rtspConnection.foreachConnections(x -> this.sendTextMessage(x, codecMessage));
    }

    /**
     * rtsp断开后，触发关闭所有websocket通道
     *
     * @param channelName 通道名称
     */
    private void destroyHandle(String channelName) {
        RtspConnection rtspConnection = this.connectionMap.get(channelName);
        rtspConnection.foreachConnections(x -> {
            try {
                if (x.getSession().isOpen()) {
                    x.getSession().close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        });
        rtspConnection.removeAllWebsocketConnection();
        this.connectionMap.remove(channelName);
    }
}
