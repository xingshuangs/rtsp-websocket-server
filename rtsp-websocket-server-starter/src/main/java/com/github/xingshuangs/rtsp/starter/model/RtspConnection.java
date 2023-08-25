package com.github.xingshuangs.rtsp.starter.model;


import com.github.xingshuangs.iot.protocol.rtsp.authentication.UsernamePasswordCredential;
import com.github.xingshuangs.iot.protocol.rtsp.service.RtspClient;
import com.github.xingshuangs.iot.protocol.rtsp.service.RtspFMp4Proxy;
import lombok.Data;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * rtsp的连接器
 *
 * @author xingshuang
 */
@Data
public class RtspConnection {

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
    private List<WebsocketConnection> connections = new ArrayList<>();


}
