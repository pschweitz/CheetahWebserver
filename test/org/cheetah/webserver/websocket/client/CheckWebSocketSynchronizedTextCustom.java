/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.websocket.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.simpleframework.http.socket.DataFrame;
import org.simpleframework.http.socket.Frame;
import org.simpleframework.http.socket.FrameType;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class CheckWebSocketSynchronizedTextCustom<RequestObject extends Serializable, ResponseObject extends Serializable> {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CheckWebSocketSynchronizedTextCustom.class);

    private Socket socket;
    private StreamCursor cursor;
    private FrameConsumer consumer;

    private String host;
    private int port;
    private String username;
    private String password;
    private String URI;
    private boolean sslEnabled;
    private boolean sslEnforceValidation;

    private int code;
    private String result;
    private String name;

    private CheckWebSocketSynchronizedTextCustom() {
    }

    public CheckWebSocketSynchronizedTextCustom(String name, String host, int port, String URI, boolean sslEnabled, int code, String result) {
        this(name,host, port, "", "", URI, sslEnabled, false, code, result);
    }

    public CheckWebSocketSynchronizedTextCustom(String name, String host, int port, String URI, boolean sslEnabled, boolean sslEnforceValidation, int code, String result) {
        this(name,host, port, "", "", URI, sslEnabled, sslEnforceValidation, code, result);
    }

    public CheckWebSocketSynchronizedTextCustom(String name, String host, int port, String username, String password, String URI, boolean sslEnabled, int code, String result) {
        this(name,host, port, username, password, URI, sslEnabled, false, code, result);
    }

    public CheckWebSocketSynchronizedTextCustom(String name, String host, int port, String username, String password, String URI, boolean sslEnabled, boolean sslEnforceValidation, int code, String result) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.URI = URI;
        this.sslEnabled = sslEnabled;
        this.code = code;
        this.result = result;
        this.name = name;
        this.sslEnforceValidation = sslEnforceValidation;

        createConnection();
    }

    protected void createConnection() {

        String bytesEncoded = "";
        String authorizationHeader = "";

        if (!username.equals("")) {
            bytesEncoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            authorizationHeader = "Authorization: BASIC " + bytesEncoded + "\r\n";
        }

        try {
            socket = getSocket(host, port, sslEnabled);
            if (socket != null) {
                cursor = new StreamCursor(socket.getInputStream());
                consumer = new FrameConsumer();
                ReplyConsumer response = new ReplyConsumer();

                byte[] random = new byte[16];
                Random reuseableRandom = new Random();
                reuseableRandom.nextBytes(random);
                String secWebSocketKey = Base64.getEncoder().encodeToString(random);

                String hostPort;

                if (port != 80 && port != 443) {
                    hostPort = host + ":" + port;
                } else {
                    hostPort = host;
                }

                byte[] request = ("GET " + URI + " HTTP/1.1\r\n"
                        + "Host: " + hostPort + "\r\n"
                        + authorizationHeader
                        + "Upgrade: websocket\r\n"
                        + "Connection: keep-alive, Upgrade\r\n"
                        + "Sec-WebSocket-Key: " + secWebSocketKey + "\r\n"
                        + "Sec-WebSocket-Version: 13\r\n"
                        + "\r\n").getBytes("ISO-8859-1");

                socket.getOutputStream().write(request);

                while (cursor.isOpen()) {
                    response.consume(cursor);

                    if (response.isFinished()) {
                        break;
                    }
                }
                String responseResult = "";
                
                if (response.getCode() == 101) {
                    responseResult = synchronizedSendString(result);
                }
                
                result = secWebSocketKey + "> " + result;
                
                if (code == response.getCode() && responseResult.startsWith(result)) {
                    System.out.println("Pass : " + this.name + " URI: " + URI + ": code :" + code + " vs " + response.getCode() + " ; result: " + result);
                } else {
                    if (responseResult.length() >= result.length()) {
                        System.err.println("Fail : " + this.name + " URI: " + URI + ": code :" + code + " vs " + response.getCode() + " ; result: " + result + " vs " + responseResult.substring(0, result.length()));
                    } else {
                        System.err.println("Fail : " + this.name + " URI: " + URI + ": code :" + code + " vs " + response.getCode() + " ; result: " + result);

                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error openning WebSocket", e);
        }

    }

    protected final Socket getSocket(String host, int port, boolean sslEnabled) throws Exception {

        if (!sslEnabled) {
            return new Socket(host, port);
        }

        Socket responseSocket = null;

        try {

            SSLContext sc;
            sc = SSLContext.getInstance("SSLv3");

            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }};

            if(this.sslEnforceValidation) {
                sc.init(null, null, null);
            }
            else{
                sc.init(null, trustAllCerts, null);
            }
            
            final SSLSocketFactory sslSocketFactory = sc.getSocketFactory();

            responseSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);

            String[] cyphers = ((SSLSocket) responseSocket).getEnabledProtocols();

            for (String cypher : cyphers) {
 //               logger.debug("EnabledProtocols: " + cypher);
            }

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("SSL initialization failed", e);
        }

        return responseSocket;
    }

    private void send(Frame frame) {

 //       logger.debug("Send: " + frame.getType().name());

        try {
            if (socket != null) {
                OutputStream stream = socket.getOutputStream();

                // make synchronization
                synchronized (stream) {
                    FrameEncoder frameEncoder = new FrameEncoder(stream);
                    frameEncoder.encode(frame);
                }
            }

        } catch (IOException ex) {
            logger.error("Error sending frame", ex);
        }
    }

    public synchronized String synchronizedSendString(String request) {

        String result = null;

        try {
            result = synchronizedSendString(request, 0);

        } catch (TimeoutException ex) {
            logger.error("Error reading frame response", ex);
        }

        return result;
    }

    public synchronized String synchronizedSendString(String request, long timeout) throws TimeoutException {
        //      createConnection();
        String response = null;

        if (cursor != null) {
            Frame frame;

            ThreadResponseString t = new ThreadResponseString();
            t.start();

            try {

                while (!t.firstPING) {
                    Thread.sleep(1);
                }

                frame = new DataFrame(FrameType.TEXT, request);
                send(frame);

                if (timeout > 0) {
                    t.join(timeout);
                } else {
                    t.join();
                }

                if (t.response == null) {
                    t.interrupt();
                    throw new TimeoutException();
                }

                response = t.response;

            } catch (InterruptedException ex) {
                logger.error("Error reading frame response", ex);
            }
            close();
        }

        return response;
    }

    public synchronized ResponseObject synchronizedSendObject(RequestObject request) {

        ResponseObject result = null;

        try {
            result = synchronizedSendObject(request, 0);

        } catch (TimeoutException ex) {
            logger.error("Error reading frame response", ex);
        }

        return result;
    }

    public synchronized ResponseObject synchronizedSendObject(RequestObject request, long timeout) throws TimeoutException {
        //    createConnection();
        ResponseObject response = null;

        if (cursor != null) {
            Frame frame;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = null;
            byte[] byteArray;

            try {
                out = new ObjectOutputStream(bos);
                out.writeObject(request);
                byteArray = bos.toByteArray();

                ThreadResponseObject t = new ThreadResponseObject();

                t.start();

                while (!t.firstPING) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                    }
                }

                frame = new DataFrame(FrameType.BINARY, byteArray);
                send(frame);
                try {

                    if (timeout > 0) {
                        t.join(timeout);
                    } else {
                        t.join();
                    }

                    if (t.response == null) {
                        t.interrupt();
                        throw new TimeoutException();
                    }

                    response = t.response;

                } catch (InterruptedException ex) {
                    logger.error("Error reading frame response", ex);
                }

                close();

            } catch (IOException ex) {
                logger.error("Error sending frame", ex);
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
        return response;
    }

    protected class ThreadResponseString extends Thread {

        protected String response = null;
        public boolean firstPING = false;

        public ThreadResponseString() {
            //  this.response = response;
        }

        @Override
        public void run() {
            if (cursor != null) {
                try {
                    if (cursor.isOpen()) {

                        while (true) {
                            consumer.consume(cursor);

                            if (consumer.isFinished()) {
                                Frame frameResponse = consumer.getFrame();

                                if (frameResponse != null) {

                                    FrameType type = frameResponse.getType();

                                    if (type == FrameType.CLOSE) {
                                        break;
                                    }

    //                                logger.debug("Got Frame Type: " + type.name());

                                    if (type == FrameType.PING) {

                                        Frame frame = new DataFrame(FrameType.PONG);
                                        send(frame);
                                        firstPING = true;
                                    }

                                    if (type == FrameType.TEXT) {
                                        this.response = frameResponse.getText();

                                        if (response != null) {
                                            break;
                                        }
                                    }

                                    if (type == FrameType.BINARY) {
  //                                      logger.debug("Waiting for TEXT Frame but got BINARY instead");
                                    }

                                } else {
  //                                  logger.debug("Frame null");
                                }
                                consumer.clear();
                            }
                        }
                    }
                } catch (ClosedByInterruptException e) {
                    //logger.error("Timeout error reading WebSocket, Thread interrupted");
                } catch (Exception e) {
                    logger.error("Error reading WebSocket", e);
                }
            }
        }
    }

    protected class ThreadResponseObject extends Thread {

        protected ResponseObject response = null;
        public boolean firstPING = false;

        public ThreadResponseObject() {
            //this.response = response;
        }

        @Override
        public void run() {
            if (cursor != null) {
                try {
                    if (cursor.isOpen()) {

                        while (true) {
                            consumer.consume(cursor);

                            if (consumer.isFinished()) {
                                Frame frameResponse = consumer.getFrame();

                                if (frameResponse != null) {

                                    FrameType type = frameResponse.getType();

   //                                 logger.debug("Got Frame Type: " + type.name());

                                    if (type == FrameType.CLOSE) {
                                        break;
                                    }

                                    if (type == FrameType.PING) {

                                        Frame frame = new DataFrame(FrameType.PONG);
                                        send(frame);
                                        firstPING = true;
                                    }

                                    if (type == FrameType.TEXT) {
                                        String text = frameResponse.getText();

  //                                      logger.debug("Waiting for BINARY Frame but got TEXT instead: " + text);
                                    }

                                    if (type == FrameType.BINARY) {

                                        ByteArrayInputStream bis = new ByteArrayInputStream(frameResponse.getBinary());
                                        ObjectInput in = null;
                                        try {
                                            in = new ObjectInputStream(bis);
                                            this.response = (ResponseObject) in.readObject();

                                            if (this.response != null) {
                                                break;
                                            }

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
                                } else {
  //                                  logger.debug("Frame null");
                                }
                                consumer.clear();
                            }
                        }
                    }
                } catch (ClosedByInterruptException e) {
                    //logger.error("Timeout error reading WebSocket, Thread interrupted");
                } catch (Exception e) {
                    logger.error("Error reading WebSocket", e);
                }
            }
        }
    }

    protected void close() {

 //       logger.debug("Explicit websocket close");

        Frame frame = new DataFrame(FrameType.CLOSE);
        send(frame);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }

        try {
            socket.close();
        } catch (Exception e) {
            logger.error("Error closing WebSocket", e);
        }
    }
}
