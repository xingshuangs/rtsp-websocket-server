package com.github.xingshuangs.rtsp.starter.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xingshuangs.iot.protocol.common.buff.ByteWriteBuff;
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

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
     * key：视频通道编号
     * value：rtsp连接对象
     */
    private final ConcurrentHashMap<Integer, RtspConnection> rtspConnectionMap = new ConcurrentHashMap<>();

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
     * 移除所有视频通道指定的websocket连接
     *
     * @param websocketConnection websocket连接
     */
    public void remove(WebsocketConnection websocketConnection) {
        List<Integer> channelNumbers = this.getChannelNumberByWebsocket(websocketConnection);
        if (channelNumbers.isEmpty()) {
            return;
        }
        // 存在该websocket连接，先移除该连接，再查看rtsp是否还有订阅连接，若没有则断开rtsp
        channelNumbers.forEach(channelNumber -> this.remove(websocketConnection, channelNumber));
    }

    /**
     * 移除指定通道的指定websocket连接
     *
     * @param websocketConnection websocket连接
     * @param channelNumber       通道编号
     */
    public void remove(WebsocketConnection websocketConnection, Integer channelNumber) {
        // 移除websocket连接订阅的通道编号，移除rtsp关联的websocket连接
        websocketConnection.removeChannelNumber(channelNumber);
        // 移除指定通道的websocket连接，先移除该连接，再查看rtsp是否还有订阅连接，若没有则断开rtsp
        RtspConnection rtspConnection = this.rtspConnectionMap.get(channelNumber);
        if (rtspConnection == null) {
            return;
        }
        rtspConnection.removeWebsocketConnection(websocketConnection);
        if (!rtspConnection.hasWebsocketConnection()) {
            rtspConnection.getRtspFMp4Proxy().stop();
            this.rtspConnectionMap.remove(channelNumber);
            log.info("由于视频通道编号[{}]没有websocket订阅，关闭RTSP连接", channelNumber);
        }
    }

    /**
     * 处理消息，有订阅和查询公恩那个
     *
     * @param websocketConnection 连接
     * @param message             消息
     */
    public void handleMessage(WebsocketConnection websocketConnection, TextMessage message) {
        RtspMessage<String> rtspMessage;
        try {
            log.debug("接收数据信息：{}", message.getPayload());
            rtspMessage = this.objectMapper.readValue(message.getPayload(), new TypeReference<RtspMessage<String>>() {
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            this.sendTextMessage(websocketConnection, RtspMessage.createError(-1, "不是标准的视频交互的标准数据格式，json转换错误"));
            return;
        }

        try {
            switch (rtspMessage.getType()) {
                case SUBSCRIBE:
                    this.handleSubscribe(websocketConnection, rtspMessage);
                    break;
                case UNSUBSCRIBE:
                    this.handleUnsubscribe(websocketConnection, rtspMessage);
                    break;
                case QUERY:
                    this.handleQuery(websocketConnection, rtspMessage);
                    break;
                default:
                    this.sendTextMessage(websocketConnection, RtspMessage.createError(rtspMessage.getNumber(), "无法识别指定消息指令"));
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            this.sendTextMessage(websocketConnection, RtspMessage.createError(rtspMessage.getNumber(), e.getMessage()));
        }
    }

    /**
     * 订阅事件处理
     *
     * @param websocketConnection websocket连接
     * @param rtspMessage         rtsp消息
     */
    private void handleSubscribe(WebsocketConnection websocketConnection, RtspMessage<String> rtspMessage) {
        Integer channelNumber = rtspMessage.getNumber();

        // 1. 判定视频通道编号有没有，若没有返回错误消息
        boolean existSubscribeName = this.rtspAddresses.stream().anyMatch(x -> x.getNumber().equals(channelNumber));
        if (!existSubscribeName) {
            log.error("websocket[{}]，没有该通道编号[{}]，无法订阅", websocketConnection.getSession().getId(), channelNumber);
            this.sendTextMessage(websocketConnection, RtspMessage.createError(rtspMessage.getNumber(), "不存在该视频通道编号：" + channelNumber));
            return;
        }

        // 2. 判定该websocket之前是否已经订阅过视频通道编号
        if (websocketConnection.containChannelNumber(channelNumber)) {
            // 已经订阅过，且同名，直接返回，无需重复订阅
            log.info("websocket[{}]，已订阅过该视频通道编号[{}]，无需重复订阅", websocketConnection.getSession().getId(), channelNumber);
            this.sendTextMessage(websocketConnection, RtspMessage.createError(rtspMessage.getNumber(), "已经订阅该通道，无需重复订阅"));
            return;
        }
        // 未订阅过，添加新的通道编号
        websocketConnection.setLastSubscribeTime(LocalDateTime.now());
        websocketConnection.addChannelNumber(channelNumber);

        // 3. 有没有rtsp对应的通道编号，有则添加websocket的连接，没有则重新创建新代理连接
        RtspConnection rtspConnection = this.rtspConnectionMap.get(channelNumber);
        if (rtspConnection != null) {
            // 已有rtsp连接，第一步需要立即返回codec，第二步需要接收视频头，第三步需要接收视频流内容
            log.info("websocket[{}]，已构建了视频通道编号[{}]，直接发送codec和mp4视频头", websocketConnection.getSession().getId(), channelNumber);
            RtspMessage<String> codecMessage = RtspMessage.createSubscribe(rtspMessage.getNumber(), rtspConnection.getCodec());
            this.sendTextMessage(websocketConnection, codecMessage);
            this.sendBinaryMessage(websocketConnection, channelNumber, rtspConnection.getMp4Header());
            rtspConnection.addWebsocketConnection(websocketConnection);
            this.printConnectionChannelInfo();
            return;
        }

        // 4. 开始订阅新视频通道
        log.info("websocket[{}]，订阅视频通道编号[{}]", websocketConnection.getSession().getId(), channelNumber);
        RtspConnection newRtspConnection = this.openRtspFmp4Proxy(websocketConnection, channelNumber);
        if (newRtspConnection != null) {
            this.rtspConnectionMap.put(channelNumber, newRtspConnection);
        } else {
            websocketConnection.removeChannelNumber(channelNumber);
        }
        this.printConnectionChannelInfo();
    }

    /**
     * 取消订阅
     *
     * @param websocketConnection websocket连接
     * @param rtspMessage         rtsp消息
     */
    private void handleUnsubscribe(WebsocketConnection websocketConnection, RtspMessage<String> rtspMessage) {
        log.info("websocket[{}]，取消订阅视频通道编号[{}]", websocketConnection.getSession().getId(), rtspMessage.getNumber());
        this.remove(websocketConnection, rtspMessage.getNumber());
        this.printConnectionChannelInfo();
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
            message = RtspMessage.createError(rtspMessage.getNumber(), "查询的content参数目前只能是channel");
        } else {
            List<Integer> names = this.rtspAddresses.stream().map(RtspAddress::getNumber).collect(Collectors.toList());
            message = RtspMessage.createQuery(rtspMessage.getNumber(), names);
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
            websocketConnection.sendMessage(textMessage);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 发送二进制消息
     *
     * @param websocketConnection websocket连接
     * @param channelNumber       通道编号
     * @param fmp4Data            消息内容，主要是字节内容
     */
    private void sendBinaryMessage(WebsocketConnection websocketConnection, Integer channelNumber, byte[] fmp4Data) {
        try {
            // 前4个字节是视频通道编号，后面的是视频数据
            byte[] buff = ByteWriteBuff.newInstance(4 + fmp4Data.length)
                    .putInteger(channelNumber)
                    .putBytes(fmp4Data)
                    .getData();
            ByteBuffer wrap = ByteBuffer.wrap(buff);
            BinaryMessage message = new BinaryMessage(wrap);
            websocketConnection.sendMessage(message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 获取订阅的视频通道编号
     *
     * @param websocketConnection websocket连接
     * @return 视频通道编号
     */
    private List<Integer> getChannelNumberByWebsocket(WebsocketConnection websocketConnection) {
        List<Integer> res = new ArrayList<>();
        for (Map.Entry<Integer, RtspConnection> entry : rtspConnectionMap.entrySet()) {
            if (entry.getValue().containWebsocketConnection(websocketConnection)) {
                res.add(entry.getKey());
            }
        }
        return res;
    }

    /**
     * 开启rtsp-fmp4的代理
     *
     * @param websocketConnection websocket的连接
     * @param channelNumber       通道编号
     * @return RtspConnection
     */
    private RtspConnection openRtspFmp4Proxy(WebsocketConnection websocketConnection, Integer channelNumber) {
        RtspFMp4Proxy rtspFMp4Proxy = null;
        try {
            Optional<RtspAddress> optRtspAddress = this.rtspAddresses.stream().filter(x -> x.getNumber().equals(channelNumber)).findFirst();
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
            rtspFMp4Proxy.onFmp4DataHandle(x -> this.fmp4DataHandle(rtspConnection, channelNumber, x));
            rtspFMp4Proxy.onCodecHandle(x -> this.codecHandle(rtspConnection, channelNumber, x));
            rtspFMp4Proxy.onDestroyHandle(() -> this.destroyHandle(channelNumber));
            rtspFMp4Proxy.start();
            return rtspConnection;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (rtspFMp4Proxy != null) {
                rtspFMp4Proxy.stop();
            }
            this.sendTextMessage(websocketConnection, RtspMessage.createError(channelNumber, e.getMessage()));
            return null;
        }
    }

    /**
     * 处理FMP4的数据帧
     *
     * @param rtspConnection rtsp连接
     * @param channelNumber  通道编号
     * @param fmp4Data       FMP4的数据帧
     */
    private void fmp4DataHandle(RtspConnection rtspConnection, Integer channelNumber, byte[] fmp4Data) {
        rtspConnection.foreachConnections(x -> {
            if (x.containChannelNumber(channelNumber)) {
                this.sendBinaryMessage(x, channelNumber, fmp4Data);
            }
        });
    }

    /**
     * 处理codec
     *
     * @param rtspConnection rtsp连接
     * @param channelNumber  通道编号
     * @param codec          codec编码
     */
    private void codecHandle(RtspConnection rtspConnection, Integer channelNumber, String codec) {
        rtspConnection.setStartTime(LocalDateTime.now());
        rtspConnection.foreachConnections(x -> {
            if (x.containChannelNumber(channelNumber)) {
                RtspMessage<String> codecMessage = RtspMessage.createSubscribe(channelNumber, codec);
                this.sendTextMessage(x, codecMessage);
                log.debug("给通道[{}]发送codec[{}]", channelNumber, codec);
            }
        });
    }

    /**
     * rtsp断开后，触发关闭所有websocket通道
     *
     * @param channelNumber 通道编号
     */
    private void destroyHandle(Integer channelNumber) {
        RtspConnection rtspConnection = this.rtspConnectionMap.get(channelNumber);
        if (rtspConnection == null) {
            return;
        }
        rtspConnection.foreachConnections(x -> {
            if (x.containChannelNumber(channelNumber)) {
                RtspMessage<String> codecMessage = RtspMessage.createError(channelNumber, "RTSP连接断开");
                this.sendTextMessage(x, codecMessage);
            }
        });
        // 移除websocket关联的视频通道，移除rtsp关联的websocket
        rtspConnection.foreachConnections(WebsocketConnection::removeAllChannelNumbers);
        rtspConnection.removeAllWebsocketConnection();
        this.rtspConnectionMap.remove(channelNumber);
    }

    /**
     * 打印当前通道信息
     */
    private void printConnectionChannelInfo() {
        log.info(String.format("当前构建的视频通道数量[%d]", this.rtspConnectionMap.size()));

        for (Map.Entry<Integer, RtspConnection> entry : rtspConnectionMap.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("视频通道编号[%s]被订阅的Websocket数量[%d]", entry.getKey(), entry.getValue().getWebsocketConnectionCount()));
            entry.getValue().foreachConnections(x -> sb.append(String.format("，Websocket[%s]订阅了通道编号[%s]", x.getSession().getId(), x.getAllChannelNumbers())));
            log.info(sb.toString());
        }
    }
}
