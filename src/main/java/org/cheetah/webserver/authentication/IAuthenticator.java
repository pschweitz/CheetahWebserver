/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cheetah.webserver.authentication;

import org.simpleframework.http.Request;

/**
 *
 * @author phs
 */
public interface IAuthenticator {
    
    boolean authenticate(Request request);
    boolean setPassword(String username, String oldPassword, String newPassword);
    Object  getSessionObject();
}
