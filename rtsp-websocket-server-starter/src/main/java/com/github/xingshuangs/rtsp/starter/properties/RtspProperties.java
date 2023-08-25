package com.github.xingshuangs.rtsp.starter.properties;


import com.github.xingshuangs.rtsp.starter.model.RtspAddress;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * RTSP的配置
 *
 * @author xingshuang
 */

@Data
@ConfigurationProperties(RtspProperties.PREFIX)
public class RtspProperties {

    public static final String PREFIX = "rtsp";

    /**
     * RTSP的地址列表
     */
    private List<RtspAddress> addresses;
}
