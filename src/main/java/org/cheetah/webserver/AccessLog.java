/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import org.simpleframework.http.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class AccessLog {
    
    static Logger logger = LoggerFactory.getLogger(AccessLog.class);
    
    public AccessLog(Request request, String username, int httpCode){
        
        logger.trace(request.getClientAddress().getHostString() + " " + request.getMethod()  + " " + request.getValue("Host") + request.getTarget()  + " HTTP/" + request.getMajor()   + "." + request.getMinor() + " " + httpCode  + " "  + username);
        
    }    
}
