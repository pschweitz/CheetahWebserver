/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.cheetah.webserver.websocket.defaults.WebSocketPage;
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
public class RequestResolver {

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

    private static Logger logger = LoggerFactory.getLogger(RequestResolver.class);

    public RequestResolver(CheetahWebserver webserver, Request request, Response response) {
        this.webserver = webserver;
        this.request = request;
        this.response = response;

        isWebsocketEnabled = this.webserver.isWebsocketEnabled();
        fileRoot = this.webserver.getFileDefaultRoot();
        packagePageName = this.webserver.getPackagePageName();
        packageRootName = this.webserver.getPackageRootName();
    }

    public void execute() {

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
            String[] elements = line.split(":");

            if (elements.length == 1) {

                debugString.append("** " + elements[0]).append(System.lineSeparator());

            } else {

                if (line.equalsIgnoreCase("Upgrade: websocket")) {
                    isWebsocketRequest = true;
                }

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

        if (name != null) {
            page = directory + name;
        } else {
            page = directory;
        }

        /*
        if (page.contains("?")) {
            page = request.getTarget().split("\\?")[0];
        }
         */
        if (!page.equals("/")) {
            while (page.endsWith("/")) {
                page = page.substring(0, page.length() - 1);
            }
        }

        if (this.webserver.isFileVirtualHostsEnabled()) {

            String host = this.webserver.getHostPort(request);
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

        String target = fileRoot + page;

        debugString.append("Target: " + target).append(System.lineSeparator());
        debugString.append("Page: " + page).append(System.lineSeparator());

        Path targetPath = Paths.get(target);

        //       debugString.append("targetPath: " + targetPath).append(System.lineSeparator());
        long time = System.currentTimeMillis();
        response.setValue("Server", this.webserver.serverName);
        response.setDate("Date", time);
        response.setDate("Last-Modified", time);

        List<Cookie> cookies = request.getCookies();

        boolean sessionCookieFound = false;

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("JSESSIONID")) {
                sessionCookieFound = true;
                response.setCookie(cookie);
                break;
            }

        }

        if (!sessionCookieFound) {
            String cookie = request.getParameter("JSESSIONID");

            if (cookie != null && !cookie.equals("")) {
                sessionCookieFound = true;
                Cookie newCookie = new Cookie("JSESSIONID", cookie);
                newCookie.setProtected(true);
                if (webserver.isNetworkSecureSSL()) {
                    newCookie.setSecure(true);
                }
                response.setCookie(newCookie);
            }
        }

        if (!sessionCookieFound) {

            String sessionCookie = UUID.randomUUID().toString();
            Cookie newCookie = new Cookie("JSESSIONID", sessionCookie);
            newCookie.setProtected(true);

            if (webserver.isNetworkSecureSSL()) {
                newCookie.setSecure(true);
            }

            response.setCookie(newCookie);
        }

        boolean authFolder = true;

        if (webserver.isSessionAuthenticationEnabled() || page.equals("/admin/Login")) {

            for (String key : this.webserver.getAuthorizationFolder().keySet()) {
                int length = (fileRoot + "/" + key).length();

                if (target.length() >= length) {

                    if (target.substring(0, length).equals(fileRoot + "/" + key)) {
                        authFolder = this.webserver.getAuthorizationFolder().get(key);
                        break;
                    }
                }
            }

            if (authFolder) {
                try {
                    body = response.getPrintStream();
                    Class lookupPage = null;

                    //lookupPage = this.webserver.getClass(packageRootName + ".PageAuthentication");
                    lookupPage = PageAuthentication.class;
                    Page page = (Page) lookupPage.newInstance();
                    page.setRessources(body, webserver, debugString);
                    page.handle(request, response);
                } catch (Exception ex) {
                    debugString.append("Error generating PageAuthentication: " + ex.toString()).append(System.lineSeparator());
                    logger.error("Error generating PageAuthentication: " + ex.toString());
                    ex.printStackTrace();
                    response.setStatus(Status.UNAUTHORIZED);
                    body.close();
                }

                if (response.getStatus() == Status.UNAUTHORIZED) {

                    new AccessLog(request, username, response.getCode());
                    debugString.append("Return code: " + response.getCode()).append(System.lineSeparator());
                    debugString.append("***** END REQUEST *******************************");
                    logger.debug(debugString.toString());
                    return;
                }
            }
        }

        try {
            body = response.getPrintStream();

            if (Files.exists(targetPath)) {

                //System.out.println("Case: File exists"); 
                //debugString.append("Case: File exists").append(System.lineSeparator());
                if (Files.isRegularFile(targetPath, LinkOption.NOFOLLOW_LINKS)) {

                    //System.out.println("Case: File exists: regular file"); 
                    //debugString.append("Case: File exists: regular file").append(System.lineSeparator());
                    readFile(request, Paths.get(targetPath.toString()).toUri().toURL());

                } else if (Files.isDirectory(targetPath, LinkOption.NOFOLLOW_LINKS)) {

                    //System.out.println("Case: File exists: is directory"); 
                    //debugString.append("Case: File exists: is directory").append(System.lineSeparator());
                    if (this.webserver.isFileFolderBrowsingEnabled()) {

                        //System.out.println("Case: File exists: is directory: folder browsing"); 
                        //debugString.append("Case: File exists: is directory: folder browsing").append(System.lineSeparator());
                        try {

                            handleFolderBrowsing();

                        } catch (Exception ex) {
                            debugString.append("Error generating folder browsing: " + ex.toString()).append(System.lineSeparator());
                            logger.error("Error generating folder browsing: " + ex.toString());
                        } finally {
                            body.close();
                        }

                    } else {
                        String defaultPages = this.webserver.getFileDefaultPage();

                        if (defaultPages.equals("")) {
                            defaultPages = "index.html;default.html;index.htm;default.htm;index;default";
                        }

                        StringTokenizer defaultPageTokenizer = new StringTokenizer(defaultPages, ";");

                        boolean found = false;

                        while (defaultPageTokenizer.hasMoreTokens()) {

                            String indexPageName = defaultPageTokenizer.nextToken();

                            Path indexPath = targetPath.resolve(indexPageName);

                            if (Files.exists(indexPath)) {

                                //System.out.println("Case: File exists: is directory: default page file found: " + indexPageName); 
                                //debugString.append("Case: File exists: is directory: default page file found: " + indexPageName).append(System.lineSeparator());
                                readFile(request, indexPath.toUri().toURL());
                                found = true;
                                break;

                            }
                        }

                        Class lookupPage = null;
                        if (!found) {

                            //System.out.println("Case: File exists: is directory: default page class"); 
                            //debugString.append("Case: File exists: is directory: default page class").append(System.lineSeparator());
                            defaultPageTokenizer = new StringTokenizer(defaultPages, ";");

                            while (defaultPageTokenizer.hasMoreTokens()) {

                                String indexPageName = defaultPageTokenizer.nextToken();

                                String packageName = page.substring(1);

                                packageName = packageName.replaceAll("/", ".");

                                if (packageName.lastIndexOf('.') > -1) {
                                    if (packageName.lastIndexOf('.') == packageName.length() - 1) {
                                        packageName = packageName.substring(0, packageName.length() - 1);
                                    }
                                }

                                if (packageName.startsWith("login")) {
                                    packagePageName = packageRootName + ".page";
                                }

                                if (packageName.startsWith("admin")) {
                                    packagePageName = packageRootName + ".page";
                                }

                                if (packageName.startsWith("ressources")) {
                                    packagePageName = packageRootName + ".page";
                                }

                                if (indexPageName.contains(".")) {
                                    indexPageName = indexPageName.substring(0, indexPageName.indexOf("."));
                                }

                                //System.out.println("packagePageName + \".\" + packageName + \".\" + indexPageName"); 
                                //System.out.println(packagePageName + "." + packageName + "." + indexPageName); 
                                //System.out.println(packageName); 
                                //System.out.println(indexPageName); 
                                try {
                                    if (!packageName.equals("")) {
                                        lookupPage = this.webserver.getClass(packagePageName + "." + packageName + "." + indexPageName);
                                    } else {
                                        lookupPage = this.webserver.getClass(packagePageName + "." + indexPageName);
                                    }

                                    //System.out.println("Case: File exists: is directory: default page class found: " + indexPageName); 
                                    //debugString.append("Case: File exists: is directory: default page class found: " + indexPageName).append(System.lineSeparator());
                                    found = true;
                                    break;
                                } catch (Exception e) {
                                }

                            }

                            if (lookupPage != null) {
                                response.setValue("Content-Type", "text/html");

                                Page page;
                                try {
                                    page = (Page) lookupPage.newInstance();
                                    page.setRessources(body, webserver, debugString);
                                    page.handle(request, response);
                                } catch (Exception ex) {
                                    Status status = Status.INTERNAL_SERVER_ERROR;
                                    try {
                                        debugString.append("Error generating response:" + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                                        for (StackTraceElement element : ex.getStackTrace()) {
                                            debugString.append("\t" + element.toString()).append(System.lineSeparator());
                                        }
                                        logger.error("Error generating response:" + status.getDescription() + ": " + ex.toString());
                                        handleDefaultPage(status, ex);

                                    } catch (Exception ex2) {
                                        debugString.append("Error generating :" + status.getDescription() + ": " + ex2.toString()).append(System.lineSeparator());
                                        logger.error("Error generating " + status.getDescription() + ": " + ex2.toString());
                                    }
                                }

                            }

                            if (!found) {

                                //System.out.println("Case: File exists: is directory: default page plugin"); 
                                //debugString.append("Case: File exists: is directory: default page plugin").append(System.lineSeparator());
                                defaultPageTokenizer = new StringTokenizer(defaultPages, ";");
                                while (defaultPageTokenizer.hasMoreTokens()) {

                                    String indexPageName = defaultPageTokenizer.nextToken();

                                    if (this.webserver.getClassLoader().getResourceAsStream(page.substring(1) + "/" + indexPageName) != null) {

                                        URL url = this.webserver.getClassLoader().getResource(page.substring(1) + "/" + indexPageName);

                                        //System.out.println("Case: File exists: is directory: default page plugin found: " + indexPageName); 
                                        //debugString.append("URL: " + url.toString()).append(System.lineSeparator());
                                        //debugString.append("Case: File exists: is directory: default page plugin found: " + indexPageName).append(System.lineSeparator());
                                        readFileRessource(url);
                                        found = true;
                                        break;

                                    }
                                }
                            }

                            lookupPage = null;
                            if (!found) {

                                //System.out.println("Case: File exists: is directory: default class plugin");   
                                //debugString.append("Case: File exists: is directory: default class plugin").append(System.lineSeparator());
                                defaultPageTokenizer = new StringTokenizer(defaultPages, ";");

                                while (defaultPageTokenizer.hasMoreTokens()) {

                                    String indexPageName = defaultPageTokenizer.nextToken();

                                    String packageName = page.substring(1);

                                    packageName = packageName.replaceAll("/", ".");

                                    if (packageName.lastIndexOf('.') > -1) {
                                        if (packageName.lastIndexOf('.') == packageName.length() - 1) {
                                            packageName = packageName.substring(0, packageName.length() - 1);
                                        }
                                    }

                                    if (packageName.startsWith("login")) {
                                        packageName = packageRootName + ".page.login";
                                    }

                                    if (packageName.startsWith("admin")) {
                                        packageName = packageRootName + ".page.admin";
                                    }

                                    if (packageName.startsWith("ressources")) {
                                        packageName = packageRootName + ".page.ressources";
                                    }

                                    if (indexPageName.contains(".")) {
                                        indexPageName = indexPageName.substring(0, indexPageName.indexOf("."));
                                    }

                                    try {
                                        lookupPage = this.webserver.getClass(packageName + "." + indexPageName);

                                        //System.out.println("Case: File exists: is directory: default class plugin found: " + indexPageName);       
                                        //debugString.append("Case: File exists: is directory: default class plugin found: " + indexPageName).append(System.lineSeparator());
                                        found = true;
                                        break;
                                    } catch (Exception e1) {
                                    }

                                }

                                if (lookupPage != null) {

                                    response.setValue("Content-Type", "text/html");

                                    Page page;
                                    try {
                                        page = (Page) lookupPage.newInstance();
                                        page.setRessources(body, webserver, debugString);
                                        page.handle(request, response);
                                    } catch (Exception ex) {
                                        Status status = Status.INTERNAL_SERVER_ERROR;
                                        try {
                                            debugString.append("Error generating response:" + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                                            for (StackTraceElement element : ex.getStackTrace()) {
                                                debugString.append("\t" + element.toString()).append(System.lineSeparator());
                                            }
                                            logger.error("Error generating response:" + status.getDescription() + ": " + ex.toString());
                                            handleDefaultPage(status, ex);

                                        } catch (Exception ex2) {
                                            debugString.append("Error generating :" + status.getDescription() + ": " + ex2.toString()).append(System.lineSeparator());
                                            logger.error("Error generating " + status.getDescription() + ": " + ex2.toString());
                                        }
                                    } finally {
                                        body.close();
                                    }
                                }
                            }

                            if (!found) {

                                //System.out.println("Case: Not found: File directory: no default page");
                                //debugString.append("Case: Not found: File directory: no default page").append(System.lineSeparator());
                                Status status = Status.NOT_FOUND;
                                try {

                                    handleDefaultPage(status);

                                } catch (Exception ex) {
                                    debugString.append("Error generating " + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                                    logger.error("Error generating " + status.getDescription() + ": " + ex.toString());
                                }
                            }
                        }
                    }
                    body.close();

                } else if (Files.isSymbolicLink(targetPath)) {

                    //System.out.println("Case: File exists: is symlink");
                    //debugString.append("Case: File exists: is symlink").append(System.lineSeparator());
                    if (this.webserver.isFileFollowSymLinks()) {

                        readFile(request, Paths.get(targetPath.toString()).toUri().toURL());

                    } else {

                        //System.out.println("Case: Not found: File: no follow symlink");
                        //debugString.append("Case: Not found: File: no follow symlink").append(System.lineSeparator());
                        Status status = Status.NOT_FOUND;
                        try {

                            handleDefaultPage(status);

                        } catch (Exception ex) {

                            debugString.append("Error generating " + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                            logger.error("Error generating " + status.getDescription() + ": " + ex.toString());

                        } finally {
                            body.close();
                        }
                    }
                } else {

                    //System.out.println("Case: Not found: File type unrecognized");
                    //debugString.append("Case: Not found: File type unrecognized").append(System.lineSeparator());
                    Status status = Status.NOT_FOUND;
                    try {

                        handleDefaultPage(status);

                    } catch (Exception ex) {

                        debugString.append("Error generating " + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                        logger.error("Error generating " + status.getDescription() + ": " + ex.toString());

                    } finally {
                        body.close();
                    }
                }

            } else {

                //System.out.println("Case: File not exists");
                //debugString.append("Case: File not exists").append(System.lineSeparator());
                String pageName = page.substring(1);

                String packageName = "";
                int pageIndex = 0;

                pageName = pageName.replaceAll("/", ".");
                pageIndex = pageName.lastIndexOf('.');

                if (pageIndex > 0) {
                    packageName = pageName.substring(0, pageIndex);
                }

                if (packageName.startsWith("login")) {
                    packagePageName = packageRootName + ".page";
                }

                if (packageName.startsWith("admin")) {
                    packagePageName = packageRootName + ".page";
                }

                if (packageName.startsWith("ressources")) {
                    packagePageName = packageRootName + ".page";
                }

                if (!packageName.equals("")) {
                    packageName = "." + packageName;
                }

                response.setValue("Content-Type", "text/html");

                Class lookupPage = null;
                try {

                    //System.out.println("packagePageName + \".\" + pageName");
                    //System.out.println(packagePageName + "." + pageName);
                    ConcurrentHashMap staticPages = this.webserver.getStaticPageMap();

                    if (staticPages != null) {
                        for (Map.Entry<String, Class<? extends Page>> entry : (Set<Map.Entry<String, Class<? extends Page>>>) staticPages.entrySet()) {
                            if (page.equals(entry.getKey())) {
                                lookupPage = entry.getValue();
                            }
                        }
                        /*
                        if (page.equals("/javascript/WebSocketURL")) {
                            lookupPage = WebSocketURL.class;

                        } else if (page.equals("/ressources/FolderWebSocket")) {
                            lookupPage = FolderWebSocket.class;

                        } else if (page.equals("/Json")) {
                            lookupPage = this.webserver.getJsonPageClass();

                        } else {
                            lookupPage = this.webserver.getClass(packagePageName + "." + pageName);
                        }
                         */
                    }

                    if (lookupPage == null) {
                        lookupPage = this.webserver.getClass(packagePageName + "." + pageName);
                    }

                    //debugString.append("Case: File not exists: found class").append(System.lineSeparator());
                    Page page = (Page) lookupPage.newInstance();
                    page.setRessources(body, webserver, debugString);

                    if (!isWebsocketRequest) {
                        if (WebSocketPage.class.isAssignableFrom(lookupPage)) {

                            debugString.append("is WebSocketPage.class").append(System.lineSeparator());
                            if (isWebsocketEnabled) {
                                ((WebSocketPage) page).initService();
                            }
                        }
                        page.handle(request, response);
                    } else if (WebSocketPage.class.isAssignableFrom(lookupPage)) {
                        debugString.append("is WebsocketRequest").append(System.lineSeparator());
                        if (isWebsocketEnabled) {
                            ((WebSocketPage) page).initService();
                            ((WebSocketPage) page).getContainer().handle(request, response);
                        } else {
                            Status status = Status.BAD_REQUEST;
                            try {

                                handleDefaultPage(status);

                            } catch (Exception ex) {
                                debugString.append("Error generating " + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                                logger.error("Error generating " + status.getDescription() + ": " + ex.toString());
                            } finally {
                                body.close();
                            }
                        }
                    } else {
                        Status status = Status.BAD_REQUEST;
                        try {

                            handleDefaultPage(status);

                        } catch (Exception ex) {
                            debugString.append("Error generating " + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                            logger.error("Error generating " + status.getDescription() + ": " + ex.toString());
                        } finally {
                            body.close();
                        }
                    }
                } catch (ClassNotFoundException e) {

                    boolean found = false;

                    if (this.webserver.getClassLoader().getResource((packagePageName + "." + page.substring(1)).replaceAll("\\.", "/")) != null) {

                        //System.out.println("Case: File not exists: is package");
                        //debugString.append("Case: File not exists: is package").append(System.lineSeparator());
                        if (this.webserver.isFileFolderBrowsingEnabled()) {
                            //System.out.println("Case: Not found: File not exists: is package directory: folder browsing");
                            //debugString.append("Case: Not found: File not exists: is package directory: folder browsing").append(System.lineSeparator());
                            try {

                                handleFolderBrowsing();

                            } catch (Exception ex) {
                                debugString.append("Error generating folder browsing: " + ex.toString()).append(System.lineSeparator());
                                logger.error("Error generating folder browsing: " + ex.toString());
                            } finally {
                                body.close();
                            }

                            found = true;
                        } else {

                            lookupPage = null;

                            //System.out.println("Case: File not exists: is package: default page class");
                            //debugString.append("Case: File not exists: is package: default page class").append(System.lineSeparator());
                            String defaultPages = this.webserver.getFileDefaultPage();

                            StringTokenizer defaultPageTokenizer = new StringTokenizer(defaultPages, ";");

                            while (defaultPageTokenizer.hasMoreTokens()) {

                                String indexPageName = defaultPageTokenizer.nextToken();

                                packageName = (packagePageName + "." + page.substring(1)).replaceAll("\\.", "/");

                                packageName = packageName.replaceAll("/", ".");
                                packageName = "." + packageName;

                                if (packageName.lastIndexOf('.') > -1) {
                                    if (packageName.lastIndexOf('.') == packageName.length() - 1) {
                                        packageName = packageName.substring(0, packageName.length() - 1);
                                    }
                                }

                                if (packageName.indexOf('.') == 0) {
                                    packageName = packageName.substring(1, packageName.length());
                                }

                                if (packageName.startsWith("login")) {
                                    packageName = packageRootName + ".page.login";
                                }

                                if (packageName.startsWith("admin")) {
                                    packageName = packageRootName + ".page.admin";
                                }

                                if (packageName.startsWith("ressources")) {
                                    packageName = packageRootName + ".page.ressources";
                                }

                                if (indexPageName.contains(".")) {
                                    indexPageName = indexPageName.substring(0, indexPageName.indexOf("."));
                                }

                                try {
                                    lookupPage = this.webserver.getClass(packageName + "." + indexPageName);

                                    //System.out.println("Case: File not exists: is package: default page class found: " + indexPageName);
                                    //debugString.append("Case: File not exists: is package: default page class found: " + indexPageName).append(System.lineSeparator());
                                    found = true;
                                    break;
                                } catch (Exception e2) {
                                }
                            }

                            if (lookupPage != null) {
                                response.setValue("Content-Type", "text/html");

                                Page page;
                                try {
                                    page = (Page) lookupPage.newInstance();
                                    page.setRessources(body, webserver, debugString);
                                    page.handle(request, response);
                                } catch (Exception ex) {
                                    Status status = Status.INTERNAL_SERVER_ERROR;
                                    try {
                                        debugString.append("Error generating response:" + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                                        for (StackTraceElement element : ex.getStackTrace()) {
                                            debugString.append("\t" + element.toString()).append(System.lineSeparator());
                                        }
                                        logger.error("Error generating response:" + status.getDescription() + ": " + ex.toString());
                                        handleDefaultPage(status, ex);

                                    } catch (Exception ex2) {
                                        debugString.append("Error generating :" + status.getDescription() + ": " + ex2.toString()).append(System.lineSeparator());
                                        logger.error("Error generating " + status.getDescription() + ": " + ex2.toString());
                                    }
                                }
                            }
                        }
                    }

                    if (!found) {

                        URL url;
                        if (this.webserver.getClassLoader().getResource(page.substring(1)) != null) {

                            //System.out.println("Case: File not exists: in plugin");
                            //debugString.append("Case: File not exists: in plugin").append(System.lineSeparator());
                            url = this.webserver.getClassLoader().getResource(page.substring(1));

                            boolean isDirectory = false;

                            String jarFile = url.getFile().toString();

                            if (jarFile.contains("!")) {
                                jarFile = jarFile.substring(0, jarFile.indexOf("!"));

                                jarFile = jarFile.substring(jarFile.indexOf(":") + 1);

                                String entryName = url.getFile().toString();

                                entryName = entryName.substring(entryName.indexOf("!") + 2);

                                if (entryName.endsWith("/")) {
                                    entryName = entryName.substring(0, entryName.length() - 1);
                                }

                                JarFile jar = new JarFile(jarFile);

                                Enumeration enumEntries = jar.entries();
                                while (enumEntries.hasMoreElements()) {
                                    JarEntry file = (JarEntry) enumEntries.nextElement();

                                    if (file.getName().substring(0, file.getName().length() - 1).equals(entryName)) {

                                        logger.debug("Entry: " + file.getName() + ": " + file.isDirectory());
                                        isDirectory = true;
                                        break;
                                    }
                                }
                            } else {
                                File file = new File(url.getPath());

                                if (file.isDirectory()) {
                                    isDirectory = true;
                                }
                            }

                            if (isDirectory) {

                                //System.out.println("Case: File not exists: is plugin directory");
                                //debugString.append("Case: File not exists: is plugin directory").append(System.lineSeparator());
                                if (this.webserver.isFileFolderBrowsingEnabled()) {

                                    //System.out.println("Case: Not found: File not exists: is plugin directory: folder browsing");
                                    //debugString.append("Case: Not found: File not exists: is plugin directory: folder browsing").append(System.lineSeparator());
                                    try {

                                        handleFolderBrowsing();

                                    } catch (Exception ex) {
                                        debugString.append("Error generating folder browsing: " + ex.toString()).append(System.lineSeparator());
                                        logger.error("Error generating folder browsing: " + ex.toString());
                                    } finally {
                                        body.close();
                                    }

                                    found = true;
                                } else {

                                    String defaultPages = this.webserver.getFileDefaultPage();

                                    StringTokenizer defaultPageTokenizer = new StringTokenizer(defaultPages, ";");

                                    defaultPageTokenizer = new StringTokenizer(defaultPages, ";");
                                    while (defaultPageTokenizer.hasMoreTokens()) {

                                        String indexPageName = defaultPageTokenizer.nextToken();

                                        if (this.webserver.getClassLoader().getResourceAsStream(page.substring(1) + "/" + indexPageName) != null) {

                                            url = this.webserver.getClassLoader().getResource(page.substring(1) + "/" + indexPageName);

                                            //System.out.println("Case: File not exists: is plugin directory: default page plugin found: " + indexPageName);
                                            //debugString.append("URL: " + url.toString()).append(System.lineSeparator());
                                            //debugString.append("Case: File not exists: is plugin directory: default page plugin found: " + indexPageName).append(System.lineSeparator());
                                            readFileRessource(url);

                                            found = true;
                                            break;
                                        }
                                    }

                                    lookupPage = null;
                                    if (!found) {

                                        //System.out.println("Case: File not exists: is plugin directory: default plugin class");
                                        //debugString.append("Case: File not exists: is plugin directory: default plugin class").append(System.lineSeparator());
                                        defaultPageTokenizer = new StringTokenizer(defaultPages, ";");

                                        while (defaultPageTokenizer.hasMoreTokens()) {

                                            String indexPageName = defaultPageTokenizer.nextToken();

                                            packageName = page.substring(1);

                                            packageName = packageName.replaceAll("/", ".");

                                            if (packageName.lastIndexOf('.') > -1) {
                                                if (packageName.lastIndexOf('.') == packageName.length() - 1) {
                                                    packageName = packageName.substring(0, packageName.length() - 1);
                                                }
                                            }

                                            if (packageName.startsWith("login")) {
                                                packageName = packageRootName + ".page.login";
                                            }

                                            if (packageName.startsWith("admin")) {
                                                packageName = packageRootName + ".page.admin";
                                            }

                                            if (packageName.startsWith("ressources")) {
                                                packageName = packageRootName + ".page.ressources";
                                            }

                                            if (indexPageName.contains(".")) {
                                                indexPageName = indexPageName.substring(0, indexPageName.indexOf("."));
                                            }

                                            try {
                                                lookupPage = this.webserver.getClass(packageName + "." + indexPageName);

                                                //System.out.println("Case: File not exists: is plugin directory: default plugin class found: " + indexPageName);
                                                //debugString.append("Case: File not exists: is plugin directory: default plugin class found: " + indexPageName).append(System.lineSeparator());
                                                found = true;
                                                break;
                                            } catch (Exception e1) {
                                            }

                                        }

                                        if (lookupPage != null) {
                                            response.setValue("Content-Type", "text/html");

                                            Page page;
                                            try {
                                                page = (Page) lookupPage.newInstance();
                                                page.setRessources(body, webserver, debugString);
                                                page.handle(request, response);
                                            } catch (Exception ex) {
                                                Status status = Status.INTERNAL_SERVER_ERROR;
                                                try {
                                                    debugString.append("Error generating response:" + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                                                    for (StackTraceElement element : ex.getStackTrace()) {
                                                        debugString.append("\t" + element.toString()).append(System.lineSeparator());
                                                    }
                                                    logger.error("Error generating response:" + status.getDescription() + ": " + ex.toString());
                                                    handleDefaultPage(status, ex);

                                                } catch (Exception ex2) {
                                                    debugString.append("Error generating :" + status.getDescription() + ": " + ex2.toString()).append(System.lineSeparator());
                                                    logger.error("Error generating " + status.getDescription() + ": " + ex2.toString());
                                                }
                                            } finally {
                                                body.close();
                                            }
                                        }
                                    }
                                }

                                if (!found) {

                                    //System.out.println("Case: Not found: File not exists: is package directory: default page plugin not found");
                                    //debugString.append("Case: Not found: File not exists: is package directory: default page plugin not found").append(System.lineSeparator());
                                    Status status = Status.NOT_FOUND;
                                    try {

                                        handleDefaultPage(status);

                                    } catch (Exception ex) {
                                        debugString.append("Error generating " + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                                        logger.error("Error generating " + status.getDescription() + ": " + ex.toString());
                                    }
                                }

                            } else {

                                //System.out.println("Case: File not exists: found package file");
                                //debugString.append("URL: " + url.toString()).append(System.lineSeparator());
                                //debugString.append("Case: File not exists: found package file").append(System.lineSeparator());
                                found = true;
                                readFileRessource(url);
                            }
                        }
                    }
                    if (!found) {
                        
                        lookupPage = this.webserver.getCustomResolvingClass();
                        if (lookupPage != null) {
                            try {
                                Page page;
                                page = (Page) lookupPage.newInstance();
                                page.setRessources(body, webserver, debugString);
                                page.handle(request, response);
                            } catch (Exception e1) {
                                Status status = Status.NOT_FOUND;
                                try {

                                    handleDefaultPage(status);

                                } catch (Exception ex) {
                                    debugString.append("Error generating CustomResolvingClass " + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                                    logger.error("Error generating CustomResolvingClass " + status.getDescription() + ": " + ex.toString());
                                }
                            }
                        } else {
                            Status status = Status.NOT_FOUND;
                            try {

                                handleDefaultPage(status);

                            } catch (Exception ex) {
                                debugString.append("Error generating " + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                                logger.error("Error generating " + status.getDescription() + ": " + ex.toString());
                            }
                        }
                        //System.out.println("Case: Not found in plugin");
                        //debugString.append("Case: Not found in plugin").append(System.lineSeparator());
                    }

                } catch (Exception ex) {
                    Status status = Status.INTERNAL_SERVER_ERROR;
                    try {
                        debugString.append("Error generating response:" + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                        for (StackTraceElement element : ex.getStackTrace()) {
                            debugString.append("\t" + element.toString()).append(System.lineSeparator());
                        }
                        logger.error("Error generating response:" + status.getDescription() + ": " + ex.toString());
                        handleDefaultPage(status, ex);

                    } catch (Exception ex2) {
                        debugString.append("Error generating :" + status.getDescription() + ": " + ex2.toString()).append(System.lineSeparator());
                        logger.error("Error generating " + status.getDescription() + ": " + ex2.toString());
                    }
                } finally {
                    if (!isWebsocketRequest || response.getCode() == 404) {
                        body.close();
                    }
                }
            }

        } catch (IOException | URISyntaxException e) {
            debugString.append("Error generating response: " + e.toString()).append(System.lineSeparator());
            logger.error("Error generating response: " + e.toString());
        } finally {
            if (!isWebsocketRequest) {
                body.close();
            }
        }

        new AccessLog(request, username, response.getCode());
        debugString.append("Return code: " + response.getCode()).append(System.lineSeparator());
        debugString.append("***** END REQUEST *******************************");
        logger.debug(debugString.toString());
    }

    private void handleDefaultPage(Status status, Exception e) throws Exception {

        Class lookupPage = null;
        //lookupPage = this.webserver.getClass(this.webserver.getDefaultPageHandler());
        lookupPage = this.webserver.getDefaultPageClass();
        AbstractPageDefault pageDefault = (AbstractPageDefault) lookupPage.newInstance();
        pageDefault.setRessources(body, webserver, debugString);
        pageDefault.setStatus(status);
        pageDefault.setException(e);

        pageDefault.handle(request, response);
    }

    private void handleDefaultPage(Status status) throws Exception {

        Class lookupPage = null;
        //lookupPage = this.webserver.getClass(this.webserver.getDefaultPageHandler());
        lookupPage = this.webserver.getDefaultPageClass();
        AbstractPageDefault pageDefault = (AbstractPageDefault) lookupPage.newInstance();
        pageDefault.setRessources(body, webserver, debugString);
        pageDefault.setStatus(status);

        pageDefault.handle(request, response);
    }

    private void handleFolderBrowsing() throws Exception {

        Class lookupPage = null;
        //lookupPage = this.webserver.getClass(this.webserver.getDefaultFolderBrowsingPageHandler());
        lookupPage = this.webserver.getDefaultFolderBrowsingPageClass();
        Page pageDefault = (Page) lookupPage.newInstance();
        pageDefault.setRessources(body, webserver, debugString);
        pageDefault.handle(request, response);
    }

    private void readFile(Request request, URL url) throws IOException, FileNotFoundException, URISyntaxException {

        int extentionIndex = url.toString().lastIndexOf('.');
        String mimeType = "application/octet-stream";

        if (extentionIndex > 0) {
            String extention = url.toString().substring(extentionIndex + 1);
            mimeType = MimeType.getMimeType(extention);

            if (MimeType.isText(extention)) {
                response.setValue("Content-Type", mimeType);
                this.webserver.getDefaultUtilsClass().readTextFile(request, body, url, Charset.forName("utf-8"));

            } else {
                response.setValue("Content-Type", mimeType);
                this.webserver.getDefaultUtilsClass().readBinaryFile(request, body, url);
            }
        } else {
            response.setValue("Content-Type", mimeType);
            this.webserver.getDefaultUtilsClass().readBinaryFile(request, body, url);
        }
    }

    // Do not return .class files for security reasons.
    private void readFileRessource(URL url) throws IOException, FileNotFoundException, URISyntaxException {

        int extentionIndex = url.toString().lastIndexOf('.');
        String mimeType = "application/octet-stream";

        if (extentionIndex > 0) {
            String extention = url.toString().substring(extentionIndex + 1);

            if (!extention.equals("class")) {
                mimeType = MimeType.getMimeType(extention);

                if (MimeType.isText(extention)) {
                    response.setValue("Content-Type", mimeType);
                    this.webserver.getDefaultUtilsClass().readTextFileRessource(request, body, url, this.webserver.getClassLoader(), Charset.forName("utf-8"));

                } else {
                    response.setValue("Content-Type", mimeType);
                    this.webserver.getDefaultUtilsClass().readBinaryFileRessource(request, body, url, this.webserver.getClassLoader());
                }
            } else {
                Status status = Status.NOT_FOUND;
                try {

                    handleDefaultPage(status);

                } catch (Exception ex) {
                    debugString.append("Error generating " + status.getDescription() + ": " + ex.toString()).append(System.lineSeparator());
                    logger.error("Error generating " + status.getDescription() + ": " + ex.toString());
                }
            }
        } else {
            response.setValue("Content-Type", mimeType);
            this.webserver.getDefaultUtilsClass().readBinaryFile(request, body, url);
        }
    }
}
