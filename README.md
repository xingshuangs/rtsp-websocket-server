# RTSP-WEBSOCKET-SERVER

![Language-java8](https://img.shields.io/badge/Language-java8-blue)
![SpringBoot-2.3.4.RELEASE](https://img.shields.io/badge/SpringBoot-2.3.4.RELEASE-yellow)
![Idea-2022.02.03](https://img.shields.io/badge/Idea-2022.02.03-lightgrey)
![CopyRight-Oscura](https://img.shields.io/badge/CopyRight-Oscura-yellow)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE)

## 简述

- 技术结构 **RTSP + H264 + FMP4 + WebSocket + MSE + WEB**
- 目前支持**海康摄像头**RTSP视频流在WEB页面上显示，亲测有效
- 视频流获取支持**TCP/UDP**两种方式，任意切换
- **纯JAVA**开发，没有任何其他依赖，**无插件**，**轻量级**，还可以定制化扩展开发
- 视频响应快速，**延时 < 1s**，几乎**无延时**，**实时性强**，即开即用
- 采用的通信库: https://github.com/xingshuangs/iot-communication

## 整体结构

Camera ==> JAVA Server(Proxy) ==> HTML5 Page.

![structure.png](https://i.postimg.cc/bw5ZJqGP/structure.png)

## 使用指南

1. 启动指令：``java -jar rtsp-websocket-server-1.0-SNAPSHOT.jar``或 IDEA 启动
2. 登录访问地址：http://127.0.0.1:8088
3. 输入正确的摄像头RTSP地址
4. 点击页面上的打开按钮

![rtsp-websocket.png](https://i.postimg.cc/vBZzrGQB/rtsp-websocket.png)