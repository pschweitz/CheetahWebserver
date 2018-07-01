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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
public class WebSocketClientSynchronized<RequestObject extends Serializable, ResponseObject extends Serializable> {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketClientSynchronized.class);

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
    protected WebSocketClientSynchronized instance;

    protected String responseResult;
    protected Serializable responseObject;

    private BlockingQueue<ResponseObject> responseQueue = new LinkedBlockingQueue();

    private WebSocketClientSynchronized() {
    }

    public WebSocketClientSynchronized(String host, int port, String URI, boolean sslEnabled) {
        this(host, port, "", "", URI, sslEnabled, false);
    }

    public WebSocketClientSynchronized(String host, int port, String URI, boolean sslEnabled, boolean sslEnforceValidation) {
        this(host, port, "", "", URI, sslEnabled, sslEnforceValidation);
    }

    public WebSocketClientSynchronized(String host, int port, String username, String password, String URI, boolean sslEnabled) {
        this(host, port, username, password, URI, sslEnabled, false);
    }

    public WebSocketClientSynchronized(String host, int port, String username, String password, String URI, boolean sslEnabled, boolean sslEnforceValidation) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.URI = URI;
        this.sslEnabled = sslEnabled;
        this.sslEnforceValidation = sslEnforceValidation;

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

    public synchronized String synchronizedSendString(String request) {

        String result = null;

        try {
            result = synchronizedSendString(request, 0);

        } catch (TimeoutException ex) {
            logger.error("Error reading response String: " + ex.toString());
        }

        return result;
    }

    public synchronized String synchronizedSendString(String request, long timeout) throws TimeoutException {
        String result = null;

        client.send(request);

        if (timeout > 0) {
            try {
                result = (String) responseQueue.poll(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
            }
            if (result == null) {
                throw new TimeoutException();
            }

        } else {
            try {
                result = (String) responseQueue.take();
            } catch (InterruptedException ex) {
            }
        }

        return result;
    }

    public synchronized ResponseObject synchronizedSendObject(RequestObject request) {

        ResponseObject result = null;

        try {
            result = synchronizedSendObject(request, 0);

        } catch (TimeoutException ex) {
            logger.error("Error reading response Object", ex);
        }

        return result;
    }

    public synchronized ResponseObject synchronizedSendObject(RequestObject request, long timeout) throws TimeoutException {
        ResponseObject result = null;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] byteArray;

        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(request);
            byteArray = bos.toByteArray();

            client.send(byteArray);

            if (timeout > 0) {
                try {
                    result = responseQueue.poll(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                }
                if (result == null) {
                    throw new TimeoutException();
                }

            } else {
                try {
                    result = responseQueue.take();
                } catch (InterruptedException ex) {
                }
            }

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

        return result;
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

            responseQueue.add((ResponseObject) message);

        }

        @Override
        public void onMessage(ByteBuffer blob) {
            logger.debug("onMessage ByteBuffer");

            ByteArrayInputStream bis = new ByteArrayInputStream(blob.array());
            ObjectInput in = null;
            try {
                in = new ObjectInputStream(bis);
                responseQueue.add((ResponseObject) in.readObject());

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
