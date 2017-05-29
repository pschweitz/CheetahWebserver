/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.cheetah.nio.file.Files;
import org.cheetah.nio.file.LinkOption;
import org.cheetah.nio.file.Paths;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class MimeType {

    private static final Logger logger = LoggerFactory.getLogger(MimeType.class);
    private static final ConcurrentHashMap<String, MimeType> list = new ConcurrentHashMap();

    private String fileExtention;
    private String mimeType;
    private FileFormat format;

    private enum FileFormat {
        TEXT, BINARY
    };

    public static void initMimeType() {

        logger.trace("START initMimeType()");

        BufferedReader br = null;
        try {
            FileInputStream fstream;
            InputStreamReader is = null;

            if (Files.exists(Paths.get("etc/mime.types"), LinkOption.NOFOLLOW_LINKS)) {

                fstream = new FileInputStream(Paths.get("etc/mime.types").toFile());
                is = new InputStreamReader(fstream, Charset.forName("utf-8"));
            } else {
                CheetahClassLoader classLoader = new CheetahClassLoader(Thread.currentThread().getContextClassLoader());
                is = new InputStreamReader(classLoader.getResourceAsStream("org/cheetah/webserver/resources/mime.types"), Charset.forName("utf-8"));
            }

            br = new BufferedReader(is);

            String line = "";

            while (line != null) {

                if (!line.equals("") && !line.equals("\r") && !line.startsWith("#")) {

                    populateMimeTypeList(line);

                }

                line = br.readLine();
            }

            br.close();

        } catch (IOException e) {
            logger.error("Error opening 'mime.type' file", e);

        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
            }
        }

        logger.trace("END initMimeType()");

    }

    public static void populateMimeTypeList(String line) {

        StringTokenizer formatTokenizer = new StringTokenizer(line, "\t");

        while (formatTokenizer.hasMoreElements()) {

            String format = formatTokenizer.nextToken();
            String type = formatTokenizer.nextToken();

            String mimetype = "";
            String extension = "";

            StringTokenizer typeTokenizer = new StringTokenizer(type, "=");

            if (typeTokenizer.hasMoreElements()) {
                mimetype = typeTokenizer.nextToken();
            }
            if (typeTokenizer.hasMoreElements()) {
                extension = typeTokenizer.nextToken();
            }

            if (!extension.equals("")) {

                FileFormat formatEnum = FileFormat.BINARY;

                if (format.toLowerCase().equals("text")) {
                    formatEnum = FileFormat.TEXT;
                }

                StringTokenizer extensionTokenizer = new StringTokenizer(extension, " ");

                while (extensionTokenizer.hasMoreElements()) {

                    String extentionString = extensionTokenizer.nextToken();

                    logger.trace("Found mimeType: " + format + "|" + mimetype + "|" + extentionString);
                    list.put(extentionString, new MimeType(extentionString, mimetype, formatEnum));
                }
            }
        }
    }

    public static String getMimeType(String extention) {
        String result = list.get("bin").mimeType;

        for (MimeType type : list.values()) {
            if (type.fileExtention.toLowerCase().equals(extention.toLowerCase())) {
                result = type.mimeType;
                break;
            }
        }
        return result;
    }

    public static MimeType getFileType(String extention) {
        MimeType result = list.get("bin");

        for (MimeType type : list.values()) {
            if (type.fileExtention.toLowerCase().equals(extention.toLowerCase())) {
                result = type;
                break;
            }
        }
        return result;
    }

    public static boolean isText(String extention) {
        boolean result = false;

        for (MimeType type : list.values()) {
            if (type.fileExtention.toLowerCase().equals(extention.toLowerCase())) {
                result = (type.format == FileFormat.TEXT);
                break;
            }
        }
        return result;
    }

    public static boolean isTextFromType(String mimeType) {
        boolean result = false;

        for (MimeType type : list.values()) {
            if (type.mimeType.toLowerCase().equals(mimeType.toLowerCase())) {
                result = (type.format == FileFormat.TEXT);
                break;
            }
        }
        return result;
    }

    MimeType(String fileExtention, String mimeType, FileFormat fileFormat) {
        this.fileExtention = fileExtention;
        this.mimeType = mimeType;
        this.format = fileFormat;
    }
}
