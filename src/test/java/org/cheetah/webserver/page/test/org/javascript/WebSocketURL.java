package org.cheetah.webserver.page.test.org.javascript;

import org.cheetah.webserver.Page;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketURL extends Page {

    private static Logger logger = LoggerFactory.getLogger(WebSocketURL.class);

    @Override
    public void handle(Request request, Response response) {
        logger.debug("START: Handle Request");

        response.setValue("Content-Type", "application/javascript; charset=utf-8");

        String URLWS = "ws://localhost:8080";

        if (!this.webserver.getURLWS().equals("")) {
            URLWS = this.webserver.getURLWS();
        }

        URLWS = URLWS.substring(0, URLWS.indexOf(':')) + "://" + this.webserver.getHostPort(request);

        logger.debug("websocketURL: " + URLWS);

        StringBuilder bodyString = new StringBuilder();

        bodyString.append("	function getWebsocketRootURL() {\n")
                .append("           return \"" + URLWS + "\";\n")
                .append("	}");

        body.println(bodyString.toString());
        logger.debug(" END : Handle Request");
    }
}
