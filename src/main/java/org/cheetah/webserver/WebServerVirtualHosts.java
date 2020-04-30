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
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phs
 * @param <K>
 * @param <V>
 */
public final class WebServerVirtualHosts<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(WebServerVirtualHosts.class);

    private static String propertiesFilename = "etc/virtualhosts.properties";

    //private static final WebserverContext<String, Object> instance = new WebserverContext();
    private ConcurrentHashMap<K, V> properties = new ConcurrentHashMap();

    public WebServerVirtualHosts(String virtualHostPropertiesFileName) {
        logger.trace("START WebserverContext()");

        if (!virtualHostPropertiesFileName.equals("")) {
            propertiesFilename = virtualHostPropertiesFileName;
        }

        properties = new ConcurrentHashMap();

        logger.trace("END WebserverContext()");
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

    public boolean contains(V value) {
        logger.trace("START contains(V)");

        logger.trace("END contains(V)");

        return this.properties.contains(value);
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

    public Enumeration<V> elements() {
        logger.trace("START elements()");

        logger.trace("END elements()");

        return this.properties.elements();
    }

    public Enumeration<K> keys() {
        logger.trace("START keys()");

        logger.trace("END keys()");

        return this.properties.keys();
    }

    public ConcurrentHashMap.KeySetView<K, V> keySet() {
        logger.trace("START keySet()");

        logger.trace("END keySet()");

        return this.properties.keySet();
    }

    public String getString(K key) {
        logger.trace("START getString(K)");

        String result = "";

        if (this.properties.containsKey(key)) {
            result = this.properties.get(key).toString();
        }

        logger.trace("END getString(K)");

        return result;
    }

    public void loadProperties() {
        logger.trace("START loadProperties()");

        loadProperties(WebServerVirtualHosts.propertiesFilename);

        logger.trace("END loadProperties()");
    }

    public synchronized void loadProperties(String propertiesFilename) {
        logger.trace("START loadProperties(String)");

        Properties propertiesSource = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(propertiesFilename);

            propertiesSource.load(input);
            if (properties == null) {
                properties = new ConcurrentHashMap();
            }

            for (K key : (Set<K>) propertiesSource.keySet()) {
                this.properties.put(key, (V) propertiesSource.getProperty(key.toString()));
            }

        } catch (IOException e) {
            logger.error("Error loading application properties file: \"" + propertiesFilename + "\"", e);

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error("Error closing Application properties file: \"" + propertiesFilename + "\"", e);
                }
            }
        }

        logger.trace("END loadProperties(String)");
    }

    public void storeProperties() {
        logger.trace("START storeProperties()");

        storeProperties(WebServerVirtualHosts.propertiesFilename);

        logger.trace("END storeProperties()");
    }

    public synchronized void storeProperties(String propertiesFilename) {
        logger.trace("START storeProperties(String)");

        Properties propertiesDestination = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream(propertiesFilename);

            logger.debug("Application properties store:");
            for (K key : this.properties.keySet()) {
                logger.debug("property[\"" + key.toString() + "\"] = " + this.properties.get(key));

                propertiesDestination.put(key, String.valueOf(this.properties.get(key)));
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
            logger.debug("property[\"" + key.toString() + "\"] = " + this.properties.get(key).toString());
        }
    }
}
