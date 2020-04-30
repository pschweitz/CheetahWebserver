/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.resources.upload;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

import javax.net.ssl.*;

/**
 * @author Philippe Schweitzer
 */
public class DownloadClient {

    private Path file;
    private URL url;
    private String destination = "";
    private String username = "";
    private String password = "";
    private int timeout = 5000;
    private boolean sslEnforceValidation = true;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DownloadClient.class);

    public static void main(String[] args) {

        try {
            Path file = Paths.get("/tmp/sevendctmjobs.pdf").toAbsolutePath();
            URL url = new URL("http://172.20.10.2:8080/admin/sevendctmjobs.pdf");

            DownloadClient downloadClient = new DownloadClient(file, url, "", "");

            downloadClient.download();

        } catch (IOException ex) {
            logger.error("Error downloading file: " + ex.toString());
        }
    }

    public DownloadClient(Path file, URL url) {
        this(file, url, "", "");
    }

    public DownloadClient(Path file, URL url, String username, String password) {
        this.file = file;
        this.url = url;
        this.username = username;
        this.password = password;

    }

    public boolean isSSLEnforceValidation() {
        return sslEnforceValidation;
    }

    public void setSSLEnforceValidation(boolean sslEnforceValidation) {
        this.sslEnforceValidation = sslEnforceValidation;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }


    public void download() throws IOException {

        ReadableByteChannel rbc;

        URLConnection connection = url.openConnection();

        if (url.getProtocol().toLowerCase().startsWith("https")) {

            try {
                SSLContext sc;
                sc = SSLContext.getInstance("SSLv3");

                // Create a trust manager that does not validate certificate chains
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

                if (this.sslEnforceValidation) {
                    sc.init(null, null, null);
                } else {
                    sc.init(null, trustAllCerts, null);
                }

                final SSLSocketFactory sslSocketFactory = sc.getSocketFactory();

                ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                logger.error("SSL initialization failed", e);
            }

        }

        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);

        if (!username.equals("")) {
            String userpass = username + ":" + password;
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
            connection.setRequestProperty("Authorization", basicAuth);
        }

        rbc = Channels.newChannel(connection.getInputStream());
        FileOutputStream fos = new FileOutputStream(file.toFile());
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.flush();
        fos.close();
        rbc.close();

    }
}
