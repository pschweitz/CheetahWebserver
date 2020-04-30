/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.websocket.client;

import java.io.Serializable;
import org.simpleframework.http.socket.Frame;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class WebSocketClientWorkerEcho extends AbstractWebSocketClientWorker {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketClientWorkerEcho.class);


    @Override
    public void processString(String responseString) {
        logger.debug("Got TEXT: " + responseString);
        
     //   this.webSocketClient.sendString(responseString);
    }

    @Override
    public void processObject(Serializable responseObject) {
        logger.debug("Got BINARY");
        
     //   this.webSocketClient.sendObject(responseObject);
    }
}
