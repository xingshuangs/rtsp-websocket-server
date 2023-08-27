class RtspStream {

    constructor(wsUrl, videoId) {
        this.wsUrl = wsUrl;
        this.videoId = videoId;
        this.queue = [];
        this.canFeed = false;
    }

    send(data) {
        this.websocket.send(data);
    }

    subscribe(value) {
        // 重置参数
        this.canFeed = false;
        this.queue = [];
        // 构建订阅参数，json的形式交互
        const params = {};
        params.type = "SUBSCRIBE";
        params.content = value;
        this.websocket.send(JSON.stringify(params))
    }

    onopen(evt) {
        console.log("ws连接成功")
        // this.websocket.send(this.rtspUrl)
    }

    onClose(evt) {
        console.log("ws连接关闭")
    }

    onMessage(evt) {
        if (typeof (evt.data) == "string") {
            let data = JSON.parse(evt.data);
            if (data.type === "SUBSCRIBE") this.init(data.content);
            else if (data.type === "QUERY") console.log(data.content);
            else if (data.type === "ERROR") console.error(data.content);
            else console.log(data.content);
        } else {
            const data = new Uint8Array(evt.data);
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
            if (this.canFeed) {
                this.removeBuffer();
                this.processDelay();
                this.feedNext();
            }
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

        this.canFeed = false;
        try {
            const data = this.queue.shift();
            this.sourceBuffer.appendBuffer(data);
            this.canFeed = true;
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

    reset() {
        this.close();
        this.open();
        console.log("触发websocket进行重连");
    }
}