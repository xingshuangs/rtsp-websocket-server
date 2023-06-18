class RtspStream {

    constructor(wsUrl, rtspUrl, videoId) {
        this.wsUrl = wsUrl;
        this.rtspUrl = rtspUrl;
        this.videoId = videoId;
        this.queue = [];
        this.canFeed = false;
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
            // console.log(evt.data)
            this.queue.push(new Uint8Array(evt.data));
            if (this.canFeed) {
                this.feedNext();
            }
        }
    }

    onError(evt) {
        console.log("ws连接错误")
    }

    open() {

        this.websocket = new WebSocket(this.wsUrl);
        this.websocket.binaryType = "arraybuffer";
        this.websocket.onopen = this.onopen.bind(this);
        this.websocket.onmessage = this.onMessage.bind(this);
        this.websocket.onclose = this.onClose.bind(this);
        this.websocket.onerror = this.onError.bind(this);
    }

    close() {
        this.websocket.close();
    }

    init(codecStr) {
        this.codec = 'video/mp4; codecs=\"' + codecStr + '\"';
        console.log("call play:", this.codec);
        if (MediaSource.isTypeSupported(this.codec)) {
            this.mediaSource = new MediaSource;
            this.mediaSource.addEventListener('sourceopen', this.onMediaSourceOpen.bind(this));
            this.mediaPlayer = document.getElementById(this.videoId);
            this.mediaPlayer.src = URL.createObjectURL(this.mediaSource);
        } else {
            console.log("Unsupported MIME type or codec: ", + this.codec);
        }
    }

    onMediaSourceOpen(e) {
        // URL.revokeObjectURL 主动释放引用
        URL.revokeObjectURL(this.mediaPlayer.src);
        this.mediaSource.removeEventListener('sourceopen', this.onMediaSourceOpen.bind(this));

        console.log("MediaSource已打开")
        this.sourceBuffer = this.mediaSource.addSourceBuffer(this.codec);
        this.sourceBuffer.addEventListener('about', e => console.log(`about `, e));
        this.sourceBuffer.addEventListener('error', e => console.log(`error `, e));
        this.sourceBuffer.addEventListener('updateend', e => this.feedNext());
        this.canFeed = true;
    }

    feedNext() {
        if (!this.queue || !this.queue.length) return

        if (this.sourceBuffer && !this.sourceBuffer.updating) {
            const data = this.queue.shift();
            this.sourceBuffer.appendBuffer(data);
        }
    }
}