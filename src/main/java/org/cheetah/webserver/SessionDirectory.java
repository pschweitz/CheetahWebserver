/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.cheetah.webserver.authentication.AbstractAuthenticator;
import org.simpleframework.http.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phs
 */
public class SessionDirectory {

    private static Logger logger = LoggerFactory.getLogger(SessionDirectory.class);

    //private static final SessionDirectory instance = new SessionDirectory();
    private final Hashtable<String, SessionData> sessionDatas = new Hashtable();

    public SessionDirectory(CheetahWebserver webserver) {

        Thread t = new Thread("SessionDirectoryClean") {
            @Override
            public void run() {

                while (true) {

                    try {
                        Thread.sleep(1000 * 60); // every minute
                    } catch (InterruptedException ex) {
                    }

                    for (SessionData sessionData : sessionDatas.values()) {
                        Date lastUseTime = sessionData.getLastUseTime();
                        Date now = new Date();

                        long diff = now.getTime() - lastUseTime.getTime();
                        long timeoutTime = webserver.getSessionTimeout() * 60 * 1000;

                        if (diff > timeoutTime) {
                            removeSessionData(sessionData.getCookie());
                        }
                    }

                }
            }
        };
        t.start();
    }

    /*
    public static SessionDirectory getInstance() {
        return instance;
    }
     */
    public SessionData getSessionData(String cookie) {
        return sessionDatas.get(cookie);
    }

    public void addSessionData(String cookie, String username, String ipAddress, String userAgent, Object SessionObject) {
        sessionDatas.put(cookie, new SessionData(cookie, username, ipAddress, userAgent, SessionObject));
    }

    public void removeSessionData(String cookie) {

        if (sessionDatas.containsKey(cookie)) {
            sessionDatas.remove(cookie);
        }
    }

    public void removeAllSessionDataExceptAdmin(ArrayList<String> adminList) {

        Hashtable<String, SessionData> sessionDatasAdmins = new Hashtable();

        for (String admin : adminList) {
            String cookie = getCookie(admin);
            SessionData adminSession = sessionDatas.get(cookie);

            if (adminSession != null) {
                sessionDatasAdmins.put(cookie, adminSession);
            }

        }

        sessionDatas.clear();

        for (Entry<String, SessionData> entry : sessionDatasAdmins.entrySet()) {
            sessionDatas.put(entry.getKey(), entry.getValue());
        }
    }

    public void removeAllSessionData() {
        sessionDatas.clear();
    }

    public String getUsername(String cookie) {
        String result = "";

        if (sessionDatas.containsKey(cookie)) {
            result = sessionDatas.get(cookie).getUsername();
        }

        return result;
    }

    public String getCookie(String username) {
        String result = "";

        for (String key : sessionDatas.keySet()) {
            if (sessionDatas.get(key).getUsername().equals(username)) {
                result = key;
                break;
            }
        }

        return result;
    }

    public boolean isExistingSession(String cookie) {

        return sessionDatas.containsKey(cookie);
    }

    public boolean isExistingSession(Request request) {

        boolean result = false;
        String username = AbstractAuthenticator.getCredentials(request).getKey();

        /*
        String userName = "";

        StringTokenizer tokenizer = new StringTokenizer(request.toString(), "\r\n");

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            String[] elements = line.split(":");

            if (elements.length > 0) {

                if (elements[0].equals("Authorization")) {
                    if (line.length() > "Authorization: ".length()) {
                        userName = elements[1].substring("Basic".length() + 2);
                    }

                    break;
                }
            }
        }

        String username = "";
        if (!userName.equals("")) {

            byte[] bytesEncoded = userName.getBytes();

            byte[] valueDecoded = Base64.getDecoder().decode(bytesEncoded);
            String[] credentials = new String(valueDecoded).split(":");

            if (credentials.length > 0) {
                username = credentials[0];
            }
        }
         */
        if (!username.equals("")) {
            for (SessionData sessionData : sessionDatas.values()) {

                if (sessionData.getUsername().equals(username)) {
                    result = sessionData.isAuthenticated();
                    break;
                }
            }
        }

        return result;
    }

    public Enumeration<SessionData> elements() {
        return sessionDatas.elements();
    }
}
