/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.authentication;

import java.util.AbstractMap;
import java.util.Base64;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import org.simpleframework.http.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ph002551
 */
public abstract class AbstractAuthenticator implements IAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAuthenticator.class);
    protected Object sessionObject = null;

    @Override
    public Object getSessionObject() {
        return this.sessionObject;
    }

    @Override
    public boolean setPassword(String username, String oldPassword, String newPassword) {
        throw new UnsupportedOperationException("Not supported");
    }

    public static Entry<String, String> getCredentials(Request request) {
        Entry result = new AbstractMap.SimpleEntry("", "");

        
        String userName = "";

        StringTokenizer tokenizer = new StringTokenizer(request.toString(), "\r\n");

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            String[] elements = line.split(":");

            //    body.println("<h1> elements[0]T: " + elements[0] + "</h1>");
            if (elements.length > 0) {

                if (elements[0].equals("Authorization")) {
                    if (line.length() > "Authorization: ".length()) {
                        userName = elements[1].substring("Basic".length() + 2);
                    }
                }
            }
        }
        

        if (!userName.equals("")) {

            byte[] bytesEncoded = userName.getBytes();

            byte[] valueDecoded = Base64.getDecoder().decode(bytesEncoded);
            String[] credentials = new String(valueDecoded).split(":");

            String username = "";
            String password = "";
            if (credentials.length > 0) {
                username = credentials[0];
            }
            if (credentials.length > 1) {
                password = credentials[1];
            }
            result = new AbstractMap.SimpleEntry(username, password);
        }

        return result;
    }
}
