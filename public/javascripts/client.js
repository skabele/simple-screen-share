'use strict';



var connectBtn = document.querySelector('button#connect');
var disconnectBtn = document.querySelector('button#disconnect');
connectBtn.onclick = connect;
disconnectBtn.onclick = disconnect;

function connect() {
  connectBtn.disabled = true;
  disconnectBtn.disabled = false;
  sendChatBtn.disabled = false;
  socket = openWebSocket();
}

function disconnect() {
  connectBtn.disabled = false;
  disconnectBtn.disabled = true;
  sendChatBtn.disabled = true;

  send({msg: 'BYE'});
  socket.close();
  socket = null;
 }
