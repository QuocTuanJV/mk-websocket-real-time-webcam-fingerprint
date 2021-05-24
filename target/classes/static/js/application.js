var webSocket = null;
var webSocketFinger = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    $("#send").prop("disabled", !connected);

    if (connected) {
        $("#conversation").show();
    } else {
        $("#conversation").hide();
    }

    $("#responses").html("");
}

function setConnectedFinger(connected) {
    $("#connectFinger").prop("disabled", connected);
    $("#disconnectFinger").prop("disabled", !connected);
    $("#sendFinger").prop("disabled", !connected);

    if (connected) {
        $("#conversation").show();
    } else {
        $("#conversation").hide();
    }

    $("#responses").html("");
}

function connect() {
    webSocket = new WebSocket('ws://localhost:7171/face-websocket',
        'subprotocol.demo.facewebsocket');

    webSocket.onopen = function () {
        setConnected(true);
        log('Client connection opened');

        console.log('Subprotocol: ' + webSocket.protocol);
        console.log('Extensions: ' + webSocket.extensions);
    };

    webSocket.onmessage = function (event) {
        log('Client 1');
        var datax = event.data;
        var obj = JSON.parse(datax);

        if (obj) {
            console.log('face quality = ', obj.quality);

            if (obj.quality >= 70) {
                webSocket.close();
            }
        }

        var imageBae64 = 'data:image/png;base64, ' + obj.image;
        var img = document.querySelector("#imgDisplayFace");
        img.src = imageBae64;
    };

    webSocket.onerror = function (event) {
        console.log("error");
        log('Client 1 error: ' + event);
    };

    webSocket.onclose = function (event) {
        setConnected(false);
        log('Client 1 connection closed: ' + event.code);
    };
}

function connectFinger() {
    webSocketFinger = new WebSocket('ws://localhost:7171/finger-websocket',
        'subprotocol.demo.fingerwebsocket');

    webSocketFinger.onopen = function () {
        setConnectedFinger(true);
        document.getElementById("imgDisplayFinger").src = "";
        log('Client connection opened');

        console.log('Subprotocol: ' + webSocketFinger.protocol);
        console.log('Extensions: ' + webSocketFinger.extensions);
    };

    webSocketFinger.onmessage = function (event) {
        //        log(event.data);

        var datax = event.data;
        // console.log(typeof datax);
        // console.log(datax);
        if (datax.localeCompare('Stop signal') == 0) {
            // console.log(datax);
            webSocketFinger.close();
        } else {
            var obj = JSON.parse(datax);
            if (obj) {
                var imageBae64 = 'data:image/png;base64, ' + obj.image;
                var img = document.querySelector("#imgDisplayFinger");
                img.src = imageBae64;
                if (obj.quality > 50) {
                    console.log(obj.quality);
                    console.log(obj.template);
                    webSocketFinger.close();
                }
            }
        }
    };

    webSocketFinger.onerror = function (event) {
        log('Client 2 error: ' + event);
    };

    webSocketFinger.onclose = function (event) {
        setConnectedFinger(false);
        log('Client 2 connection closed: ' + event.code);
    };
}

function disconnect() {
    if (webSocket != null) {
        webSocket.close();
        webSocket = null;
    }
    setConnected(false);
}

function disconnectFinger() {
    if (webSocketFinger != null) {
        webSocketFinger.close();
        document.getElementById("imgDisplayFinger").src = "";
        webSocketFinger = null;
    }
    setConnectedFinger(false);
}


function send() {
    const message = $("#request").val();
    //    log('Client sends: ' + message);
    webSocket.send(message);
}

function sendFinger() {
    const message = $("#request").val();
    log('Client 2 sends: ' + message);
    webSocketFinger.send(message);
}

function log(message) {
    //    $("#responses").append("<tr><td>" + message + "</td></tr>");
    //    console.log(message);
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $("#connect").click(function () {
        connect();
    });
    $("#disconnect").click(function () {
        disconnect();
    });
    $("#send").click(function () {
        send();
    });

    $("#connectFinger").click(function () {
        connectFinger();
    });
    $("#disconnectFinger").click(function () {
        disconnectFinger();
    });
    $("#sendFinger").click(function () {
        sendFinger();
    });
});