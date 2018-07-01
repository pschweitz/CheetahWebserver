package org.cheetah.webserver;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.simpleframework.http.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Philippe Schweitzer
 *
 */
public class RessourceFinder {

    private static Logger logger = LoggerFactory.getLogger(RessourceFinder.class);

    //   public static FileInformation[] listRessources(String packagePageName, String packageName, Path folderRoot, String target, String page, CheetahWebserver webserver, Request request) {
    public static FileInformation[] listRessources(CheetahWebserver webserver, Request request) {

        FileInformation[] result = new FileInformation[0];
        ArrayList<FileInformation> fileInformationList = new ArrayList();

        TreeMap<String, FileInformation> folderMap = new TreeMap();
        TreeMap<String, FileInformation> fileMap = new TreeMap();

        String fileRoot = webserver.getFileDefaultRoot();
        String packagePageName = webserver.getPackagePageName();
        String packageRootName = webserver.getPackageRootName();

        org.simpleframework.http.Path path = request.getPath();
        String directory = path.getDirectory();
        String name = path.getName();
        String page;

        if (name != null) {
            page = directory + name;
        } else {
            page = directory;
        }

        if (webserver.isFileVirtualHostsEnabled()) {

            String host = webserver.getHostPort(request);
            if (host.contains(":")) {
                host = host.substring(0, host.indexOf(':'));
            }

            String virtualFileDefaultRoot = webserver.getWebserverVirtualHosts().getString(host);

            if (!virtualFileDefaultRoot.equals("")) {
                fileRoot = virtualFileDefaultRoot;
                packagePageName = packagePageName + "." + host;
            }
        }

        String target = fileRoot + page;
        Path folderRoot = Paths.get(target).toAbsolutePath();

        if (!page.endsWith("/")) {
            page = page + "/";
        }

        String packageName = page.substring(1);

        packageName = packageName.replaceAll("/", ".");
        packageName = "." + packageName;

        if (packageName.lastIndexOf('.') > -1) {
            if (packageName.lastIndexOf('.') == packageName.length() - 1) {
                packageName = packageName.substring(0, packageName.length() - 1);
            }
        }
        if (packageName.startsWith("admin")) {
            packagePageName = packageRootName + ".page";
        }

        if (packageName.startsWith("ressources")) {
            packagePageName = packageRootName + ".page";
        }

        if (!webserver.isFileFolderFilesystemOnly()) {
            logger.debug("PACKAGE: " + packagePageName + packageName);

            List<FileInformation> packages = RessourceFinder.getPackageFromPackage(packagePageName + packageName, webserver.getClassLoader());

            for (FileInformation packageNameInJar : packages) {

                logger.debug("PACKAGE Folder: " + packageNameInJar.getName());
                folderMap.put(packageNameInJar.getName(), packageNameInJar);
            }

            String pluginPackageName = packageName;

            if (pluginPackageName.startsWith(".")) {
                pluginPackageName = pluginPackageName.substring(1);
            }

            List<FileInformation> plugin = RessourceFinder.getFolderFromPlugin(pluginPackageName, webserver.getClassLoader());

            for (FileInformation packageNameInJar : plugin) {

                logger.debug("PLUGIN Folder: " + packageNameInJar.getName());
                folderMap.put(packageNameInJar.getName(), packageNameInJar);
            }

            List<Class<?>> classes = RessourceFinder.getClassesFromPackage(packagePageName + packageName, webserver.getClassLoader());

            for (Class c : classes) {

                logger.debug("PACKAGE CLASS: " + packagePageName + packageName + "." + c.getSimpleName());
                if (Page.class.isAssignableFrom(c)) {
                    FileInformation fileInformation = new FileInformation(c.getSimpleName(), 0l, 0l, true, true, false);
                    fileMap.put(fileInformation.getName(), fileInformation);
                }
            }

            List<FileInformation> files = RessourceFinder.getRessourcesFromPackage(pluginPackageName, webserver.getClassLoader());

            for (FileInformation file : files) {

                logger.debug("PLUGIN FILE: " + pluginPackageName + "." + file.getName());
                fileMap.put(file.getName(), file);
            }
        }

        if (Files.exists(folderRoot)) {
            try  {
                DirectoryStream<Path> stream = Files.newDirectoryStream(folderRoot);
                Iterator<Path> iterator = stream.iterator();
                while (iterator.hasNext()){
                    Path filePath = iterator.next();
                    File file = filePath.toFile();

                    String fileName = file.getName();
                    boolean displayfile = false;
                    FileInformation fileInformation = null;
                    

                    if (Files.isDirectory(filePath, LinkOption.NOFOLLOW_LINKS)) {
                        fileInformation = new FileInformation(file.getName(), file.length(), file.lastModified(), false, false, true);
                        folderMap.put(fileName, fileInformation);
                        
                    }

                    if (Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
                        fileInformation = new FileInformation(file.getName(), file.length(), file.lastModified(), false, false, false);
                        displayfile = true;
                    }
                    if (Files.isSymbolicLink(filePath) && webserver.isFileFollowSymLinks()) {
                        fileInformation = new FileInformation(file.getName(), file.length(), file.lastModified(), false, false, false);
                        displayfile = true;
                    }

                    if (displayfile) {
                        fileMap.put(fileName, fileInformation);
                    }
                }
            } catch (Exception ex) {
                logger.error("Failed to browse folder: " + folderRoot + ": " + ex.toString());
            }
        }

        for (FileInformation file : folderMap.values()) {
            if (webserver.isSessionAuthenticationEnabled() && fileRequireAuthentication(target, page + file.getName(), webserver)) {
                if (!webserver.getUsername(request).equals("")) {
                    fileInformationList.add(file);
                }
            } else {
                fileInformationList.add(file);
            }
        }

        for (FileInformation file : fileMap.values()) {
            if (webserver.isSessionAuthenticationEnabled() && fileRequireAuthentication(target, page + file.getName(), webserver)) {
                if (!webserver.getUsername(request).equals("")) {
                    fileInformationList.add(file);
                }
            } else {
                fileInformationList.add(file);
            }
        }

        result = fileInformationList.toArray(result);
        return result;
    }

    public static URL[] getJarURLs(URLClassLoader cl) {

        URL[] result = cl.getURLs();
        ArrayList<URL> urls = new ArrayList();
        String[] classPaths = CheetahClassLoader.getClassPaths();

        for (URL url : result) {

            try {
                Path jarPath = Paths.get(url.toURI());

                for (String classPathString : classPaths) {

                    Path classPath = Paths.get(classPathString).toAbsolutePath();

                    if (jarPath.startsWith(classPath)) {
                        urls.add(url);
                    }
                }

            } catch (URISyntaxException ex) {
            }
        }

        result = new URL[urls.size()];
        result = urls.toArray(result);

        return result;
    }

    private static List<Class<?>> getClassesFromPackage(String pckgname, CheetahClassLoader cl) {
        logger.trace("START: getClassesFromPackage(String, URLClassLoader)");

        ArrayList<Class<?>> result = new ArrayList();
        ArrayList<File> files = new ArrayList();
        HashMap<File, String> packageNames = null;

        try {

            for (URL jarURL : getJarURLs(cl)) {

//                logger.trace("JAR in classpath CLASS: " + jarURL);
                getClassesInSamePackageFromJar(result, pckgname, jarURL.getPath(), cl);
                String path = pckgname;
                Enumeration<URL> resources = cl.getResources(path);
                File file = null;
                while (resources.hasMoreElements()) {
                    String path2 = resources.nextElement().getPath();
                    file = new File(URLDecoder.decode(path2, "UTF-8"));
                    files.add(file);
                }
                if (packageNames == null) {
                    packageNames = new HashMap<File, String>();
                }
                packageNames.put(file, pckgname);
            }
        } catch (NullPointerException x) {
            // throw new ClassNotFoundException(pckgname + " does not appear to be a valid package (Null pointer exception)");
        } catch (UnsupportedEncodingException encex) {
            // throw new ClassNotFoundException(pckgname + " does not appear to be a valid package (Unsupported encoding)");
        } catch (IOException ioex) {
            // throw new ClassNotFoundException("IOException was thrown when trying to get all resources for " + pckgname);
        }

        for (File file : files) {
            if (file.exists()) {

                String[] filesPath = file.list();
                for (String filePath : filesPath) {
                    if (filePath.endsWith(".class")) {
                        try {
                            result.add(Class.forName(packageNames.get(file) + '.' + filePath.substring(0, filePath.length() - 6)));
                        } catch (Throwable e) {
                        }
                    }
                }
            }
        }

        logger.trace(" END : getClassesFromPackage(String, URLClassLoader)");
        return result;
    }

    /**
     * Returns the list of classes in the same directories as Classes in
     * <code>classes</code>.
     *
     * @param result
     * @param classes
     * @param jarPath
     */
    private static void getClassesInSamePackageFromJar(List<Class<?>> result, String packageName, String jarPath, CheetahClassLoader cl) {
        logger.trace("START: getClassesInSamePackageFromJar(List, String, String, URLClassLoader)");

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarPath);

            Enumeration<JarEntry> en = jarFile.entries();
            while (en.hasMoreElements()) {
                try {
                    JarEntry entry = en.nextElement();
                    String entryName = entry.getName();

                    packageName = packageName.replace('.', '/');

//                    logger.debug("entryName: " + entryName + " JAR: " + jarPath);

                    if (entryName != null && entryName.endsWith(".class") && entryName.startsWith(packageName) && !entryName.substring(packageName.length() + 1).contains("/")) {

//                    logger.debug("entryName sub: " + entryName.substring(packageName.length() + 1));
                        try {
                            Class<?> entryClass = cl.loadClassPlugin(entryName.substring(0, entryName.length() - 6).replace('/', '.'));
                            if (entryClass != null) {
                                result.add(entryClass);
                            }
                        } catch (Throwable e) {
                            logger.debug("Error instanciating: " + entryName + " " + e.toString());
                        }
                    }
                } catch (Exception e) {

                }
            }
        } catch (Exception e) {

        } finally {
            try {
                if (jarFile != null) {
                    jarFile.close();
                }
            } catch (Exception e) {
            }
        }

        logger.trace(" END : getClassesInSamePackageFromJar(List, String, String, URLClassLoader)");
    }

    private static List<FileInformation> getPackageFromPackage(String pckgname, URLClassLoader cl) {
        logger.trace("START: getPackageFromPackage(String, URLClassLoader)");

        ArrayList<FileInformation> result = new ArrayList();

        try {

            for (URL jarURL : getJarURLs(cl)) {

//                logger.debug("JAR in classpath PACKAGE: " + jarURL);
                getPackageInPackageFromJar(result, pckgname, jarURL.getPath());
            }
        } catch (NullPointerException e) {
        }

        logger.trace(" END : getPackageFromPackage(String, ClassLoader)");
        return result;
    }

    private static void getPackageInPackageFromJar(List<FileInformation> result, String packageName, String jarPath) {
        logger.trace("START: getPackageInPackageFromJar(List, String, String)");

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarPath);

            Enumeration<JarEntry> en = jarFile.entries();
            while (en.hasMoreElements()) {
                JarEntry entry = en.nextElement();
                String entryName = entry.getName();

                packageName = packageName.replace('.', '/');

                if (entryName != null && entryName.endsWith("/") && entryName.startsWith(packageName + "/")) {

                    try {
                        String packageEntryName = entryName.substring(packageName.length() + 1);
                        packageEntryName = packageEntryName.substring(0, packageEntryName.indexOf("/"));

                        FileInformation fileInformation = new FileInformation(packageEntryName, 4096l, entry.getLastModifiedTime().toMillis(), false, true, true);
                        if (!result.contains(fileInformation)) {
                            result.add(fileInformation);
                        }
                    } catch (Throwable e) {
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            try {
                if (jarFile != null) {
                    jarFile.close();
                }
            } catch (Exception e) {
            }
        }

        logger.trace(" END : getPackageInPackageFromJar(List, String, String)");
    }

    private static List<FileInformation> getFolderFromPlugin(String pckgname, URLClassLoader cl) {
        logger.trace("START: getFolderFromPlugin(String, URLClassLoader)");

        ArrayList<FileInformation> result = new ArrayList();

        try {

            for (URL jarURL : getJarURLs(cl)) {

//                logger.debug("JAR in classpath PLUGIN: " + jarURL);
                getFolderInPluginFromJar(result, pckgname, jarURL.getPath());
            }
        } catch (NullPointerException e) {
        }

        logger.trace(" END : getFolderFromPlugin(String, ClassLoader)");
        return result;
    }

    private static void getFolderInPluginFromJar(List<FileInformation> result, String packageName, String jarPath) {
        logger.trace("START: getFolderInPluginFromJar(List, String, String)");

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarPath);

            Enumeration<JarEntry> en = jarFile.entries();
            while (en.hasMoreElements()) {
                JarEntry entry = en.nextElement();
                String entryName = entry.getName();

                packageName = packageName.replace('.', '/');

                if (packageName.equals("")) {
                    if (entryName != null && entryName.endsWith("/")) {

                        try {
                            String packageEntryName = entryName.substring(packageName.length());
                            packageEntryName = packageEntryName.substring(0, packageEntryName.indexOf("/"));

                            FileInformation fileInformation = new FileInformation(packageEntryName, 4096l, entry.getLastModifiedTime().toMillis(), false, true, true);
                            if (!result.contains(fileInformation)) {
                                result.add(fileInformation);
                            }
                        } catch (Throwable e) {
                        }
                    }
                } else if (entryName != null && entryName.endsWith("/") && entryName.startsWith(packageName + "/")) {

                    try {
                        String packageEntryName = entryName.substring(packageName.length() + 1);
                        packageEntryName = packageEntryName.substring(0, packageEntryName.indexOf("/"));

                        FileInformation fileInformation = new FileInformation(packageEntryName, 4096l, entry.getLastModifiedTime().toMillis(), false, true, true);
                        if (!result.contains(fileInformation)) {
                            result.add(fileInformation);
                        }
                    } catch (Throwable e) {
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            try {
                if (jarFile != null) {
                    jarFile.close();
                }
            } catch (Exception e) {
            }
        }

        logger.trace(" END : getFolderInPluginFromJar(List, String, String)");
    }

    private static List<FileInformation> getRessourcesFromPackage(String pckgname, URLClassLoader cl) {
        logger.trace("START: getRessourcesFromPackage(String, URLClassLoader)");

        ArrayList<FileInformation> result = new ArrayList();

        try {
            for (URL jarURL : getJarURLs(cl)) {

//                logger.trace("JAR in classpath PACKAGE: " + jarURL);
                getRessourcesInPackageFromJar(result, pckgname, jarURL.getPath());
            }
        } catch (NullPointerException x) {

        }

        logger.trace(" END : getRessourcesFromPackage(String, URLClassLoader)");
        return result;
    }

    private static void getRessourcesInPackageFromJar(List<FileInformation> result, String packageName, String jarPath) {
        logger.trace("START: getRessourcesInPackageFromJar(List, String, String)");

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarPath);

            Enumeration<JarEntry> en = jarFile.entries();
            while (en.hasMoreElements()) {
                JarEntry entry = en.nextElement();
                String entryName = entry.getName();

                packageName = packageName.replace('.', '/');

                if (packageName.equals("")) {                    

                    if (entryName != null && !entryName.endsWith(".class") && !entryName.contains("/")) {

                        try {
                            String packageEntryName = entryName.substring(packageName.length());

                            //FileInformation fileInformation = new FileInformation(packageEntryName, 4096l, entry.getLastModifiedTime().toMillis(), false, true, false);
                            FileInformation fileInformation = new FileInformation(packageEntryName, entry.getSize(), entry.getLastModifiedTime().toMillis(), false, true, false);
                            if (!result.contains(fileInformation)) {
                                result.add(fileInformation);
                            }
                        } catch (Throwable e) {
                        }
                    }
                } else if (entryName != null && !entryName.endsWith(".class") && !entryName.endsWith("/") && entryName.startsWith(packageName) && !entryName.substring(packageName.length() + 1).contains("/")) {

                    try {
                        String packageEntryName = entryName.substring(packageName.length() + 1);

                        //FileInformation fileInformation = new FileInformation(packageEntryName, 4096l, entry.getLastModifiedTime().toMillis(), false, true, false);
                        FileInformation fileInformation = new FileInformation(packageEntryName, entry.getSize(), entry.getLastModifiedTime().toMillis(), false, true, false);
                        
                        if (!result.contains(fileInformation)) {
                            result.add(fileInformation);
                        }
                    } catch (Throwable e) {
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            try {
                if (jarFile != null) {
                    jarFile.close();
                }
            } catch (Exception e) {
            }
        }

        logger.trace(" END : getRessourcesInPackageFromJar(List, String, String)");
    }
    
    
    public static FileInformation getFileInformationFromRessourceURL(URL url) {
        logger.trace("START: getFileInformationFromRessourceURL(URL)");
        FileInformation result = new FileInformation("",0l,0l,false,false,false);

        // jar:file:/users/JAVA_PROJECT/CheetahWebserver/CheetahWebserver/plugins/file-icons.zip!/ressources/file-icons/32px/folder-new.png
                
        String[] urlString = {url.toString()};
        String fileName = "";
        
        if(url.toString().contains("!")){
            urlString = url.toString().split("!");
            fileName = urlString[1];
        }
        
        if(urlString[0].startsWith("jar")){
            urlString[0] = urlString[0].substring(4);
        }
        if(urlString[0].startsWith("file")){
            urlString[0] = urlString[0].substring(5);
        }
        
        if(fileName.startsWith("/")){
            fileName = fileName.substring(1);
        }
                
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(urlString[0]);

            Enumeration<JarEntry> en = jarFile.entries();
            while (en.hasMoreElements()) {
                JarEntry entry = en.nextElement();
                String entryName = entry.getName();

                entryName = entryName.replace('.', '/');
                fileName = fileName.replace('.', '/');
                
                if (fileName.equals(entryName)) {    
                    result = new FileInformation(fileName, entry.getSize(), entry.getLastModifiedTime().toMillis(), false, true, false);
                    
                    break;
                }
            }
        } catch (Exception e) {
            
            logger.error("Error reading jarfile '" + url.toString() +"' " + e.toString() );
            
        } finally {
            try {
                if (jarFile != null) {
                    jarFile.close();
                }
            } catch (Exception e) {
            }
        }

        logger.trace(" END : getFileInformationFromRessourceURL(URL)");
        return result;
    }

    private static boolean fileRequireAuthentication(String fileRoot, String filePath, CheetahWebserver webserver) {

        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }

        String target = fileRoot + filePath;
        boolean authFolder = false;

        for (String key : webserver.getAuthorizationFolder().keySet()) {
            int length = (fileRoot + key).length();

            if (target.length() >= length) {

                if (target.substring(0, length).equals(fileRoot + key)) {
                    authFolder = webserver.getAuthorizationFolder().get(key);
                    break;
                }
            }
        }

        return authFolder;
    }
}
