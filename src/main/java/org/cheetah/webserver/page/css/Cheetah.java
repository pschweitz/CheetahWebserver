/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page.css;

import java.net.URL;
import java.nio.charset.Charset;
import org.cheetah.webserver.AbstractPageDefault;
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
public class Cheetah extends Page {

    private static Logger logger = LoggerFactory.getLogger(Cheetah.class);

    @Override
    public void handle(Request request, Response response) {
        logger.debug("START: Handle Request");

        response.setValue("Content-Type", "text/css");

        try {
            URL url = this.webserver.getClassLoader().getResource("org/cheetah/webserver/resources/cheetah.css");

            logger.debug("url: " + url.toString());

            this.webserver.getDefaultUtilsClass().readTextFileRessource(request, body, url, this.webserver.getClassLoader(), Charset.forName("UTF-8"));

        } catch (Exception ex) {

            Status status = Status.INTERNAL_SERVER_ERROR;
            response.setStatus(status);
            logger.error("Error generating page: " + ex.toString());
            try {
                handleDefaultPage(status, ex, request, response);
            } catch (Exception ex2) {
                debugString.append("Error generating :" + status.getDescription() + ": " + ex2.toString()).append(System.lineSeparator());
                logger.error("Error generating " + status.getDescription() + ": " + ex2.toString());
            }
        }
        logger.debug(" END : Handle Request");
    }
    
    private void handleDefaultPage(Status status, Exception e, Request request, Response response) throws Exception {

        Class lookupPage = null;
        //lookupPage = this.webserver.getClass(this.webserver.getDefaultPageHandler());
        lookupPage = this.webserver.getDefaultPageClass();
        AbstractPageDefault pageDefault = (AbstractPageDefault) lookupPage.newInstance();
        pageDefault.setRessources(body, webserver, debugString);
        pageDefault.setStatus(status);
        pageDefault.setException(e);

        pageDefault.handle(request, response);
    }
}
