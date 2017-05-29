/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page;

import org.cheetah.webserver.Page;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

/**
 *
 * @author Philippe Schweitzer
 */
public class index extends Page {

    @Override
    public void handle(Request rqst, Response rspns) {        
        body.append("TEST 2");
             
        body.append(" <a href='http://localhost:8080/admin/changePassword.html'>Change Password</a>");
        
    }    
}
