# RTSP-WEBSOCKET-SERVER

## 简述

- 技术结构 RTSP + H264 + FMP4 + WebSocket + WEB
- 目前支持海康摄像头RTSP视频流在WEB页面上显示，亲测有效
- 视频流获取支持TCP/UDP两种方式，任意切换
- 采用的通信库: https://github.com/xingshuangs/iot-communication
- 通讯库纯JAVA开发，没有任何其他依赖，轻量级

## 注意事项
- iot-communication的rtsp版本库**未发布**，对应git分支rtsp，需要自行拉取源码然后install

## 使用指南

登录访问地址：http://127.0.0.1:8088/client

[![rtsp-websocket.png](https://i.postimg.cc/63gMnY7M/rtsp-websocket.png)](https://i.postimg.cc/63gMnY7M/rtsp-websocket.png)