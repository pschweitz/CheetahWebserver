/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.websocket.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.simpleframework.http.socket.DataFrame;
import org.simpleframework.http.socket.Frame;
import org.simpleframework.http.socket.FrameType;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class WebSocketServerWorkerEcho extends AbstractWebSocketServerWorker {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketServerWorkerEcho.class);

    @Override
    public void processString(String resquestString) {
        String response = null;
        logger.debug("Got TEXT: " + resquestString);

        response = resquestString;

        Frame replay = new DataFrame(FrameType.TEXT, response);
        String user = this.webSocketListener.getUserName(this.session);
        this.webSocketService.send(replay, user);

        // webSocketService.getWebserver().distributeToWebsocketServiceFrame("org.cheetah.webserver.page.websocket.Chat2", session, replay);
    }

    @Override
    public void processObject(Serializable resquestObject) {
        Serializable response = null;
        logger.debug("Got BINARY");

        response = resquestObject;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] byteArray = new byte[0];

        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(response);
            byteArray = bos.toByteArray();

            if (response != null) {
                Frame replay = new DataFrame(FrameType.BINARY, byteArray);
                String user = this.webSocketListener.getUserName(this.session);
                this.webSocketService.send(replay, user);
                
                // webSocketService.getWebserver().distributeToWebsocketServiceFrame("org.cheetah.webserver.page.websocket.Chat2", session, replay);
            }

        } catch (IOException ex) {

            logger.error("Error sending object response: " + ex.toString());
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {

            }
            try {
                bos.close();
            } catch (IOException ex) {
            }
        }
    }
}
