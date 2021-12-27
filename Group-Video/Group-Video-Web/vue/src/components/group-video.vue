<template>
  <h1>QNRTC Web 多人连麦示例代码</h1>
  <label>请输入 RoomToken 加入房间开始连麦</label>
  <input v-model="token" type="text" /><br />
  <button @click="joinRoom">加入房间</button>
  <button @click="leaveRoom">离开房间</button>
  <p class="tips">
    如果您不知道如何生成 RoomToken，查看<a
      href="https://developer.qiniu.io/rtc/8802/pd-overview"
      target="_black"
      >这里</a
    >
  </p>
  <div id="trackcontainer">
    <p>本地视频</p>
    <div class="video" ref="localVideo"></div>
    <p>远端视频</p>
    <div class="video" ref="remoteVideo"></div>
  </div>
</template>

<script lang="ts">
import { ref, reactive, onMounted, defineProps } from "vue";
import QNRTC from "qnweb-rtc";
export default {
  setup() {
    const token = ref("");
    const localTracks = reactive([]);
    const localVideo = ref(null);
    const remoteVideo = ref(null);
    const client = ref(null);
    return {
      token,
      localVideo,
      remoteVideo,
      client,
    };
  },
  mounted() {
    window.addEventListener("beforeunload", () => {
      this.client && this.client.leave();
    });
    this.client = QNRTC.createClient();
    this.client.on(
      "connection-state-changed",
      function (connectionState, info) {
        console.log("connection-state-changed", connectionState, info);
      }
    );
    this.client.on("user-joined", (user) => {
      console.log("user-joined", user);
    });
    this.client.on("user-left", (user) => {
      console.log("user-left", user);
    });
    this.autoSubscribe();
  },
  methods: {
    // 加入房间
    async joinRoom() {
      await this.client.join(this.token);
      this.publish();
    },
    // 自动订阅
    autoSubscribe() {
      // 添加事件监听，当房间中出现新的 Track 时就会触发，参数是 trackInfo 列表
      this.client.on("user-published", (userId, tracks) => {
        console.log("user-published!", userId, tracks);
        this.subscribe(tracks)
          .then(() => console.log("subscribe success!"))
          .catch((e) => console.error("subscribe error", e));
      });
      // 就是这样，就像监听 DOM 事件一样通过 on 方法监听相应的事件并给出处理函数即可
    },
    // 离开房间
    leaveRoom() {
      for (let i of this.localTracks) {
        i.destroy();
      }
      this.localTracks = [];
      this.client.leave();
    },
    // 订阅 Track
    async subscribe(trackInfoList) {
      // 通过传入 trackInfoList 调用订阅方法发起订阅，成功会返回相应的 Track 对象，也就是远端的 Track 列表了
      const remoteTracks = await this.client.subscribe(trackInfoList);
      // 选择页面上的一个元素作为父元素，播放远端的音视频轨
      const remoteElement = this.remoteVideo;
      // 遍历返回的远端 Track，调用 play 方法完成在页面上的播放
      for (const remoteTrack of [
        ...remoteTracks.videoTracks,
        ...remoteTracks.audioTracks,
      ]) {
        remoteTrack.play(remoteElement);
      }
    },
    // 发布 Track
    async publish() {
      // 同时采集麦克风音频和摄像头视频轨道。
      // 这个函数会返回一组 audio track 与 video track
      const localTracks = await QNRTC.createMicrophoneAndCameraTracks();
      console.log("my local tracks", localTracks);
      this.localTracks = localTracks;
      // 将刚刚的 Track 列表发布到房间中
      await this.client.publish(localTracks);
      console.log("publish success!");
      // 在这里添加
      // 获取页面上的一个元素作为播放画面的父元素
      const localElement = this.localVideo;
      // 遍历本地采集的 Track 对象
      for (const localTrack of localTracks) {
        // 如果这是麦克风采集的音频 Track，我们就不播放它。
        if (localTrack.track.info.kind === "audio") continue;
        // 调用 Track 对象的 play 方法在这个元素下播放视频轨
        localTrack.play(localElement, {
          mirror: true,
        });
      }
    },
  },
};
</script>

<style scoped>
select {
  width: 300px;
}

section {
  margin-bottom: 20px;
}

.video {
  width: 320px;
  height: 240px;
  background: #000;
}

button {
  margin-right: 10px;
}
</style>
