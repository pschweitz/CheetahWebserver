/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.websocket.client;

import java.io.Serializable;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class CheckWebSocketTextClient<RequestObject extends Serializable, ResponseObject extends Serializable> {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CheckWebSocketTextClient.class);

    private Socket socket;

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

    private CheckWebSocketTextClient() {
    }

    public CheckWebSocketTextClient(String name, String host, int port, String URI, boolean sslEnabled, int code, String result) {
        this(name, host, port, "", "", URI, sslEnabled, false, code, result);
    }

    public CheckWebSocketTextClient(String name, String host, int port, String URI, boolean sslEnabled, boolean sslEnforceValidation, int code, String result) {
        this(name, host, port, "", "", URI, sslEnabled, sslEnforceValidation, code, result);
    }

    public CheckWebSocketTextClient(String name, String host, int port, String username, String password, String URI, boolean sslEnabled, int code, String result) {
        this(name, host, port, username, password, URI, sslEnabled, false, code, result);
    }

    public CheckWebSocketTextClient(String name, String host, int port, String username, String password, String URI, boolean sslEnabled, boolean sslEnforceValidation, int code, String result) {
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

        WebSocketClient webSocketClient = new WebSocketClient(host, port, username, password, URI, sslEnabled, sslEnforceValidation, WebSocketClientWorkerTextTest.class);

        webSocketClient.sendString(result);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
        }
        webSocketClient.close();
    }
}
