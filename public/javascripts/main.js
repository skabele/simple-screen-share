'use strict';

log.setDefaultLevel("info");

var chatInput = document.querySelector('textarea#chat-input');
var sendChatBtn = document.querySelector('button#send-chat');
var chatMessages = document.querySelector('textarea#chat-messages');
sendChatBtn.onclick = sendChat;

function openWebSocket() {
  var socket = new WebSocket('ws://' + location.host + '/shared-desktop');

  socket.onopen = function () {
    log.info('WebSocket opened');
    sendChatBtn.disabled = false;
    send({msg: 'READY'});
  };

  socket.onerror = function (error) {
    log.error('WebSocket error', error);
  };

  socket.onmessage = function (event) {
    var message = JSON.parse(event.data);
    log.debug('WebSocket message', message);
    if (!_.has(message, 'msg')) {
      log.warn("WS message without msg field received", message);
    } else switch(message.msg) {
      case "CHAT":
        chatMessages.value = chatMessages.value + message.text + '\n';
        break;
      case "ERROR":
        log.error("Server reports error", message.text);
        break;
      default:
        log.warning("Unexpected message type received", message);
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
    send({msg: 'CHAT', text: text});
  }
}