package org.cheetah.webserver.websocket.defaults;

import java.util.StringTokenizer;
import org.simpleframework.http.socket.*;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.slf4j.LoggerFactory;

public class WebSocketDefaultListener implements FrameListener {

//   private final CertificateUserExtractor extractor;
    protected final WebSocketDefaultService webSocketService;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketDefaultListener.class);

    public WebSocketDefaultListener(WebSocketDefaultService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Override
    public void onFrame(Session session, Frame frame) {
        String user = getUserName(session);

        FrameType type = frame.getType();
        Frame replay = null;

        if (type == FrameType.PONG || type == FrameType.CLOSE) {
            return;
        }

        logger.debug("onFrame type: " + type + " from user: " + user);

        if (type == FrameType.TEXT) {
            try {

                replay = new DataFrame(type, frame.getText());

            } catch (Exception e) {
                logger.error("Error creating text replay frame", e);
            }
        }

        if (type == FrameType.BINARY) {
            try {

                replay = new DataFrame(type, frame.getBinary());

            } catch (Exception e) {
                logger.error("Error creating binary replay frame", e);
            }
        }

        if (replay != null) {
            webSocketService.distribute(replay);
        } else {
            logger.error("Replay is null");
        }

        printRequest(session);
    }

    @Override
    public void onError(Session session, Exception cause) {
        String user = getUserName(session);

        if (!cause.toString().equals("java.io.IOException: Connection reset by peer")) {
            logger.error("onError from user: " + user + ": " + cause.toString());
        } else {
            logger.warn("onError disconnected user: " + user);
        }
    }

    @Override
    public void onClose(Session session, Reason reason) {
        String secWebSocketKey = session.getRequest().getValue("Sec-WebSocket-Key");              
        String user = webSocketService.getUsers().get(secWebSocketKey);

        logger.debug("onClose from user: " + user + " Reason: " + reason.getText());

        webSocketService.leave(secWebSocketKey);
    }

    public final String getUserName(Session session) {

        Request request = session.getRequest();

        String user = webSocketService.getWebserver().getUsername(request);

        if (user.equals("")) {
            user = webSocketService.getWebserver().getSessionID(request);
        }

        if (user.equals("")) {
            user = request.getValue("Sec-WebSocket-Key");
        }

        return user;
    }

    protected final void printRequest(Session session) {

        Request request = session.getRequest();

        String page = "";

        String wwwRoot = this.webSocketService.getWebserver().getFileDefaultRoot();
        StringBuilder debugString = new StringBuilder();
        debugString.append(System.lineSeparator());
        debugString.append("***** REQUEST ***********************************").append(System.lineSeparator());
        StringTokenizer tokenizer = new StringTokenizer(request.toString(), "\r\n");

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            String[] elements = line.split(":");

            if (elements.length == 1) {

                debugString.append("** " + elements[0]).append(System.lineSeparator());

            } else {

                debugString.append("** " + elements[0]);

                for (int i = 1; i < elements.length; i++) {
                    debugString.append(":" + elements[i]);
                }

                debugString.append(System.lineSeparator());
            }

            if (elements[0].equals("Cookie")) {
                //  debugString.append("Cookie elements :" + elements.length).append(System.lineSeparator());

            }
        }

        org.simpleframework.http.Path path = request.getPath();
        String directory = path.getDirectory();
        String name = path.getName();
        String[] segments = path.getSegments();

        debugString.append("** directory:" + directory).append(System.lineSeparator());
        debugString.append("** name:" + name).append(System.lineSeparator());
        for (String segment : segments) {

            debugString.append("** segment:" + segment).append(System.lineSeparator());

        }
        page = request.getTarget();
        if (request.getTarget().contains("?")) {
            page = request.getTarget().split("\\?")[0];
        }

        String target = wwwRoot + page;

        debugString.append("Target: " + target).append(System.lineSeparator());
        debugString.append("Page: " + page).append(System.lineSeparator());

        debugString.append("***** END REQUEST *******************************").append(System.lineSeparator());
        logger.debug(debugString.toString());

    }

    /*
     public static class CertificateUserExtractor {
      
     private final Map<String, String> cache;
     private final Pattern pattern;
      
     public CertificateUserExtractor(String pattern) {
     this.cache = new ConcurrentHashMap<String, String>();
     this.pattern = Pattern.compile(pattern);
     }  
      
     public String extractUser(Request request) throws Exception {
     try {
     Certificate certificate = request.getClientCertificate();
            
     if(certificate != null) {
     X509Certificate[] certificates = certificate.getChain();
               
     for(X509Certificate entry : certificates) {
     String user = extractCertificateUser(entry);
                  
     if(user != null) {
     return user;
     }
     }
     }
     } catch(Exception e) {
     e.printStackTrace();
     }
     return null;
     } 
      
     private String extractCertificateUser(X509Certificate certificate) throws Exception {
     Principal principal = certificate.getSubjectDN();
     String name = principal.getName();
     String user = cache.get(name);
         
     if(user == null) {
     if(!cache.containsKey(name)) {
     Matcher matcher = pattern.matcher(name);
            
     if(matcher.matches()) {
     user = matcher.group(1);
     }
     cache.put(name, user);
     }
     }
     return user;
     }
     }
     */
}
