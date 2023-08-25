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
     * 名称
     */
    private String name;

    /**
     * 路径
     */
    private String url;
}
