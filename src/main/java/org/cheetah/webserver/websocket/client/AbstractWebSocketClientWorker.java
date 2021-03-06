/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.websocket.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import org.simpleframework.http.socket.FrameType;
import static org.simpleframework.http.socket.FrameType.BINARY;
import static org.simpleframework.http.socket.FrameType.TEXT;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public abstract class AbstractWebSocketClientWorker<ResponseObject extends Serializable> implements Runnable {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractWebSocketClientWorker.class);

    public abstract void processString(String responseString);

    public abstract void processObject(ResponseObject responseObject);

    //private Frame frame;
    private String responseString;
    private ResponseObject responseObject;

    protected WebSocketClient webSocketClient;
    
    private FrameType frameType;

    public final void setMessage(String stringMessage) {
        frameType = FrameType.TEXT;
        responseString = stringMessage;
    }

    public final void setMessage(ByteBuffer byteMessage) {
        frameType = FrameType.BINARY;
        ByteArrayInputStream bis = new ByteArrayInputStream(byteMessage.array());
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);

            responseObject = (ResponseObject) in.readObject();

        } catch (IOException | ClassNotFoundException ex) {
            logger.error("Error reading frame response", ex);

        } finally {
            try {
                bis.close();
            } catch (IOException ex) {
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    @Override
    public final void run() {

        switch (frameType) {

            case TEXT:

                processString(responseString);
                break;

            case BINARY:

                processObject(responseObject);
                break;
        }
    }

    public WebSocketClient getWebSocketClient() {
        return webSocketClient;
    }

    public void setWebSocketClient(WebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }
}
