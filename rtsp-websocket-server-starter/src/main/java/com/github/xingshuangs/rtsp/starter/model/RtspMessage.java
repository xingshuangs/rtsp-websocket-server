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
     * 视频通道编号，4字节
     */
    private Integer number;

    /**
     * 内容
     */
    private T content;

    /**
     * 创建查询消息
     *
     * @param number  视频通道编号
     * @param content 内容
     * @param <T>     类型
     * @return RtspMessage
     */
    public static <T> RtspMessage<T> createQuery(Integer number, T content) {
        RtspMessage<T> resMessage = new RtspMessage<>();
        resMessage.setType(ERtspMessageType.QUERY);
        resMessage.setNumber(number);
        resMessage.setContent(content);
        return resMessage;
    }

    /**
     * 创建订阅消息
     *
     * @param number  视频通道编号
     * @param content 内容
     * @param <T>     类型
     * @return RtspMessage
     */
    public static <T> RtspMessage<T> createSubscribe(Integer number, T content) {
        RtspMessage<T> resMessage = new RtspMessage<>();
        resMessage.setType(ERtspMessageType.SUBSCRIBE);
        resMessage.setNumber(number);
        resMessage.setContent(content);
        return resMessage;
    }

    /**
     * 创建错误消息
     *
     * @param number  视频通道编号
     * @param content 内容
     * @param <T>     类型
     * @return RtspMessage
     */
    public static <T> RtspMessage<T> createError(Integer number, T content) {
        RtspMessage<T> resMessage = new RtspMessage<>();
        resMessage.setType(ERtspMessageType.ERROR);
        resMessage.setNumber(number);
        resMessage.setContent(content);
        return resMessage;
    }
}
