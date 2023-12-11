# RTSP-WEBSOCKET-SERVER

![Language-java8](https://img.shields.io/badge/Language-java8-blue)
![SpringBoot-2.3.4.RELEASE](https://img.shields.io/badge/SpringBoot-2.3.4.RELEASE-yellow)
![Idea-2022.02.03](https://img.shields.io/badge/Idea-2022.02.03-lightgrey)
![CopyRight-Oscura](https://img.shields.io/badge/CopyRight-Oscura-yellow)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE)

## 简述

- 技术结构 **RTSP + H264 + FMP4 + WebSocket + MSE + WEB**
- 目前支持**海康、大华摄像头**RTSP视频流在WEB页面上显示，亲测有效
- 视频流获取支持**TCP/UDP**两种方式，任意切换
- **纯JAVA**开发，没有任何其他依赖，**无插件**，**轻量级**，还可以定制化扩展开发
- 视频响应快速，**延时 < 1s**，几乎**无延时**，**实时性强**，即开即用
- 采用的通信库: https://github.com/xingshuangs/iot-communication

## 整体结构

Camera ==> JAVA Server(Proxy) ==> HTML5 Page.

![structure.png](https://i.postimg.cc/bw5ZJqGP/structure.png)

## 使用指南

### rtsp-websocket-server-sample(rtsp地址模式)

1. jar包启动 或 IDEA启动
2. 登录访问地址：http://127.0.0.1:8088
3. 输入正确的摄像头RTSP地址
4. 点击页面上的打开按钮

![rtsp-websocket.png](https://i.postimg.cc/vBZzrGQB/rtsp-websocket.png)

### rtsp-websocket-server-starter(订阅模式)

先在配置文件中配置RTSP的访问地址

```text
rtsp:
  addresses:
    - number: 1001
      url: rtsp://admin:123456@192.168.3.251:554/h264/ch1/main/av_stream
    - number: 1002
      url: rtsp://admin:123456@192.168.3.250:554/h264/ch1/main/av_stream
```

1. jar包启动 或 IDEA启动
2. 登录访问地址：http://127.0.0.1:8089
3. 点击websocket的连接
4. 选择对应的视频通道，点击订阅

![rtsp-websocket-starter.png](https://i.postimg.cc/Yqk1SF4v/rtsp-websocket-starter.jpg)

## 联系方式

如果有任何问题，可以通过以下方式联系作者，作者在空余时间会做解答。

- QQ群：**759101350**
- 邮件：**xingshuang_cool@163.com**

## 许可证

根据MIT许可证发布，更多信息请参见[`LICENSE`](./LICENSE)。<br>
@2019 - 2099 Oscura版权所有。

## 赞助

一杯奶茶足矣<br>
**微信** (请备注上你的姓名)<br>
![微信](https://i.postimg.cc/brBG5vx8/image.png)