package com.github.xingshuangs.rtsp.starter.model;


import lombok.Data;

/**
 * RTSP的地址
 *
 * @author xingshuang
 */
@Data
public class RtspAddress {

    /**
     * 视频通道编号
     */
    private Integer number;

    /**
     * 路径
     */
    private String url;
}
