package org.cheetah.webserver;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 *
 * @author Philippe Schweitzer
 */
public class CheckRequestJWT<RequestObject extends Serializable, ResponseObject extends Serializable> {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CheckRequestJWT.class);

    private Socket socket;
    private InputStream inputStream;

    private String host;
    private int port;
    private String token;
    private String password;
    private String URI;
    private boolean sslEnabled;

    private int code;
    private String result;
    private String name;

    private String secret = "mysecret";

    private CheckRequestJWT() {
    }

    public CheckRequestJWT(String name, String host, int port, String URI, boolean sslEnabled, int code, String result) {
        this(name, host, port, "", "", URI, sslEnabled, code, result);
    }

    public CheckRequestJWT(String name, String host, int port, String username, String password, String URI, boolean sslEnabled, int code, String result) {
        this.host = host;
        this.port = port;
        this.token = username;
        //this.password = password;
        this.URI = URI.replaceAll(" ", "%20");
        this.sslEnabled = sslEnabled;
        this.code = code;
        this.result = result;
        this.name = name;

        JSONObject properties = new JSONObject();
        properties.put("sampleProperty", "sampleValue");


        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            token = JWT.create()
                    .withSubject("5a96d12da9c216b26e9ee758")
                    .withClaim("properties", properties.toString())
                    .sign(algorithm);
        } catch (JWTCreationException e){
            logger.error("Error creating token: " + token + ": " + e.toString());
        }

        createConnection();

    }

    protected void createConnection() {

        String responseResult = "";
        int responseCode = 0;
        String hostPort;

        if (port != 80 && port != 443) {
            hostPort = host + ":" + port;
        } else {
            hostPort = host;
        }

        URL url;
        try {

            if (sslEnabled) {

                SSLSocketFactory sslSocketFactory = null;
                HostnameVerifier allHostsValid = null;
                try {

                    SSLContext sc = SSLContext.getInstance("SSL");
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

                    allHostsValid = new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    };

                    //sc.init(null, trustAllCerts, new java.security.SecureRandom());
                    sc.init(null, trustAllCerts, null);
                    //sc.init(null, null, null);
                    sslSocketFactory = sc.getSocketFactory();

                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    logger.error("SSL initialization failed: " + e.toString());
                    e.printStackTrace();
                }

                url = new URL("https://" + hostPort + URI);

                logger.debug("URL: " + url);
                
                HttpsURLConnection httpUrlConn = null;

                httpUrlConn = (HttpsURLConnection) url.openConnection();
                httpUrlConn.setSSLSocketFactory(sslSocketFactory);
                httpUrlConn.setHostnameVerifier(allHostsValid);
                httpUrlConn.setRequestMethod("GET");
                // Set timeouts in milliseconds
                httpUrlConn.setConnectTimeout(5000);
                httpUrlConn.setReadTimeout(5000);

                httpUrlConn.setRequestProperty("Authorization", "Bearer " + token);

                inputStream = httpUrlConn.getInputStream();
                httpUrlConn.connect();

                responseCode = httpUrlConn.getResponseCode();
                // Print HTTP status code/message for your information.
                //                  System.out.println("Response Code: " + httpUrlConn.getResponseCode());
                //                  System.out.println("Response Message: " + httpUrlConn.getResponseMessage());

                int port = httpUrlConn.getURL().getPort();

                if (port > 65535) {
                    throw new IllegalArgumentException("port out of range:" + port);
                }
                /*
                 for (X509Certificate cert : (X509Certificate[]) httpUrlConn.getServerCertificates()) {

                 //logger.debug("cert:" + System.lineSeparator() + cert.toString());
                 logger.debug("SUBJECT: " + cert.getSubjectDN().getName());
                 logger.debug("Validity: " + cert.getExtensionValue(url.toString()));

                 }
                 */

            } else {

                url = new URL("http://" + hostPort + URI);

                logger.debug("URL: " + url);

                HttpURLConnection httpUrlConn = null;

                httpUrlConn = (HttpURLConnection) url.openConnection();
                httpUrlConn.setRequestMethod("GET");
                // Set timeouts in milliseconds
                httpUrlConn.setConnectTimeout(5000);
                httpUrlConn.setReadTimeout(5000);

                httpUrlConn.setRequestProperty("Authorization", "Bearer " + token);

                inputStream = httpUrlConn.getInputStream();
                httpUrlConn.connect();

                responseCode = httpUrlConn.getResponseCode();

                // Print HTTP status code/message for your information.
//                System.out.println("Response Code: " + httpUrlConn.getResponseCode());
//                System.out.println("Response Message: " + httpUrlConn.getResponseMessage());
                int port = httpUrlConn.getURL().getPort();

                if (port > 65535) {
                    throw new IllegalArgumentException("port out of range:" + port);
                }
            }

            byte[] buffer = new byte[1024];
            int len;
            while (true) {

                len = inputStream.read(buffer);
                if (len == -1) {
                    break;
                }

                //               System.out.println("len +" + len);
                /*             
                 for (int i = 0; i < len; i++) {
                 logger.debug("\"" + buffer[i] + "\"");
                 }
                 logger.debug(len + ": \"" + new String(buffer, 0, len, Charset.forName("UTF-8")) + "\"");
                 */
                responseResult += new String(buffer, 0, len, CheetahWebserverTest.charset);
                //                          System.out.println("responseResult: " + responseResult);
            }

            if (code == responseCode && responseResult.startsWith(result)) {
                System.out.println("Pass : " + this.name + " URI: " + URI + ": code :" + code + " vs " + responseCode + " ; result: " + result);
            } else {

                if (responseResult.length() >= result.length()) {
                    System.err.println("Fail : " + this.name + " URI: " + URI + ": code :" + code + " vs " + responseCode + " ; result: " + result + " vs " + responseResult.substring(0, result.length()));

                } else {
                    System.err.println("Fail : " + this.name + " URI: " + URI + ": code :" + code + " vs " + responseCode + " ; result: " + result + " vs " + responseResult);
                }
            }

        } catch (FileNotFoundException e) {
            responseCode = 404;
            if (code == 404) {

                if (code == responseCode && responseResult.startsWith(result)) {
                    System.out.println("Pass : " + this.name + " URI: " + URI + ": code :" + code + " vs " + responseCode + " ; result: " + result);
                } else {

                    if (responseResult.length() >= result.length()) {
                        System.err.println("Fail : " + this.name + " URI: " + URI + ": code :" + code + " vs " + responseCode + " ; result: " + result + " vs " + responseResult.substring(0, result.length()));

                    } else {
                        System.err.println("Fail : " + this.name + " URI: " + URI + ": code :" + code + " vs " + responseCode + " ; result: " + result + " vs " + responseResult);
                    }
                }
            } else {
                System.err.println("Fail : " + this.name + ":" + e.toString());
                e.printStackTrace();
            }

        } catch (IOException e) {
            responseCode = 401;
            if (code == 401) {

                if (code == responseCode && responseResult.startsWith(result)) {
                    System.out.println("Pass : " + this.name + " URI: " + URI + ": code :" + code + " vs " + responseCode + " ; result: " + result);
                } else {

                    if (responseResult.length() >= result.length()) {
                        System.err.println("Fail : " + this.name + " URI: " + URI + ": code :" + code + " vs " + responseCode + " ; result: " + result + " vs " + responseResult.substring(0, result.length()));

                    } else {
                        System.err.println("Fail : " + this.name + " URI: " + URI + ": code :" + code + " vs " + responseCode + " ; result: " + result + " vs " + responseResult);
                    }
                }
            } else {
                System.err.println("Fail : " + this.name + ":" + e.toString());
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("Fail : " + this.name + ":" + e.toString());
        }

    }

    protected final Socket getSocket(String host, int port, boolean sslEnabled) throws Exception {

        if (!sslEnabled) {
            return new Socket(host, port);
        }

        Socket responseSocket = null;

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

            sc.init(null, trustAllCerts, null);

            // for enabling trusted certificates
            //sc.init(null, null, null);
            final SSLSocketFactory sslSocketFactory = sc.getSocketFactory();

            responseSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);

            String[] cyphers = ((SSLSocket) responseSocket).getEnabledProtocols();

            for (String cypher : cyphers) {
                logger.debug("EnabledProtocols: " + cypher);
            }

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("SSL initialization failed", e);
            e.printStackTrace();
        }

        return responseSocket;
    }
}
