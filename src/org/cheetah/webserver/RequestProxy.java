/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phs
 */
public class RequestProxy {

    private String fileRoot = "";
    private boolean isWebsocketEnabled = false;

    private String packageRootName = "";
    private String packagePageName = "";

    private final CheetahWebserver webserver;
    private final Request request;
    private final Response response;

    private String page = "";
    private PrintStream body;
    private boolean isWebsocketRequest = false;

    private StringBuilder debugString;

    private InputStream inputStream;

    private static Logger logger = LoggerFactory.getLogger(RequestProxy.class);

    private static ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap();

    /* TODO: 
                - Validate get parameters
                - Integrate with global webserver configs (virtual hosts)
    */
    
    
    
    public RequestProxy(CheetahWebserver webserver, Request request, Response response) {
        this.webserver = webserver;
        this.request = request;
        this.response = response;

        isWebsocketEnabled = this.webserver.isWebsocketEnabled();
        fileRoot = this.webserver.getFileDefaultRoot();
        packagePageName = this.webserver.getPackagePageName();
        packageRootName = this.webserver.getPackageRootName();
    }

    public void execute() {

        String host = "localhost";
        int port = 8080;
        boolean sslEnabled = true;
        String suffixURI = "";

        String responseResult = "";
        int responseCode = 0;
        String hostPort;
        String proxyHostPort = "localhost";

        debugString = new StringBuilder();
        debugString.append(System.lineSeparator());
        debugString.append("***** REQUEST ***********************************").append(System.lineSeparator());
        StringTokenizer requestTokenizer = new StringTokenizer(request.toString(), "\r\n");

        String username = this.webserver.getUsername(request);

        if (username.equals("")) {
            username = this.webserver.getSessionID(request);
        }

        while (requestTokenizer.hasMoreTokens()) {
            String line = requestTokenizer.nextToken();
            String[] elements = new String[2];
            if (line.contains(":")) {
                elements[0] = line.substring(0, line.indexOf(":"));
                elements[1] = line.substring(line.indexOf(":") + 1);
            } else {
                elements[0] = line;
            }

            if (elements.length == 1) {

                debugString.append("** " + elements[0]).append(System.lineSeparator());

            } else {

                if (line.equalsIgnoreCase("Upgrade: websocket")) {
                    isWebsocketRequest = true;
                }

                debugString.append("** " + elements[0] + ": " + elements[1]).append(System.lineSeparator());

                /*
                 for (int i = 1; i < elements.length; i++) {
                 debugString.append(":" + elements[i]);
                 }
                 */
                if (elements[0].equalsIgnoreCase("host")) {
                    //                debugString.append("REQUEST elements[0] :" + elements[0]).append(System.lineSeparator());
                    proxyHostPort = elements[1].trim();
                    //                debugString.append("REQUEST elements[1] :" + proxyHostPort).append(System.lineSeparator());
                    //  debugString.append("Cookie elements :" + elements.length).append(System.lineSeparator());

                }
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

        if (name != null) {
            page = directory + name;
        } else {
            page = directory;
        }

        if (!page.equals("/")) {
            while (page.endsWith("/")) {
                page = page.substring(0, page.length() - 1);
            }
        }

        if (this.webserver.isFileVirtualHostsEnabled()) {

            if (host.contains(":")) {
                host = host.substring(0, host.indexOf(':'));
            }

            debugString.append("Virtual Host: " + host).append(System.lineSeparator());

            String virtualFileDefaultRoot = this.webserver.getWebserverVirtualHosts().getString(host);

            if (!virtualFileDefaultRoot.equals("")) {
                fileRoot = virtualFileDefaultRoot;
                packagePageName = packagePageName + "." + host;
            }
        }

        //String target = fileRoot + page;
        String target = request.getTarget();

        debugString.append("Target: " + target).append(System.lineSeparator());
        debugString.append("Page: " + page).append(System.lineSeparator());

        //Path targetPath = Paths.get(target);
        long time = System.currentTimeMillis();
        response.setValue("Server", this.webserver.serverName);
        response.setDate("Date", time);
        response.setDate("Last-Modified", time);

        if (port != 80 && port != 443) {
            hostPort = host + ":" + port;
        } else {
            hostPort = host;
        }

        URL url;
        HttpURLConnection httpUrlConn = null;

        List<Cookie> cookies = request.getCookies();

        boolean sessionCookieFound = false;
        String sessionCookie = "";

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("D2_PROXY")) {
                sessionCookie = cookie.getValue();
                sessionCookieFound = true;
                break;
            }

        }

        String parameters = "";

        if (target != null && target.contains("?")) {
            try {
                parameters = target.substring(target.indexOf("?") + 1);
            } catch (Exception e) {
                debugString.append("ERROR params: " + e.toString()).append(System.lineSeparator());
            }
        }

        debugString.append("parameters: " + parameters).append(System.lineSeparator());
        try {

            body = response.getPrintStream();

            if (!sessionCookieFound) {

                debugString.append("D2_PROXY Cookie not found").append(System.lineSeparator());

                sessionCookie = UUID.randomUUID().toString();
                Cookie newCookie = new Cookie("D2_PROXY", sessionCookie);
                newCookie.setProtected(true);

                if (webserver.isNetworkSecureSSL()) {
                    newCookie.setSecure(true);
                }

                userSessions.put(sessionCookie, parameters);

                response.setCookie(newCookie);

                response.setValue("Content-Type", "text/html");

                //response.setStatus(Status.TEMPORARY_REDIRECT);
                //response.setValue("Location", "http://localhost:8080/****/");
                /*
                body.println("<!DOCTYPE html>");
                body.println("<html>");
                body.println("    <head>");
                body.println("        <script type=\"text/javascript\">");
                //body.println("              window.location = document.referrer;");
                body.println("              window.location = " + "'http://localhost:8080/D2/'" + ";");
                body.println("        </script>");
                body.println("    </head>");
                body.println("    <body>");
                body.println("    </body>");
                body.println("</html>");

                body.close();
                new AccessLog(request, username, response.getCode());
                debugString.append("Return code: " + response.getCode()).append(System.lineSeparator());
                debugString.append("Return type: " + response.getContentType()).append(System.lineSeparator());
                debugString.append("***** END REQUEST *******************************");
                logger.debug(debugString.toString());
                return;
                 */
            } else {

                String newParameters = userSessions.get(sessionCookie);

                if (newParameters != null && !newParameters.equals("")) {
                    parameters = newParameters;
                }

                debugString.append("D2_PROXY Cookie found , parameters: " + parameters).append(System.lineSeparator());

                /*
                if (userSessions.containsKey(sessionCookie)) {
                    userSessions.replace(sessionCookie, "");
                }
                 */
            }

            if (sslEnabled) {

                SSLSocketFactory sslSocketFactory = null;
                HostnameVerifier allHostsValid = null;
                try {

                    SSLContext sc = SSLContext.getInstance("SSL");

                    final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
                        }

                        @Override
                        public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    }};

                    allHostsValid = new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    };

                    sc.init(null, trustAllCerts, null);
                    sslSocketFactory = sc.getSocketFactory();

                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    logger.error("SSL initialization failed: " + e.toString());
                }

                if (parameters != null && !parameters.equals("")) {
                    url = new URL("https://" + hostPort + suffixURI + page + "?" + parameters);
                } else {
                    url = new URL("https://" + hostPort + suffixURI + page);
                }

                httpUrlConn = (HttpsURLConnection) url.openConnection();
                ((HttpsURLConnection) httpUrlConn).setSSLSocketFactory(sslSocketFactory);
                ((HttpsURLConnection) httpUrlConn).setHostnameVerifier(allHostsValid);

                if (port > 65535) {
                    throw new IllegalArgumentException("port out of range:" + port);
                }

            } else {

                if (parameters != null) {
                    url = new URL("http://" + hostPort + suffixURI + page + "?" + userSessions.get(sessionCookie));
                } else {
                    url = new URL("http://" + hostPort + suffixURI + page);
                }

                httpUrlConn = (HttpURLConnection) url.openConnection();

            }

            debugString.append("URL: " + url).append(System.lineSeparator());

            //System.out.println("HEADER: " + request.getHeader());
            StringTokenizer headerTokenizer = new StringTokenizer(request.getHeader().toString(), "\r\n");

            debugString.append("").append(System.lineSeparator());
            debugString.append("REQUEST HEADER").append(System.lineSeparator());
            debugString.append("--------------------------------------").append(System.lineSeparator());
            debugString.append(request.getHeader().toString());

            debugString.append("--------------------------------------").append(System.lineSeparator());

            String requestContent = request.getContent();
            if (requestContent != null && !requestContent.equals("")) {

                debugString.append("REQUEST CONTENT").append(System.lineSeparator());
                debugString.append(requestContent).append(System.lineSeparator());
                debugString.append("--------------------------------------").append(System.lineSeparator());
            }

            debugString.append("").append(System.lineSeparator());
            debugString.append("NEW HEADER").append(System.lineSeparator());
            debugString.append("--------------------------------------").append(System.lineSeparator());

            int lineNB = 1;

            while (headerTokenizer.hasMoreTokens()) {
                String line = headerTokenizer.nextToken();

                String[] elements = new String[2];
                if (line.contains(":")) {
                    elements[0] = line.substring(0, line.indexOf(":"));
                    elements[1] = line.substring(line.indexOf(":") + 1);
                } else {
                    elements[0] = line;
                }

                if (lineNB == 1) {
                    httpUrlConn.setRequestMethod(line.substring(0, line.indexOf(" ")));
                    debugString.append(line.substring(0, line.indexOf(" ")) + " " + httpUrlConn.getURL()).append(System.lineSeparator());
                    //             System.out.println(line.substring(0, line.indexOf(" ")) + " " + httpUrlConn.getURL());
                } else if (elements.length == 1) {
                    httpUrlConn.setRequestProperty(elements[0], "");
                    debugString.append(elements[0]).append(System.lineSeparator());
                    //                 System.out.println(elements[0]);

                } else {

                    String value = "";
                    if (elements[0].equalsIgnoreCase("Host")) {
                        value = hostPort;
                    } else if (elements[0].equalsIgnoreCase("Referer")) {

                        //                 value = elements[1].replace(proxyHostPort, hostPort);
                        //                        debugString.append("REFERER HOSTPORT value    : " + value).append(System.lineSeparator());
                    } else {
                        value = elements[1];
                        /*
                             for (int i = 1; i < elements.length; i++) {
                             value += elements[i];
                             }*/
                    }
                    httpUrlConn.setRequestProperty(elements[0], value);
                    debugString.append(elements[0] + ": " + value).append(System.lineSeparator());
                    //                 System.out.println(elements[0] + ": " + value);
                }
                lineNB++;
            }

            debugString.append("--------------------------------------").append(System.lineSeparator());

            // httpUrlConn.setRequestMethod("GET");
            // Set timeouts in milliseconds
            httpUrlConn.setConnectTimeout(5000);
            httpUrlConn.setReadTimeout(5000);

            if (requestContent != null && !requestContent.equals("")) {
                httpUrlConn.setDoOutput(true);

                OutputStream outputStream = httpUrlConn.getOutputStream();

                byte[] buffer = new byte[1024];
                //debugString.append("BEFORE READ").append(System.lineSeparator());

                if (outputStream != null) {

                    InputStream is = request.getInputStream();

                    int readbytes = is.read(buffer);

                    //    debugString.append("readbytes: " + readbytes).append(System.lineSeparator());
                    while (readbytes != -1) {

                        outputStream.write(buffer, 0, readbytes);
                        readbytes = is.read(buffer);
                        //        debugString.append("readbytes: " + readbytes).append(System.lineSeparator());
                    }

                    outputStream.close();
                }

                /*
                 BufferedWriter bw = null;

                 OutputStreamWriter os = new OutputStreamWriter(httpUrlConn.getOutputStream(), Charset.forName("UTF-8"));
                 bw = new BufferedWriter(os);

                 bw.write(URLEncoder.encode(requestContent, "UTF-8"));
                
                 bw.flush();
                 bw.close();*/
            }

            //        debugString.append("RESPONSE HEADER -1").append(System.lineSeparator());
            //              httpUrlConn.setUseCaches(false);
            //              httpUrlConn.setDoInput(true);
            //              httpUrlConn.setDoOutput(true);
            httpUrlConn.connect();

            responseCode = httpUrlConn.getResponseCode();
            this.response.setStatus(Status.getStatus(responseCode));

            inputStream = httpUrlConn.getInputStream();
            //        debugString.append("RESPONSE HEADER 0").append(System.lineSeparator());

            buildResponseHeader(httpUrlConn);
            buildResponseData(httpUrlConn);

        } catch (FileNotFoundException e) {
            responseCode = 404;
            this.response.setStatus(Status.getStatus(responseCode));

            try {
                buildResponseHeader(httpUrlConn);
            } catch (Exception ex) {
                logger.error("Error handling request: " + ex.toString());
            }

            try {
                buildResponseData(httpUrlConn);
            } catch (Exception ex) {
                logger.error("Error handling request: " + ex.toString());
            }

            /*
             try {
             this.handleDefaultPage(Status.getStatus(responseCode));
             } catch (Exception ex) {
             logger.error("Error handling request: " + ex.toString());
             }
             */
        } catch (IOException e) {
            responseCode = 401;
            this.response.setStatus(Status.getStatus(responseCode));

            try {
                buildResponseHeader(httpUrlConn);
            } catch (Exception ex) {
                logger.error("Error handling request: " + ex.toString());
            }

            try {
                this.handleDefaultPage(Status.getStatus(responseCode));
            } catch (Exception ex) {
                logger.error("Error handling request: " + ex.toString());
            }

        } catch (Exception e) {
            responseCode = 500;
            this.response.setStatus(Status.getStatus(responseCode));
            logger.error("Error handling request: " + e.toString());
            try {
                this.handleDefaultPage(Status.getStatus(responseCode));
            } catch (Exception ex) {
                logger.error("Error handling request: " + ex.toString());
            }
        } finally {
            if (body != null) {
                body.close();
            }
        }

        new AccessLog(request, username, response.getCode());

        debugString.append("Return code: " + response.getCode()).append(System.lineSeparator());
        debugString.append("Return type: " + response.getContentType()).append(System.lineSeparator());
        debugString.append("***** END REQUEST *******************************");
        logger.debug(debugString.toString());
    }

    private void buildResponseHeader(HttpURLConnection httpUrlConn) throws IOException {
        debugString.append("").append(System.lineSeparator());
        debugString.append("RESPONSE HEADER").append(System.lineSeparator());
        debugString.append("--------------------------------------").append(System.lineSeparator());

        //StringBuilder responseHeader = new StringBuilder();
        for (Entry<String, List<String>> e : httpUrlConn.getHeaderFields().entrySet()) {

            if (e.getKey() != null && !e.getKey().equals("null")) {

                //responseHeader.append(e.getKey() + ": " + e.getValue().get(0)).append(System.lineSeparator());
                if (e.getKey().equalsIgnoreCase("Set-Cookie")) {

                    StringTokenizer cookieTokenizer = new StringTokenizer(e.getValue().get(0), "; ");
                    int count = 0;

                    Cookie cookie = null;

                    while (cookieTokenizer.hasMoreTokens()) {

                        String token = cookieTokenizer.nextToken().trim();
                        if (count == 0) {

                            String[] name = token.split("=");

                            if (name.length > 0) {
                                cookie = new Cookie(name[0], name[1], "/", true);
                            }

                            count++;
                        }

                        if (cookie != null) {
                            String[] tokenTab;

                            if (token.contains("=")) {
                                tokenTab = token.split("=");
                            } else {
                                tokenTab = new String[]{token};
                            }

                            if (tokenTab[0].equalsIgnoreCase("Path")) {
                                if (tokenTab.length > 0) {
                                    cookie.setPath(tokenTab[1]);
                                }
                            }

                            if (tokenTab[0].equalsIgnoreCase("Version")) {
                                if (tokenTab.length > 0) {
                                    cookie.setVersion(Integer.valueOf(tokenTab[1]));
                                }
                            }

                            if (tokenTab[0].equalsIgnoreCase("may-age")) {
                                if (tokenTab.length > 0) {
                                    cookie.setExpiry(Integer.valueOf(tokenTab[1]));
                                }
                            }

                            if (tokenTab[0].equalsIgnoreCase("Domain")) {
                                if (tokenTab.length > 0) {
                                    cookie.setDomain(tokenTab[1]);
                                }
                            }

                            if (tokenTab[0].equalsIgnoreCase("Secure")) {
                                //cookie.setSecure(true);
                            }

                            if (tokenTab[0].equalsIgnoreCase("HttpOnly")) {
                                cookie.setProtected(true);
                            }
                        }
                    }

                    if (cookie != null) {
                        //debugString.append("Cookie: " + cookie).append(System.lineSeparator());
                        response.setCookie(cookie);

                        debugString.append(e.getKey() + ": " + e.getValue().get(0)).append(System.lineSeparator());
                    }

                } else if (!e.getKey().equalsIgnoreCase("WWW-Authenticate")) {
                    debugString.append(e.getKey() + ": " + e.getValue().get(0)).append(System.lineSeparator());
                    this.response.setValue(e.getKey(), e.getValue().get(0));
                } //        else{                        
                //            debugString.append(e.getKey() + ": " + e.getValue().get(0)).append(System.lineSeparator());
                //            this.response.addValue(e.getKey(), e.getValue().get(0));
                //        }
                /*else if (e.getKey().equalsIgnoreCase("Server")) {

                     debugString.append(e.getKey() + ": " + e.getValue().get(0)).append(System.lineSeparator());
                     this.response.setValue(e.getKey(), e.getValue().get(0));
                     }
                 */

            } else {

                debugString.append(e.getValue().get(0)).append(System.lineSeparator());
                // responseHeader.append(e.getValue().get(0)).append(System.lineSeparator());
            }

            //System.out.println(e.getKey() + ": " + e.getValue().get(0));
        }

        debugString.append("--------------------------------------").append(System.lineSeparator());
    }

    public void buildResponseData(HttpURLConnection httpUrlConn) throws IOException {
        byte[] buffer = new byte[1024];
        //debugString.append("BEFORE READ").append(System.lineSeparator());

        if (inputStream != null && httpUrlConn.getContentLength() > 0) {
            DataInputStream din = new DataInputStream(inputStream);

            int readbytes = din.read(buffer);

            //    debugString.append("readbytes: " + readbytes).append(System.lineSeparator());
            while (readbytes != -1) {

                body.write(buffer, 0, readbytes);
                readbytes = din.read(buffer);
                //        debugString.append("readbytes: " + readbytes).append(System.lineSeparator());
            }
        }
    }

    private void handleDefaultPage(Status status) throws Exception {

        Class lookupPage = null;
        lookupPage = this.webserver.getClass(this.webserver.getDefaultPageHandler());
        AbstractPageDefault pageDefault = (AbstractPageDefault) lookupPage.newInstance();
        pageDefault.setRessources(body, webserver, debugString);
        pageDefault.setStatus(status);

        pageDefault.handle(request, response);
    }
}
