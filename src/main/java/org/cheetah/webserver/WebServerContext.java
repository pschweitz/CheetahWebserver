/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phs
 * @param <K>
 * @param <V>
 */
public final class WebServerContext<K, V> {
    
    private static final Logger logger = LoggerFactory.getLogger(WebServerContext.class);

    //private static final WebserverContext<String, Object> instance = new WebserverContext();
    SortedMap<K, V> properties = Collections.synchronizedSortedMap(new TreeMap());
    //private ConcurrentHashSet<K, V> properties = new ConcurrentHashSet();
    private Path propertieFile;
    
    public enum PropertyType {
        
        FileDefaultPage("FileDefaultPage", String.class, false),
        FileFolderBrowsingEnabled("FileFolderBrowsingEnabled", Boolean.class, false),
        FileFolderBrowsingReadWrite("FileFolderBrowsingReadWrite", Boolean.class, false),
        FileFolderFilesystemOnly("FileFolderFilesystemOnly", Boolean.class, false),        
        FileFollowSymLinks("FileFollowSymLinks", Boolean.class, false),
        FileSessionAuthFolderFree("FileSessionAuthFolderFree", String.class, false),
        FileSessionAuthFolderMandatory("FileSessionAuthFolderMandatory", String.class, false),    
        FileUploadAdminOnly("FileUploadAdminOnly", Boolean.class, false),
        FileUploadEnabled("FileUploadEnabled", Boolean.class, false),
        FileUploadLimit("FileUploadLimit", Long.class, false),
        FileVirtualHostsEnabled("FileVirtualHostsEnabled", Boolean.class, false),
        FileDefaultRoot("FileDefaultRoot", String.class, false),
        NetworkInterface("NetworkInterface", String.class, true),
        NetworkPort("NetworkPort", Integer.class, true),
        NetworkSecureSSL("NetworkSecureSSL", Boolean.class, true),
        NetworkSecureSSLEnforceValidation("NetworkSecureSSLEnforceValidation", Boolean.class, false),     
        SessionAdminAccount("SessionAdminAccount", String.class, false),
        SessionAuthenticationEnabled("SessionAuthenticationEnabled", Boolean.class, false),
        SessionAuthenticationMechanism("SessionAuthenticationMechanism", String.class, false),
        SessionEnableBruteForceProtection("SessionEnableBruteForceProtection", Boolean.class, false),
        SessionTimeout("SessionTimeout", Integer.class, false),
        SessionUseLoginPage("SessionUseLoginPage", Boolean.class, false),
        ThreadWorkerHTTP("ThreadWorkerHTTP", Integer.class, true),
        ThreadWorkerWebsocket("ThreadWorkerWebsocket", Integer.class, true),
        WebserverMode("WebserverMode", String.class, false),
        WebserverName("WebserverName", String.class, false),
        WebsocketEnabled("WebsocketEnabled", Boolean.class, false);
        
        private final String propertyName;
        private final Class c;
        private final Boolean requireRestart;
        
        PropertyType(String propertyName, Class c, boolean requireRestart) {
            this.propertyName = propertyName;
            this.c = c;
            this.requireRestart = requireRestart;
        }
        
        public boolean isRequireRestart() {
            return this.requireRestart;
        }
        
        public static boolean isRecognized(String propertyName) {
            boolean result = false;
            
            for (PropertyType property : values()) {
                if (propertyName.equals(property.propertyName)) {
                    result = true;
                    break;
                }
            }
            
            return result;
        }
                
        public static Class getClass(String propertyName) {
            Class result = String.class;
            boolean propertyNameRecognized = false;
            
            for (PropertyType property : values()) {
                if (propertyName.equals(property.propertyName)) {
                    result = property.c;
                    propertyNameRecognized = true;
                    break;
                }
            }
            
            return result;
        }       
        
        
        public static boolean validatePropertyType(String propertyName, String propertyValue) {
            boolean result = true;
            
            Class c = getClass(propertyName);
            String className = c.getSimpleName();
            
            if (className.equals("Integer") || className.equals("Long") || className.equals("Double") || className.equals("Short") || className.equals("Float")) {
                Method method;
                try {
                    method = c.getMethod("valueOf", String.class);
                    method.invoke(null, propertyValue);
                    
                } catch (NoSuchMethodException e) {
                    logger.error("Type error with property \"" + propertyName + "\" for value: \"" + propertyValue + "\", value must be instance of " + c.getSimpleName(), e);
                    result = false;
                    
                } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    logger.error("Type error with property \"" + propertyName + "\" for value: \"" + propertyValue + "\", value must be instance of " + c.getSimpleName(), e);
                    result = false;
                }
            } else if (className.equals("Boolean")) {
                propertyValue = propertyValue.toLowerCase();
                if (!(propertyValue.equals("true") || propertyValue.equals("false"))) {
                    logger.error("Type error with property \"" + propertyName + "\" for value: \"" + propertyValue + "\", value must be \"true\" or \"false\"");
                }
            }
            
            return result;
        }
        
    }
    
    public WebServerContext() {
        logger.trace("START WebserverContext()");
        
        properties = Collections.synchronizedSortedMap(new TreeMap());
        
        propertieFile = Paths.get("etc/webserver.properties");
        
        logger.trace("END WebserverContext()");
    }
    
    public WebServerContext(Path propertyFile) {
        this();
        logger.trace("START WebserverContext(Path)");
        propertieFile = propertyFile;
        
        logger.trace("END WebserverContext(Path)");
    }
    
    public void put(K key, V value) {
        logger.trace("START put(K, V)");
        
        this.properties.put(key, value);
        
        logger.trace("END put(K, V)");
    }
    
    public void remove(K key) {
        logger.trace("START remove(K)");
        
        this.properties.remove(key);
        
        logger.trace("END remove(K)");
    }
    
    public void replace(K key, V value) {
        logger.trace("START replace(K, V)");
        
        this.properties.replace(key, value);
        
        logger.trace("END replace(K, V)");
    }
    
    public boolean containsValue(V value) {
        logger.trace("START contains(V)");
        
        logger.trace("END contains(V)");
        
        return this.properties.containsValue(value);
    }
    
    public boolean containsKey(K key) {
        logger.trace("START containsKey(K)");
        
        logger.trace("END containsKey(K)");
        
        return this.properties.containsKey(key);
    }
    
    public V get(K key) {
        logger.trace("START get(K)");
        
        logger.trace("END get(K)");
        
        return (V) this.properties.get(key);
    }
    
    public Collection<K> keySet() {
        logger.trace("START keySet()");
        
        logger.trace("END keySet()");
        
        return this.properties.keySet();
    }
    
    public Set<Entry<K, V>> entrySet() {
        logger.trace("START entrySet()");
        
        logger.trace("END entrySet()");
        
        return this.properties.entrySet();
    }
    
    public Collection<V> values() {
        logger.trace("START values()");
        
        logger.trace("END values()");
        
        return this.properties.values();
    }
    
    public String getString(K key) {
        logger.trace("START getString(K)");
        
        String result = "";
        
        if (this.properties.containsKey(key)) {
            result = this.properties.get(key).toString();
        } else {
            logger.warn("Application context does not contains property: \"" + key.toString() + "\"");
        }
        
        logger.trace("END getString(K)");
        
        return result;
    }
    
    public Integer getInteger(K key) {
        logger.trace("START getInteger(K)");
        
        Integer result = 0;
        
        if (this.properties.containsKey(key)) {
            String value = "";
            try {
                value = this.properties.get(key).toString();
                result = Integer.valueOf(value);
                
            } catch (IllegalArgumentException e) {
                logger.error("Type error with property \"" + key.toString() + "\" for value: \"" + value + "\", value must be instance of " + Integer.class.getSimpleName(), e);
            }
        } else {
            logger.warn("Application context does not contains property: \"" + key.toString() + "\"");
        }
        
        logger.trace("END getInteger(K)");
        
        return result;
    }
    
    public Long getLong(K key) {
        logger.trace("START getLong(K)");
        
        Long result = 0L;
        
        if (this.properties.containsKey(key)) {
            String value = "";
            try {
                value = this.properties.get(key).toString();
                result = Long.valueOf(value);
                
            } catch (IllegalArgumentException e) {
                logger.error("Type error with property \"" + key.toString() + "\" for value: \"" + value + "\", value must be instance of " + Long.class.getSimpleName(), e);
            }
        } else {
            logger.warn("Application context does not contains property: \"" + key.toString() + "\"");
        }
        
        logger.trace("END getLong(K)");
        
        return result;
    }
    
    public Double getDouble(K key) {
        logger.trace("START getDouble(K)");
        
        Double result = 0.0;
        
        if (this.properties.containsKey(key)) {
            String value = "";
            try {
                value = this.properties.get(key).toString();
                result = Double.valueOf(value);
                
            } catch (IllegalArgumentException e) {
                logger.error("Type error with property \"" + key.toString() + "\" for value: \"" + value + "\", value must be instance of " + Double.class.getSimpleName(), e);
            }
        } else {
            logger.warn("Application context does not contains property: \"" + key.toString() + "\"");
        }
        
        logger.trace("END getDouble(K)");
        
        return result;
    }
    
    public Boolean getBoolean(K key) {
        logger.trace("START getBoolean(K)");
        
        Boolean result = false;
        
        if (this.properties.containsKey(key)) {
            String value = "";
            try {
                value = this.properties.get(key).toString();
                value = value.toLowerCase();
                if (!(value.equals("true") || value.equals("false"))) {
                    logger.error("Type error with property \"" + key.toString() + "\" for value: \"" + value + "\", value must be \"true\" or \"false\"");
                }
                result = Boolean.valueOf(value);
                
            } catch (IllegalArgumentException e) {
                logger.error("Type error with property \"" + key.toString() + "\" for value: \"" + value + "\", value must be instance of " + Boolean.class.getSimpleName(), e);
            }
        } else {
            logger.warn("Application context does not contains property: \"" + key.toString() + "\"");
        }
        
        logger.trace("END getBoolean(K)");
        
        return result;
    }
    
    public void loadProperties() {
        logger.trace("START loadProperties()");
        
        loadProperties(propertieFile.toString());
        
        logger.trace("END loadProperties()");
    }
    
    private synchronized void loadProperties(String propertiesFilename) {
        logger.trace("START loadProperties(String)");
        
        Properties propertiesSource = new Properties();
        InputStream input = null;
        
        try {
            input = new FileInputStream(propertiesFilename);
            
            propertiesSource.load(input);
            if (properties == null) {
                properties = Collections.synchronizedSortedMap(new TreeMap());;
            }
            
            for (K key : (Set<K>) propertiesSource.keySet()) {
                this.properties.put(key, (V) propertiesSource.getProperty(key.toString()));
            }
            
        } catch (IOException e) {
            logger.warn("Warning loading webserver properties file: \"" + propertiesFilename + "\" : " + e.toString());
            if (e.getClass().isInstance(new FileNotFoundException())) {
                logger.warn("Ignore previous warning if you want to use default values");
            }
            
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error("Error closing webserver properties file: \"" + propertiesFilename + "\"", e);
                }
            }
        }
        
        logger.trace("END loadProperties(String)");
    }
    
    public void storeProperties() {
        logger.trace("START storeProperties()");
        
        storeProperties(propertieFile.toString());
        
        logger.trace("END storeProperties()");
    }
    
    private synchronized void storeProperties(String propertiesFilename) {
        logger.trace("START storeProperties(String)");
        
        Properties propertiesDestination = new Properties() {
            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<Object>(super.keySet()));
            }
        };
        OutputStream output = null;
        
        try {
            output = new FileOutputStream(propertiesFilename);
            
            logger.debug("Webserver properties store:");
            for (K key : this.properties.keySet()) {
                logger.debug("  property[\"" + key.toString() + "\"] = " + this.properties.get(key));
                
                boolean validatePropertytype = PropertyType.validatePropertyType(key.toString(), this.properties.get(key).toString());
                if (!validatePropertytype) {
                    logger.error("Error writing application properties file: \"" + propertiesFilename + "\"");
                    return;
                }
                if (PropertyType.isRecognized(key.toString())) {
                    propertiesDestination.put(key, String.valueOf(this.properties.get(key)));
                }
            }
            
            propertiesDestination.store(output, null);
            
        } catch (FileNotFoundException e) {
            logger.error("Error writing application properties file: \"" + propertiesFilename + "\"", e);
            
        } catch (IOException e) {
            logger.error("Error writing application properties file: \"" + propertiesFilename + "\"", e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    logger.error("Error closing application properties file: \"" + propertiesFilename + "\"", e);
                }
            }
        }
        
        logger.trace("END storeProperties(String)");
    }
    
    public void printProperties() {
        
        logger.debug("Webserver properties:");
        for (K key : this.properties.keySet()) {
            logger.debug("  property[\"" + key.toString() + "\"] = " + this.properties.get(key).toString());
            
            boolean validatePropertytype = PropertyType.validatePropertyType(key.toString(), this.properties.get(key).toString());
            if (!validatePropertytype) {
                logger.error("Error loading application properties file: \"" + propertieFile.toString() + "\"");
                return;
            }
        }
    }
}
