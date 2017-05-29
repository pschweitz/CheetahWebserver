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
public class WebSocketClientWorkerTextTest extends AbstractWebSocketClientWorker {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketClientWorkerTextTest.class);
    
    @Override
    public void processString(String responseString) {

        String result = "test client text";

        if (responseString.startsWith(result)) {
            System.out.println("Pass : " + "11- WebSocketClientWorkerTextTest" + " URI: " + this.webSocketClient.URI  + ": result: " + result);
        } else {
            if (responseString.length() >= result.length()) {
                System.err.println("Fail : " + "11- WebSocketClientWorkerTextTest" + " URI: " + this.webSocketClient.URI   + ": result: " + result + " vs " + responseString.substring(0, result.length()));
            } else {
                System.err.println("Fail : " + "11- WebSocketClientWorkerTextTest" + " URI: " + this.webSocketClient.URI + ": result: " + result);
            }
        }
    }

    @Override
    public void processObject(Serializable responseObject) {
        System.err.println("Fail : " + "11- WebSocketClientWorkerTextTest" + ": result: Got BINARY instead of TEXT");
    }
}
