'use strict';

var mirrorVideo = document.querySelector('video#mirror');
var shareBtn = document.querySelector('button#share');
var stopBtn = document.querySelector('button#stop');
shareBtn.onclick = startSharing;
stopBtn.onclick = stopSharing;

var cameraStream;

function streamReceived(stream) {
  cameraStream = stream;
  mirrorVideo.srcObject = stream;
  log.info('Local camera stream received');
}

function startSharing() {
  shareBtn.disabled = true;
  stopBtn.disabled = false;
  socket = openWebSocket();

  navigator.mediaDevices.getUserMedia({ audio: false, video: true })
  .then(streamReceived)
  .catch(function(error) {
    log.error("getUserMedia() failed", error)
    alert('Unable to start sharing - getUserMedia() failed: ' + error.name);
  });
}

function stopSharing () {
  shareBtn.disabled = false;
  stopBtn.disabled = true;
  sendChatBtn.disabled = true;

  mirrorVideo.srcObject = null;
  cameraStream.getTracks().forEach(function (track) {
    track.stop();
  });
  cameraStream = null;
  send({msg: 'BYE'});
  socket.close();
  socket = null;
  log.info('Sharing stopped');
}



