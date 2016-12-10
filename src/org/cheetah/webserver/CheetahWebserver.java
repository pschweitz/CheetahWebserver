/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import org.cheetah.webserver.websocket.defaults.WebSocketService;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.cheetah.webserver.authentication.AbstractAuthenticator;
import org.cheetah.webserver.authentication.FILE;
import org.simpleframework.common.buffer.Allocator;
import org.simpleframework.common.buffer.FileAllocator;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerTransportProcessor;
import org.simpleframework.http.socket.Frame;
import org.simpleframework.http.socket.Session;
import org.simpleframework.transport.Socket;
import org.simpleframework.transport.SocketProcessor;
import org.simpleframework.transport.Transport;
import org.simpleframework.transport.TransportProcessor;
import org.simpleframework.transport.TransportSocketProcessor;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CheetahWebserver implements Container, SocketProcessor, TransportProcessor {

    static {
        System.setProperty("logback.configurationFile", "etc/logback.xml");
        MimeType.initMimeType();
    }

    private static Logger logger = LoggerFactory.getLogger(CheetahWebserver.class);
    public static ArrayList<String> pluginList = new ArrayList();

    public String serverName = "Cheetah WebServer/1.0";
    public String URLHTTP = "";
    public String URLWS = "";
    public String webserverName = "";
    public Path configurationFile = Paths.get("etc/webserver.properties");

    private CheetahClassLoader classLoader = new CheetahClassLoader(Thread.currentThread().getContextClassLoader());
    private final String packageRootName = "org.cheetah.webserver";
    private String packagePageName = "org.cheetah.webserver.page";
    private String packageAuthName = "org.cheetah.webserver.authentication";

    private String defaultPageHandler = packageRootName + ".PageDefault";
    private String defaultFolderBrowsingPageHandler = packageRootName + ".PageDefaultFolder";    
    private String defaultLoginPage = packageRootName + ".page.admin.Login";

    private Connection connection;
    private SocketProcessor server;
    private TransportProcessor processor;

    /* arg1 = class/page name arg2 = service object */
    private ConcurrentHashMap<String, WebSocketService> websocketServiceList;

    private WebServerContext webserverContext;
    private WebServerVirtualHosts webserverVirtualHosts;
    private SessionDirectory sessionDirectory;

    /* for bruteforce protection */
    private ConcurrentHashMap<String, PageAuthentication.BruteForceData> ipAttempt;
    private TreeMap<String, Boolean> authorizationFolder;

    private boolean closed = false;

    @Override
    public void handle(Request request, Response response) {
        logger.trace("handle Request " + request.getClass().getName());
        RequestResolver requestExecutor = new RequestResolver(this, request, response);
        requestExecutor.execute();
    }

    @Override
    public void process(Transport transport) throws IOException {
        Map map = transport.getAttributes();
        map.put(Transport.class, transport);
        processor.process(transport);
    }

    @Override
    public void process(Socket socket) throws IOException {
        logger.trace("HTTP Connect...");
        server.process(socket);
    }

    @Override
    public void stop() {

        if (!this.closed) {
            if (!this.webserverName.equals("")) {
                logger.info("\"" + this.webserverName + "\" Stopping...");
            } else {
                logger.info("Stopping...");
            }
            this.closed = true;
            this.sessionDirectory.removeAllSessionData();

            try {
                logger.debug("Closing processor");
                processor.stop();
            } catch (Exception e) {
                logger.error("Error closing processor", e);
            }

            try {
                logger.debug("Closing server");
                server.stop();
            } catch (Exception e) {
                logger.error("Error closing server", e);
            }

            try {
                logger.debug("Closing connection");
                connection.close();

            } catch (Exception e) {
                logger.error("Error closing connection", e);
            }

            Runtime.getRuntime().gc();
        }
    }

    public CheetahWebserver() {
        init("");
        createConnection();
    }

    public CheetahWebserver(String name) {
        init(name);
        createConnection();
    }

    public CheetahWebserver(Path configurationFile) {
        this.configurationFile = configurationFile;
        init("");
        createConnection();
    }

    public CheetahWebserver(String hostname, int port, boolean ssl, boolean websocket) {
        this(hostname, port, ssl, websocket, "");
    }

    public CheetahWebserver(String hostname, int port, boolean ssl, boolean websocket, String name) {
        this(hostname, port, ssl, websocket, name, Paths.get("etc/webserver.properties"));
    }

    public CheetahWebserver(String hostname, int port, boolean ssl, boolean websocket, String name, Path configurationFile) {
        this.configurationFile = configurationFile;
        init(name);

        if (hostname != null && !hostname.equals("") && !hostname.equals("null")) {
            webserverContext.put("NetworkInterface", hostname);
        }
        webserverContext.put("NetworkPort", port);
        webserverContext.put("NetworkSecureSSL", ssl);
        webserverContext.put("WebsocketEnabled", websocket);

        createConnection();
    }

    private void init(String name) {
        webserverContext = new WebServerContext(this.configurationFile);
        webserverVirtualHosts = new WebServerVirtualHosts("etc/virtualhosts.properties");
        sessionDirectory = new SessionDirectory();
        ipAttempt = new ConcurrentHashMap();
        websocketServiceList = new ConcurrentHashMap();
        webserverName = name;

        webserverContext.put("FileDefaultPage", "index.html;default.html;index.htm;default.htm;index;default");
        webserverContext.put("FileFolderBrowsingEnabled", false);
        webserverContext.put("FileFolderBrowsingReadWrite", false);
        webserverContext.put("FileFolderFilesystemOnly", true);
        webserverContext.put("FileFollowSymLinks", "false");
        webserverContext.put("FileSessionAuthFolderFree", "login");
        webserverContext.put("FileSessionAuthFolderMandatory", "admin");
        webserverContext.put("FileUploadAdminOnly", false);
        webserverContext.put("FileUploadEnabled", false);
        webserverContext.put("FileUploadLimit", 10485760);
        webserverContext.put("FileVirtualHostsEnabled", false);
        webserverContext.put("FileDefaultRoot", "www");

//        webserverContext.put("NetworkInterface", "");
        webserverContext.put("NetworkPort", "8080");
        webserverContext.put("NetworkSecureSSL", false);
        webserverContext.put("NetworkSecureSSLEnforceValidation", false);

        webserverContext.put("SessionAdminAccount", "admin");
        webserverContext.put("SessionAuthenticationEnabled", false);
        webserverContext.put("SessionAuthenticationMechanism", "FREE");
        webserverContext.put("SessionEnableBruteForceProtection", true);
        webserverContext.put("SessionTimeout", 30);
        webserverContext.put("SessionUseLoginPage", true);

        webserverContext.put("ThreadWorkerHTTP", 3);
        webserverContext.put("ThreadWorkerWebsocket", 3);

        webserverContext.put("WebserverMode", "Production");

        webserverContext.put("WebserverName", name);

        webserverContext.put("WebsocketEnabled", true);

        webserverContext.loadProperties();

        String hostname = "";
        if (webserverContext.containsKey("NetworkInterface")) {
            hostname = webserverContext.getString("NetworkInterface");
        }

        if (hostname.equals("")) {

            hostname = "localhost";
            try {
                java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
                hostname = addr.getHostAddress();

            } catch (Exception e) {
            }

            if (hostname.equals("127.0.1.1")) {
                hostname = "127.0.0.1";
            }

            webserverContext.put("NetworkInterface", hostname);
        }

        if (!webserverContext.getString("WebserverName").equals("")) {
            this.webserverName = webserverContext.getString("WebserverName");
        }

        if (webserverContext.getBoolean("FileVirtualHostsEnabled")) {
            webserverVirtualHosts.loadProperties();
        }

        if (this.getSessionAuthenticationMechanism().equals("FILE")) {

            Class authenticatorClass = null;
            if (!this.getPackageAuthName().equals(this.getPackageRootName() + ".authentication")) {
                try {
                    authenticatorClass = Class.forName(this.getPackageAuthName() + "." + this.getSessionAuthenticationMechanism());

                } catch (Exception e) {
                    logger.warn("Warning generating Authentication Class: " + e.toString());
                }
            }

            try {
                if (authenticatorClass == null) {
                    authenticatorClass = Class.forName(this.getPackageRootName() + ".authentication." + this.getSessionAuthenticationMechanism());
                }

                FILE authenticator = (FILE) authenticatorClass.newInstance();
                authenticator.encrypt();

            } catch (Exception ex) {
                logger.error("Error Encrypting passwords in FILE", ex);
            }
        }

        populateAuthorizationFolder();

    }

    public void populateAuthorizationFolder() {

        authorizationFolder = new TreeMap(Collections.reverseOrder());

        if (!this.getFileSessionAuthFolderFree().equals("")) {

            StringTokenizer tokenizer = new StringTokenizer(this.getFileSessionAuthFolderFree(), ";");

            while (tokenizer.hasMoreElements()) {

                String key = tokenizer.nextToken();

                if (key.startsWith("/")) {
                    key = key.substring(1);
                }

                if (key.endsWith("/")) {
                    key = key.substring(0, key.length() - 1);
                }

                this.authorizationFolder.put(key, false);
            }
        }

        if (!this.getFileSessionAuthFolderMandatory().equals("")) {

            StringTokenizer tokenizer = new StringTokenizer(this.getFileSessionAuthFolderMandatory(), ";");

            while (tokenizer.hasMoreElements()) {

                String key = tokenizer.nextToken();

                if (key.startsWith("/")) {
                    key = key.substring(1);
                }

                if (key.endsWith("/")) {
                    key = key.substring(0, key.length() - 1);
                }

                this.authorizationFolder.put(key, true);
            }
        }
    }

    private void createConnection() {

        try {

            String hostname = webserverContext.getString("NetworkInterface");
            int port = Integer.parseInt(webserverContext.getString("NetworkPort"));
            boolean ssl = Boolean.parseBoolean(webserverContext.getString("NetworkSecureSSL"));
            boolean websocket = Boolean.parseBoolean(webserverContext.getString("WebsocketEnabled"));
            int workerThreads = Integer.parseInt(webserverContext.getString("ThreadWorkerHTTP"));

            String sslPropertiesFile = "etc/ssl.properties";

            Allocator allocator = new FileAllocator();
            processor = new ContainerTransportProcessor(this, allocator, workerThreads);
            this.server = new TransportSocketProcessor(this, workerThreads);

            SocketAddress address = new InetSocketAddress(hostname, port);

            if (websocket) {
                connection = new SocketConnection((TransportSocketProcessor) server);
            } else {
                connection = new SocketConnection((SocketProcessor) this);
            }

            if (!ssl) {
                connection.connect(address);

                String protocolHTTP = "http";
                String protocolWS = "ws";

                URLHTTP = protocolHTTP + "://" + hostname + ":" + port;

                webserverContext.put("URLHTTP", URLHTTP);

                if (!webserverName.equals("")) {
                    logger.info("HTTP Server \"" + webserverName + "\" listening on: " + URLHTTP);
                } else {
                    logger.info("HTTP Server listening on: " + URLHTTP);
                }

                if (websocket) {
                    URLWS = protocolWS + "://" + hostname + ":" + port;
                    webserverContext.put("URLWS", URLWS);

                    if (!webserverName.equals("")) {
                        logger.info("HTTP Websocket Server \"" + webserverName + "\" listening on: " + URLWS);
                    } else {
                        logger.info("HTTP Websocket Server listening on: " + URLWS);
                    }
                }
            } else {

                SSLContext sc = SSLContext.getInstance("SSLv3");
                logger.trace("SSLContext class: " + sc.getClass());
                logger.trace("   Protocol: " + sc.getProtocol());
                logger.trace("   Provider: " + sc.getProvider());

                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                logger.trace("KeyManagerFactory class: " + kmf.getClass());
                logger.trace("   Algorithm: " + kmf.getAlgorithm());
                logger.trace("   Provider: " + kmf.getProvider());

                Properties propertiesSource = new Properties();
                InputStream input = null;

                try {
                    input = new FileInputStream(sslPropertiesFile);

                    propertiesSource.load(input);

                    if (!propertiesSource.containsKey("keystore")) {
                        throw new CheetahWebserverSSLExcpetion("SSL properties file does not contains property: \"keystore\" ");
                    }

                    if (!propertiesSource.containsKey("password")) {
                        throw new CheetahWebserverSSLExcpetion("SSL properties file does not contains property: \"password\" ");
                    }

                    String keystoreLocation = propertiesSource.getProperty("keystore");
                    String keystorePassword = propertiesSource.getProperty("password");

                    char ksPass[] = keystorePassword.toCharArray();
                    char ctPass[] = keystorePassword.toCharArray();
                    KeyStore ks = KeyStore.getInstance("JKS");
                    ks.load(new FileInputStream(keystoreLocation), ksPass);

                    logger.trace("KeyStore class: " + ks.getClass());
                    logger.trace("   Type: " + ks.getType());
                    logger.trace("   Provider: " + ks.getProvider());
                    logger.trace("   Size: " + ks.size());

                    // Generating KeyManager list
                    kmf.init(ks, ctPass);
                    KeyManager[] kmList = kmf.getKeyManagers();

                    logger.trace("KeyManager class: " + kmList[0].getClass());
                    logger.trace("   # of key manager: " + kmList.length);

                    sc.init(kmList, null, null);

                    connection.connect(address, sc);

                    String protocolHTTP = "https";
                    String protocolWS = "wss";

                    URLHTTP = protocolHTTP + "://" + hostname + ":" + port;
                    webserverContext.put("URLHTTP", URLHTTP);

                    if (!webserverName.equals("")) {
                        logger.info("HTTP Server \"" + webserverName + "\" listening on: " + URLHTTP);
                    } else {
                        logger.info("HTTP Server listening on: " + URLHTTP);
                    }

                    if (websocket) {
                        URLWS = protocolWS + "://" + hostname + ":" + port;
                        webserverContext.put("URLWS", URLWS);

                        if (!webserverName.equals("")) {
                            logger.info("HTTP Websocket Server \"" + webserverName + "\" listening on: " + URLWS);
                        } else {
                            logger.info("HTTP Websocket Server listening on: " + URLWS);
                        }
                    }

                } catch (IOException e) {
                    logger.error("Webserver initialization failed", e);

                } catch (CheetahWebserverSSLExcpetion e) {
                    logger.error("Error loading SSL properties file: \"" + sslPropertiesFile + "\"", e);

                } finally {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e) {
                            logger.error("Error closing SSL properties file: \"" + sslPropertiesFile + "\"", e);
                        }
                    }
                }
            }

        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
            logger.error("Webserver initialization failed", e);
        }
    }

    public synchronized String getUsername(Request request) {
        List<Cookie> cookies = request.getCookies();

        String user = "";

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("JSESSIONID")) {
                String sessionID = cookie.getValue();

                user = sessionDirectory.getUsername(sessionID);

                break;
            }
        }

        if (user.equals("")) {
            if (sessionDirectory.isExistingSession(request)) {
                user = AbstractAuthenticator.getCredentials(request).getKey();
                /*

                String userName = "";
                StringTokenizer tokenizer = new StringTokenizer(request.toString(), "\r\n");
                while (tokenizer.hasMoreTokens()) {
                    String line = tokenizer.nextToken();
                    String[] elements = line.split(":");

                    if (elements.length > 0) {

                        if (elements[0].equals("Authorization")) {
                            if (line.length() > "Authorization: ".length()) {
                                userName = elements[1].substring("Basic".length() + 2);
                            }

                            break;
                        }
                    }
                }

                if (!userName.equals("")) {

                    byte[] bytesEncoded = userName.getBytes();

                    byte[] valueDecoded = Base64.getDecoder().decode(bytesEncoded);
                    String[] credentials = new String(valueDecoded).split(":");

                    if (credentials.length > 0) {
                        user = credentials[0];
                    }
                }
                 */
            }
        }

        return user;
    }

    public synchronized String getHostPort(Request request) {

        String result = "";

        StringTokenizer tokenizer = new StringTokenizer(request.toString(), "\r\n");

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            String[] elements = line.split(":");

            if (elements.length > 1) {

                if (line.toLowerCase().startsWith("host:")) {

                    result = elements[1].trim();

                    for (int i = 2; i < elements.length; i++) {
                        result += ":" + elements[i];
                    }

                    break;
                }
            }
        }

        return result;
    }

    public synchronized String getSessionID(Request request) {
        List<Cookie> cookies = request.getCookies();

        String sessionID = "";

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("JSESSIONID")) {
                sessionID = cookie.getValue();
                break;
            }
        }
        /*
        if (sessionID.equals("")) {
            if (sessionDirectory.isExistingSession(request)) {
                String username = AbstractAuthenticator.getCredentials(request).getKey();
                sessionID = sessionDirectory.getCookie(username);                
            }
        }*/

        return sessionID;
    }

    public synchronized String getSessionID(Response response) {
        List<Cookie> cookies = response.getCookies();

        String sessionID = "";

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("JSESSIONID")) {
                sessionID = cookie.getValue();
                break;
            }
        }
        return sessionID;
    }

    public synchronized Object getSessionObject(Request request) {
        List<Cookie> cookies = request.getCookies();

        Object sessionObject = null;

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("JSESSIONID")) {
                sessionObject = sessionDirectory.getSessionData(cookie.getValue()).getSessionObject();
                break;
            }
        }

        if (sessionObject == null) {
            if (sessionDirectory.isExistingSession(request)) {
                String username = AbstractAuthenticator.getCredentials(request).getKey();
                sessionObject = sessionDirectory.getSessionData(sessionDirectory.getCookie(username)).getSessionObject();

            }
        }

        return sessionObject;
    }

    public synchronized Object getSessionObject(Response response) {
        List<Cookie> cookies = response.getCookies();

        Object sessionObject = null;

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("JSESSIONID")) {
                sessionObject = sessionDirectory.getSessionData(cookie.getValue()).getSessionObject();
                break;
            }
        }

        return sessionObject;
    }

    public void removeAllSessionDataExceptAdmin() {
        this.sessionDirectory.removeAllSessionDataExceptAdmin(this.getSessionAdminAccountList());

        if (this.isWebsocketEnabled()) {
            for (WebSocketService webSocketService : websocketServiceList.values()) {

                ArrayList<String> secWebSocketKeyList = new ArrayList();

                for (String secWebSocketKey : webSocketService.getUsers().keySet()) {
                    String user = webSocketService.getUsers().get(secWebSocketKey);
                    if (!this.getSessionAdminAccountList().contains(user)) {
                        secWebSocketKeyList.add(secWebSocketKey);
                    }
                }

                for (String secWebSocketKey : secWebSocketKeyList) {
                    webSocketService.leave(secWebSocketKey);
                }
            }
        }
    }

    public void removeSessionData(String cookie) {

        if (this.sessionDirectory.isExistingSession(cookie)) {
            String username = this.sessionDirectory.getUsername(cookie);
            this.sessionDirectory.removeSessionData(cookie);

            if (this.isWebsocketEnabled()) {
                for (WebSocketService webSocketService : websocketServiceList.values()) {

                    ArrayList<String> secWebSocketKeyList = new ArrayList();

                    for (String secWebSocketKey : webSocketService.getUsers().keySet()) {
                        String user = webSocketService.getUsers().get(secWebSocketKey);
                        if (!username.contains(user)) {
                            secWebSocketKeyList.add(secWebSocketKey);
                        }
                    }

                    for (String secWebSocketKey : secWebSocketKeyList) {
                        webSocketService.leave(secWebSocketKey);
                    }
                }
            }
        }
    }

    public void removeAllSessionData() {
        this.sessionDirectory.removeAllSessionData();

        if (this.isWebsocketEnabled()) {
            for (WebSocketService webSocketService : websocketServiceList.values()) {
                for (String secWebSocketKey : webSocketService.getUsers().keySet()) {
                    webSocketService.leave(secWebSocketKey);
                }
            }
        }
    }

    // allows cascade sending of frames if in distribute(String) of webssocket service, it forwards to another websocket.
    public void distributeToWebsocketServiceMessage(String serviceClassName, String message) {

        if (websocketServiceList.containsKey(serviceClassName)) {
            WebSocketService webSocketService = websocketServiceList.get(serviceClassName);
            webSocketService.distribute(message);
        } else {
            // logger.warn("WebSocket Service \"" + serviceClassName + "\" not found");
        }
    }

    // allows cascade sending of frames if in onframe of webssocket service, it forwards to another websocket.
    public void distributeToWebsocketServiceFrame(String serviceClassName, Session session, Frame frame) {

        if (websocketServiceList.containsKey(serviceClassName)) {
            WebSocketService webSocketService = websocketServiceList.get(serviceClassName);
            webSocketService.getListener().onFrame(session, frame);
        } else {
            // logger.warn("WebSocket Service \"" + serviceClassName + "\" not found");
        }
    }

    // allows cascade sending of frames if in distribute(Frame) of webssocket service, it forwards to another websocket.
    public void distributeToWebsocketServiceFrame(String serviceClassName, Frame frame) {

        if (websocketServiceList.containsKey(serviceClassName)) {
            WebSocketService webSocketService = websocketServiceList.get(serviceClassName);
            webSocketService.distribute(frame);
        } else {
            // logger.warn("WebSocket Service \"" + serviceClassName + "\" not found");
        }
    }

    public String getURLHTTP() {
        return URLHTTP;
    }

    public void setURLHTTP(String URLHTTP) {
        this.URLHTTP = URLHTTP;
    }

    public String getURLWS() {
        return URLWS;
    }

    public void setURLWS(String URLWS) {
        this.URLWS = URLWS;
    }

    public WebServerContext getWebserverContext() {
        return webserverContext;
    }

    public WebServerVirtualHosts getWebserverVirtualHosts() {
        return webserverVirtualHosts;
    }

    public void updateWebserverVirtualHosts(String virtualHostPropertiesFileName) {
        if (webserverContext.getBoolean("FileVirtualHostsEnabled")) {
            webserverVirtualHosts = new WebServerVirtualHosts(virtualHostPropertiesFileName);
            webserverVirtualHosts.loadProperties();
        }
    }

    public SessionDirectory getSessionDirectory() {
        return sessionDirectory;
    }

    public ConcurrentHashMap<String, PageAuthentication.BruteForceData> getIpAttempt() {
        return ipAttempt;
    }

    public ConcurrentHashMap<String, WebSocketService> getWebsocketServiceList() {
        return websocketServiceList;
    }

    public String getFileDefaultPage() {
        return webserverContext.getString("FileDefaultPage");
    }

    public void setFileDefaultPage(String defaultPage) {
        webserverContext.put("FileDefaultPage", defaultPage);
    }

    public boolean isFileFolderBrowsingEnabled() {
        return webserverContext.getBoolean("FileFolderBrowsingEnabled");
    }

    public void setFileFolderBrowsingEnabled(boolean folderBrowsingEnabled) {
        webserverContext.put("FileFolderBrowsingEnabled", folderBrowsingEnabled);
    }

    public boolean isFileFolderBrowsingReadWrite() {
        return webserverContext.getBoolean("FileFolderBrowsingReadWrite");
    }

    public void setFileFolderBrowsingReadWrite(boolean folderBrowsingReadWrite) {
        webserverContext.put("FileFolderBrowsingReadWrite", folderBrowsingReadWrite);
    }

    public boolean isFileFolderFilesystemOnly() {
        return webserverContext.getBoolean("FileFolderFilesystemOnly");
    }

    public void setFileFolderFilesystemOnly(boolean folderFilesystemOnly) {
        webserverContext.put("FileFolderFilesystemOnly", folderFilesystemOnly);
    }

    public boolean isFileFollowSymLinks() {
        return webserverContext.getBoolean("FileFollowSymLinks");
    }

    public void setFileFollowSymLinks(boolean followSymLinks) {
        webserverContext.put("FileFollowSymLinks", followSymLinks);
    }

    public String getFileSessionAuthFolderFree() {
        return webserverContext.getString("FileSessionAuthFolderFree");
    }

    public void setFileSessionAuthFolderFree(String authFolderFree) {
        webserverContext.put("FileSessionAuthFolderFree", authFolderFree);
        populateAuthorizationFolder();
    }

    public String getFileSessionAuthFolderMandatory() {
        return webserverContext.getString("FileSessionAuthFolderMandatory") + ";/admin/Login";
    }

    public void setFileSessionAuthFolderMandatory(String authFolderMandatory) {
        webserverContext.put("FileSessionAuthFolderMandatory", authFolderMandatory);
        populateAuthorizationFolder();
    }

    public boolean isFileUploadEnabled() {
        return webserverContext.getBoolean("FileUploadEnabled");
    }

    public void setFileUploadEnabled(boolean uploadEnabled) {
        webserverContext.put("FileUploadEnabled", uploadEnabled);
    }

    public boolean isFileUploadAdminOnly() {
        return webserverContext.getBoolean("FileUploadAdminOnly");
    }

    public void setFileUploadAdminOnly(boolean uploadAdminOnly) {
        webserverContext.put("FileUploadAdminOnly", uploadAdminOnly);
    }

    public Long getFileUploadLimit() {
        return webserverContext.getLong("FileUploadLimit");
    }

    public void setFileUploadLimit(Long uploadLimit) {
        webserverContext.put("FileUploadLimit", uploadLimit);
    }

    public boolean isFileVirtualHostsEnabled() {
        return webserverContext.getBoolean("FileVirtualHostsEnabled");
    }

    public void setFileVirtualHostsEnabled(boolean virtualHostsEnabled) {
        webserverContext.put("FileVirtualHostsEnabled", virtualHostsEnabled);
    }

    public String getFileDefaultRoot() {
        return webserverContext.getString("FileDefaultRoot");
    }

    public String getFileRoot(Request request) {

        String result = getFileDefaultRoot();
        if (this.isFileVirtualHostsEnabled()) {

            String host = this.getHostPort(request);
            if (host.contains(":")) {
                host = host.substring(0, host.indexOf(':'));
            }
            String virtualFileDefaultRoot = this.getWebserverVirtualHosts().getString(host);

            if (!virtualFileDefaultRoot.equals("")) {
                result = virtualFileDefaultRoot;
            }
        }

        return result;
    }

    public void setFileDefaultRoot(String wwwDefaultRoot) {
        webserverContext.put("FileDefaultRoot", wwwDefaultRoot);
    }

    public String getNetworkInterface() {
        return webserverContext.getString("NetworkInterface");
    }

    public void setNetworkInterface(String networkInterface) {
        webserverContext.put("NetworkInterface", networkInterface);
    }

    public int getNetworkPort() {
        return webserverContext.getInteger("NetworkPort");
    }

    public void setNetworkPort(int networkPort) {
        webserverContext.put("NetworkPort", networkPort);
    }

    public boolean isNetworkSecureSSL() {
        return webserverContext.getBoolean("NetworkSecureSSL");
    }

    public void setNetworkSecureSSL(boolean networkSecureSSL) {
        webserverContext.put("NetworkSecureSSL", networkSecureSSL);
    }

    public boolean isNetworkSecureSSLEnforceValidation() {
        return webserverContext.getBoolean("NetworkSecureSSLEnforceValidation");
    }

    public void setNetworkSecureSSLEnforceValidation(boolean networkSecureSSLEnforceValidation) {
        webserverContext.put("NetworkSecureSSLEnforceValidation", networkSecureSSLEnforceValidation);
    }

    public String getSessionAdminAccount() {
        return webserverContext.getString("SessionAdminAccount");
    }

    public void setSessionAdminAccount1(String sessionAdminAccount) {
        webserverContext.put("SessionAdminAccount", sessionAdminAccount);
    }

    public boolean isSessionAuthenticationEnabled() {
        return webserverContext.getBoolean("SessionAuthenticationEnabled");
    }

    public void setSessionAuthenticationEnabled(boolean sessionAuthenticationEnabled) {
        webserverContext.put("SessionAuthenticationEnabled", sessionAuthenticationEnabled);
    }

    public String getSessionAuthenticationMechanism() {
        return webserverContext.getString("SessionAuthenticationMechanism");
    }

    public void setSessionAuthenticationMechanism(String sessionAuthenticationMechanism) {
        webserverContext.put("SessionAuthenticationMechanism", sessionAuthenticationMechanism);
    }

    public boolean isSessionEnableBruteForceProtection() {
        return webserverContext.getBoolean("SessionEnableBruteForceProtection");
    }

    public void setSessionEnableBruteForceProtection(boolean sessionEnableBruteForceProtection) {
        webserverContext.put("SessionEnableBruteForceProtection", sessionEnableBruteForceProtection);
    }

    public int getSessionTimeout() {
        return webserverContext.getInteger("SessionTimeout");
    }

    public void setSessionTimeout(int sessionTimeout) {
        webserverContext.put("SessionTimeout", sessionTimeout);
    }

    public boolean isSessionUseLoginPage() {
        return webserverContext.getBoolean("SessionUseLoginPage");
    }

    public void setSessionUseLoginPage(boolean sessionUseLoginPage) {
        webserverContext.put("SessionUseLoginPage", sessionUseLoginPage);
    }

    public int getThreadWorkerHTTP() {
        return webserverContext.getInteger("ThreadWorkerHTTP");
    }

    public void setThreadWorkerHTTP(int threadWorkerHTTP) {
        webserverContext.put("ThreadWorkerHTTP", threadWorkerHTTP);
    }

    public int getThreadWorkerWebsocket() {
        return webserverContext.getInteger("ThreadWorkerWebsocket");
    }

    public void setThreadWorkerWebsocket(int threadWorkerWebsocket) {
        webserverContext.put("ThreadWorkerWebsocket", threadWorkerWebsocket);
    }

    public String getWebserverMode() {
        return webserverContext.getString("WebserverMode");
    }

    public void setWebserverMode(String webserverMode) {
        webserverContext.put("WebserverMode", webserverMode);
    }

    public String getWebserverName() {
        return this.webserverName;
    }

    public void setWebserverName(String webserverName) {
        webserverContext.put("WebserverName", webserverName);

        this.webserverName = webserverName;
    }

    public boolean isWebsocketEnabled() {
        return webserverContext.getBoolean("WebsocketEnabled");
    }

    public String getPackageAuthName() {
        return this.packageAuthName;
    }

    public void setPackageAuthName(String packageAuthName) {
        this.packageAuthName = packageAuthName;
    }

    public String getPackageRootName() {
        return packageRootName;
    }

    public String getPackagePageName() {
        return this.packagePageName;
    }

    public void setPackagePageName(String packagePageName) {
        this.packagePageName = packagePageName;
    }

    public void printProperties() {
        webserverContext.printProperties();
    }

    public void setClassLoader(CheetahClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public CheetahClassLoader getClassLoader() {

        if (!this.getWebserverMode().equals("Development")) {
            return classLoader;
        } else {
            classLoader = new CheetahClassLoader(Thread.currentThread().getContextClassLoader());
            return classLoader;
        }
    }

    public String getDefaultPageHandler() {
        return defaultPageHandler;
    }

    public void setDefaultPageHandler(String defaultPageHandler) {
        this.defaultPageHandler = defaultPageHandler;
    }

    public String getDefaultFolderBrowsingPageHandler() {
        return defaultFolderBrowsingPageHandler;
    }

    public void setDefaultFolderBrowsingPageHandler(String defaultFolderBrowsingPageHandler) {
        this.defaultFolderBrowsingPageHandler = defaultFolderBrowsingPageHandler;
    }

    public String getDefaultLoginPage() {
        return defaultLoginPage;
    }

    public void setDefaultLoginPage(String defaultLoginPage) {
        this.defaultLoginPage = defaultLoginPage;
    }    
    
    public ArrayList<String> getSessionAdminAccountList() {
        ArrayList<String> adminList = new ArrayList();
        String sessionAdminAccount = this.getSessionAdminAccount();

        if (sessionAdminAccount.contains(";")) {
            for (String admin : getSessionAdminAccount().split(";")) {
                adminList.add(admin);
            }
        } else {
            adminList.add(sessionAdminAccount);
        }

        return adminList;
    }

    public boolean isAdminUser(String username) {
        return getSessionAdminAccountList().contains(username);
    }

    public Class getClass(String classname) throws ClassNotFoundException {
//        logger.debug("getClass: " + classname);
//        Class c = classLoader.loadClass(classname);
        Class c = this.getClassLoader().loadClassPlugin(classname);
        return c;
    }

    public static void main(String[] args) {

        String configurationFile = "";
        Path configurationFilePath = null;

        for (String arg : args) {

            if (arg.startsWith("--config=")) {
                configurationFile = arg.substring("--config=".length());

                if (Files.exists(Paths.get(configurationFile))) {
                    configurationFilePath = Paths.get(configurationFile);
                }
            }
        }

        CheetahWebserver webserver = null;

        if (configurationFilePath != null) {
            webserver = new CheetahWebserver(configurationFilePath);
        } else {
            webserver = new CheetahWebserver();
        }
        if (webserver != null) {
            webserver.printProperties();
        }
    }

    public TreeMap<String, Boolean> getAuthorizationFolder() {
        return this.authorizationFolder;
    }

    /*
    
     chrome://view-response/content/view-response.html?method=GET&uri=https%3A%2F%2F192.168.1.4%3A8080%2F&headers=&body=login.username%3Dtoto
    
     */
}
