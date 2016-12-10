/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.authentication;

import org.simpleframework.http.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phs
 */
public class FREE extends AbstractAuthenticator {

    private static Logger logger = LoggerFactory.getLogger(FREE.class);

    @Override
    public boolean authenticate(Request request) {
        
        String username = this.getCredentials(request).getKey();

        if (!username.equals("")) {
            return true;
        } else {
            return false;
        }
    }
}
