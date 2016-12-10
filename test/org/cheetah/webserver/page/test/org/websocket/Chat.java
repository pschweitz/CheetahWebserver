package org.cheetah.webserver.page.test.org.websocket;

import org.cheetah.webserver.page.websocket.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import org.cheetah.webserver.Utils;
import org.cheetah.webserver.websocket.defaults.WebSocketDefaultPage;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.slf4j.LoggerFactory;

public class Chat extends WebSocketDefaultPage {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Chat.class);

    public void handle(Request request, Response response) {
        
        String pageURI = "/websocket/WebSocketChatRoom2.html";

        try {

            String page = Utils.loadPage(Paths.get(this.webserver.getFileRoot(request) + pageURI), Charset.forName("utf-8"));
            
            String user = this.webserver.getUsername(request);

            page = page.replaceAll("%1", user);

            body.println(page);
        } catch (Exception ex) {
            logger.error("Error reading page: " + this.webserver.getFileRoot(request) + pageURI, ex);
        }
    }
}
