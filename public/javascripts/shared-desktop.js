'use strict';

var mirrorVideo = document.querySelector('video#mirror');
var shareBtn = document.querySelector('button#share');
var stopBtn = document.querySelector('button#stop');
shareBtn.onclick = startSharing;
stopBtn.onclick = stopSharing;

var sharedStream;
var requestId;

var clients = {};

function getClient(name) {
  if (!clients[name]) {
    log.warn("Client name='" + name + "' does not exists. Clients:", clients);
    return false;
  } else {
    return clients[name];
  }
}

function addClient(name) {
  if (clients[name]) {
    log.error("Client name='" + name + "' already exists. Clients:", clients);
    return clients[name];
  }
  var client = {
    name: name,
    isStarted: false,
    isReady: false,
    pc: null
  };
  clients[name] = client;
  return client;
}

function streamReceived(stream) {
  sharedStream = stream;
  mirrorVideo.srcObject = stream;
  log.info('Local camera stream received');
  startReadyClients();
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
  socket.close();
  socket = null;
  log.info('Sharing stopped');
}

function createWS() {
  return new WebSocket('ws://' + location.host + '/shared-desktop-ws');
}

function onWSConnected() {
  send({id: 'SCREEN_READY', data: {name: chatName.value}});
}

function onWSMessage(message) {
  var client, name;
  switch(message.id) {

    case "CHAT_MSG":
      chatMessages.value = chatMessages.value + message.data.name + ': ' + message.data.text + '\n';
      break;

    case "SERVER_ERROR":
      log.error("Server reports error:", message.data.text);
      break;

    case "CLIENT_READY":
      name = message.data.clientName;
      if (!clients[name]) {
        client = addClient(name);
      } else {
        client = clients[name];
        client.name = name;
      }
      client.isReady = true;
      log.debug("On CLIENT_READY client data", client, message.data);
      if (typeof sharedStream !== 'undefined') {
        startClient(client);
      }
      break;

    case "CLIENT_LEFT":
      name = message.data.clientName;
      if (clients[name] && clients[name].pc !== null) {
        clients[name].pc.close();
      }
      delete clients[message.data.clientName];
      break;

    case "RTC_SESSION_DESCRIPTION_WITH_NAME":
      client = getClient(message.data.clientName);
      if (client) {
        if (client.pc === null) {
          log.error("clients['first'].pc is null")
        } else {
          client.pc.setRemoteDescription(new RTCSessionDescription(message.data.session));
        }
      }
      break;

    case "RTC_ICE_CANDIDATE_WITH_NAME":
      client = getClient(message.data.clientName);
      if (client) {
        if (client.pc === null) {
          createPeerConnection(client);
        }
        var candidate = new RTCIceCandidate(message.data.candidate);
        client.pc.addIceCandidate(candidate);
      }
      break;

    default:
      log.warn("Unexpected message type received", message);
  }
}

function startClient(_client) {
  var client = _client;
  log.debug('startClient() - creating peer connection', client);
  createPeerConnection(client);
  client.pc.addStream(sharedStream);
  client.isStarted = true;
  log.debug('Sending WebRtc offer to peer');

  var setLocalAndSendMessage = function(sessionDescription) {
    client.pc.setLocalDescription(sessionDescription);
    log.debug('setLocalAndSendMessage sending message', sessionDescription);
    send({
      id: 'RTC_SESSION_DESCRIPTION_WITH_NAME',
      data: {
        clientName: client.name,
        session: sessionDescription
      }
    });
  };

  client.pc.createOffer(setLocalAndSendMessage, onCreateOfferError);
}

function startReadyClients() {
  if (typeof sharedStream === 'undefined') {
    log.debug('startReadyClients() stream not ready');
    return;
  }
  _.forEach(clients, function(client){
    if (!client.isStarted && client.isReady) {
      startClient(client);
    } else {
      log.debug('startReadyClients() not starting client', client);
    }
  });
}


function createPeerConnection(_client) {
  try {
    var client = _client;
    client.pc = new RTCPeerConnection(null);
    var onIceCandidate = function(event) {
      log.debug('icecandidate event: ', event);
      if (event.candidate) {
        send({
          id: 'RTC_ICE_CANDIDATE_WITH_NAME',
          data: {
            clientName: client.name,
            candidate: event.candidate
          }
        });
      } else {
        log.debug('End of ICE candidates.');
      }
    };

    client.pc.onicecandidate = onIceCandidate;
    client.pc.onaddstream = onRemoteStreamAdded;
    client.pc.onremovestream = onRemoteStreamRemoved;
    log.debug('Created RTCPeerConnnection');
  } catch (e) {
    log.debug('Failed to create PeerConnection, exception: ' + e.message);
    alert('Cannot create RTCPeerConnection object.');
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

function onCreateSessionDescriptionError(error) {
  log.error('Failed to create session description: ' + error.toString());
}

////////////////////////////////////////////////////////////

window.addEventListener('message', function (event) {
  log.debug("MESSAGE", event);
  if (typeof event.data === "object") {
    var data = event.data;

    if(data.answer == 1 && data.requestId === requestId && data.state === "completed") {

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
         log.error("webkitGetUserMedia() failed", error);
         alert('Unable to start sharing - webkitGetUserMedia() failed: ' + error.name);
      });
    }
  }
});