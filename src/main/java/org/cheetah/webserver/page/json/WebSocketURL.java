package org.cheetah.webserver.page.json;

import org.cheetah.webserver.Page;
import org.json.JSONObject;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketURL extends Page {

    private static Logger logger = LoggerFactory.getLogger(WebSocketURL.class);

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

        logger.debug("WebSocketURL: " + URLWS);

        jsonResult.put("WebSocketURL", URLWS);

        //logger.debug("WS URL:" + jsonResult);

        body.println(jsonResult);
        logger.debug(" END : Handle Request");
    }
}
