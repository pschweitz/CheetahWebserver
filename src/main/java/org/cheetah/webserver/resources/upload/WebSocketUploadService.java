/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.resources.upload;

import org.cheetah.webserver.websocket.defaults.*;
import java.io.IOException;
import java.text.DecimalFormat;
import org.cheetah.webserver.page.admin.Upload;
import static org.cheetah.webserver.page.admin.Upload.uploadInformationMap;
import org.json.JSONObject;
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
public class WebSocketUploadService extends WebSocketDefaultService {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketUploadService.class);

    private static UploadThread uploadThread;

    private class UploadThread extends Thread {

        private boolean enabled = true;

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void run() {
            int i = 0;

            JSONObject response = new JSONObject();
            while (enabled) {

                logger.debug("WEBSOCKET UPLOAD");

                for (UploadInformation uploadInformation : Upload.uploadInformationMap.values()) {
                    float percent = 0;
                    if (uploadInformation.finished) {
                        uploadInformationMap.remove(uploadInformation.destinationFile);
                        percent = 100;
                    } else {
                        percent = Float.parseFloat(String.valueOf(uploadInformation.fileSent)) * 100.0f;
                        percent = percent / Float.parseFloat(String.valueOf(uploadInformation.fileSize));
                    }
                    DecimalFormat df = new DecimalFormat();
                    df.setMaximumFractionDigits(2);

                    JSONObject status = new JSONObject();
                    status.put("destinationFile", uploadInformation.destinationFile);
                    status.put("fileSize", uploadInformation.fileSize);
                    status.put("fileSent", uploadInformation.fileSent);
                    status.put("percent", df.format(percent));
                    response.put("MessageType", "Status");
                    response.put("Status", status);

                    Frame replay = new DataFrame(FrameType.TEXT, response.toString());
                    send(replay, uploadInformation.user);

                    if (uploadInformation.finished) {

                        response = new JSONObject();
                        
                        String errorMessage = uploadInformation.errorMessage;
                        
                        if(errorMessage == null){
                            errorMessage = "";
                        }
                        
                        if (!errorMessage.equals("")) {
                            response.put("MessageType", "Error");
                            response.put("errorMessage", errorMessage);
                        } else {                            
                            response.put("MessageType", "Redirect");
                            response.put("Location", uploadInformation.referer);
                        }

                        replay = new DataFrame(FrameType.TEXT, response.toString());
                        send(replay, uploadInformation.user);
                    }
                }


                /*
                OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
                for (Method method : operatingSystemMXBean.getClass().getDeclaredMethods()) {
                    method.setAccessible(true);
                    if (method.getName().equals("getSystemCpuLoad")
                            && Modifier.isPublic(method.getModifiers())) {
                        Object value;
                        try {
                            value = method.invoke(operatingSystemMXBean);
                        } catch (Exception e) {
                            value = e;
                        }
                        i = (int) (Float.parseFloat(value.toString()) * 100);
                        logger.debug(method.getName() + " = " + value);
                    }
                }

                distribute(String.valueOf(i++));
                 */
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    @Override
    public void connect(Session connection) {

        if (uploadThread == null) {
            uploadThread = new UploadThread();
            uploadThread.start();
        }

        logger.debug("CONNECT");

        FrameChannel socket = connection.getChannel();

        String user = listener.getUserName(connection);
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

    /*
    @Override
    public void join(String secWebSocketKey, String user, FrameChannel operation) {
        logger.debug("JOIN: " + user);
        sockets.put(secWebSocketKey, operation);
        users.put(secWebSocketKey, user);
    }
     */
    @Override
    public void leave(String secWebSocketKey) {
        String user = users.get(secWebSocketKey);
        logger.debug("LEAVE: " + user);

        FrameChannel frameChannel = sockets.get(secWebSocketKey);

        try {
            frameChannel.remove(listener);
        } catch (IOException ex) {
            logger.error("Error closing session for user: " + user, ex);
        }
        sockets.remove(secWebSocketKey);
        users.remove(secWebSocketKey);

        if (sockets.size() == 0) {
            if (uploadThread != null) {
                uploadThread.setEnabled(false);
                uploadThread = null;
            }
        }

        System.gc();
    }
    /*

    @Override
    public void distribute(String text) {
        logger.debug(text);
        Frame replay = new DataFrame(FrameType.TEXT, text);
        distribute(replay);
    }

    @Override
    public synchronized void distribute(Frame frame) {
        logger.debug("DISTRIBUTE");

        try {
            Enumeration<String> keys = users.keys();

            while (keys.hasMoreElements()) {
                String secWebSocketKey = keys.nextElement();
                FrameChannel operation = sockets.get(secWebSocketKey);
                String user = users.get(secWebSocketKey);

                try {
                    logger.debug("SEND TO: " + user);

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

    public synchronized void send(Frame frame, String destinationUser) {

        try {
            for (String secWebSocketKey : users.keySet()) {

                String user = users.get(secWebSocketKey);
                if (destinationUser.equals(user)) {

                    FrameChannel operation = sockets.get(secWebSocketKey);

                    try {

                        logger.debug("SEND TO: " + user);

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
     */
}
