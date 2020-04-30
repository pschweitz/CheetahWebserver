
var websocketUpload;

function closeUploadWebsocket(){
    try{
        websocketUpload.close();
    }
    catch(error){}
}

function openUploadWebsocket(){
    let websocketHost = getWebsocketRootURL();

    websocketUpload = new WebSocket(websocketHost + "/admin/Upload");

    websocketUpload.onopen = function () {
    };

    websocketUpload.onmessage = function (evt) {
        console.log(evt.data);
        data = JSON.parse(evt.data);
        if (data.MessageType === "Status") {

            message = data.Status.destinationFile + ': ' + data.Status.fileSent + '/' + data.Status.fileSize + ' - ' + data.Status.percent + '%';

            try{$("#jqxProgressBar").val(data.Status.percent);}catch(error){}
            
            //alert(message);
            //document.getElementById('status').innerHTML = message;

        } else if (data.MessageType === "Redirect") {
            websocket.close();
            window.location = data.Location;

        } else if (data.MessageType === "Error") {
            alert(data.errorMessage);
        }
    };

    websocketUpload.onerror = function (evt) {
    };

    websocketUpload.onclose = function (evt) {
    };
}
