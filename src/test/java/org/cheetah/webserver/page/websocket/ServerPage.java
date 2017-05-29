package org.cheetah.webserver.page.websocket;

import org.cheetah.webserver.websocket.server.WebSocketServerWorkerEcho;
import org.cheetah.webserver.websocket.server.WebSocketServerPage;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.slf4j.LoggerFactory;

public class ServerPage extends WebSocketServerPage {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ServerPage.class);

    public ServerPage(){
        super(new WebSocketServerWorkerEcho());
    }
    
    public void handle(Request request, Response response) {

    }
}
