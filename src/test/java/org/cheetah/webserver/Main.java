/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.nio.file.Paths;
import javax.net.ssl.SSLHandshakeException;
import org.cheetah.webserver.websocket.client.CheckWebSocketSynchronizedObject;
import org.cheetah.webserver.websocket.client.CheckWebSocketSynchronizedText;
import org.simpleframework.http.core.Controller;

public final class Main {

    static {
        System.setProperty("logback.configurationFile", "etc_test/logback_debug.xml");
    }

    public static void main(String[] args) {

        //   boolean ssl = true;
        //     String hostname = "localhost";
        /*
         String hostname = "192.168.1.164";
         int port = 8080;
         boolean websocket = true;
         */
 /*
         try {
         java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
         hostname = addr.getHostAddress();
         } catch (Exception e) {

         }*/
        //CheetahWebserver webserver = new CheetahWebserver("localhost", 8080, false, true);
        CheetahWebserver webserver = new CheetahWebserver(Paths.get("etc_test/VIRTUALHOST_SYMLINK_WEBSOCKET_SSL_webserver.properties"));
        webserver.setStopStrategy(Controller.STOP_STRATEGY.KILL);

        int port = webserver.getNetworkPort();
        boolean sslEnabled = webserver.isNetworkSecureSSL();
        String hostname = webserver.getNetworkInterface();

        webserver.updateWebserverVirtualHosts("etc_test/virtualhosts.properties");

        String virtual = "virtual ";

        hostname = "test.org";
        //new CheckRequest("22- Page white space", hostname, port, "/white space page.html", sslEnabled, 200, "white space page.htm");

        try {
            new CheckWebSocketSynchronizedText("11- CheckWebSocketSynchronizedText", hostname, port, "/websocket/Chat", sslEnabled, 101, virtual + "test websocket");
        } catch (SSLHandshakeException ex) {

            System.err.println("Fail : 11- CheckWebSocketSynchronizedText: " + ex.toString());

        }
        new CheckWebSocketSynchronizedObject("11- CheckWebSocketSynchronizedObject", hostname, port, "/websocket/Chat", sslEnabled, 101, virtual + "test websocket");

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
