'use strict';

log.setDefaultLevel("info");

function openWebSocket() {
    var socket = createWS();

    socket.onopen = function () {
        log.info('WebSocket opened');
        onWSConnected();
    };

    socket.onerror = function (error) {
        log.error('WebSocket error', error);
    };

    socket.onclose = function (closeEvent) {
        log.debug('WebSocket closed', closeEvent);
        socket = null;
        onWSClosed();
    };

    socket.onmessage = function (event) {
        var message = JSON.parse(event.data);
        log.debug('WebSocket message', message);
        if (!_.has(message, 'id')) {
            log.warn("WS message without msg field received", message);
        } else {
            onWSMessage(message);
        }
    };

    return socket;
}

var socket;

function send(payload) {
    log.debug("sending to ws", payload);
    socket.send(JSON.stringify(payload));
}

function sendChat() {
    var text = chatInput.value;
    chatInput.value = "";
    if (text !== '') {
        send({id: 'SEND_CHAT', data: {text: text}});
    }
}


var chatMessages = [];

function renderChatMessages() {
    chatMessagesDiv.innerHTML = _.join(
        _.map(chatMessages, function(message) {
            return '<span class="chat-time">' + message.receivedAt.toString().split(" ")[4] + '</span>'
                +  ' <strong>' + he.encode(message.name) + '</strong> ' + he.encode(message.text);
        }),  ' <br>\n'
    );
}

function addChatMessage(message) {
    message.receivedAt = new Date();
    chatMessages.unshift(message);
    renderChatMessages();
}
