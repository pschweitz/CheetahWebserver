/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page.admin;

import org.cheetah.webserver.Page;
import java.net.URL;
import java.nio.charset.Charset;
import org.cheetah.webserver.AbstractPageDefault;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author philou
 */
public class Logoff extends Page {

    private static Logger logger = LoggerFactory.getLogger(Logoff.class);

    @Override
    public void handle(Request request, Response response) {

        String sessionCookie = this.webserver.getSessionID(request);

        System.out.println("sessionCookie" + sessionCookie);

        this.webserver.getSessionDirectory().removeSessionData(sessionCookie);

        try {
            response.setValue("Content-Type", "text/html");
            response.setValue("WWW-Authenticate", this.webserver.getSessionAuthenticationScheme() + " realm=" + this.webserver.getWebserverName());

            Cookie newCookie = new Cookie("Message", "");
            newCookie.setExpiry(0);

            if (this.webserver.isNetworkSecureSSL()) {
                newCookie.setSecure(true);
            }
            response.setCookie(newCookie);

            response.setValue("Content-Type", "text/html");

            URL url = this.webserver.getClassLoader().getResource("org/cheetah/webserver/resources/login/logoff.htm");

            logger.debug("url: " + url.toString());

            this.webserver.getDefaultUtilsClass().readTextFileRessource(request, body, url, this.webserver.getClassLoader(), Charset.forName("utf-8"));


        } catch (Exception ex) {

            Status status = Status.INTERNAL_SERVER_ERROR;
            response.setStatus(status);
            logger.error("Error uploading file: " + ex.toString());
            try {
                handleDefaultPage(status, ex, request, response);
            } catch (Exception ex2) {
                debugString.append("Error generating :" + status.getDescription() + ": " + ex2.toString()).append(System.lineSeparator());
                logger.error("Error generating " + status.getDescription() + ": " + ex2.toString());
            }
        }
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
