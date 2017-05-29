/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.websocket.client;

import java.io.Serializable;
import java.net.Socket;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class CheckWebSocketSynchronizedObjectClient<RequestObject extends Serializable, ResponseObject extends Serializable> {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CheckWebSocketSynchronizedObjectClient.class);

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

    private CheckWebSocketSynchronizedObjectClient() {
    }

    public CheckWebSocketSynchronizedObjectClient(String name, String host, int port, String URI, boolean sslEnabled, int code, String result) {
        this(name,host, port, "", "", URI, sslEnabled, false, code, result);
    }

    public CheckWebSocketSynchronizedObjectClient(String name, String host, int port, String URI, boolean sslEnabled, boolean sslEnforceValidation, int code, String result) {
        this(name,host, port, "", "", URI, sslEnabled, sslEnforceValidation, code, result);
    }

    public CheckWebSocketSynchronizedObjectClient(String name, String host, int port, String username, String password, String URI, boolean sslEnabled, int code, String result) {
        this(name,host, port, username, password, URI, sslEnabled, false, code, result);
    }

    public CheckWebSocketSynchronizedObjectClient(String name, String host, int port, String username, String password, String URI, boolean sslEnabled, boolean sslEnforceValidation, int code, String result) {
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

        WebSocketClientSynchronized<String, String> webSocketClientSynchronized = new WebSocketClientSynchronized(host, port, username, password, URI, sslEnabled, sslEnforceValidation);
                      
        String responseResult = webSocketClientSynchronized.synchronizedSendObject(result);
                
                if (responseResult.startsWith(result)) {
                    System.out.println("Pass : " + this.name + " URI: " + URI + ": result: " + result);
                } else {
                    if (responseResult.length() >= result.length()) {
                        System.err.println("Fail : " + this.name + " URI: " + URI + ": result: " + result + " vs " + responseResult.substring(0, result.length()));
                    } else {
                        System.err.println("Fail : " + this.name + " URI: " + URI + ": result: " + result);
                    }
                }        
    }
}
