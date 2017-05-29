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
public class WebSocketClientWorkerObjectTest extends AbstractWebSocketClientWorker {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketClientWorkerObjectTest.class);
 
    @Override
    public void processString(String responseString) {
        System.err.println("Fail : " + "11- WebSocketClientWorkerObjectTest" + ": result: Got TEXT instead of BINARY");

    }

    @Override
    public void processObject(Serializable responseObject) {
        String result = "test client object";
        
        String responseString = (String) responseObject;

        if (responseString.startsWith(result)) {
            System.out.println("Pass : " + "11- WebSocketClientWorkerObjectTest" + " URI: " + this.webSocketClient.URI + ": result: " + result);
        } else {
            if (responseString.length() >= result.length()) {
                System.err.println("Fail : " + "11- WebSocketClientWorkerObjectTest" + " URI: " + this.webSocketClient.URI + ": result: " + result + " vs " + responseString.substring(0, result.length()));
            } else {
                System.err.println("Fail : " + "11- WebSocketClientWorkerObjectTest" + " URI: " + this.webSocketClient.URI + ": result: " + result);
            }
        }
    }
}
