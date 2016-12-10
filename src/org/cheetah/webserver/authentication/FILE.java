/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.authentication;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.simpleframework.http.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phs
 */
public class FILE extends AbstractAuthenticator {

    private static Logger logger = LoggerFactory.getLogger(FILE.class);

    private ConcurrentHashMap<String, String> properties = new ConcurrentHashMap();

    private byte[] secret;

    private String userFilename = "etc/user.properties";

    public FILE() {

        try {
            java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
            secret = addr.getAddress();
            secret = Arrays.copyOf(secret, 16);

        } catch (Exception e) {
            secret = new byte[]{5, 4, 5, 9, 5, 1, 2, 9, 3, 1, 8, 3, 8, 6, 2, 1};
        }

        loadProperties();
    }

    public void encrypt() {

        for (String key : this.properties.keySet()) {

            String password = this.properties.get(key);
            if (!password.endsWith("=")) {
                try {
                    Cipher aesCipher = Cipher.getInstance("AES/ECB/NoPadding");

                    SecretKey SecKey = new SecretKeySpec(secret, "AES");

                    byte[] byteText = password.getBytes();
                    byteText = Arrays.copyOf(byteText, 64);

                    aesCipher.init(Cipher.ENCRYPT_MODE, SecKey);

                    byte[] byteCipherText = aesCipher.doFinal(byteText);

                    byte[] encodedBytes = Base64.getEncoder().encode(byteCipherText);
                    this.properties.replace(key, new String(encodedBytes));

                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                    logger.error("Unable to encrypt password", e);
                }
            }
        }

        storeProperties();
    }

    @Override
    public boolean authenticate(Request request) {

        String username = this.getCredentials(request).getKey();
        String password = this.getCredentials(request).getValue();
                            
        byte[] bytesOfPassword = password.getBytes();
        bytesOfPassword = Arrays.copyOf(bytesOfPassword, 64);

        if (this.properties.containsKey(username)) {

            String checkedPassword = this.properties.get(username);
            if (!checkedPassword.endsWith("=")) {
                encrypt();
            }
            
            byte[] bytesOFCheckedPassword = Base64.getDecoder().decode(this.properties.get(username));

            Cipher aesCipher;
            try {
                aesCipher = Cipher.getInstance("AES/ECB/NoPadding");
                SecretKey SecKey = new SecretKeySpec(secret, "AES");

                aesCipher.init(Cipher.DECRYPT_MODE, SecKey);
                byte[] bytePlainText = aesCipher.doFinal(bytesOFCheckedPassword);

                if (new String(bytesOfPassword).equals(new String(bytePlainText))) {
                    logger.debug("AUTH OK");
                    return true;
                } else {
                    // send error message
                }

            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                logger.error("Unable to decrypt password", e);
            }

        }

        return false;
    }

    @Override
    public boolean setPassword(String username, String oldPassword, String newPassword) {
        boolean result = false;

        if (!oldPassword.endsWith("=")) {

            try {
                Cipher aesCipher = Cipher.getInstance("AES/ECB/NoPadding");

                SecretKey SecKey = new SecretKeySpec(secret, "AES");

                byte[] byteText = oldPassword.getBytes();
                byteText = Arrays.copyOf(byteText, 64);

                aesCipher.init(Cipher.ENCRYPT_MODE, SecKey);

                byte[] byteCipherText = aesCipher.doFinal(byteText);

                byte[] encodedBytes = Base64.getEncoder().encode(byteCipherText);
                oldPassword = new String(encodedBytes);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                logger.error("Unable to encrypt password", e);
            }
        }

        if (this.properties.containsKey(username)) {
            if (this.properties.get(username).equals(oldPassword)) {
                try {
                    Cipher aesCipher = Cipher.getInstance("AES/ECB/NoPadding");

                    SecretKey SecKey = new SecretKeySpec(secret, "AES");

                    byte[] byteText = newPassword.getBytes();
                    byteText = Arrays.copyOf(byteText, 64);

                    aesCipher.init(Cipher.ENCRYPT_MODE, SecKey);

                    byte[] byteCipherText = aesCipher.doFinal(byteText);

                    byte[] encodedBytes = Base64.getEncoder().encode(byteCipherText);
                    this.properties.replace(username, new String(encodedBytes));
                    storeProperties();
                    result = true;
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                    logger.error("Unable to encrypt password", e);
                }
            }
        }
        return result;
    }

    private synchronized void loadProperties() {
        logger.trace("START loadProperties()");

        Properties propertiesSource = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(userFilename);

            propertiesSource.load(input);

            for (Object key : (Set<Object>) propertiesSource.keySet()) {
                properties.put(key.toString(), propertiesSource.getProperty(key.toString()));
            }

        } catch (IOException e) {
            logger.error("Error loading User properties file: \"" + userFilename + "\"", e);

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error("Error closing User properties file: \"" + userFilename + "\"", e);
                }
            }
        }

        logger.trace("END loadProperties()");
    }

    private synchronized void storeProperties() {
        logger.trace("START storeProperties()");

        Properties propertiesDestination = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream(userFilename);

            logger.debug("Application properties store:");

            for (String key : (Set<String>) properties.keySet()) {
                logger.debug("property[\"" + key + "\"] = " + this.properties.get(key));

                propertiesDestination.put(key, this.properties.get(key));
            }

            propertiesDestination.store(output, null);

        } catch (FileNotFoundException e) {
            logger.error("Error writing application properties file: \"" + userFilename + "\"", e);

        } catch (IOException e) {
            logger.error("Error writing application properties file: \"" + userFilename + "\"", e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    logger.error("Error closing application properties file: \"" + userFilename + "\"", e);
                }
            }
        }

        logger.trace("END storeProperties()");
    }

}
