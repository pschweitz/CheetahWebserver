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
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
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
public class WebSocketClient<WebSocketClientWorkerImpl extends AbstractWebSocketClientWorker> {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketClient.class);

    private Socket socket;
    private StreamCursor cursor;
    private FrameConsumer consumer;

    private ThreadResponse threadResponse;

    public String host;
    public int port;
    public String username;
    private String password;
    public String URI;
    public boolean sslEnabled;
    public boolean sslEnforceValidation;

    private ConcurrentLinkedQueue<Long> cookieQueue = new ConcurrentLinkedQueue();

    private WebSocketClient() {
    }

    public WebSocketClient(String host, int port, String URI, boolean sslEnabled, WebSocketClientWorkerImpl webSocketClientWorker) {
        this(host, port, "", "", URI, sslEnabled, false, webSocketClientWorker);
    }

    public WebSocketClient(String host, int port, String URI, boolean sslEnabled, boolean sslEnforceValidation, WebSocketClientWorkerImpl webSocketClientWorker) {
        this(host, port, "", "", URI, sslEnabled, sslEnforceValidation, webSocketClientWorker);
    }

    public WebSocketClient(String host, int port, String username, String password, String URI, boolean sslEnabled, WebSocketClientWorkerImpl webSocketClientWorker) {
        this(host, port, username, password, URI, sslEnabled, false, webSocketClientWorker);
    }

    public WebSocketClient(String host, int port, String username, String password, String URI, boolean sslEnabled, boolean sslEnforceValidation, WebSocketClientWorkerImpl webSocketClientWorker) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.URI = URI;
        this.sslEnabled = sslEnabled;
        this.sslEnforceValidation = sslEnforceValidation;

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
                        logger.debug("response: \n " + response);
                        break;
                    }
                }

                logger.debug("response.getDescription() :" + response.getDescription());
                logger.debug("response.getCode() :" + response.getCode());
            }

        } catch (Exception e) {
            logger.error("Error openning WebSocket", e);
        }

        threadResponse = new ThreadResponse(webSocketClientWorker, this);
        threadResponse.start();
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

    public long lock() {
        logger.trace("START lock()");

        Long lockCookie = new Random().nextLong();
        cookieQueue.add(lockCookie);

        if (cookieQueue.peek() != null) {

            if (!lockCookie.equals(cookieQueue.peek())) {
                logger.trace("Thread " + lockCookie + "           wait");

                synchronized (lockCookie) {
                    try {
                        lockCookie.wait();
                    } catch (InterruptedException e) {

                        // Remove of the lockCookie inside the queue if waiting Thread is interrupted.
                        // Otherwise there will be zombie Thread attempts remaining in queue which will 
                        // never unlock access for new comming Threads.
                        cookieQueue.remove(lockCookie);
                        logger.error("Thread " + lockCookie + "           interrupted while waiting for lockCookie", e);
                    }
                }
            } else {
                logger.trace("Thread " + lockCookie + "      gets lock");
            }
        }

        logger.trace("END lock()");
        return lockCookie;
    }

    public void unlock(long lockCookie) {
        logger.trace("START unlock(long)");

        logger.trace("Thread " + lockCookie + " releases lock");
        cookieQueue.poll();

        Long lockCookieQueued = cookieQueue.peek();

        if (lockCookieQueued != null) {

            synchronized (lockCookieQueued) {

                logger.trace("Thread " + lockCookieQueued + " gets lock");
                lockCookieQueued.notify();
            }
        }

        logger.trace("END unlock(long)");
    }

    private synchronized void send(Frame frame) {

        Long lockCookie = lock();
        
        try {
            if (socket != null) {

                /*
                while (!threadResponse.firstPING) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                    }
                }
*/

                OutputStream stream = socket.getOutputStream();

                FrameEncoder frameEncoder = new FrameEncoder(stream);
                frameEncoder.encode(frame);
                stream.flush();
            }

        } catch (IOException ex) {
            logger.error("Error sending frame", ex);
        }
        unlock(lockCookie);
    }

    public void sendString(String request) {

        Frame frame;

        frame = new DataFrame(FrameType.TEXT, request);
        send(frame);

    }

    public void sendObject(Serializable request) {

        Frame frame;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] byteArray;

        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(request);
            byteArray = bos.toByteArray();

            frame = new DataFrame(FrameType.BINARY, byteArray);
            send(frame);

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

    protected class ThreadResponse extends Thread {

        private WebSocketClientWorkerImpl webSocketClientWorker = null;
        private WebSocketClient webSocketClient;
        //public boolean firstPING = false;

        public ThreadResponse(WebSocketClientWorkerImpl webSocketClientWorker, WebSocketClient webSocketClient) {
            this.webSocketClientWorker = webSocketClientWorker;
            this.webSocketClient = webSocketClient;
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

                                    logger.debug("Frame Type: " + type.name());

                                    if (type == FrameType.CLOSE) {
                                        break;
                                    }

                                    if (type == FrameType.PING) {

                                        Frame frame = new DataFrame(FrameType.PONG);
                                      //  firstPING = true;
                                        send(frame);
                                    }

                                    if (type == FrameType.TEXT || type == FrameType.BINARY) {

                                        AbstractWebSocketClientWorker webSocketClientWorker = (WebSocketClientWorkerImpl) this.webSocketClientWorker.getClass().newInstance();
                                        webSocketClientWorker.setFrame(frameResponse);
                                        webSocketClientWorker.setWebSocketClient(webSocketClient);

                                        Thread t = new Thread(webSocketClientWorker, webSocketClientWorker.getClass().getSimpleName());
                                        t.start();
                                    }

                                } else {
                                    logger.debug("Frame null");
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

    public void close() {

        logger.debug("Explicit websocket close");

        Frame frame = new DataFrame(FrameType.CLOSE);
        send(frame);

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
        }

        try {
            threadResponse.interrupt();
            socket.close();
        } catch (Exception e) {
            logger.error("Error closing WebSocket", e);
        }
    }
}
