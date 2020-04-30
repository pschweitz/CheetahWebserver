/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page.admin;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import org.cheetah.webserver.Page;
import java.util.StringTokenizer;
import org.cheetah.webserver.AbstractPageDefault;
import org.cheetah.webserver.authentication.IAuthenticator;
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
public class ChangePassword extends Page {

    private static Logger logger = LoggerFactory.getLogger(ChangePassword.class);

    @Override
    public void handle(Request request, Response response) {

        boolean result = false;
        boolean firstCall = true;

        String message = "";
        String oldPassword = "";
        String newPassword = "";
        String primaryReferer = "/";

        StringTokenizer tokenizer = new StringTokenizer(request.toString(), "\r\n");

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            String[] elements = line.split(":");

            if (elements.length > 0) {

                if (elements[0].equals("oldPassword")) {
                    if (elements.length > 1) {
                        oldPassword = new String(Base64.getDecoder().decode(elements[1].substring(1).getBytes()));
                    }

                } else if (elements[0].equals("newPassword")) {
                    if (elements.length > 1) {
                        newPassword = new String(Base64.getDecoder().decode(elements[1].substring(1).getBytes()));
                    }
                } else if (elements[0].equals("PrimaryReferer")) {
                    firstCall = false;
                    primaryReferer = line.substring(line.indexOf(":") + 1);
                }
            }
        }

        if (!firstCall) {
            String username = this.webserver.getUsername(request);

            Class authenticatorClass = null;
            if (!this.webserver.getPackageAuthName().equals(this.webserver.getPackageRootName() + ".authentication")) {
                try {
                    authenticatorClass = Class.forName(this.webserver.getPackageAuthName() + "." + this.webserver.getSessionAuthenticationMechanism());

                } catch (Exception e) {
                    try {
                        authenticatorClass = this.webserver.getClassLoader().loadClass(this.webserver.getPackageAuthName() + "." + this.webserver.getSessionAuthenticationMechanism());

                    } catch (Exception e1) {
                        logger.warn("Warning generating Authentication Class: " + e1.toString());
                    }
                }
            }

            try {
                if (authenticatorClass == null) {
                    authenticatorClass = Class.forName(this.webserver.getPackageRootName() + ".authentication." + this.webserver.getSessionAuthenticationMechanism());
                }

                IAuthenticator authenticator = (IAuthenticator) authenticatorClass.newInstance();

                result = authenticator.setPassword(username, oldPassword, newPassword);

            } catch (Exception ex) {
                try {
                    if (authenticatorClass == null) {
                        authenticatorClass = this.webserver.getClassLoader().loadClass(this.webserver.getPackageRootName() + ".authentication." + this.webserver.getSessionAuthenticationMechanism());
                    }

                    IAuthenticator authenticator = (IAuthenticator) authenticatorClass.newInstance();

                    result = authenticator.setPassword(username, oldPassword, newPassword);

                } catch (Exception ex1) {
                    logger.error("Error generating Authentication Class: " + ex1.toString());
                    body.close();
                }
            }

            if (!result) {
                message = "Password change not sucessful !";
            }

            response.setValue("Content-Type", "text/html");
            Cookie newCookie = new Cookie("Message", message);
            newCookie.setExpiry(10);

            if (this.webserver.isNetworkSecureSSL()) {
                newCookie.setSecure(true);
            }
            response.setCookie(newCookie);
            logger.debug("primaryReferer: " + primaryReferer);
            body.append(primaryReferer);
        } else {

            try {

                if (Files.exists(Paths.get(this.webserver.getFileRoot(request) + "/admin/changePassword.htm"))) {
                    this.webserver.getDefaultUtilsClass().readTextFile(request, body, Paths.get(this.webserver.getFileRoot(request) + "/admin/changePassword.htm").toUri().toURL(), Charset.forName("utf-8"));
                } else {

                    URL url = this.webserver.getClassLoader().getResource("org/cheetah/webserver/resources/admin/changePassword.htm");

                    logger.debug("url: " + url.toString());

                    this.webserver.getDefaultUtilsClass().readTextFileRessource(request, body, url, this.webserver.getClassLoader(), Charset.forName("utf-8"));

                }

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
