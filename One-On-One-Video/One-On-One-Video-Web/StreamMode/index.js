// 获取一些页面上需要操作的 DOM 对象
const joinRoomBtn = document.getElementById("joinroom");
const roomTokenInput = document.getElementById("roomtoken");
const audioDeviceSelect = document.getElementById("audiodevice");
const videoDeviceSelect = document.getElementById("videodevice");
const localVideo = document.getElementById("localvideo");
const remoteVideo = document.getElementById("remotevideo");

joinRoomBtn.addEventListener("click", joinRoom);

// 全局房间对象
const myRoom = new QNRTC.StreamModeSession();

// 如果此时枚举设备的操作完成，就更新页面设备列表
if (QNRTC.deviceManager.deviceInfo) {
  addDeviceToSelect(QNRTC.deviceManager.deviceInfo);
}
// 当检测到枚举完成或者设备列表更新的时候，更新页面设备列表
QNRTC.deviceManager.on("device-update", deviceInfo => {
  addDeviceToSelect(deviceInfo);
});

// 将枚举到的设备信息添加到页面上
function addDeviceToSelect(deviceInfo) {
  // 清空之前 select 下的元素，重新遍历添加
  while (audioDeviceSelect.firstChild) {
    audioDeviceSelect.removeChild(audioDeviceSelect.firstChild);
  }
  while (videoDeviceSelect.firstChild) {
    videoDeviceSelect.removeChild(videoDeviceSelect.firstChild);
  }

  // 遍历每个设备，添加到页面上供用户选择
  deviceInfo.forEach(info => {
    const optionElement = document.createElement("option");
    optionElement.value = info.deviceId;
    optionElement.text = info.label;
    if (info.kind === "audioinput") {
      audioDeviceSelect.appendChild(optionElement);
    } else if (info.kind === "videoinput") {
      videoDeviceSelect.appendChild(optionElement);
    }
  });
}

async function joinRoom() {
  // 从输入框中获取 roomToken
  const roomToken = roomTokenInput.value;
  try {
    // 加入房间
    const users= await myRoom.joinRoomWithToken(roomToken);
    // 因为我们假设是一对一连麦，如果加入后发现房间人数超过就退出报错
    // 实际上这里更好的做法是在 portal 上连麦应用中配置好房间人数上限
    // 这样就不要在前端做检查了
    if (users.length > 2) {
      myRoom.leaveRoom();
      alert("房间人数已满！");
      return;
    }

    // 如果加入房间后发现房间已经有人发布，就订阅他
    for (const user of users) {
      if (user.userId !== myRoom.userId && user.published) {
        subscribeUser(user.userId);
      }
    }
  } catch (e) {
    console.error(e);
    alert(`加入房间失败！ErrorCode: ${e.code || ""}`);
    return;
  }

  // 监听房间中其他人发布的事件，自动订阅他
  myRoom.on("user-publish", (user) => {
    subscribeUser(user.userId);
  });

  // 自动发布
  await publish();
}

async function publish() {
  let stream;
  try {
    // 通过用户在页面上指定的设备发起采集
    // 也可以不指定设备，这样会由浏览器自动选择
    stream = await QNRTC.deviceManager.getLocalStream({
      video: {
        enabled: true,
        deviceId: videoDeviceSelect.value,
        bitrate: 1000,
      },
      audio: {
        enabled: true,
        deviceId: audioDeviceSelect.value,
      },
    });
  } catch (e) {
    console.error(e);
    alert(`采集失败，请检查您的设备。ErrorCode: ${e.code}`);
    return;
  }

  // 将采集到的流静音播放
  stream.play(localVideo, true);

  try {
    // 发布采集流
    await myRoom.publish(stream);
  } catch (e) {
    console.error(e);
    alert(`发布失败，ErrorCode: ${e.code}`);
  }
}

function subscribeUser(userId) {
  // 订阅目标用户
  myRoom.subscribe(userId).then(stream => {
    stream.play(remoteVideo);
  })
}
