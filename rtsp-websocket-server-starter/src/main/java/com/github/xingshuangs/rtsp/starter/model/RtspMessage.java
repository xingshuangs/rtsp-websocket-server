package com.github.xingshuangs.rtsp.starter.model;


import com.github.xingshuangs.rtsp.starter.enums.ERtspMessageType;
import lombok.Data;

/**
 * 指令消息
 *
 * @author xingshuang
 */
@Data
public class RtspMessage<T> {

    /**
     * 类型，目前分为两种，1、订阅类型，2、查询类型
     */
    private ERtspMessageType type;

    /**
     * 内容
     */
    private T content;

    public static <T> RtspMessage<T> createQuery(T content) {
        RtspMessage<T> resMessage = new RtspMessage<>();
        resMessage.setType(ERtspMessageType.QUERY);
        resMessage.setContent(content);
        return resMessage;
    }

    public static <T> RtspMessage<T> createSubscribe(T content) {
        RtspMessage<T> resMessage = new RtspMessage<>();
        resMessage.setType(ERtspMessageType.SUBSCRIBE);
        resMessage.setContent(content);
        return resMessage;
    }

    public static <T> RtspMessage<T> createError(T content) {
        RtspMessage<T> resMessage = new RtspMessage<>();
        resMessage.setType(ERtspMessageType.ERROR);
        resMessage.setContent(content);
        return resMessage;
    }
}
