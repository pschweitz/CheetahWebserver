/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page.javascript;

import java.net.URL;
import java.nio.charset.Charset;
import org.cheetah.webserver.Page;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author aberbier
 */
public class WebSocketUpload extends Page {

    private static Logger logger = LoggerFactory.getLogger(WebSocketUpload.class);

    @Override
    public void handle(Request request, Response response) {
        logger.debug("START: Handle Request");

        response.setValue("Content-Type", "application/javascript");
        try {
            URL url = this.webserver.getClassLoader().getResource("org/cheetah/webserver/resources/upload/websocketUpload.js");

            //logger.debug("url: " + url.toString());
            this.webserver.getDefaultUtilsClass().readTextFileRessource(request, body, url, this.webserver.getClassLoader(), Charset.forName("utf-8"));
            
        } catch (Exception ex) {
            response.setStatus(Status.NOT_FOUND);
            logger.error("Error sendinf file: " + ex.toString());
        } 
        logger.debug(" END : Handle Request");
    }
}
