'use strict';

var mirrorVideo = document.querySelector('video#mirror');
var shareBtn = document.querySelector('button#share');
var stopBtn = document.querySelector('button#stop');
shareBtn.onclick = startSharing;
stopBtn.onclick = stopSharing;

var sharedStream;

var isStarted = false;
var requestId;

function streamReceived(stream) {
  sharedStream = stream;
  mirrorVideo.srcObject = stream;
  log.info('Local camera stream received');
}

function startSharing() {
  shareBtn.disabled = true;
  stopBtn.disabled = false;

  requestId = Math.random();
  window.postMessage({requestId: requestId, data: ["screen", "window", "tab"]}, "*");

//  navigator.mediaDevices.getUserMedia({ audio: false, video: true })
//  .then(streamReceived)
//  .catch(function(error) {
//    log.error("getUserMedia() failed", error)
//    alert('Unable to start sharing - getUserMedia() failed: ' + error.name);
//  });

  socket = openWebSocket();
}

function stopSharing () {
  shareBtn.disabled = false;
  stopBtn.disabled = true;
  sendChatBtn.disabled = true;

  mirrorVideo.srcObject = null;
  sharedStream.getTracks().forEach(function (track) {
    track.stop();
  });
  sharedStream = null;
  send({id: 'BYE'});
  socket.close();
  socket = null;
  log.info('Sharing stopped');
}

function createWS() {
  return new WebSocket('ws://' + location.host + '/shared-desktop-ws');
}

function onWSConnected() {
  send({id: 'SCREEN_READY'});
}

function onWSMessage(message) {
  switch(message.id) {
    case "CHAT":
      chatMessages.value = chatMessages.value + message.data.text + '\n';
      break;
    case "ERROR":
      log.error("Server reports error:", message.data.text);
      break;
    case "CLIENT_READY":
      maybeStart();
      break;
    case "RTC_SESSION_DESCRIPTION":
        if (pc === null) {
          log.error("pc is null")
        }
        pc.setRemoteDescription(new RTCSessionDescription(message.data));
      break;
    case "RTC_ICE_CANDIDATE":
        if (pc === null) {
          createPeerConnection();
        }
        var candidate = new RTCIceCandidate(message.data);
        pc.addIceCandidate(candidate);
      break;
    default:
      log.warn("Unexpected message type received", message);
  }
}

function maybeStart() {
  console.log('maybeStart() ', isStarted, sharedStream);
  if (!isStarted && typeof sharedStream !== 'undefined') {
    log.info('creating peer connection');
    createPeerConnection();
    pc.addStream(sharedStream);
    isStarted = true;
    log.debug('Sending WebRtc offer to peer');
    pc.createOffer(setLocalAndSendMessage, onCreateOfferError);
  }
}

var pc;

function createPeerConnection() {
  try {
    pc = new RTCPeerConnection(null);
    pc.onicecandidate = onIceCandidate;
    pc.onaddstream = onRemoteStreamAdded;
    pc.onremovestream = onRemoteStreamRemoved;
    log.debug('Created RTCPeerConnnection');
  } catch (e) {
    log.debug('Failed to create PeerConnection, exception: ' + e.message);
    alert('Cannot create RTCPeerConnection object.');
    return;
  }
}

function onIceCandidate(event) {
  log.debug('icecandidate event: ', event);
  if (event.candidate) {
    send({
      id: 'RTC_ICE_CANDIDATE',
      data: event.candidate
    });
  } else {
    log.debug('End of ICE candidates.');
  }
}

function onRemoteStreamAdded(event) {
  log.warn('Remote stream added.');
}

function onRemoteStreamRemoved(event) {
  log.warn('Remote stream removed. Event: ', event);
}

function onCreateOfferError(event) {
  log.warn('createOffer() error: ', event);
}

function doAnswer() {
  log.debug('Sending answer to peer.');
  pc.createAnswer().then(
    setLocalAndSendMessage,
    onCreateSessionDescriptionError
  );
}

function setLocalAndSendMessage(sessionDescription) {
  pc.setLocalDescription(sessionDescription);
  log.debug('setLocalAndSendMessage sending message', sessionDescription);
  send({id: 'RTC_SESSION_DESCRIPTION', data: sessionDescription});
}

function onCreateSessionDescriptionError(error) {
  log.error('Failed to create session description: ' + error.toString());
}

////////////////////////////////////////////////////////////

window.addEventListener('message', function (event) {
  log.debug("MESSAGE", event);
  if (typeof event.data == "object") {
    var data = event.data;

    if(data.answer == 1 && data.requestId === requestId && data.state == "completed") {

      var screen_constraints = {
        //audio: false,
        video: {
        mandatory: {
          chromeMediaSourceId: data.streamId,
          chromeMediaSource: 'desktop',
          maxWidth: window.screen.width,
          maxHeight: window.screen.height
          //,maxFrameRate: 3
         },
         optional: []
       }
      };

      log.debug("screen_constraints", screen_constraints);
      navigator.getUserMedia(screen_constraints, function (stream) {
          streamReceived(stream);
      }, function (error) {
         log.error("webkitGetUserMedia() failed", error)
         alert('Unable to start sharing - webkitGetUserMedia() failed: ' + error.name);
      });
    }
  }
});