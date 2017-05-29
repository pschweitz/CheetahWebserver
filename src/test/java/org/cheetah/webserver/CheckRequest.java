package org.cheetah.webserver;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Schweitzer
 */
public class CheckRequest<RequestObject extends Serializable, ResponseObject extends Serializable> {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CheckRequest.class);

    private Socket socket;
    private InputStream inputStream;

    private String host;
    private int port;
    private String username;
    private String password;
    private String URI;
    private boolean sslEnabled;

    private int code;
    private String result;
    private String name;

    private CheckRequest() {
    }

    public CheckRequest(String name, String host, int port, String URI, boolean sslEnabled, int code, String result) {
        this(name, host, port, "", "", URI, sslEnabled, code, result);
    }

    public CheckRequest(String name, String host, int port, String username, String password, String URI, boolean sslEnabled, int code, String result) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.URI = URI.replaceAll(" ", "%20");
        this.sslEnabled = sslEnabled;
        this.code = code;
        this.result = result;
        this.name = name;

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
                }

                url = new URL("https://" + hostPort + URI);

//                logger.debug("URL: " + url);
                
                HttpsURLConnection httpUrlConn = null;

                httpUrlConn = (HttpsURLConnection) url.openConnection();
                httpUrlConn.setSSLSocketFactory(sslSocketFactory);
                httpUrlConn.setHostnameVerifier(allHostsValid);
                httpUrlConn.setRequestMethod("GET");
                // Set timeouts in milliseconds
                httpUrlConn.setConnectTimeout(5000);
                httpUrlConn.setReadTimeout(5000);

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

                HttpURLConnection httpUrlConn = null;

                httpUrlConn = (HttpURLConnection) url.openConnection();
                httpUrlConn.setRequestMethod("GET");
                // Set timeouts in milliseconds
                httpUrlConn.setConnectTimeout(5000);
                httpUrlConn.setReadTimeout(5000);

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
            }

        } catch (Exception e) {
            System.err.println("Fail : " + this.name + ":" + e.toString());
        }


        /*
         String bytesEncoded = "";
         String authorizationHeader = "";

         if (!username.equals("")) {
         bytesEncoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
         authorizationHeader = "Authorization: BASIC " + bytesEncoded + "\r\n";
         }

         try {
            
            
         socket = getSocket(host, port, sslEnabled);
         if (socket != null) {
         cursor = new StreamCursor(socket.getInputStream());
         ReplyConsumer response = new ReplyConsumer();

         byte[] random = new byte[16];
         Random reuseableRandom = new Random();
         reuseableRandom.nextBytes(random);

         String hostPort;

         if (port != 80 && port != 443) {
         hostPort = host + ":" + port;
         } else {
         hostPort = host;
         }

         byte[] request = ("GET " + URI + " HTTP/1.1\r\n"
         + "Host: " + hostPort + "\r\n"
         + authorizationHeader
         + "\r\n").getBytes("ISO-8859-1");

         socket.getOutputStream().write(request);

         System.err.println("TEST 1");
         while (cursor.isOpen()) {

         response.consume(cursor);

         if (response.isFinished()) {
         break;
         }

         }

         System.err.println("TEST 2");
         String responseResult = "";

         boolean eof = false;

         byte[] buffer = new byte[1024];
         int len;
         while (!eof) {

         System.err.println("TEST 3");
         len = cursor.read(buffer);
         if (len == 0) {
         System.err.println("TEST FINISH");
         break;
         }

         System.err.println("TEST 3+");
         for (int i = 0; i < len; i++) {
         logger.debug("\"" + buffer[i] + "\"");
         }
         logger.debug(len + ": \"" + new String(buffer, 3, len - 10, Charset.forName("UTF-8")) + "\"");

         responseResult = new String(buffer, 3, result.length(), WebserverTest.charset);
         System.out.println("responseResult: " + responseResult);
         }
         try {
         socket.close();
         } catch (Exception e) {
         System.err.println("Error Closing socket: " + e.toString());
         }

         System.err.println("TEST 4");
                
                

         //              System.err.println("TEST: " + response.getCode());
         if (code == response.getCode() && responseResult.startsWith(result)) {
         //                  System.err.println("TEST 2");
         System.out.println("Pass : " + this.name + " URI: " + URI + ": code :" + code + " vs " + response.getCode() + " ; result: " + result.replaceAll("\n", " "));
         } else {
         //                  System.err.println("TEST 3");
         if (responseResult.length() >= result.length()) {
         //                      System.err.println("TEST 4: " + responseResult + " " + result );
         System.err.println("Fail : " + this.name + " URI: " + URI + ": code :" + code + " vs " + response.getCode() + " ; result: " + result.replaceAll("\n", " ") + " vs " + responseResult.substring(0, result.length()));

         //                      System.err.println("TEST 41");
         } else {
         //                      System.err.println("TEST 5");
         System.err.println("Fail : " + this.name + " URI: " + URI + ": code :" + code + " vs " + response.getCode() + " ; result: " + result.replaceAll("\n", " ") + " vs " + responseResult);
         }
         }

         //              System.err.println("TEST 6");
         } else {
         System.err.println("Failed : socket is null");
         }

         } catch (Exception e) {
         System.err.println("Failed : " + this.name + ":" + e.toString());

         }
         */
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
        }

        return responseSocket;
    }
}
