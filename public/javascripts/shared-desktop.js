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

var shareBtn = document.querySelector('button#share');
shareBtn.onclick = startSharing;
var stopBtn = document.querySelector('button#stop');
stopBtn.onclick = stopSharing;

var mirrorVideo = document.querySelector('video#mirror');
var showMirrorBtn = document.querySelector('button#show-mirror');
showMirrorBtn.onclick = toggleMirror;

function toggleMirror() {
    mirrorVideo.classList.toggle('hidden');
    if (mirrorVideo.classList.contains('hidden')) {
        showMirrorBtn.innerHTML = '<span class="glyphicon glyphicon-eye-open"></span> Show mirror';
    } else {
        showMirrorBtn.innerHTML = '<span class="glyphicon glyphicon-eye-close"></span> Hide mirror';
    }
}

var listOfConnected = document.querySelector('div#connected-clients');

function connectedClientsChanged() {
    var count = _.keys(clients).length;
    if (count > 0) {
        listOfConnected.innerHTML =
            '<strong>Connected clients (' + count + ') :</strong> ' +
            _.join(_.map(clients, function (client) {
                return client.name;
            }), ', ')
        ;
    } else {
        listOfConnected.innerHTML = '<strong>No connected clients</strong>';
    }
}

function isFullyConnected() {
    return !!(sharedStream && sharedStream.active && socket && socket.readyState === socket.OPEN);
}

function updateUiConnected() {
    if (isFullyConnected()) {
        shareBtn.disabled = true;
        stopBtn.disabled = false;
        sendChatBtn.disabled = false;
        showMirrorBtn.disabled = false;
        chatName.disabled = true;
    } else {
        shareBtn.disabled = false;
        stopBtn.disabled = true;
        sendChatBtn.disabled = true;
        showMirrorBtn.disabled = true;
        chatName.disabled = false;
        mirrorVideo.classList.remove('hidden');
        toggleMirror();
    }
}

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
    connectedClientsChanged();
    return client;
}

function streamReceived(stream) {
    sharedStream = stream;
    sharedStream.getVideoTracks()[0].onended = stopSharing;
    mirrorVideo.srcObject = stream;
    log.info('Local camera stream received');
    startReadyClients();
    updateUiConnected();
}

function startSharing() {
    requestId = Math.random();
    window.postMessage({requestId: requestId, data: ["screen", "window", "tab"]}, "*");

    socket = openWebSocket();
    updateUiConnected()
}

function stopSharing() {
    mirrorVideo.srcObject = null;
    if (sharedStream) {
        sharedStream.getTracks().forEach(function (track) {
            track.stop();
        });
        sharedStream = null;
    }
    if (socket) {
        socket.close();
        socket = null;
    }
    clients = {};
    log.info('Sharing stopped');
    updateUiConnected();
}

function createWS() {
    return new WebSocket('ws://' + location.host + '/shared-desktop-ws');
}

function onWSConnected() {
    log.debug('Web socket connected');
    connectedClientsChanged();
    updateUiConnected();
    send({id: 'SCREEN_READY', data: {name: chatName.value}});
}

function onWSClosed() {
    log.debug('Web socket closed');
    clients = {};
    socket = null;
    connectedClientsChanged();
    updateUiConnected();
}

function onWSMessage(message) {
    var client, name;
    switch (message.id) {

        case "CHAT_MSG":
            addChatMessage(message.data);
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
            if (sharedStream) {
                startClient(client);
            }
            break;

        case "CLIENT_LEFT":
            name = message.data.clientName;
            if (clients[name] && clients[name].pc !== null) {
                clients[name].pc.close();
            }
            delete clients[message.data.clientName];
            connectedClientsChanged();
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

    var setLocalAndSendMessage = function (sessionDescription) {
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
    _.forEach(clients, function (client) {
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
        var onIceCandidate = function (event) {
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
    log.debug('Remote stream added.');
}

function onRemoteStreamRemoved(event) {
    log.debug('Remote stream removed. Event: ', event);
}

function onCreateOfferError(event) {
    log.error('createOffer() error: ', event);
}

function onCreateSessionDescriptionError(error) {
    log.error('Failed to create session description: ' + error.toString());
}

////////////////////////////////////////////////////////////

window.addEventListener('message', function (event) {
    log.debug("MESSAGE", event);
    if (typeof event.data === "object") {
        var data = event.data;

        if (data.answer === 1 && data.requestId === requestId && data.state === "completed") {

            var screen_constraints = {
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
                stopSharing();
            });
        }
    }
});

function onLoad() {
    connectedClientsChanged();
    updateUiConnected();
    shareBtn.disabled = true;
    window.setTimeout(function(){
        if (!document.querySelector('#bistri-screen-sharing-installed')) {
            log.error("Crome extension not installed");
            document.querySelector('#extension-not-found').classList.remove('hidden');
        } else {
            shareBtn.disabled = false;
        }
    }, 250);
}
window.onload = onLoad;