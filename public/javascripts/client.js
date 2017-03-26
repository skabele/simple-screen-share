'use strict';


var connectBtn = document.querySelector('button#connect');
var disconnectBtn = document.querySelector('button#disconnect');
connectBtn.onclick = connect;
disconnectBtn.onclick = disconnect;

var remoteVideo = document.querySelector('video#remote-screen');
var remoteStream;
var clientName;

function connect() {
  connectBtn.disabled = true;
  disconnectBtn.disabled = false;
  sendChatBtn.disabled = false;
  clientName = chatName.value;
  chatName.disabled = true;
  socket = openWebSocket();
}

function disconnect() {
  connectBtn.disabled = false;
  disconnectBtn.disabled = true;
  sendChatBtn.disabled = true;
  chatName.disabled = false;

  socket.close();
  socket = null;
}

function createWS() {
  return new WebSocket('ws://' + location.host + '/client-ws');
}

function onWSConnected() {
  send({id: 'CLIENT_READY', data: {clientName: clientName} });
}

function onWSMessage(message) {
  switch(message.id) {
    case "CHAT_MSG":
      chatMessages.value = chatMessages.value + message.data.name + ': ' + message.data.text + '\n';
      break;
    case "SERVER_ERROR":
      log.error("Server reports error:", message.data.text);
      break;
    case "NAME_ALREADY_TAKEN":
      alert("This chat name is already taken - please choose another one");
      disconnect();
    case "SCREEN_READY":
      log.info("SCREEN_READY");
      break;
    case "RTC_SESSION_DESCRIPTION":
        if (pc === null) {
          createPeerConnection();
        }
        pc.setRemoteDescription(new RTCSessionDescription(message.data));
        doAnswer();
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

var pc = null;

function createPeerConnection() {
  try {
    pc = new RTCPeerConnection(null);
    pc.onicecandidate = onIceCandidate;
    pc.onaddstream = onRemoteStreamAdded;
    pc.onremovestream = onRemoteStreamRemoved;
    log.debug('Created RTCPeerConnnection');
  } catch (e) {
    log.debug('Failed to create PeerConnection ', e);
    alert('Cannot create RTCPeerConnection');
    return;
  }
}

function onIceCandidate(event) {
  log.debug('icecandidate event', event);
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
  log.debug('Remote stream added.');
  remoteVideo.src = window.URL.createObjectURL(event.stream);
  remoteStream = event.stream;
}

function onRemoteStreamRemoved(event) {
  log.debug('Remote stream removed', event);
}

function onCreateOfferError(event) {
  log.warn('createOffer() error', event);
}

function doCall() {
  log.debug('Sending offer to peer');
  pc.createOffer(setLocalAndSendMessage, onCreateOfferError);
}

function doAnswer() {
  log.debug('Sending answer to peer');
  pc.createAnswer().then(
    setLocalAndSendMessage,
    onCreateSessionDescriptionError
  );
}

function setLocalAndSendMessage(sessionDescription) {
  pc.setLocalDescription(sessionDescription);
  log.debug('setLocalAndSendMessage sending message', sessionDescription);
  send({id: "RTC_SESSION_DESCRIPTION", data: sessionDescription});
}

function onCreateSessionDescriptionError(error) {
  log.error('Failed to create session description: ' + error.toString());
}
