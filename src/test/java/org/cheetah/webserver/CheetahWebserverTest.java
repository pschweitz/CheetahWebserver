package org.cheetah.webserver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import org.cheetah.nio.file.Files;
import org.cheetah.nio.file.Path;
import org.cheetah.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import javax.net.ssl.SSLHandshakeException;
import org.cheetah.webserver.websocket.client.CheckWebSocketObjectClient;
import org.cheetah.webserver.websocket.client.CheckWebSocketSynchronizedObject;
import org.cheetah.webserver.websocket.client.CheckWebSocketSynchronizedObjectClient;
import org.cheetah.webserver.websocket.client.CheckWebSocketSynchronizedText;
import org.cheetah.webserver.websocket.client.CheckWebSocketSynchronizedTextBadRequest;
import org.cheetah.webserver.websocket.client.CheckWebSocketSynchronizedTextClient;
import org.cheetah.webserver.websocket.client.CheckWebSocketTextClient;

public class CheetahWebserverTest {

    static {
        System.setProperty("logback.configurationFile", "etc_test/logback.xml");
    }

    public static Charset charset = Charset.forName("UTF-8");

    public static WebServerContext webserverContext = new WebServerContext();

    public static Path[] propertiesFiles;

    String hostname = "";
    int port = 0;
    boolean sslEnabled = false;

    String notfound = "";
    String badrequest = "";

    String folder = "<h1>Folder";

    CheetahWebserver webserver;

    public static void main(String[] list) throws Exception {

        propertiesFiles = getPropertiesFiles(Paths.get("etc_test"));

        //      propertiesFiles = new Path[]{Paths.get("etc_test/NO_FOLDER_webserver.properties"), Paths.get("etc/NO_WEBSOCKET_webserver.properties"), Paths.get("etc/NO_FOLDER_SSL_webserver.properties")};
        //propertiesFiles = new Path[]{Paths.get("etc_test/VIRTUALHOST_SYMLINK_WEBSOCKET_SSL_webserver.properties")};
        //   propertiesFiles = new Path[]{Paths.get("etc_test/SYMLINK_WEBSOCKET_SSL_webserver.properties")};
        // propertiesFiles = new Path[]{Paths.get("etc_test/FULL_80_webserver.properties")};
        //propertiesFiles = new Path[]{Paths.get("etc_test/AUTHENTICATION_webserver.properties")};
        for (Path path : propertiesFiles) {
            System.out.println("");
            System.out.println("* " + path.toString());
            System.out.println("");
            new CheetahWebserverTest(path);
            System.out.println("******************");
            System.out.println("");

            //   Thread.sleep(100);
        }

        System.exit(0);
    }

    public CheetahWebserverTest(Path propertyFile) {
        webserverContext = new WebServerContext(propertyFile);
        webserverContext.loadProperties();

        webserver = new CheetahWebserver(propertyFile);

        hostname = webserver.getNetworkInterface();

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

            webserver.setNetworkInterface(hostname);
        }
        //webserver.setDefaultFolderBrowsingPageHandler("org.cheetah.webserver.PageFolder");
        webserver.setDefaultFolderBrowsingPageClass(PageFolder.class);
        webserver.setFileDefaultRoot("www_test");

        port = webserver.getNetworkPort();
        sslEnabled = webserver.isNetworkSecureSSL();

        if (webserver.isFileVirtualHostsEnabled()) {

            webserver.updateWebserverVirtualHosts("etc_test/virtualhosts.properties");
            performTestsVirtual();

            hostname = "127.0.0.1";
            performTests(false);

        } else if (webserver.isSessionAuthenticationEnabled()) {

            new CheckRequest("22- AuthenticationGranted1", hostname, port, "/login", sslEnabled, 404, notfound);
            new CheckRequest("22- AuthenticationRefused1", hostname, port, "/login/admin/", sslEnabled, 401, notfound);
            new CheckRequest("22- AuthenticationGranted2", hostname, port, "/login/admin/free", sslEnabled, 404, notfound);
            new CheckRequest("22- AuthenticationRefused2", hostname, port, "/admin", sslEnabled, 401, notfound);
            new CheckRequest("22- AuthenticationGranted3", hostname, port, "/admin/free", sslEnabled, 404, notfound);

        } else {

            performTests(true);

        }

        webserver.stop();
    }

    private void performTestsVirtual() {

        String virtual = "virtual ";

        // + admin page
        // + ressources pages (FolderWebsocket)
        // + white space in files and folder
        new CheckRequest("22- Page white space", hostname, port, "/white space page.html", sslEnabled, 200, virtual + "white space page.htm");
        new CheckRequest("22- Page", hostname, port, "/page.html", sslEnabled, 200, virtual + "page.htm");
        new CheckRequest("22- Page admin class", hostname, port, "/admin/ChangeProperties", sslEnabled, 200, "<!DOCTYPE html>");
        new CheckRequest("22- Page admin", hostname, port, "/admin/changePassword.html", sslEnabled, 200, virtual + "<!DOCTYPE html>");
        new CheckRequest("22- Page admin package", hostname, port, "/admin/package.html", sslEnabled, 200, "admin package");
        // under linux type command: www$> mkfifo pipe                        
        new CheckRequest("13- NotRegularFileNotSymlink", hostname, port, "/pipe", sslEnabled, 404, notfound);
        new CheckRequest("12- Class", hostname, port, "/test/test", sslEnabled, 200, virtual + "test class");
        new CheckRequest("12- PluginClass", hostname, port, "/plugin_browse/indexBrowse", sslEnabled, 200, virtual + "index plugin browse");

        if (this.webserver.isFileFollowSymLinks()) {
            new CheckRequest("15- FollowSymlink", hostname, port, "/page_sym_follow.html", sslEnabled, 200, virtual + "symlink page.html");
        } else {
            new CheckRequest("14- NotFollowSymlink", hostname, port, "/page_sym_follow.html", sslEnabled, 404, notfound);
        }

        if (this.webserver.isFileFolderBrowsingEnabled()) {

            new CheckRequest("21- FolderBrowse", hostname, port, "/test_folder_browse", sslEnabled, 200, folder);
            new CheckRequest("9 - PluginPackage", hostname, port, "/plugin_browse", sslEnabled, 200, folder);
            new CheckRequest("9 - Package", hostname, port, "/test_browse", sslEnabled, 200, folder);
            new CheckRequest("6 - PluginFolder", hostname, port, "/folder_browse", sslEnabled, 200, folder);

        } else {
            new CheckRequest("22- Index", hostname, port, "/", sslEnabled, 200, virtual + "index.htm");
            new CheckRequest("22- Utils.loadPage", hostname, port, "/websocket/Chat", sslEnabled, 200, "<!DOCTYPE html>");
            new CheckRequest("20- FolderIndexPlugin1", hostname, port, "/test_folder_index1", sslEnabled, 200, "index.htm");
            new CheckRequest("20- FolderIndexPlugin2", hostname, port, "/test_folder_index2", sslEnabled, 200, "index.html");
            new CheckRequest("20- FolderIndexPlugin3", hostname, port, "/test_folder_index3", sslEnabled, 200, "default.htm");
            new CheckRequest("20- FolderIndexPlugin4", hostname, port, "/test_folder_index4", sslEnabled, 200, "default.html");
            new CheckRequest("19- Page admin index", hostname, port, "/admin", sslEnabled, 200, "<!DOCTYPE html>");
            new CheckRequest("19- FolderIndexPackage", hostname, port, "/test_folder_package_index", sslEnabled, 200, virtual + "index package");
            new CheckRequest("18- FolderIndexPlugin1", hostname, port, "/test_folder_plugin_index1", sslEnabled, 200, "plugin index.htm");
            new CheckRequest("18- FolderIndexPlugin2", hostname, port, "/test_folder_plugin_index2", sslEnabled, 200, "plugin index.html");
            new CheckRequest("18- FolderIndexPlugin3", hostname, port, "/test_folder_plugin_index3", sslEnabled, 200, "plugin default.htm");
            new CheckRequest("18- FolderIndexPlugin4", hostname, port, "/test_folder_plugin_index4", sslEnabled, 200, "plugin default.html");
            new CheckRequest("17- FolderIndexPlugin5", hostname, port, "/test_folder_plugin_index5", sslEnabled, 200, "index plugin");
            new CheckRequest("16- FolderIndexPluginNotFound", hostname, port, "/test_folder_plugin_index6", sslEnabled, 404, notfound);
            new CheckRequest("8 - PackageIndex", hostname, port, "/test", sslEnabled, 200, virtual + "index package test");
            new CheckRequest("8 - PluginPackageIndex", hostname, port, "/plugin", sslEnabled, 200, virtual + "index plugin");
            new CheckRequest("7 - PluginFolderIndex6", hostname, port, "/folder_index6", sslEnabled, 404, notfound);
            new CheckRequest("5 - PluginFolderIndex2", hostname, port, "/folder_index2", sslEnabled, 200, "index.htm");
            new CheckRequest("5 - PluginFolderIndex3", hostname, port, "/folder_index3", sslEnabled, 200, "index.html");
            new CheckRequest("5 - PluginFolderIndex4", hostname, port, "/folder_index4", sslEnabled, 200, "default.htm");
            new CheckRequest("5 - PluginFolderIndex5", hostname, port, "/folder_index5", sslEnabled, 200, "default.html");
            new CheckRequest("4 - PluginFolderIndex1", hostname, port, "/folder_index1", sslEnabled, 200, "index1");
            new CheckRequest("3 - PluginFolderIndexNotFound", hostname, port, "/plugin_browse", sslEnabled, 404, notfound);
        }

        new CheckRequest("2 - PluginFolderNotFound", hostname, port, "/test1", sslEnabled, 404, notfound);
        new CheckRequest("1 - PluginClassNotFound", hostname, port, "/index.class", sslEnabled, 404, notfound);

        if (this.webserver.isWebsocketEnabled()) {

            try {
                new CheckWebSocketSynchronizedText("11- CheckWebSocketSynchronizedText", hostname, port, "/websocket/Chat", sslEnabled, 101, virtual + "test websocket");
            } catch (SSLHandshakeException ex) {

                System.err.println("Fail : 11- CheckWebSocketSynchronizedText: " + ex.toString());

            }
            new CheckWebSocketSynchronizedObject("11- CheckWebSocketSynchronizedObject", hostname, port, "/websocket/Chat", sslEnabled, 101, virtual + "test websocket");

            if (this.webserver.isNetworkSecureSSLEnforceValidation()) {
                try {
                    new CheckWebSocketSynchronizedText("11- CheckWebSocketSynchronizedTextSSLFAILED", hostname, port, "/websocket/Chat", sslEnabled, true, 101, virtual + "test syncronized client text");
                } catch (SSLHandshakeException ex) {

                    System.out.println("Pass : 11- CheckWebSocketSynchronizedTextSSLFAILED");

                }
                /*
                new CheckWebSocketSynchronizedObjectClient("11- CheckWebSocketSynchronizedObjectClient", hostname, port, "/websocket/Chat2", sslEnabled, true, 101, virtual + "test syncronized client object");
                new CheckWebSocketTextClient("11- CheckWebSocketTextClient", hostname, port, "/websocket/Chat2", sslEnabled, true, 101, "test client text");
                new CheckWebSocketObjectClient("11- CheckWebSocketObjectClient", hostname, port, "/websocket/Chat2", sslEnabled, true, 101, "test client object");
                new CheckWebSocketSynchronizedText("11- CheckWebSocketServerText", hostname, port, "/websocket/ServerPage", sslEnabled, true, 101, virtual + "test websocket server");
                new CheckWebSocketSynchronizedObject("11- CheckWebSocketServerObject", hostname, port, "/websocket/ServerPage", sslEnabled, true, 101, virtual + "test websocket server");
                new CheckWebSocketSynchronizedObject("11- CheckWebSocketServerObjectNotFound", hostname, port, "/websocket/NotFound", sslEnabled, true, 404, "");
                 */

            } else {
                new CheckWebSocketSynchronizedTextClient("11- CheckWebSocketSynchronizedTextClient", hostname, port, "/websocket/Chat", sslEnabled, 101, virtual + "test syncronized client text");

                new CheckWebSocketSynchronizedObjectClient("11- CheckWebSocketSynchronizedObjectClient", hostname, port, "/websocket/Chat", sslEnabled, 101, virtual + "test syncronized client object");
                new CheckWebSocketTextClient("11- CheckWebSocketTextClient", hostname, port, "/websocket/Chat", sslEnabled, 101, "test client text");
                new CheckWebSocketObjectClient("11- CheckWebSocketObjectClient", hostname, port, "/websocket/Chat", sslEnabled, 101, "test client object");

                try {
                    new CheckWebSocketSynchronizedText("11- CheckWebSocketServerText", hostname, port, "/websocket/ServerPage", sslEnabled, 101, virtual + "test websocket server");
                } catch (SSLHandshakeException ex) {

                    System.err.println("Fail : 11- CheckWebSocketServerText: " + ex.toString());

                }
                new CheckWebSocketSynchronizedObject("11- CheckWebSocketServerObject", hostname, port, "/websocket/ServerPage", sslEnabled, 101, virtual + "test websocket server");
                new CheckWebSocketSynchronizedObject("11- CheckWebSocketServerObjectNotFound", hostname, port, "/websocket/NotFound", sslEnabled, 404, "");

            }

        } else {
            new CheckWebSocketSynchronizedTextBadRequest("10- CheckWebSocketSynchronizedTextBadRequest", hostname, port, "/websocket/Chat", sslEnabled, 400, badrequest);
            new CheckWebSocketSynchronizedObject("10- CheckWebSocketSynchronizedTextBadRequestNotFound", hostname, port, "/websocket/NotFound", sslEnabled, 404, "");
        }
    }

    private void performTests(boolean sslEnforceValidation) {

        new CheckRequest("22- Page white space", hostname, port, "/white space page.html", sslEnabled, 200, "white space page.htm");
        new CheckRequest("22- Page", hostname, port, "/page.html", sslEnabled, 200, "page.htm");
        new CheckRequest("22- Page admin class", hostname, port, "/admin/ChangeProperties", sslEnabled, 200, "<!DOCTYPE html>");
        new CheckRequest("22- Page admin", hostname, port, "/admin/changePassword.html", sslEnabled, 200, "<!DOCTYPE html>");
        new CheckRequest("22- Page admin package", hostname, port, "/admin/package.html", sslEnabled, 200, "admin package");
        new CheckRequest("22- Utils.loadPage", hostname, port, "/websocket/Chat", sslEnabled, 200, "<!DOCTYPE html>");

        // under linux type command: www$> mkfifo pipe                        
        new CheckRequest("13- NotRegularFileNotSymlink", hostname, port, "/pipe", sslEnabled, 404, notfound);
        new CheckRequest("12- Class", hostname, port, "/test/test", sslEnabled, 200, "test class");
        new CheckRequest("12- PluginClass", hostname, port, "/plugin_browse/indexBrowse", sslEnabled, 200, "index plugin browse");

        if (this.webserver.isFileFollowSymLinks()) {
            new CheckRequest("15- FollowSymlink", hostname, port, "/page_sym_follow.html", sslEnabled, 200, "symlink page.html");
        } else {
            new CheckRequest("14- NotFollowSymlink", hostname, port, "/page_sym_follow.html", sslEnabled, 404, notfound);
        }

        if (this.webserver.isFileFolderBrowsingEnabled()) {

            new CheckRequest("21- FolderBrowse", hostname, port, "/test_folder_browse", sslEnabled, 200, folder);
            new CheckRequest("9 - PluginPackage", hostname, port, "/plugin_browse", sslEnabled, 200, folder);
            new CheckRequest("9 - Package", hostname, port, "/test_browse", sslEnabled, 200, folder);
            new CheckRequest("6 - PluginFolder", hostname, port, "/folder_browse", sslEnabled, 200, folder);

        } else {
            new CheckRequest("22- Index", hostname, port, "/", sslEnabled, 200, "index.htm");
            new CheckRequest("20- FolderIndexPlugin1", hostname, port, "/test_folder_index1", sslEnabled, 200, "index.htm");
            new CheckRequest("20- FolderIndexPlugin2", hostname, port, "/test_folder_index2", sslEnabled, 200, "index.html");
            new CheckRequest("20- FolderIndexPlugin3", hostname, port, "/test_folder_index3", sslEnabled, 200, "default.htm");
            new CheckRequest("20- FolderIndexPlugin4", hostname, port, "/test_folder_index4", sslEnabled, 200, "default.html");
            new CheckRequest("19- Page admin index", hostname, port, "/admin", sslEnabled, 200, "<!DOCTYPE html>");
            new CheckRequest("19- FolderIndexPackage", hostname, port, "/test_folder_package_index", sslEnabled, 200, "index package");
            new CheckRequest("18- FolderIndexPlugin1", hostname, port, "/test_folder_plugin_index1", sslEnabled, 200, "plugin index.htm");
            new CheckRequest("18- FolderIndexPlugin2", hostname, port, "/test_folder_plugin_index2", sslEnabled, 200, "plugin index.html");
            new CheckRequest("18- FolderIndexPlugin3", hostname, port, "/test_folder_plugin_index3", sslEnabled, 200, "plugin default.htm");
            new CheckRequest("18- FolderIndexPlugin4", hostname, port, "/test_folder_plugin_index4", sslEnabled, 200, "plugin default.html");
            new CheckRequest("17- FolderIndexPlugin5", hostname, port, "/test_folder_plugin_index5", sslEnabled, 200, "index plugin");
            new CheckRequest("16- FolderIndexPluginNotFound", hostname, port, "/test_folder_plugin_index6", sslEnabled, 404, notfound);
            new CheckRequest("8 - PackageIndex", hostname, port, "/test", sslEnabled, 200, "index package test");
            new CheckRequest("8 - PluginPackageIndex", hostname, port, "/plugin", sslEnabled, 200, "index plugin");
            new CheckRequest("7 - PluginFolderIndex6", hostname, port, "/folder_index6", sslEnabled, 404, notfound);
            new CheckRequest("5 - PluginFolderIndex2", hostname, port, "/folder_index2", sslEnabled, 200, "index.htm");
            new CheckRequest("5 - PluginFolderIndex3", hostname, port, "/folder_index3", sslEnabled, 200, "index.html");
            new CheckRequest("5 - PluginFolderIndex4", hostname, port, "/folder_index4", sslEnabled, 200, "default.htm");
            new CheckRequest("5 - PluginFolderIndex5", hostname, port, "/folder_index5", sslEnabled, 200, "default.html");
            new CheckRequest("4 - PluginFolderIndex1", hostname, port, "/folder_index1", sslEnabled, 200, "index1");
            new CheckRequest("3 - PluginFolderIndexNotFound", hostname, port, "/plugin_browse", sslEnabled, 404, notfound);
        }

        new CheckRequest("2 - PluginFolderNotFound", hostname, port, "/test1", sslEnabled, 404, notfound);
        new CheckRequest("1 - PluginClassNotFound", hostname, port, "/index.class", sslEnabled, 404, notfound);

        if (this.webserver.isWebsocketEnabled()) {
            try {
                new CheckWebSocketSynchronizedText("11- CheckWebSocketSynchronizedText", hostname, port, "/websocket/Chat", sslEnabled, 101, "test websocket");
            } catch (SSLHandshakeException ex) {

                System.err.println("Fail : 11- CheckWebSocketSynchronizedTextClientSSLFAILED: " + ex.toString());

            }
            new CheckWebSocketSynchronizedObject("11- CheckWebSocketSynchronizedObject", hostname, port, "/websocket/Chat", sslEnabled, 101, "test websocket");

            if (this.webserver.isNetworkSecureSSLEnforceValidation()) {
                try {
                    new CheckWebSocketSynchronizedText("11- CheckWebSocketSynchronizedTextClientSSLFAILED", hostname, port, "/websocket/Chat", sslEnabled, sslEnforceValidation, 101, "test syncronized client text");
                } catch (SSLHandshakeException ex) {

                    System.out.println("Pass : 11- CheckWebSocketSynchronizedTextClientSSLFAILED");

                }
                /*
                new CheckWebSocketSynchronizedObjectClient("11- CheckWebSocketSynchronizedObjectClient", hostname, port, "/websocket/Chat2", sslEnabled, sslEnforceValidation, 101, "test syncronized client object");
                new CheckWebSocketTextClient("11- CheckWebSocketTextClient", hostname, port, "/websocket/Chat2", sslEnabled, sslEnforceValidation, 101, "test client text");
                new CheckWebSocketObjectClient("11- CheckWebSocketObjectClient", hostname, port, "/websocket/Chat2", sslEnabled, sslEnforceValidation, 101, "test client object");
                new CheckWebSocketSynchronizedText("11- CheckWebSocketServerText", hostname, port, "/websocket/ServerPage", sslEnabled, sslEnforceValidation, 101, "test websocket server");
                new CheckWebSocketSynchronizedObject("11- CheckWebSocketServerObject", hostname, port, "/websocket/ServerPage", sslEnabled, sslEnforceValidation, 101, "test websocket server");
                new CheckWebSocketSynchronizedObject("11- CheckWebSocketServerObjectNotFound", hostname, port, "/websocket/NotFound", sslEnabled, sslEnforceValidation, 404, "");

                 */
            } else {
                new CheckWebSocketSynchronizedTextClient("11- CheckWebSocketSynchronizedTextClient", hostname, port, "/websocket/Chat", sslEnabled, 101, "test syncronized client text");

                new CheckWebSocketSynchronizedObjectClient("11- CheckWebSocketSynchronizedObjectClient", hostname, port, "/websocket/Chat", sslEnabled, 101, "test syncronized client object");
                new CheckWebSocketTextClient("11- CheckWebSocketTextClient", hostname, port, "/websocket/Chat", sslEnabled, 101, "test client text");
                new CheckWebSocketObjectClient("11- CheckWebSocketObjectClient", hostname, port, "/websocket/Chat", sslEnabled, 101, "test client object");

                try {
                    new CheckWebSocketSynchronizedText("11- CheckWebSocketServerText", hostname, port, "/websocket/ServerPage", sslEnabled, 101, "test websocket server");
                } catch (SSLHandshakeException ex) {

                    System.err.println("Fail : 11- CheckWebSocketServerText: " + ex.toString());

                }
                new CheckWebSocketSynchronizedObject("11- CheckWebSocketServerObject", hostname, port, "/websocket/ServerPage", sslEnabled, 101, "test websocket server");
                new CheckWebSocketSynchronizedObject("11- CheckWebSocketServerObjectNotFound", hostname, port, "/websocket/NotFound", sslEnabled, 404, "");
            }

        } else {
            new CheckWebSocketSynchronizedTextBadRequest("10- CheckWebSocketSynchronizedTextBadRequest", hostname, port, "/websocket/Chat", sslEnabled, 400, badrequest);
            new CheckWebSocketSynchronizedObject("10- CheckWebSocketSynchronizedTextBadRequestNotFound", hostname, port, "/websocket/NotFound", sslEnabled, 404, "");
        }
    }

    private static Path[] getPropertiesFiles(Path etcPath) {
        Path[] result;
        ArrayList<Path> pathList = new ArrayList();

        try {
            File folder = etcPath.toFile();

            if (folder.isDirectory()) {

                for (File file : folder.listFiles()) {

                    if (file.getName().endsWith("webserver.properties")) {
                        pathList.add(Paths.get(file.getAbsolutePath()));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get properties files: " + e);
        }
        /*
        try {
            Files.walkFileTree(etcPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    if (file.toString().endsWith("webserver.properties")) {
                        //                    System.out.println("file: " + file.toString());
                        pathList.add(file);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to get properties files: " + e);
        }
         */

        result = new Path[pathList.size()];
        result = pathList.toArray(result);

        return result;
    }
}
