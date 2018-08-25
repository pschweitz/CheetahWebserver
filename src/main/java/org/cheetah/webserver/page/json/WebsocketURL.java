/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.page.json;

import org.cheetah.webserver.Page;
import org.json.JSONObject;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author aberbier
 */
public class WebsocketURL extends Page {

    private static Logger logger = LoggerFactory.getLogger(WebsocketURL.class);

    @Override
    public void handle(Request request, Response response) {
        logger.debug("START: Handle Request");

        response.setValue("Content-Type", "application/json");

        JSONObject jsonResult = new JSONObject();

        String URLWS = "http://localhost;8080";

        if (!this.webserver.getURLWS().equals("")) {
            URLWS = this.webserver.getURLWS();
        }

        URLWS = URLWS.substring(0, URLWS.indexOf(':')) + "://" + this.webserver.getHostPort(request);

        logger.debug("websocketURL: " + URLWS);

        jsonResult.put("websocketURL", URLWS);
        jsonResult.put("TEST3", URLWS);

        //logger.debug("WS URL:" + jsonResult);

        body.println(jsonResult);
        logger.debug(" END : Handle Request");
    }
}
