/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.websocket.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import org.cheetah.webserver.websocket.defaults.WebSocketDefaultListener;
import org.cheetah.webserver.websocket.defaults.WebSocketDefaultService;
import org.cheetah.webserver.websocket.defaults.WebSocketService;
import org.simpleframework.http.Request;
import org.simpleframework.http.socket.Frame;
import org.simpleframework.http.socket.Session;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public abstract class AbstractWebSocketServerWorker<RequestObject extends Serializable, ResponseObject extends Serializable>  implements Runnable{

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractWebSocketServerWorker.class);

    public abstract void processString(String responseString);

    public abstract void processObject(ResponseObject responseObject);

    private Frame frame;

    protected String responseString;
    protected ResponseObject responseObject;
    protected WebSocketDefaultService webSocketService;
    protected Session session;
    protected WebSocketDefaultListener webSocketListener;
    

    public final void setFrame(Frame frame) {
        this.frame = frame;

        switch (frame.getType()) {

            case TEXT:

                responseString = frame.getText();
                break;

            case BINARY:

                ByteArrayInputStream bis = new ByteArrayInputStream(frame.getBinary());
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
                break;
        }
    }

    @Override
    public final void run() {

        switch (frame.getType()) {

            case TEXT:

                processString(responseString);
                break;

            case BINARY:

                processObject(responseObject);
                break;
        }
    }

    public WebSocketService getWebSocketService() {
        return webSocketService;
    }

    public void setWebSocketService(WebSocketDefaultService webSocketService) {
        this.webSocketService = webSocketService;
    }

    public Session getSession() {
        return session;
    }

    public void setRequest(Session session) {
        this.session = session;
    }

    public WebSocketDefaultListener getWebSocketListener() {
        return webSocketListener;
    }

    public void setWebSocketListener(WebSocketDefaultListener webSocketListener) {
        this.webSocketListener = webSocketListener;
    }
    
    
}
