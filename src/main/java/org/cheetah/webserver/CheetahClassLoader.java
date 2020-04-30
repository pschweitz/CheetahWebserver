/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author phs
 */
public class CheetahClassLoader extends URLClassLoader {

    static Logger logger = LoggerFactory.getLogger(CheetahClassLoader.class);
    static String[] classpaths = {"plugins", ".", "dist", "build/libs"};

    public CheetahClassLoader(ClassLoader parent) {
        this(findJarURLsInClasspath(), parent);
    }

    protected CheetahClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        super.clearAssertionStatus();
    }

    public void initPlugins() {

        // load of plugins' init classes
        String packageName = "org.cheetah.webserver.plugin";

        ArrayList<String> pluginList = new ArrayList();

        for (URL jarfileUrl : super.getURLs()) {

            JarFile jarFile = null;
            try {

                jarFile = new JarFile(jarfileUrl.getFile());

                String jarName = jarFile.getName();

                if(jarName.contains("/")){
                    jarName = jarName.substring(jarName.lastIndexOf("/")+1);
                }

                if(!pluginList.contains(jarName)) {
                    pluginList.add(jarName);

                    Enumeration<JarEntry> en = jarFile.entries();
                    String entryName = "";

                    while (en.hasMoreElements()) {
                        JarEntry entry = en.nextElement();
                        entryName = entry.getName();
                        try {

                            packageName = packageName.replace('.', '/');

                            if (entryName != null && entryName.endsWith(".class") && entryName.startsWith(packageName) && !entryName.substring(packageName.length() + 1).contains("/")) {

                                logger.debug("Init plugin from: " + jarName);

                                String className = entryName.substring(packageName.length() + 1);
                                className = packageName.replace('/', '.') + "." + className;
                                className = className.substring(0, className.lastIndexOf("."));

                                CheetahClassLoader classloader = new CheetahClassLoader(this);
                                InputStream inputStream = jarFile.getInputStream(entry);
                                byte[] classBytes = getBytes(inputStream);

                                Class c = classloader.defineClass(className, classBytes, 0, classBytes.length);
                                c.newInstance();
                            }

                        } catch (Exception e) {
                            logger.error("Error loading plugin Init class '" + entryName + "': " + e.toString());
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

        }

    }

    private static byte[] getBytes(InputStream is) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
            byte[] buffer = new byte[0xFFFF];
            for (int len; (len = is.read(buffer)) != -1; )
                os.write(buffer, 0, len);
            os.flush();
            return os.toByteArray();
        }
    }

    public static String[] getClassPaths() {
        return classpaths;
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    /*
    public URL[] getURLs() {
        return super.getURLs();
    }
     */

    /*
    public URL[] getPluginURLs() {

        return super.getURLs();
    }
    */

    /*
    public URL[] getPluginURLs() {

        URL[] result = getURLs();
        ArrayList<URL> urls = new ArrayList();
        String[] classPaths = getClassPaths();

        for (URL url : result) {

            try {
                Path jarPath = Paths.get(url.toURI());

                for (String classPathString : classPaths) {

                    if (!(classPathString.equals(".") || classPathString.equals("dist"))) {
                        Path classPath = Paths.get(classPathString).toAbsolutePath();

                        //logger.debug("jarPath  : " + jarPath);
                        //logger.debug("classPath: " + classPath);
                        if (jarPath.startsWith(classPath)) {
                            urls.add(url);
                        }
                    }
                }

            } catch (URISyntaxException ex) {
            }
        }

        result = new URL[urls.size()];
        result = urls.toArray(result);

        return result;
    }
    */

    private static URL[] findJarURLsInClasspath() {
        URL url;

        ArrayList<JarFile> jarFiles = new ArrayList();
        ArrayList<URL> jarURLs = new ArrayList();

        for (String path : classpaths) {

            File pathFile = new File(path);
            File[] jars = pathFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {

                    return pathname.getName().toLowerCase().endsWith(".jar") || pathname.getName().toLowerCase().endsWith(".zip");
                }
            });

            if (jars != null) {

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy H:mm:ss");

                for (int i = 0; i < jars.length; i++) {
                    if (CheetahWebserver.pluginList == null) {
                        CheetahWebserver.pluginList = new ArrayList();
                    }

                    String jarName = jars[i].getName();

                    if (!CheetahWebserver.pluginList.contains(jarName)) {
                        CheetahWebserver.pluginList.add(jarName);
                        logger.debug("Found plugin: '" + jarName + "' Last modified (UTC): " + LocalDateTime.ofInstant(Instant.ofEpochMilli(jars[i].lastModified()), ZoneId.of("Z")).format(formatter));
//                    System.out.println("CHEETAH jar URL '" + jars[i].getPath() + "'");

                    }
                    try {
                        jarFiles.add(new JarFile(jars[i].getCanonicalPath()));
                        url = jars[i].toURI().toURL();

                        jarURLs.add(url);

                    } catch (Exception e) {

                    }
                }
            }
        }

        for (JarFile jarFile : jarFiles) {
            try {
                Manifest manifest = jarFile.getManifest();
                String manifestClasspath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);

                if (classpaths != null) {
                    StringTokenizer tokenize = new StringTokenizer(manifestClasspath, " ");

                    while (tokenize.hasMoreElements()) {
                        String jar = tokenize.nextToken();
                        url = new File(jar).toURI().toURL();
                        jarURLs.add(url);
                    }
                }
            } catch (Exception e) {
            }
        }

        URL[] urls = jarURLs.toArray(new URL[0]);
        return urls;
    }

    public boolean prohibitedPackage(String name) {
        boolean result = false;

        String[] prohibitedPackages = {"sun.", "java.", "javax."};

        for (String prohibitedPackage : prohibitedPackages) {
            if (name.startsWith(prohibitedPackage)) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Every request for a class passes through this method. If the requested
     * class is in "javablogging" package, it will load it using the
     * {@link CustomClassLoader#getClass()} method. If not, it will use the
     * super.loadClass() method which in turn will pass the request to the
     * parent.
     *
     * @param name Full class name
     * @throws java.lang.ClassNotFoundException
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class result = null;

//    System.out.println("CHEETAH CLASSLOADER loading class '" + name + "'");
        try {

            result = this.getParent().loadClass(name);
//    System.out.println("CHEETAH PARENT loading class '" + name + "'");

        } catch (ClassNotFoundException e) {

            try {
                if (prohibitedPackage(name)) {
                    throw new Exception();
                }

//    System.out.println("CHEETAH findLoadedClass '" + name + "'");
                result = this.findLoadedClass(name);
                if (result == null) {

//    System.out.println("CHEETAH findClass '" + name + "'");
                    result = this.findClass(name);
                    if (result != null) {
//    System.out.println("CHEETAH FOUND Class '" + name + "'");
                    } else {
//    System.out.println("CHEETAH NOT FOUND Class '" + name + "'");
                    }

                }

                /*
                if (result == null) {
                    System.out.println("CHEETAH getClass '" + name + "'");
                    result = this.getClass();
                }*/
            } catch (ClassNotFoundException ex) {

                ex.printStackTrace();
//    System.out.println("CHEETAH loading class ClassNotFoundException 2 '" + name + "'");
                //throw ex;

            } catch (NullPointerException ex) {
                ex.printStackTrace();
//    System.out.println("CHEETAH loading class Exception NULL '" + name + "': " + ex);
            } catch (Exception ex) {
                ex.printStackTrace();
//    System.out.println("CHEETAH  BIG EXCEPTION '" + name + "': " + e);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
//    System.out.println("CHEETAH loading class Exception '" + name + "': " + ex);
        }
        return result;

    }

    public Class<?> loadClassPlugin(String name) throws ClassNotFoundException {
        Class result = null;

        //System.out.println("CHEETAH CLASSLOADER loading class '" + name + "'");
        try {

            result = this.getParent().loadClass(name);
            //System.out.println("CHEETAH PARENT loading class '" + name + "'");

        } catch (ClassNotFoundException e) {

            try {
                if (prohibitedPackage(name)) {
                    throw new Exception();
                }

                //System.out.println("CHEETAH findLoadedClass '" + name + "'");
                result = this.findLoadedClass(name);
                if (result == null) {

                    //System.out.println("CHEETAH findClass '" + name + "'");
                    result = this.findClass(name);
                    if (result != null) {
                        //System.out.println("CHEETAH FOUND Class '" + name + "'");
                    } else {
                        //System.out.println("CHEETAH NOT FOUND Class '" + name + "'");
                    }

                }

                /*
                if (result == null) {
                    System.out.println("CHEETAH getClass '" + name + "'");
                    result = this.getClass();
                }*/
            } catch (ClassNotFoundException ex) {

                //System.out.println("CHEETAH loading class ClassNotFoundException 2 '" + name + "'");
                throw ex;

            } catch (NullPointerException ex) {
                //System.out.println("CHEETAH loading class Exception NULL '" + name + "': " + ex);
            } catch (Exception ex) {
                //System.out.println("CHEETAH  BIG EXCEPTION '" + name + "': " + e);
            }

        } catch (Exception e) {
            //System.out.println("CHEETAH loading class Exception '" + name + "': " + e);
        }
        return result;

    }

}
