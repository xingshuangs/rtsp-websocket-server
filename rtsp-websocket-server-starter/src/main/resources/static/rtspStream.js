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

class RtspStream {

    constructor(wsUrl) {
        this.wsUrl = wsUrl;
        this.channelMap = new Map();
    }

    send(data) {
        this.websocket.send(data);
    }

    /**
     * 触发订阅
     * @param number 通道编号
     * @param videoId video标签Id
     */
    subscribe(number, videoId) {
        console.debug(`subscribe channel[${number}]，videoId[${videoId}]`)
        // 构建订阅参数，json的形式交互
        const params = {};
        params.type = "SUBSCRIBE";
        params.number = number;
        params.content = "";
        const channelMedia = new ChannelMedia(number, videoId);
        channelMedia.onReset = (number, videoId) => this.resubscribe(number, videoId);
        this.channelMap.set(number, channelMedia)
        this.websocket.send(JSON.stringify(params))
    }

    /**
     * 重新订阅
     * @param number 视频通道编号
     * @param videoId video标签Id
     */
    resubscribe(number, videoId) {
        console.debug(`resubscribe channel[${number}]，videoId[${videoId}]`)
        this.unsubscribe(number);
        this.subscribe(number, videoId);
    }

    /**
     * 取消订阅
     * @param number 视频通道编号
     */
    unsubscribe(number) {
        console.debug(`取消订阅通道[${number}]`)
        // 构建订阅参数，json的形式交互
        const params = {};
        params.type = "UNSUBSCRIBE";
        params.number = number;
        params.content = "";
        this.websocket.send(JSON.stringify(params))
        this.channelMap.delete(number)
    }

    /**
     * 打开事件
     * @param evt 数据
     */
    onopen(evt) {
        console.log("ws连接成功", this.wsUrl)
    }

    /**
     * 关闭事件
     * @param evt 数据
     */
    onClose(evt) {
        console.log("ws连接关闭", this.wsUrl)
    }

    /**
     * 接收消息事件
     * @param evt 数据
     */
    onMessage(evt) {
        if (typeof (evt.data) == "string") {
            let data = JSON.parse(evt.data);
            if (data.type === "SUBSCRIBE") this.channelMap.get(data.number).init(data.content);
            else if (data.type === "QUERY") console.log(`channel[${data.number}]: ${data.content}`);
            else if (data.type === "ERROR") console.error(`channel[${data.number}]: ${data.content}`);
            else console.log(data.content);
        } else {
            const data = new Uint8Array(evt.data);
            // 解析通道编号
            const numberSrc = data.slice(0, 4);
            const view = new DataView(numberSrc.buffer);
            const number = view.getUint32(0);
            // 向指定通道编号添加数据
            const videoData = data.slice(4);
            // console.log("通道编号：", number, videoData.length)
            this.channelMap.get(number).pushData(videoData);
        }
    }

    /**
     * 错误
     * @param evt 数据
     */
    onError(evt) {
        console.log("ws连接错误")
    }

    /**
     * 打开
     */
    open() {
        this.close();

        this.websocket = new WebSocket(this.wsUrl);
        this.websocket.binaryType = "arraybuffer";
        this.websocket.onopen = this.onopen.bind(this);
        this.websocket.onmessage = this.onMessage.bind(this);
        this.websocket.onclose = this.onClose.bind(this);
        this.websocket.onerror = this.onError.bind(this);
    }

    /**
     * 关闭
     */
    close() {
        if (this.websocket) this.websocket.close();
    }
}

class ChannelMedia {

    constructor(number, videoId) {
        this.number = number;
        this.videoId = videoId;
        this.queue = [];
        this.canFeed = false;
        this.onReset = null;
    }

    /**
     * 初始化
     * @param codecStr 视频编码
     */
    init(codecStr) {
        this.codec = 'video/mp4; codecs=\"' + codecStr + '\"';
        console.log(`channel[${this.number}] call play [${this.codec}]`);
        if (MediaSource.isTypeSupported(this.codec)) {
            this.mediaSource = new MediaSource;
            this.mediaSource.addEventListener('sourceopen', this.onMediaSourceOpen.bind(this));
            this.mediaPlayer = document.getElementById(this.videoId);
            this.mediaPlayer.src = URL.createObjectURL(this.mediaSource);
        } else {
            console.log("Unsupported MIME type or codec: ", +this.codec);
        }
    }

    /**
     * MediaSource已打开事件
     * @param e 事件
     */
    onMediaSourceOpen(e) {
        // URL.revokeObjectURL 主动释放引用
        URL.revokeObjectURL(this.mediaPlayer.src);
        this.mediaSource.removeEventListener('sourceopen', this.onMediaSourceOpen.bind(this));

        // console.log("MediaSource已打开")
        this.sourceBuffer = this.mediaSource.addSourceBuffer(this.codec);
        this.sourceBuffer.addEventListener('about', e => console.log(`about `, e));
        this.sourceBuffer.addEventListener('error', e => console.log(`error `, e));
        this.sourceBuffer.addEventListener('updateend', e => {
            if (this.canFeed) {
                this.removeBuffer();
                this.processDelay();
                this.feedNext();
            }
        });
        this.canFeed = true;
    }

    /**
     * 压入数据
     * @param data 数据
     */
    pushData(data) {
        this.queue.push(data);
        if (this.canFeed) this.feedNext();
    }

    /**
     * 喂数据
     * append的时候遇到The HTMLMediaElement.error attribute is not null就是数据时间戳有问题
     */
    feedNext() {
        if (!this.queue || !this.queue.length) return
        if (!this.sourceBuffer || this.sourceBuffer.updating) return;

        this.canFeed = false;
        try {
            const data = this.queue.shift();
            this.sourceBuffer.appendBuffer(data);
            this.canFeed = true;
        } catch (e) {
            console.log(e);
            this.canFeed = false;
            this.queue = [];
            this.onReset(this.number, this.videoId);
        }
    }

    /**
     * 处理延时或画面卡主
     */
    processDelay() {
        if (!this.sourceBuffer || !this.sourceBuffer.buffered.length || this.sourceBuffer.updating) return;

        const end = this.sourceBuffer.buffered.end(this.sourceBuffer.buffered.length - 1);
        const current = this.mediaPlayer.currentTime;
        // 解决延迟并防止画面卡主
        if (Math.abs(end - current) >= 1.8) {
            this.mediaPlayer.currentTime = end - 0.01;
        }
    }

    /**
     * 移除缓存
     */
    removeBuffer() {
        if (!this.sourceBuffer || !this.sourceBuffer.buffered.length || this.sourceBuffer.updating) return;

        const length = this.sourceBuffer.buffered.length;
        const firstStart = this.sourceBuffer.buffered.start(0);
        const firstEnd = this.sourceBuffer.buffered.end(0);
        const lastStart = this.sourceBuffer.buffered.start(this.sourceBuffer.buffered.length - 1);
        const lastEnd = this.sourceBuffer.buffered.end(this.sourceBuffer.buffered.length - 1);
        const currentTime = this.mediaPlayer.currentTime;

        if (Math.abs(firstStart - lastEnd) > 47000) {
            this.sourceBuffer.remove(firstEnd + 10, lastEnd);
        } else if (currentTime - firstStart > 120 && lastEnd > currentTime) {
            this.sourceBuffer.remove(firstStart, lastEnd - 10)
        }
    }
}