/*
 * MIT License
 *
 * Copyright (c) 2021-2099 Oscura (xingshuang) <xingshuang_cool@163.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
