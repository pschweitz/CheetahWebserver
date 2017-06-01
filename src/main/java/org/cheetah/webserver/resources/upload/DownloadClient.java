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
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class DownloadClient {

    private Path file;
    private URL url;
    private String destination = "";
    private String username = "";
    private String password = "";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DownloadClient.class);

    public static void main(String[] args) {

        try {
            Path file = Paths.get("/tmp/sevendctmjobs.pdf").toAbsolutePath();
            URL url = new URL("http://172.20.10.2:8080/admin/sevendctmjobs.pdf");

            DownloadClient downloadClient = new DownloadClient(file, url, "", "");

            downloadClient.download();

        } catch (MalformedURLException ex) {
            Logger.getLogger(DownloadClient.class.getName()).log(Level.SEVERE, null, ex);
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

    public void download() {

        ReadableByteChannel rbc;
        try {
            
            URLConnection connection = url.openConnection();            
            
            if (!username.equals("")) {
                String userpass = username + ":" + password;
                String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
                connection.setRequestProperty("Authorization", basicAuth);
            }
            
            rbc = Channels.newChannel(connection.getInputStream());
            FileOutputStream fos = new FileOutputStream(file.toFile());
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            logger.error("Error downloading file: \"" + url + "\": " + e.toString());
        }
    }
}
