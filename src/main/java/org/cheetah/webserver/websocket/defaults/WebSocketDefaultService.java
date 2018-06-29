/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.websocket.defaults;

import java.io.IOException;
import java.util.Enumeration;
import org.simpleframework.http.socket.CloseCode;
import org.simpleframework.http.socket.DataFrame;
import org.simpleframework.http.socket.Frame;
import org.simpleframework.http.socket.FrameChannel;
import org.simpleframework.http.socket.FrameType;
import org.simpleframework.http.socket.Reason;
import org.simpleframework.http.socket.Session;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phs
 */
public class WebSocketDefaultService extends WebSocketService {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketDefaultService.class);

    @Override
    public void connect(Session connection) {


        FrameChannel socket = connection.getChannel();

        String user = listener.getUserName(connection);
        logger.debug("Websocket CONNECT: " + user);
        
        String secWebSocketKey = connection.getRequest().getValue("Sec-WebSocket-Key");

        if (!users.containsKey(secWebSocketKey)) {
            try {
                socket.register(listener);
                join(secWebSocketKey, user, socket);

            } catch (Exception e) {
                logger.error("Error to register to websocket: " + user + ": ", e);
            }
        } else {

            try {
                socket.close(new Reason(CloseCode.ABNORMAL_CLOSURE));
            } catch (IOException ex) {
            }
            logger.error("MALICIOUS trial of Sec-WebSocket-Key usurpation for user: " + users.get(secWebSocketKey) + " with key: " + secWebSocketKey + " from: " + connection.getRequest().getClientAddress().getHostName());
        }
    }

    @Override
    public void join(String secWebSocketKey, String user, FrameChannel operation) {
        logger.debug("Websocket JOIN: " + user);
        sockets.put(secWebSocketKey, operation);
        users.put(secWebSocketKey, user);
    }

    @Override
    public void leave(String secWebSocketKey) {
        String user = users.get(secWebSocketKey);
        logger.debug("Websocket LEAVE: " + user);

        FrameChannel frameChannel = sockets.get(secWebSocketKey);

        try {
            frameChannel.remove(listener);
        } catch (IOException ex) {
            logger.error("Error closing session for user: " + user, ex);
        }
        sockets.remove(secWebSocketKey);
        users.remove(secWebSocketKey);

        System.gc();
    }

    @Override
    public void distribute(String text) {
        Frame replay = new DataFrame(FrameType.TEXT, text);
        distribute(replay);
    }

    @Override
    public synchronized void distribute(Frame frame) {
        logger.debug("Websocket distribute " + WebSocketDefaultService.class.getSimpleName());

        try {

            Enumeration<String> keys = users.keys();

            while (keys.hasMoreElements()) {
                String secWebSocketKey = keys.nextElement();
                FrameChannel operation = sockets.get(secWebSocketKey);
                String user = users.get(secWebSocketKey);

                try {
                    logger.debug("Websocket send to: " + user);

                    operation.send(frame);
                } catch (Exception e) {

                    logger.error("Error to send to websocket: " + user, e);

                    sockets.remove(secWebSocketKey);
                    users.remove(secWebSocketKey);
                    try {
                        operation.close();
                    } catch (Exception e1) {
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error to distribute to websocket", e);
        }
    }

    @Override
    public void send(String text, String destinationUser) {
        Frame replay = new DataFrame(FrameType.TEXT, text);
        send(replay, destinationUser);
    }

    public synchronized void send(Frame frame, String destinationUser) {

        try {
            for (String secWebSocketKey : users.keySet()) {

                String user = users.get(secWebSocketKey);
                if (destinationUser.equals(user)) {

                    FrameChannel operation = sockets.get(secWebSocketKey);

                    try {

                        logger.debug("Websocket send to: " + user);

                        operation.send(frame);
                    } catch (Exception e) {

                        logger.error("Error to send to websocket: " + user, e);

                        sockets.remove(user);
                        users.remove(user);
                        try {
                            operation.close();
                        } catch (Exception e1) {
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error to distribute to websocket", e);
        }
    }
}
