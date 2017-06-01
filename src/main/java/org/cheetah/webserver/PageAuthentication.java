/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import org.cheetah.webserver.authentication.IAuthenticator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.cheetah.webserver.authentication.AbstractAuthenticator;
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
public class PageAuthentication extends Page {

    private static final Logger logger = LoggerFactory.getLogger(PageAuthentication.class);
    private ConcurrentHashMap<String, PageAuthentication.BruteForceData> ipAttempt;

    public class BruteForceData {

        public int nbAttempts;
        public Date lastAttemptDate;

    }

    @Override
    public void handle(Request request, Response response) {

        ipAttempt = this.webserver.getIpAttempt();

        String userAgent = "";
        String ipAddress = request.getClientAddress().toString().split(":")[0].substring(1);

        String sessionCookie = "";
        boolean askAuthentication = true;
        /*
        String userName = "";

        StringTokenizer tokenizer = new StringTokenizer(request.toString(), "\r\n");


        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            String[] elements = line.split(":");

            //    body.println("<h1> elements[0]T: " + elements[0] + "</h1>");
            if (elements.length > 0) {

                if (elements[0].equals("User-Agent")) {

                    for (int i = 1; i < elements.length; i++) {
                        userAgent += ":" + elements[i];
                    }
                } else if (elements[0].equals("Authorization")) {
                    if (line.length() > "Authorization: ".length()) {
                        userName = elements[1].substring("Basic".length() + 2);
                    }
                }
            }
        }
         */
        List<Cookie> cookies = request.getCookies();

        String message = "";

        for (Cookie cookie : cookies) {

            if (cookie.getName().equals("JSESSIONID")) {
                sessionCookie = cookie.getValue();
            }

            if (cookie.getName().equals("Message")) { // && this.webserver.getSessionDirectory().isExistingSession(cookie)) {
                message = cookie.getValue();
            }
        }

        if (sessionCookie.equals("")) {
            sessionCookie = request.getValue("Sec-WebSocket-Key");
        }

        if (sessionCookie == null) {

            cookies = response.getCookies();

            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("JSESSIONID")) {
                    sessionCookie = cookie.getValue();
                }
            }
        }

        if (sessionCookie == null) {
            sessionCookie = "";
        }

        if (this.webserver.getSessionDirectory().isExistingSession(sessionCookie) && !sessionCookie.equals("")) {
            this.debugString.append("isExistingSession: " + sessionCookie).append(System.lineSeparator());

            SessionData sessionData = this.webserver.getSessionDirectory().getSessionData(sessionCookie);

            if (sessionData.isAuthenticated()) {

                if (sessionData.getIpAddress().equals(ipAddress)
                        && sessionData.getUserAgent().equals(userAgent)) {

                    Date lastUseTime = sessionData.getLastUseTime();
                    Date now = new Date();

                    long diff = now.getTime() - lastUseTime.getTime();
                    long timeoutTime = this.webserver.getSessionTimeout() * 60 * 1000;

                    if (diff <= timeoutTime) {

                        this.debugString.append("isAuthenticated: " + true).append(System.lineSeparator());
                        sessionData.setLastUseTime(new Date());
                        sessionData.setNbTry(0);
                        askAuthentication = false;
                    } else {

                        this.webserver.getSessionDirectory().removeSessionData(sessionCookie);
                        this.debugString.append("session timeout: " + sessionCookie).append(System.lineSeparator());
                        message = "Session timed out !";
                    }
                } else {

                    //message = "Problem with your IP address. Please login again !";
                    //malicious activity detected
                    this.webserver.getSessionDirectory().removeSessionData(sessionCookie);
                    this.debugString.append(" !!! MALICIOUS ACTIVITY DETECTED !!! ").append(System.lineSeparator());
                    this.debugString.append("Problem with your IP address. Please login again: " + sessionCookie).append(System.lineSeparator());
                    this.debugString.append("Former address: \"" + sessionData.getIpAddress() + "\" new comming address: \"" + ipAddress + "\"").append(System.lineSeparator());
                    logger.error(" !!! MALICIOUS ACTIVITY DETECTED !!! ");
                    logger.error("Problem with your IP address. Please login again: " + sessionCookie);
                    logger.error("Former address: \"" + sessionData.getIpAddress() + "\" new comming address: \"" + ipAddress + "\"");
                }
            }

        } else {
            this.debugString.append("not isExistingSession: " + sessionCookie).append(System.lineSeparator());

            String username = AbstractAuthenticator.getCredentials(request).getKey();
            String password = AbstractAuthenticator.getCredentials(request).getValue();

            if (username.equals("")) {
                if (request.getParameter("username") != null) {
                    username = request.getParameter("username");
                }
            }

            if (!username.equals("")) {
                /*
                byte[] bytesEncoded = userName.getBytes();

                byte[] valueDecoded = Base64.getDecoder().decode(bytesEncoded);
                String[] credentials = new String(valueDecoded).split(":");

                if (credentials.length > 0) {
                    username = credentials[0];
                }
                if (credentials.length > 1) {
                    password = credentials[1];
                }
                 */

                this.debugString.append("username: " + username).append(System.lineSeparator());
                this.debugString.append("password: ********").append(System.lineSeparator());

                Class authenticatorClass = null;                
                authenticatorClass = this.webserver.getDefaultAuthenticationClass();
                
                if (authenticatorClass == null) {
                    if (!this.webserver.getPackageAuthName().equals(this.webserver.getPackageRootName() + ".authentication")) {
                        try {
                            authenticatorClass = this.webserver.getClass(this.webserver.getPackageAuthName() + "." + this.webserver.getSessionAuthenticationMechanism());

                        } catch (Exception e) {
                            this.debugString.append("Warning generating Authentication Class: " + e.toString()).append(System.lineSeparator());
                            logger.warn("Warning generating Authentication Class: " + e.toString());
                        }
                    }
                }

                try {
                    if (authenticatorClass == null) {
                        authenticatorClass = this.webserver.getClass(this.webserver.getPackageRootName() + ".authentication." + this.webserver.getSessionAuthenticationMechanism());
                    }

                    IAuthenticator authenticator = (IAuthenticator) authenticatorClass.newInstance();

                    BruteForceData brutForceData = null;
                    int attempts = 0;
                    Date lastAttemptDate = new Date();

                    boolean bruteForceDetected = false;

                    if (this.webserver.isSessionEnableBruteForceProtection()) {

                        if (ipAttempt.containsKey(ipAddress)) {
                            brutForceData = ipAttempt.get(ipAddress);
                            attempts = brutForceData.nbAttempts;
                            lastAttemptDate = brutForceData.lastAttemptDate;
                        }

                        this.debugString.append("bruteForceDetected attempts: " + attempts).append(System.lineSeparator());

                        if (attempts != 0 && attempts % 4 == 0) {
                            bruteForceDetected = true;
                        }

                        if (attempts / 4 >= 1.0) {

                            Date now = new Date();
                            long timeToWait = 0;

                            if (attempts % 4 == 0) {

                                int factor = (int) attempts / 4;

                                factor = (int) Math.pow(factor, 3);

                                if (factor > 1440) {
                                    factor = 1440;
                                }

                                timeToWait = factor * 60 * 1000;
                            }

                            if (now.getTime() - lastAttemptDate.getTime() < timeToWait) {
                                bruteForceDetected = true;

                                long remainingTime = lastAttemptDate.getTime() + timeToWait - now.getTime();

                                long second = (remainingTime / 1000) % 60;
                                long minute = (remainingTime / (1000 * 60)) % 60;
                                long hour = (remainingTime / (1000 * 60 * 60)) % 24;

                                String dateFormatted = "";
                                if (hour >= 1.0) {
                                    dateFormatted = String.format("%02dh%02dm%02ds", hour, minute, second);
                                } else {
                                    dateFormatted = String.format("%02dm%02ds", minute, second);
                                }

                                message = "Too many attempts, please try again in " + dateFormatted;
                            } else {
                                bruteForceDetected = false;
                            }

                        }
                        this.debugString.append("bruteForceDetected: " + bruteForceDetected).append(System.lineSeparator());
                    }

                    if (!bruteForceDetected) {

                        Boolean auth = authenticator.authenticate(request);
                        // perform authentication
                        if (auth) {
                            askAuthentication = false;

                            Object sessionObject = authenticator.getSessionObject();

                            if (sessionObject == null) {
                                sessionObject = sessionCookie;
                            }

                            this.webserver.getSessionDirectory().addSessionData(sessionCookie, username, ipAddress, userAgent, sessionObject);
                            this.webserver.getSessionDirectory().getSessionData(sessionCookie).setIsAuthenticated(true);

                            if (ipAttempt.containsKey(ipAddress)) {
                                ipAttempt.remove(ipAddress);
                            }

                            Cookie newCookie = new Cookie("Message", "");
                            newCookie.setExpiry(0);
                            //newCookie.setProtected(true);

                            if (this.webserver.isNetworkSecureSSL()) {
                                newCookie.setSecure(true);
                            }
                            response.setCookie(newCookie);

                        } else {
                            message = "Username or password incorrect !";

                            if (this.webserver.isSessionEnableBruteForceProtection()) {
                                if (ipAttempt.containsKey(ipAddress)) {
                                    brutForceData = ipAttempt.get(ipAddress);
                                    attempts = brutForceData.nbAttempts;
                                    ipAttempt.remove(ipAddress);
                                } else {
                                    brutForceData = new BruteForceData();
                                }
                                attempts++;

                                brutForceData.nbAttempts = attempts;
                                brutForceData.lastAttemptDate = new Date();

                                ipAttempt.put(ipAddress, brutForceData);

                                if (ipAttempt.containsKey(ipAddress)) {
                                    brutForceData = ipAttempt.get(ipAddress);
                                    attempts = brutForceData.nbAttempts;
                                    lastAttemptDate = brutForceData.lastAttemptDate;
                                }

                                if (attempts != 0 && attempts % 4 == 0) {
                                    bruteForceDetected = true;
                                }

                                if (attempts / 4 >= 1.0) {

                                    Date now = new Date();
                                    long timeToWait = 0;

                                    if (attempts % 4 == 0) {

                                        int factor = (int) attempts / 4;

                                        factor = (int) Math.pow(factor, 3);

                                        if (factor > 1440) {
                                            factor = 1440;
                                        }

                                        timeToWait = factor * 60 * 1000;
                                    }

                                    if (now.getTime() - lastAttemptDate.getTime() < timeToWait) {

                                        long remainingTime = lastAttemptDate.getTime() + timeToWait - now.getTime();

                                        long second = (remainingTime / 1000) % 60;
                                        long minute = (remainingTime / (1000 * 60)) % 60;
                                        long hour = (remainingTime / (1000 * 60 * 60)) % 24;

                                        String dateFormatted = "";
                                        if (hour >= 1.0) {
                                            dateFormatted = String.format("%02dh%02dm%02ds", hour, minute, second);
                                        } else {
                                            dateFormatted = String.format("%02dm%02ds", minute, second);
                                        }

                                        message = "Too many attempts, please try again in " + dateFormatted;
                                    }
                                }

                            }
                        }
                    }

                } catch (Exception ex) {
                    this.debugString.append("Error generating Authentication Class: " + ex.toString()).append(System.lineSeparator());
                    logger.error("Error generating Authentication Class", ex);
                    body.close();
                }
            }
        }

        if (askAuthentication) {
            Class lookupPage = null;

            Status status = Status.UNAUTHORIZED;
            response.setStatus(status);

            if (!this.webserver.isSessionUseLoginPage()) {
                response.setValue("WWW-Authenticate", "Basic realm=" + this.webserver.getWebserverName());

                try {

                    handleDefaultPage(status, request, response);

                } catch (Exception ex) {
                    this.debugString.append("Error generating " + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                    logger.error("Error generating " + status.getDescription() + ": ", ex);
                }
            } else {

                try {
                    Cookie newCookie = new Cookie("Message", message);
                    newCookie.setExpiry(10);
                    //newCookie.setProtected(true);

                    if (this.webserver.isNetworkSecureSSL()) {
                        newCookie.setSecure(true);
                    }

                    response.setCookie(newCookie);

                    if (Files.exists(Paths.get(this.webserver.getFileRoot(request) + "/login/login.html"))) {
                        this.webserver.getDefaultUtilsClass().readTextFile(request, body, Paths.get(this.webserver.getFileRoot(request) + "/login/login.html").toUri().toURL(), Charset.forName("utf-8"));
                    } else {

                        try {
                            //lookupPage = this.webserver.getClass(this.webserver.getDefaultLoginPage());
                            lookupPage = this.webserver.getDefaultLoginPageClass();

                            //System.out.println("Case: File exists: is directory: default class plugin found: " + indexPageName);       
                            //debugString.append("Case: File exists: is directory: default class plugin found: " + indexPageName).append(System.lineSeparator());
                        } catch (Exception e1) {

                        }

                        if (lookupPage != null) {

                            Page page;
                            try {
                                page = (Page) lookupPage.newInstance();
                                page.setRessources(body, webserver, debugString);
                                page.handle(request, response);
                            } catch (Exception ex) {
                                status = Status.INTERNAL_SERVER_ERROR;
                                try {
                                    debugString.append("Error generating response:" + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                                    for (StackTraceElement element : ex.getStackTrace()) {
                                        debugString.append("\t" + element.toString()).append(System.lineSeparator());
                                    }
                                    logger.error("Error generating response:" + status.getDescription() + ": " + ex.toString());
                                    handleDefaultPage(status, ex, request, response);

                                } catch (Exception ex2) {
                                    debugString.append("Error generating :" + status.getDescription() + ": " + ex2.toString()).append(System.lineSeparator());
                                    logger.error("Error generating " + status.getDescription() + ": " + ex2.toString());
                                }
                            } finally {
                                body.close();
                            }
                        } else {
                            this.debugString.append("Error login page not found").append(System.lineSeparator());
                            logger.error("Error login page not found");
                            body.close();
                        }
                    }

                } catch (IOException | URISyntaxException ex) {
                    this.debugString.append("Error login page not found: " + ex.toString()).append(System.lineSeparator());
                    logger.error("Error login page not found: ", ex);
                    body.close();
                }
            }
            body.close();
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
