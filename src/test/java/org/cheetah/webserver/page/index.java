/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page;

import com.auth0.jwt.interfaces.Claim;
import org.cheetah.webserver.Page;
import org.json.JSONObject;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 *
 * @author Philippe Schweitzer
 */
public class index extends Page {


    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(index.class);

    @Override
    public void handle(Request request, Response response) {

        Map<String, Claim> claims = (Map<String, Claim>) this.webserver.getSessionObject(request);
        JSONObject jwtPayload = new JSONObject();

        if(claims.containsKey("properties")){
            jwtPayload = new JSONObject(claims.get("properties").asString());
            logger.debug("properties: " + jwtPayload.toString());
        }


        body.append("TEST 2");
             
        body.append(" <a href='http://localhost:8080/admin/changePassword.html'>Change Password</a>");
        
    }    
}
