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

    constructor(wsUrl, rtspUrl, videoId) {
        this.wsUrl = wsUrl;
        this.rtspUrl = rtspUrl;
        this.videoId = videoId;
        this.queue = [];
        this.canFeed = false;
        this.lastTime = new Date();
    }

    onopen(evt) {
        console.log("ws连接成功")
        this.websocket.send(this.rtspUrl)
    }

    onClose(evt) {
        console.log("ws连接关闭")
    }

    onMessage(evt) {
        if (typeof (evt.data) == "string") {
            this.init(evt.data)
        } else {
            const data = new Uint8Array(evt.data);
            // console.log(data)
            this.queue.push(data);
            if (this.canFeed) this.feedNext();
        }
    }

    onError(evt) {
        console.log("ws连接错误")
    }

    open() {
        if (this.websocket) this.websocket.close();

        this.websocket = new WebSocket(this.wsUrl);
        this.websocket.binaryType = "arraybuffer";
        this.websocket.onopen = this.onopen.bind(this);
        this.websocket.onmessage = this.onMessage.bind(this);
        this.websocket.onclose = this.onClose.bind(this);
        this.websocket.onerror = this.onError.bind(this);
        this.queue = [];
        this.canFeed = false;
    }

    close() {
        if (this.websocket) this.websocket.close();
    }

    /**
     * 初始化
     * @param codecStr 编解码信息
     */
    init(codecStr) {
        this.codec = 'video/mp4; codecs=\"' + codecStr + '\"';
        console.log("call play:", this.codec);
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
            this.removeBuffer();
            this.processDelay();
            this.canFeed = true;
            this.feedNext();
        });
        this.canFeed = true;
    }

    /**
     * 喂数据
     * append的时候遇到The HTMLMediaElement.error attribute is not null就是数据时间戳有问题
     */
    feedNext() {
        if (!this.queue || !this.queue.length) return
        if (!this.sourceBuffer || this.sourceBuffer.updating) return;
        if (!this.canFeed) return;

        // const now = new Date();
        // if (now.getTime() - this.lastTime.getTime() > 120 * 1000) {
        //     console.log("喂数据进行中", now, this.queue.length, this.sourceBuffer.buffered.end(this.sourceBuffer.buffered.length - 1));
        //     this.lastTime = now;
        // }

        try {
            const data = this.queue.shift();
            this.sourceBuffer.appendBuffer(data);
            this.canFeed = false;
        } catch (e) {
            console.log(e);
            this.reset();
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
            // console.log("画面存在延迟", this.sourceBuffer.buffered.length, current, end);
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
            // console.log("时间戳存在溢出", length, firstStart, firstEnd, currentTime, lastStart, lastEnd);
        } else if (currentTime - firstStart > 120 && lastEnd > currentTime) {
            this.sourceBuffer.remove(firstStart, lastEnd - 10)
            // console.log("正常移除缓存数据", length, firstStart, firstEnd, currentTime, lastStart, lastEnd);
        }
    }

    reset() {
        this.close();
        this.open();
        console.log("触发websocket进行重连");
    }
}