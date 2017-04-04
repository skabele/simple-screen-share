'use strict';

log.setDefaultLevel("info");

var chatInput = document.querySelector('textarea#chat-input');
var sendChatBtn = document.querySelector('a#send-chat');
var chatMessages = document.querySelector('textarea#chat-messages');
var chatName = document.querySelector('input#chat-name');
sendChatBtn.onclick = sendChat;

function openWebSocket() {
  var socket = createWS();

  socket.onopen = function () {
    log.info('WebSocket opened');
    sendChatBtn.disabled = false;
    onWSConnected();
  };

  socket.onerror = function (error) {
    log.error('WebSocket error', error);
  };

  socket.onmessage = function (event) {
    var message = JSON.parse(event.data);
    log.debug('WebSocket message', message);
    if (!_.has(message, 'id')) {
      log.warn("WS message without msg field received", message);
    } else {
        onWSMessage(message);
    }
  }

  return socket;
}

var socket;

function send(payload) {
  log.debug("sending to ws", payload);
  socket.send(JSON.stringify(payload));
}

function sendChat() {
  var text = chatInput.value
  if (text != '') {
    send({id: 'SEND_CHAT', data: {text: text} });
  }
}