package com.github.xingshuangs.rtsp.starter.model;


import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;

/**
 * websocket的连接器
 *
 * @author xingshuang
 */
@Data
public class WebsocketConnection {

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
     * 订阅的通道名称
     */
    private String subscribeChannelName;
}
