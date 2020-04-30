/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import org.cheetah.webserver.websocket.client.CheckWebSocketSynchronizedObject;
import org.cheetah.webserver.websocket.client.CheckWebSocketSynchronizedText;
import org.simpleframework.http.core.Controller;

import javax.net.ssl.SSLHandshakeException;
import java.nio.file.Paths;

public final class MainJWT {

    static {
        System.setProperty("logback.configurationFile", "etc_test/logback_debug.xml");
    }

    static String notfound = "";

    public static void main(String[] args) {

        CheetahWebserver webserver = new CheetahWebserver(Paths.get("etc_test/JWT_webserver.properties"));
        webserver.setStopStrategy(Controller.STOP_STRATEGY.KILL);
        webserver.setPrintURLResolvingTraces(true);

        int port = webserver.getNetworkPort();
        String hostname = webserver.getNetworkInterface();

        if (webserver.isSessionAuthenticationEnabled()) {

            new CheckRequestJWT("JWT_HS256 - AuthenticationGranted", hostname, port, "TOKEN", "PASS", "/", false, 200, notfound);



        }


        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
        }

        webserver.stop();
        //       webserver.setWwwRoot("www");
        //webserver.printProperties();

        //     CheetahWebserver webserver2 = new CheetahWebserver(hostname, 9090, ssl, websocket);
        //     CheetahWebserver webserver2 = new CheetahWebserver();
        //     webserver2.setWwwRoot("www");
        //     webserver.printProperties();
    }

    /*
    
     chrome://view-response/content/view-response.html?method=GET&uri=https%3A%2F%2F192.168.1.4%3A8080%2F&headers=&body=login.username%3Dtoto
    
     */
}
