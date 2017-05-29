/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.websocket.server;

import java.io.Serializable;
import org.cheetah.webserver.websocket.defaults.WebSocketDefaultService;
import org.simpleframework.http.socket.Frame;
import org.simpleframework.http.socket.FrameChannel;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phs
 */
class WebSocketServerService<RequestObject extends Serializable, ResponseObject extends Serializable> extends WebSocketDefaultService {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketServerService.class);

     
    public WebSocketServerService(AbstractWebSocketServerWorker webSocketServerWorker){    
        this.listener = new WebSocketServerListener(this, webSocketServerWorker);
    }    
}
