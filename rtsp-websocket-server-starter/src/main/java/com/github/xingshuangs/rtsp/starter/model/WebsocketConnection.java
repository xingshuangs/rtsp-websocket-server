package com.github.xingshuangs.rtsp.starter.model;

import lombok.Data;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

/**
 * websocket的连接器
 *
 * @author xingshuang
 */
@Data
public class WebsocketConnection {

    private final Object objLock = new Object();

    /**
     * session对象
     */
    private WebSocketSession session;

    /**
     * 初次连入的时间
     */
    private LocalDateTime lastConnectionTime;

    /**
     * 最新订阅触发时间
     */
    private LocalDateTime lastSubscribeTime;

    /**
     * 订阅的视频通道编号
     */
    private final Set<Integer> subscribeChannelNumbers = new HashSet<>();

    /**
     * 获取所有视频通道编号的字符串
     *
     * @return 通道编号字符串
     */
    public String getAllChannelNumbers() {
        synchronized (this.objLock) {
            return StringUtils.join(this.subscribeChannelNumbers.stream().map(String::valueOf).collect(Collectors.toList()), ",");
        }
    }

    /**
     * 添加视频通道编号
     *
     * @param number 视频通道编号
     */
    public void addChannelNumber(Integer number) {
        synchronized (this.objLock) {
            this.subscribeChannelNumbers.add(number);
        }
    }

    /**
     * 移除视频通道编号
     *
     * @param number 视频通道编号
     */
    public void removeChannelNumber(Integer number) {
        synchronized (this.objLock) {
            this.subscribeChannelNumbers.remove(number);
        }
    }

    /**
     * 移除所有视频通道编号
     */
    public void removeAllChannelNumbers() {
        synchronized (this.objLock) {
            this.subscribeChannelNumbers.clear();
        }
    }

    /**
     * 是否包含视频通道编号
     *
     * @param number 视频通道编号
     * @return true:包含，false：不包含
     */
    public boolean containChannelNumber(Integer number) {
        synchronized (this.objLock) {
            return this.subscribeChannelNumbers.contains(number);
        }
    }

    /**
     * 是否有视频通道编号
     *
     * @return true：有，false：没有
     */
    public boolean hasChannelNumber() {
        synchronized (this.objLock) {
            return !this.subscribeChannelNumbers.isEmpty();
        }
    }

    /**
     * 遍历所有视频通道编号
     *
     * @param consumer 对应的事件
     */
    public void foreachChannelNumbers(IntConsumer consumer) {
        synchronized (this.objLock) {
            for (Integer number : this.subscribeChannelNumbers) {
                consumer.accept(number);
            }
        }
    }

    /**
     * 发送消息
     *
     * @param message 消息数据
     * @throws IOException 异常
     */
    public synchronized void sendMessage(WebSocketMessage<?> message) throws IOException {
        if (this.session.isOpen()) {
            this.session.sendMessage(message);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebsocketConnection that = (WebsocketConnection) o;
        return session.getId().equals(that.session.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(session.getId());
    }
}
