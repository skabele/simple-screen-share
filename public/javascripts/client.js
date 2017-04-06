'use strict';

var chatInput = document.querySelector('input#chat-input');
var sendChatBtn = document.querySelector('button#send-chat');
var chatMessagesDiv = document.querySelector('div#chat-messages');
var chatName = document.querySelector('input#chat-name');
sendChatBtn.onclick = sendChat;
chatInput.onkeydown = function(event) {
    if (event.which == 13 || event.keyCode == 13) {
        sendChat();
        return false;
    }
    return true;
}


var mainDiv = document.querySelector('div#main');
var minimizeBtn = document.querySelector('button#minimize-controlls');
minimizeBtn.onclick = function() {
    mainDiv.classList.add('minimized');
};
var expandBtn = document.querySelector('button#expand-controlls');
expandBtn.onclick = function() {
    mainDiv.classList.remove('minimized');
};

var connectBtn = document.querySelector('button#connect');
var disconnectBtn = document.querySelector('button#disconnect');
connectBtn.onclick = connect;
disconnectBtn.onclick = disconnect;

var remoteVideo = document.querySelector('video#remote-screen');
var remoteStream;
var clientName;

function isConnected() {
    return !!(socket && socket.readyState === socket.OPEN);
}

function updateUiConnected() {
    if (isConnected()) {
        connectBtn.disabled = true;
        disconnectBtn.disabled = false;
        sendChatBtn.disabled = false;
        minimizeBtn.disabled = false;
        expandBtn.disabled = false;
        chatName.disabled = true;
        chatInput.disabled = false;
    } else {
        connectBtn.disabled = false;
        disconnectBtn.disabled = true;
        sendChatBtn.disabled = true;
        chatName.disabled = false;
        minimizeBtn.disabled = true;
        expandBtn.disabled = true;
        chatInput.disabled = true;
        mainDiv.classList.remove('minimized');
    }
}

function connect() {
    socket = openWebSocket();
    updateUiConnected();
}

function disconnect() {
    terminateWebRtc();
    socket.close();
    socket = null;
    log.info('Disconnected');
    updateUiConnected();
}

function terminateWebRtc() {
    if (pc) {
        pc.close();
        pc = null;
    }
    remoteVideo.src = "";
    remoteVideo.load();
    if (remoteStream) {
        remoteStream.getTracks().forEach(function (track) {
            track.stop();
        });
        remoteStream = null;
    }
}

function createWS() {
    return new WebSocket('ws://' + location.host + '/client-ws');
}

function onWSConnected() {
    log.debug('Web socket connected');
    send({id: 'CLIENT_READY', data: {clientName: chatName.value}});
    updateUiConnected();
}

function onWSClosed() {
    log.debug('Web socket closed');
    socket = null;
    updateUiConnected();
}

function onWSMessage(message) {
    switch (message.id) {
        case "CHAT_MSG":
            addChatMessage(message.data);
            break;
        case "SERVER_ERROR":
            log.error("Server reports error:", message.data.text);
            break;
        case "NAME_ALREADY_TAKEN":
            alert("This chat name is already taken - please choose another one");
            disconnect();
            break;
        case "SCREEN_LEFT":
            chatMessagesDiv.value += 'Screen disconnected\n';
            terminateWebRtc();
            break;
        case "SCREEN_READY":
            log.info("SCREEN_READY");
            break;
        case "RTC_SESSION_DESCRIPTION":
            if (!pc) {
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
        log.debug('Failed to create RTCPeerConnnection ', e);
        alert('Cannot create RTCPeerConnection');
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

function onLoad() {
    updateUiConnected();
}
window.onload = onLoad;