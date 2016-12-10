/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phs
 */
public class Utils {

    static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static String loadPage(Path path, Charset charset) throws IOException, URISyntaxException {
        StringBuffer result = new StringBuffer();

        if (Files.exists(path)) {
            File file = path.toFile();
            BufferedReader br = null;

            FileInputStream fstream = new FileInputStream(file);
            InputStreamReader is = new InputStreamReader(fstream, charset);
            br = new BufferedReader(is);

            StringBuilder trace = new StringBuilder();

            String line = "";
            line = br.readLine();
            while (line != null) {
                result.append(line).append(System.lineSeparator());
                line = br.readLine();
            }
            
            br.close();
        } else {
            logger.error("Path not found: " + path.toString());
        }

        return result.toString();
    }

    public static void readTextFile(PrintStream body, URL url, Charset charset) throws FileNotFoundException, IOException, URISyntaxException {

        File file = new File(url.toURI());
        BufferedReader br = null;

        FileInputStream fstream = new FileInputStream(file);
        InputStreamReader is = new InputStreamReader(fstream, charset);
        br = new BufferedReader(is);

        String line = "";
        line = br.readLine();
        while (line != null) {
            body.println(line);
            line = br.readLine();
        }
        
        br.close();
    }

    public static void readBinaryFile(PrintStream body, URL url) throws FileNotFoundException, IOException, URISyntaxException {

        File file = new File(url.toURI());

        DataInputStream in = new DataInputStream(new FileInputStream(file));
        byte[] buffer = new byte[1048576];

        int readbytes = in.read(buffer);

        while (readbytes != -1) {
            body.write(buffer, 0, readbytes);
            readbytes = in.read(buffer);
        }
        
        in.close();
    }

    public static void readTextFileRessource(PrintStream body, URL url, ClassLoader classLoader, Charset charset) throws FileNotFoundException, IOException, URISyntaxException {

        String fileName = url.toString();

        BufferedReader br = null;
        InputStream in = null;

        if (fileName.startsWith("jar")) {
            fileName = fileName.substring(fileName.lastIndexOf("!") + 1).substring(1);

            in = classLoader.getResourceAsStream(fileName);
        } else {
            fileName = fileName.substring(fileName.lastIndexOf(":") + 1);

            in = new FileInputStream(fileName);
        }

        InputStreamReader is = new InputStreamReader(in, charset);
        br = new BufferedReader(is);

        String line = "";
        line = br.readLine();

        while (line != null) {
            body.println(line);
            line = br.readLine();
        }
        
        in.close();
    }

    public static void readBinaryFileRessource(PrintStream body, URL url, ClassLoader classLoader) throws FileNotFoundException, IOException, URISyntaxException {

        String fileName = url.toString();
        InputStream in = null;

        if (fileName.startsWith("jar")) {
            fileName = fileName.substring(fileName.lastIndexOf("!") + 1).substring(1);

            in = classLoader.getResourceAsStream(fileName);
        } else {
            fileName = fileName.substring(fileName.lastIndexOf(":") + 1);

            in = new FileInputStream(fileName);
        }

        DataInputStream din = new DataInputStream(in);
        byte[] buffer = new byte[1048576];

        int readbytes = din.read(buffer);

        while (readbytes != -1) {
            body.write(buffer, 0, readbytes);
            readbytes = din.read(buffer);
        }
        
        in.close();
    }
}
