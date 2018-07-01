/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.websocket.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.java_websocket.client.AbstractWebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class WebSocketClient<WebSocketClientWorkerImpl extends AbstractWebSocketClientWorker> {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketClient.class);

    /*
    private Socket socket;
    private StreamCursor cursor;
    private FrameConsumer consumer;
     */
    //private ThreadResponse threadResponse;
    private String host;
    private int port;
    private String username;
    private String password;
    private String URI;
    private boolean sslEnabled;
    private boolean sslEnforceValidation;
    private AbstractWebSocketClientWorker worker;
    protected WebSocketClientImpl client;
    protected WebSocketClient instance;
    
    protected String responseResult;
    protected Serializable responseObject;
    
    private Class<? extends AbstractWebSocketClientWorker> webSocketClientWorker = null;

    private WebSocketClient() {
    }

    public WebSocketClient(String host, int port, String URI, boolean sslEnabled, Class<? extends AbstractWebSocketClientWorker> webSocketClientWorker) {
        this(host, port, "", "", URI, sslEnabled, false, webSocketClientWorker);
    }

    public WebSocketClient(String host, int port, String URI, boolean sslEnabled, boolean sslEnforceValidation, Class<? extends AbstractWebSocketClientWorker> webSocketClientWorker) {
        this(host, port, "", "", URI, sslEnabled, sslEnforceValidation, webSocketClientWorker);
    }

    public WebSocketClient(String host, int port, String username, String password, String URI, boolean sslEnabled, Class<? extends AbstractWebSocketClientWorker> webSocketClientWorker) {
        this(host, port, username, password, URI, sslEnabled, false, webSocketClientWorker);
    }

    public WebSocketClient(String host, int port, String username, String password, String URI, boolean sslEnabled, boolean sslEnforceValidation, Class<? extends AbstractWebSocketClientWorker> webSocketClientWorker) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.URI = URI;
        this.sslEnabled = sslEnabled;
        this.sslEnforceValidation = sslEnforceValidation;
        this.webSocketClientWorker = webSocketClientWorker;

        HashMap<String, String> headers = new HashMap();

        if (!username.equals("")) {
            String bytesEncoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            headers.put("Authorization", "BASIC " + bytesEncoded);
        }

        String protocol = "ws";
        if (sslEnabled) {
            protocol = "wss";
        }
        String uriString = protocol + "://" + host;

        if (port != 80) {
            uriString += ":" + port;
        }
        uriString += URI;

        try {
            URI uri = new URI(uriString);
            this.client = new WebSocketClientImpl(uri, headers);
            this.client.setSocket(getSocket(host, port, sslEnabled));
            this.client.connect();

            long timeoutCount = 30;
            long count = 0;
            while (!this.client.isOpen() && count < timeoutCount) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
                count++;
            }

        } catch (Exception ex) {
            logger.error("Error connexting to WebSocket: '" + uriString + "': " + ex.toString());
        }
        instance = this;
    }

    public boolean isOpen() {
        return client.isOpen();
    }

    private final Socket getSocket(String host, int port, boolean sslEnabled) throws Exception {

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

            if (this.sslEnforceValidation) {
                sc.init(null, null, null);
            } else {
                sc.init(null, trustAllCerts, null);
            }

            final SSLSocketFactory sslSocketFactory = sc.getSocketFactory();

            responseSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);

            String[] cyphers = ((SSLSocket) responseSocket).getEnabledProtocols();

            for (String cypher : cyphers) {
                logger.debug("EnabledProtocols: " + cypher);
            }

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("SSL initialization failed", e);
        }

        return responseSocket;
    }

    public String getURI() {
        return URI;
    }
    
    
    public void sendString(String request) {

        client.send(request);
    }

    public void sendObject(Serializable request) {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] byteArray;

        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(request);
            byteArray = bos.toByteArray();

            client.send(byteArray);

        } catch (IOException ex) {
            logger.error("Error sending Object: " + ex.toString());
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

    public void close() {

        logger.debug("Explicit websocket close");
        client.close();

    }

    private class WebSocketClientImpl extends AbstractWebSocketClient {

        public WebSocketClientImpl(URI serverURI, Map headers) {
            super(serverURI, headers);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            logger.debug("onOpen WebSocketClientImpl");
            // if you plan to refuse connection based on ip or httpfields overload: onWebsocketHandshakeReceivedAsClient
        }

        @Override
        public void onMessage(String message) {
            logger.debug("onMessage String: " + message);
            
            try {
                worker = webSocketClientWorker.newInstance();
                worker.setMessage(message);
                worker.setWebSocketClient(instance);
                Thread t = new Thread(worker, webSocketClientWorker.getSimpleName());
                t.start();
            } catch (Exception ex) {
                logger.error("Error processing Websocker incomming String message: " + ex.toString());
            }

        }

        @Override
        public void onMessage(ByteBuffer blob) {
            logger.debug("onMessage ByteBuffer");

            AbstractWebSocketClientWorker worker;
            try {
                worker = webSocketClientWorker.newInstance();
                worker.setMessage(blob);
                worker.setWebSocketClient(instance);
                Thread t = new Thread(worker, webSocketClientWorker.getSimpleName());
                t.start();
            } catch (Exception ex) {
                logger.error("Error processing Websocker incomming Byte message: " + ex.toString());
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            // The codecodes are documented in class org.java_websocket.framing.CloseFrame            
            logger.debug("onClose Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: " + reason);
        }

        @Override
        public void onError(Exception ex) {
            logger.debug("onError WebSocketClientImpl: " + ex.toString());
            ex.printStackTrace();
            // if the error is fatal then onClose will be called additionally
        }
    }
}
