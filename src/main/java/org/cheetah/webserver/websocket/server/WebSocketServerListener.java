package org.cheetah.webserver.websocket.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import org.cheetah.webserver.websocket.defaults.WebSocketDefaultListener;
import org.cheetah.webserver.websocket.defaults.WebSocketDefaultService;
import org.simpleframework.http.socket.*;

import org.slf4j.LoggerFactory;

class WebSocketServerListener<RequestObject extends Serializable, ResponseObject extends Serializable> extends WebSocketDefaultListener {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketServerListener.class);

    AbstractWebSocketServerWorker webSocketServerWorker;

    public WebSocketServerListener(WebSocketDefaultService webSocketService, AbstractWebSocketServerWorker webSocketServerWorker) {
        super(webSocketService);
        this.webSocketServerWorker = webSocketServerWorker;
    }

    @Override
    public void onFrame(Session session, Frame frame) {

        String user = getUserName(session);

        FrameType type = frame.getType();
        Frame replay = null;

        if (type == FrameType.PONG || type == FrameType.CLOSE) {
            return;
        }

        logger.debug("SERVER onFrame type: " + type + " from user: " + user);

        if (type == FrameType.TEXT) {
            
            try {

                String requeststring = frame.getText();

                AbstractWebSocketServerWorker webSocketServerWorker = this.webSocketServerWorker.getClass().newInstance();
                webSocketServerWorker.setFrame(frame);
                webSocketServerWorker.setRequest(session);
                webSocketServerWorker.setWebSocketService(this.webSocketService);
                webSocketServerWorker.setWebSocketListener(this);

                Thread t = new Thread(webSocketServerWorker);
                t.start();

                /*
                 Thread t = new Thread(webSocketClientWorker, webSocketClientWorker.getClass().getSimpleName());
                 t.start();
                 */
                /// DO WORK
                //      response = haha();
                ///
                /*
                 if (response != null) {
                 replay = new DataFrame(FrameType.TEXT, response);
                 }*/
            } catch (Exception e) {
                logger.error("Error creating text replay frame", e);
            }
        }

        if (type == FrameType.BINARY) {
            
            try {

                RequestObject requestObject = null;

                ByteArrayInputStream bis = new ByteArrayInputStream(frame.getBinary());
                ObjectInput in = null;
                try {
                    in = new ObjectInputStream(bis);
                    requestObject = (RequestObject) in.readObject();

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

                    ResponseObject response = null;

                    AbstractWebSocketServerWorker webSocketServerWorker = this.webSocketServerWorker.getClass().newInstance();
                    webSocketServerWorker.setFrame(frame);
                    webSocketServerWorker.setRequest(session);
                    webSocketServerWorker.setWebSocketService(this.webSocketService);
                    webSocketServerWorker.setWebSocketListener(this);

                    Thread t = new Thread(webSocketServerWorker);
                    t.start();

                    /// DO WORK
                    //      response = haha();
                    ///
                    //    response = (ResponseObject) requestObject;
/*
                     ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ObjectOutput out = null;
                     byte[] byteArray = new byte[0];

                     try {
                     out = new ObjectOutputStream(bos);
                     out.writeObject(response);
                     byteArray = bos.toByteArray();

                     if (response != null) {
                     replay = new DataFrame(FrameType.BINARY, byteArray);
                     }

                     } catch (IOException ex) {

                     //create Response error !!! 
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
                     */
                }

            } catch (Exception e) {
                logger.error("Error creating binary replay frame", e);
            }
        }
    }
}
