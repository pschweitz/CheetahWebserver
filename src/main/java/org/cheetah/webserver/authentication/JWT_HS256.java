/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.webserver.authentication;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.simpleframework.http.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author phs
 */
public class JWT_HS256 extends AbstractAuthenticator {

    private static Logger logger = LoggerFactory.getLogger(JWT_HS256.class);

    private ConcurrentHashMap<String, String> properties = new ConcurrentHashMap();

    private byte[] secret;

    private String JWTFilename = "etc/jwt.properties";

    private String JWTSubject = "";

    int ivSize = 16;
    int keySize = 16;

    public JWT_HS256() {

        try {
            secret = getServerUniqueFootPrint();
            secret = Arrays.copyOf(secret, keySize);

        } catch (Exception e) {
            secret = new byte[]{5, 4, 5, 9, 5, 1, 2, 9, 3, 1, 8, 3, 8, 6, 2, 1};
        }
        ivSize = secret.length;
        keySize = secret.length;

        loadProperties();

        instance = this;
    }

    public void encryptAndStore() {

        for (String key : this.properties.keySet()) {

            String password = this.properties.get(key);
            if (!password.endsWith("=")) {

                byte[] encrypted = encrypt(password);

                byte[] encodedBytes = Base64.getEncoder().encode(encrypted);
                this.properties.replace(key, new String(encodedBytes) + "=");
            }
        }

        storeProperties();
    }

    private byte[] encrypt(String password) {
        byte[] result = {};
        try {
            byte[] byteText = password.getBytes();
            byteText = Arrays.copyOf(byteText, 64);

            // Generating IV.
            byte[] iv = new byte[ivSize];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            // Hashing key.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            //digest.update(key.getBytes("UTF-8"));
            digest.update(secret);
            byte[] keyBytes = new byte[keySize];
            System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            // Encrypt.
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] encrypted = cipher.doFinal(byteText);

            // Combine IV and encrypted part.
            byte[] encryptedIVAndText = new byte[ivSize + encrypted.length];
            System.arraycopy(iv, 0, encryptedIVAndText, 0, ivSize);

            System.arraycopy(encrypted, 0, encryptedIVAndText, ivSize, encrypted.length);

            result = encryptedIVAndText;

        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException ex) {
            logger.error("Unable to encrypt password: " + ex.toString());
        }
        return result;

    }

    private byte[] decrypt(byte[] encrypted) {
        byte[] result = {};

        try {
            byte[] iv = new byte[ivSize];
            System.arraycopy(encrypted, 0, iv, 0, iv.length);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            // Extract encrypted part.
            int encryptedSize = encrypted.length - ivSize;
            byte[] encryptedBytes = new byte[encryptedSize];
            System.arraycopy(encrypted, ivSize, encryptedBytes, 0, encryptedSize);

            // Hash key.
            byte[] keyBytes = new byte[keySize];
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            //md.update(key.getBytes());
            md.update(secret);
            System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.length);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            // Decrypt.
            Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] decrypted = cipherDecrypt.doFinal(encryptedBytes);

            result = decrypted;
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException ex) {
            logger.error("Unable to decrypt password: " + ex.toString());
        }
        return result;
    }

    @Override
    public boolean authenticate(Request request) {

        String token = "";

        StringTokenizer tokenizer = new StringTokenizer(request.toString(), "\r\n");

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            String[] elements = line.split(":");

            //    body.println("<h1> elements[0]T: " + elements[0] + "</h1>");
            if (elements.length > 0) {

                if (elements[0].equals("Authorization")) {
                    if (line.length() > "Authorization: ".length()) {
                        token = elements[1].substring("Bearer".length() + 2);
                    }
                }
            }
        }

        logger.debug("Received token: " + token);

        if (this.properties.containsKey("subject")) {

            JWTSubject = this.properties.get("subject");
            if (!JWTSubject.endsWith("=")) {
                encryptAndStore();
                JWTSubject = this.properties.get("subject");
            }

            JWTSubject = JWTSubject.substring(0, JWTSubject.length()-1);

            byte[] bytesOFJWTSubject = Base64.getDecoder().decode(JWTSubject);

            JWTSubject = new String(decrypt(bytesOFJWTSubject)).trim();
        }

        if (this.properties.containsKey("secret")) {

            String checkedPassword = this.properties.get("secret");
            if (!checkedPassword.endsWith("=")) {
                encryptAndStore();
                checkedPassword = this.properties.get("secret");
            }

            checkedPassword = checkedPassword.substring(0, checkedPassword.length()-1);

            byte[] bytesOFCheckedPassword = Base64.getDecoder().decode(checkedPassword);

            byte[] bytePlainText = decrypt(bytesOFCheckedPassword);

            try {
                Algorithm algorithm = Algorithm.HMAC256(bytePlainText);
                JWTVerifier verifier = com.auth0.jwt.JWT.require(algorithm)
                        .withSubject(JWTSubject)
                        .build(); //Reusable verifier instance
                DecodedJWT jwt = verifier.verify(token);

                Map<String, Claim> claims = jwt.getClaims();

                for(Map.Entry<String, Claim> entry: claims.entrySet()){
                    logger.debug( entry.getKey()+ ":" + entry.getValue().asString());
                }

                this.sessionObject = claims;

                return true;
            } catch (JWTVerificationException e){
                logger.error("Error verifying token: " + token + ": " + e.toString());

            }
        }


        return false;
    }

    @Override
    public boolean setPassword(String username, String oldPassword, String newPassword) {
        boolean result = false;

        if (!oldPassword.endsWith("=")) {

            byte[] byteCipherText = encrypt(oldPassword);

            byte[] encodedBytes = Base64.getEncoder().encode(byteCipherText);
            oldPassword = new String(encodedBytes);
        }

        if (this.properties.containsKey(username)) {
            if (this.properties.get(username).equals(oldPassword)) {

                byte[] byteCipherText = encrypt(newPassword);

                byte[] encodedBytes = Base64.getEncoder().encode(byteCipherText);
                this.properties.replace(username, new String(encodedBytes));
                storeProperties();
                result = true;
            }
        }
        return result;
    }

    private synchronized void loadProperties() {
        logger.trace("START loadProperties()");

        Properties propertiesSource = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(JWTFilename);

            propertiesSource.load(input);

            for (Object key : (Set<Object>) propertiesSource.keySet()) {
                properties.put(key.toString(), propertiesSource.getProperty(key.toString()));
            }

        } catch (IOException e) {
            logger.error("Error loading User properties file: \"" + JWTFilename + "\"", e);

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error("Error closing User properties file: \"" + JWTFilename + "\"", e);
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
            output = new FileOutputStream(JWTFilename);

            logger.trace("User authentication store");

            for (String key : (Set<String>) properties.keySet()) {
                propertiesDestination.put(key, this.properties.get(key));
            }

            propertiesDestination.store(output, null);

        } catch (FileNotFoundException e) {
            logger.error("Error writing application properties file: \"" + JWTFilename + "\"", e);

        } catch (IOException e) {
            logger.error("Error writing application properties file: \"" + JWTFilename + "\"", e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    logger.error("Error closing application properties file: \"" + JWTFilename + "\"", e);
                }
            }
        }

        logger.trace("END storeProperties()");
    }

    private static class Properties extends java.util.Properties {

        /**
         * use serialVersionUID from JDK 1.1.X for interoperability
         */
        private static final long serialVersionUID = 4112578634029874840L;

        /**
         * A property list that contains default values for any keys not found
         * in this property list.
         *
         * @serial
         */
        protected Properties defaults;

        /**
         * Creates an empty property list with no default values.
         */
        public Properties() {
            this(null);
        }

        /**
         * Creates an empty property list with the specified defaults.
         *
         * @param defaults the defaults.
         */
        public Properties(Properties defaults) {
            this.defaults = defaults;
        }

        /**
         * Calls the <tt>Hashtable</tt> method {@code put}. Provided for
         * parallelism with the <tt>getProperty</tt> method. Enforces use of
         * strings for property keys and values. The value returned is the
         * result of the <tt>Hashtable</tt> call to {@code put}.
         *
         * @param key the key to be placed into this property list.
         * @param value the value corresponding to <tt>key</tt>.
         * @return the previous value of the specified key in this property
         * list, or {@code null} if it did not have one.
         * @see #getProperty
         * @since 1.2
         */
        public synchronized Object setProperty(String key, String value) {
            return put(key, value);
        }

        /**
         * Reads a property list (key and element pairs) from the input
         * character stream in a simple line-oriented format.
         * <p>
         * Properties are processed in terms of lines. There are two kinds of
         * line, <i>natural lines</i> and <i>logical lines</i>. A natural line
         * is defined as a line of characters that is terminated either by a set
         * of line terminator characters ({@code \n} or {@code \r} or
         * {@code \r\n}) or by the end of the stream. A natural line may be
         * either a blank line, a comment line, or hold all or some of a
         * key-element pair. A logical line holds all the data of a key-element
         * pair, which may be spread out across several adjacent natural lines
         * by escaping the line terminator sequence with a backslash character
         * {@code \}. Note that a comment line cannot be extended in this
         * manner; every natural line that is a comment must have its own
         * comment indicator, as described below. Lines are read from input
         * until the end of the stream is reached.
         *
         * <p>
         * A natural line that contains only white space characters is
         * considered blank and is ignored. A comment line has an ASCII
         * {@code '#'} or {@code '!'} as its first non-white space character;
         * comment lines are also ignored and do not encode key-element
         * information. In addition to line terminators, this format considers
         * the characters space ({@code ' '}, {@code '\u005Cu0020'}), tab
         * ({@code '\t'}, {@code '\u005Cu0009'}), and form feed
         * ({@code '\f'}, {@code '\u005Cu000C'}) to be white space.
         *
         * <p>
         * If a logical line is spread across several natural lines, the
         * backslash escaping the line terminator sequence, the line terminator
         * sequence, and any white space at the start of the following line have
         * no affect on the key or element values. The remainder of the
         * discussion of key and element parsing (when loading) will assume all
         * the characters constituting the key and element appear on a single
         * natural line after line continuation characters have been removed.
         * Note that it is <i>not</i> sufficient to only examine the character
         * preceding a line terminator sequence to decide if the line terminator
         * is escaped; there must be an odd number of contiguous backslashes for
         * the line terminator to be escaped. Since the input is processed from
         * left to right, a non-zero even number of 2<i>n</i> contiguous
         * backslashes before a line terminator (or elsewhere) encodes <i>n</i>
         * backslashes after escape processing.
         *
         * <p>
         * The key contains all of the characters in the line starting with the
         * first non-white space character and up to, but not including, the
         * first unescaped {@code '='},
         * {@code ':'}, or white space character other than a line terminator.
         * All of these key termination characters may be included in the key by
         * escaping them with a preceding backslash character; for example,
         * <p>
         *
         * {@code \:\=}
         * <p>
         *
         * would be the two-character key {@code ":="}. Line terminator
         * characters can be included using {@code \r} and {@code \n} escape
         * sequences. Any white space after the key is skipped; if the first
         * non-white space character after the key is {@code '='} or
         * {@code ':'}, then it is ignored and any white space characters after
         * it are also skipped. All remaining characters on the line become part
         * of the associated element string; if there are no remaining
         * characters, the element is the empty string {@code ""}. Once the raw
         * character sequences constituting the key and element are identified,
         * escape processing is performed as described above.
         *
         * <p>
         * As an example, each of the following three lines specifies the key
         * {@code "Truth"} and the associated element value {@code "Beauty"}:
         * <pre>
         * Truth = Beauty
         *  Truth:Beauty
         * Truth                    :Beauty
         * </pre> As another example, the following three lines specify a single
         * property:
         * <pre>
         * fruits                           apple, banana, pear, \
         *                                  cantaloupe, watermelon, \
         *                                  kiwi, mango
         * </pre> The key is {@code "fruits"} and the associated element is:
         * <pre>"apple, banana, pear, cantaloupe, watermelon, kiwi, mango"</pre>
         * Note that a space appears before each {@code \} so that a space will
         * appear after each comma in the final result; the {@code \}, line
         * terminator, and leading white space on the continuation line are
         * merely discarded and are <i>not</i> replaced by one or more other
         * characters.
         * <p>
         * As a third example, the line:
         * <pre>cheeses
         * </pre> specifies that the key is {@code "cheeses"} and the associated
         * element is the empty string {@code ""}.
         * <p>
         * <a name="unicodeescapes"></a>
         * Characters in keys and elements can be represented in escape
         * sequences similar to those used for character and string literals
         * (see sections 3.3 and 3.10.6 of
         * <cite>The Java&trade; Language Specification</cite>).
         *
         * The differences from the character escape sequences and Unicode
         * escapes used for characters and strings are:
         *
         * <ul>
         * <li> Octal escapes are not recognized.
         *
         * <li> The character sequence {@code \b} does <i>not</i>
         * represent a backspace character.
         *
         * <li> The method does not treat a backslash character, {@code \},
         * before a non-valid escape character as an error; the backslash is
         * silently dropped. For example, in a Java string the sequence
         * {@code "\z"} would cause a compile time error. In contrast, this
         * method silently drops the backslash. Therefore, this method treats
         * the two character sequence {@code "\b"} as equivalent to the single
         * character {@code 'b'}.
         *
         * <li> Escapes are not necessary for single and double quotes; however,
         * by the rule above, single and double quote characters preceded by a
         * backslash still yield single and double quote characters,
         * respectively.
         *
         * <li> Only a single 'u' character is allowed in a Unicode escape
         * sequence.
         *
         * </ul>
         * <p>
         * The specified stream remains open after this method returns.
         *
         * @param reader the input character stream.
         * @throws IOException if an error occurred when reading from the input
         * stream.
         * @throws IllegalArgumentException if a malformed Unicode escape
         * appears in the input.
         * @since 1.6
         */
        public synchronized void load(Reader reader) throws IOException {
            load0(new LineReader(reader));
        }

        /**
         * Reads a property list (key and element pairs) from the input byte
         * stream. The input stream is in a simple line-oriented format as
         * specified in {@link #load(Reader) load(Reader)} and is
         * assumed to use the ISO 8859-1 character encoding; that is each byte
         * is one Latin1 character. Characters not in Latin1, and certain
         * special characters, are represented in keys and elements using
         * Unicode escapes as defined in section 3.3 of
         * <cite>The Java&trade; Language Specification</cite>.
         * <p>
         * The specified stream remains open after this method returns.
         *
         * @param inStream the input stream.
         * @exception IOException if an error occurred when reading from the
         * input stream.
         * @throws IllegalArgumentException if the input stream contains a
         * malformed Unicode escape sequence.
         * @since 1.2
         */
        public synchronized void load(InputStream inStream) throws IOException {
            load0(new LineReader(inStream));
        }

        private void load0(LineReader lr) throws IOException {
            char[] convtBuf = new char[1024];
            int limit;
            int keyLen;
            int valueStart;
            char c;
            boolean hasSep;
            boolean precedingBackslash;

            while ((limit = lr.readLine()) >= 0) {
                c = 0;
                keyLen = 0;
                valueStart = limit;
                hasSep = false;

                //System.out.println("line=<" + new String(lineBuf, 0, limit) + ">");
                precedingBackslash = false;
                while (keyLen < limit) {
                    c = lr.lineBuf[keyLen];
                    //need check if escaped.
                    if ((c == '=' || c == ':') && !precedingBackslash) {
                        valueStart = keyLen + 1;
                        hasSep = true;
                        break;
                    } else if ((c == ' ' || c == '\t' || c == '\f') && !precedingBackslash) {
                        valueStart = keyLen + 1;
                        break;
                    }
                    if (c == '\\') {
                        precedingBackslash = !precedingBackslash;
                    } else {
                        precedingBackslash = false;
                    }
                    keyLen++;
                }
                while (valueStart < limit) {
                    c = lr.lineBuf[valueStart];
                    if (c != ' ' && c != '\t' && c != '\f') {
                        if (!hasSep && (c == '=' || c == ':')) {
                            hasSep = true;
                        } else {
                            break;
                        }
                    }
                    valueStart++;
                }
                String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
                String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
                put(key, value);
            }
        }

        /* Read in a "logical line" from an InputStream/Reader, skip all comment
     * and blank lines and filter out those leading whitespace characters
     * (\u0020, \u0009 and \u000c) from the beginning of a "natural line".
     * Method returns the char length of the "logical line" and stores
     * the line in "lineBuf".
         */
        class LineReader {

            public LineReader(InputStream inStream) {
                this.inStream = inStream;
                inByteBuf = new byte[8192];
            }

            public LineReader(Reader reader) {
                this.reader = reader;
                inCharBuf = new char[8192];
            }

            byte[] inByteBuf;
            char[] inCharBuf;
            char[] lineBuf = new char[1024];
            int inLimit = 0;
            int inOff = 0;
            InputStream inStream;
            Reader reader;

            int readLine() throws IOException {
                int len = 0;
                char c = 0;

                boolean skipWhiteSpace = true;
                boolean isCommentLine = false;
                boolean isNewLine = true;
                boolean appendedLineBegin = false;
                boolean precedingBackslash = false;
                boolean skipLF = false;

                while (true) {
                    if (inOff >= inLimit) {
                        inLimit = (inStream == null) ? reader.read(inCharBuf)
                                : inStream.read(inByteBuf);
                        inOff = 0;
                        if (inLimit <= 0) {
                            if (len == 0 || isCommentLine) {
                                return -1;
                            }
                            if (precedingBackslash) {
                                len--;
                            }
                            return len;
                        }
                    }
                    if (inStream != null) {
                        //The line below is equivalent to calling a
                        //ISO8859-1 decoder.
                        c = (char) (0xff & inByteBuf[inOff++]);
                    } else {
                        c = inCharBuf[inOff++];
                    }
                    if (skipLF) {
                        skipLF = false;
                        if (c == '\n') {
                            continue;
                        }
                    }
                    if (skipWhiteSpace) {
                        if (c == ' ' || c == '\t' || c == '\f') {
                            continue;
                        }
                        if (!appendedLineBegin && (c == '\r' || c == '\n')) {
                            continue;
                        }
                        skipWhiteSpace = false;
                        appendedLineBegin = false;
                    }
                    if (isNewLine) {
                        isNewLine = false;
                        if (c == '#' || c == '!') {
                            isCommentLine = true;
                            continue;
                        }
                    }

                    if (c != '\n' && c != '\r') {
                        lineBuf[len++] = c;
                        if (len == lineBuf.length) {
                            int newLength = lineBuf.length * 2;
                            if (newLength < 0) {
                                newLength = Integer.MAX_VALUE;
                            }
                            char[] buf = new char[newLength];
                            System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
                            lineBuf = buf;
                        }
                        //flip the preceding backslash flag
                        if (c == '\\') {
                            precedingBackslash = !precedingBackslash;
                        } else {
                            precedingBackslash = false;
                        }
                    } else {
                        // reached EOL
                        if (isCommentLine || len == 0) {
                            isCommentLine = false;
                            isNewLine = true;
                            skipWhiteSpace = true;
                            len = 0;
                            continue;
                        }
                        if (inOff >= inLimit) {
                            inLimit = (inStream == null)
                                    ? reader.read(inCharBuf)
                                    : inStream.read(inByteBuf);
                            inOff = 0;
                            if (inLimit <= 0) {
                                if (precedingBackslash) {
                                    len--;
                                }
                                return len;
                            }
                        }
                        if (precedingBackslash) {
                            len -= 1;
                            //skip the leading whitespace characters in following line
                            skipWhiteSpace = true;
                            appendedLineBegin = true;
                            precedingBackslash = false;
                            if (c == '\r') {
                                skipLF = true;
                            }
                        } else {
                            return len;
                        }
                    }
                }
            }
        }

        /*
     * Converts encoded &#92;uxxxx to unicode chars
     * and changes special saved chars to their original forms
         */
        private String loadConvert(char[] in, int off, int len, char[] convtBuf) {
            if (convtBuf.length < len) {
                int newLen = len * 2;
                if (newLen < 0) {
                    newLen = Integer.MAX_VALUE;
                }
                convtBuf = new char[newLen];
            }
            char aChar;
            char[] out = convtBuf;
            int outLen = 0;
            int end = off + len;

            while (off < end) {
                aChar = in[off++];
                if (aChar == '\\') {
                    aChar = in[off++];
                    if (aChar == 'u') {
                        // Read the xxxx
                        int value = 0;
                        for (int i = 0; i < 4; i++) {
                            aChar = in[off++];
                            switch (aChar) {
                                case '0':
                                case '1':
                                case '2':
                                case '3':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                case '8':
                                case '9':
                                    value = (value << 4) + aChar - '0';
                                    break;
                                case 'a':
                                case 'b':
                                case 'c':
                                case 'd':
                                case 'e':
                                case 'f':
                                    value = (value << 4) + 10 + aChar - 'a';
                                    break;
                                case 'A':
                                case 'B':
                                case 'C':
                                case 'D':
                                case 'E':
                                case 'F':
                                    value = (value << 4) + 10 + aChar - 'A';
                                    break;
                                default:
                                    throw new IllegalArgumentException(
                                            "Malformed \\uxxxx encoding.");
                            }
                        }
                        out[outLen++] = (char) value;
                    } else {
                        if (aChar == 't') {
                            aChar = '\t';
                        } else if (aChar == 'r') {
                            aChar = '\r';
                        } else if (aChar == 'n') {
                            aChar = '\n';
                        } else if (aChar == 'f') {
                            aChar = '\f';
                        }
                        out[outLen++] = aChar;
                    }
                } else {
                    out[outLen++] = aChar;
                }
            }
            return new String(out, 0, outLen);
        }

        /*
     * Converts unicodes to encoded &#92;uxxxx and escapes
     * special characters with a preceding slash
         */
        private String saveConvert(String theString,
                boolean escapeSpace,
                boolean escapeUnicode) {
            int len = theString.length();
            int bufLen = len * 2;
            if (bufLen < 0) {
                bufLen = Integer.MAX_VALUE;
            }
            StringBuffer outBuffer = new StringBuffer(bufLen);

            for (int x = 0; x < len; x++) {
                char aChar = theString.charAt(x);
                // Handle common case first, selecting largest block that
                // avoids the specials below
                if ((aChar > 61) && (aChar < 127)) {
                    if (aChar == '\\') {
                        outBuffer.append('\\');
                        outBuffer.append('\\');
                        continue;
                    }
                    outBuffer.append(aChar);
                    continue;
                }
                switch (aChar) {
                    case ' ':
                        if (x == 0 || escapeSpace) {
                            outBuffer.append('\\');
                        }
                        outBuffer.append(' ');
                        break;
                    case '\t':
                        outBuffer.append('\\');
                        outBuffer.append('t');
                        break;
                    case '\n':
                        outBuffer.append('\\');
                        outBuffer.append('n');
                        break;
                    case '\r':
                        outBuffer.append('\\');
                        outBuffer.append('r');
                        break;
                    case '\f':
                        outBuffer.append('\\');
                        outBuffer.append('f');
                        break;
                    case '=': // Fall through
                    case ':': // Fall through
                    case '#': // Fall through
                    case '!':
                        outBuffer.append('\\');
                        outBuffer.append(aChar);
                        break;
                    default:
                        if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
                            outBuffer.append('\\');
                            outBuffer.append('u');
                            outBuffer.append(toHex((aChar >> 12) & 0xF));
                            outBuffer.append(toHex((aChar >> 8) & 0xF));
                            outBuffer.append(toHex((aChar >> 4) & 0xF));
                            outBuffer.append(toHex(aChar & 0xF));
                        } else {
                            outBuffer.append(aChar);
                        }
                }
            }
            return outBuffer.toString();
        }

        private static void writeComments(BufferedWriter bw, String comments)
                throws IOException {
            bw.write("#");
            int len = comments.length();
            int current = 0;
            int last = 0;
            char[] uu = new char[6];
            uu[0] = '\\';
            uu[1] = 'u';
            while (current < len) {
                char c = comments.charAt(current);
                if (c > '\u00ff' || c == '\n' || c == '\r') {
                    if (last != current) {
                        bw.write(comments.substring(last, current));
                    }
                    if (c > '\u00ff') {
                        uu[2] = toHex((c >> 12) & 0xf);
                        uu[3] = toHex((c >> 8) & 0xf);
                        uu[4] = toHex((c >> 4) & 0xf);
                        uu[5] = toHex(c & 0xf);
                        bw.write(new String(uu));
                    } else {
                        bw.newLine();
                        if (c == '\r'
                                && current != len - 1
                                && comments.charAt(current + 1) == '\n') {
                            current++;
                        }
                        if (current == len - 1
                                || (comments.charAt(current + 1) != '#'
                                && comments.charAt(current + 1) != '!')) {
                            bw.write("#");
                        }
                    }
                    last = current + 1;
                }
                current++;
            }
            if (last != current) {
                bw.write(comments.substring(last, current));
            }
            bw.newLine();
        }

        /**
         * Calls the {@code store(OutputStream out, String comments)} method and
         * suppresses IOExceptions that were thrown.
         *
         * @deprecated This method does not throw an IOException if an I/O error
         * occurs while saving the property list. The preferred way to save a
         * properties list is via the {@code store(OutputStream out,
         * String comments)} method or the
         * {@code storeToXML(OutputStream os, String comment)} method.
         *
         * @param out an output stream.
         * @param comments a description of the property list.
         * @exception ClassCastException if this {@code Properties} object
         * contains any keys or values that are not {@code Strings}.
         */
        @Deprecated
        public void save(OutputStream out, String comments) {
            try {
                store(out, comments);
            } catch (IOException e) {
            }
        }

        /**
         * Writes this property list (key and element pairs) in this
         * {@code Properties} table to the output character stream in a format
         * suitable for using the {@link #load(Reader) load(Reader)}
         * method.
         * <p>
         * Properties from the defaults table of this {@code Properties} table
         * (if any) are <i>not</i> written out by this method.
         * <p>
         * If the comments argument is not null, then an ASCII {@code #}
         * character, the comments string, and a line separator are first
         * written to the output stream. Thus, the {@code comments} can serve as
         * an identifying comment. Any one of a line feed ('\n'), a carriage
         * return ('\r'), or a carriage return followed immediately by a line
         * feed in comments is replaced by a line separator generated by the
         * {@code Writer} and if the next character in comments is not character
         * {@code #} or character {@code !} then an ASCII {@code #} is written
         * out after that line separator.
         * <p>
         * Next, a comment line is always written, consisting of an ASCII
         * {@code #} character, the current date and time (as if produced by the
         * {@code toString} method of {@code Date} for the current time), and a
         * line separator as generated by the {@code Writer}.
         * <p>
         * Then every entry in this {@code Properties} table is written out, one
         * per line. For each entry the key string is written, then an ASCII
         * {@code =}, then the associated element string. For the key, all space
         * characters are written with a preceding {@code \} character. For the
         * element, leading space characters, but not embedded or trailing space
         * characters, are written with a preceding {@code \} character. The key
         * and element characters {@code #}, {@code !}, {@code =}, and {@code :}
         * are written with a preceding backslash to ensure that they are
         * properly loaded.
         * <p>
         * After the entries have been written, the output stream is flushed.
         * The output stream remains open after this method returns.
         * <p>
         *
         * @param writer an output character stream writer.
         * @param comments a description of the property list.
         * @exception IOException if writing this property list to the specified
         * output stream throws an <tt>IOException</tt>.
         * @exception ClassCastException if this {@code Properties} object
         * contains any keys or values that are not {@code Strings}.
         * @exception NullPointerException if {@code writer} is null.
         * @since 1.6
         */
        public void store(Writer writer, String comments)
                throws IOException {
            store0((writer instanceof BufferedWriter) ? (BufferedWriter) writer
                    : new BufferedWriter(writer),
                    comments,
                    false);
        }

        /**
         * Writes this property list (key and element pairs) in this
         * {@code Properties} table to the output stream in a format suitable
         * for loading into a {@code Properties} table using the
         * {@link #load(InputStream) load(InputStream)} method.
         * <p>
         * Properties from the defaults table of this {@code Properties} table
         * (if any) are <i>not</i> written out by this method.
         * <p>
         * This method outputs the comments, properties keys and values in the
         * same format as specified in
         * {@link #store(Writer, String) store(Writer)}, with
         * the following differences:
         * <ul>
         * <li>The stream is written using the ISO 8859-1 character encoding.
         *
         * <li>Characters not in Latin-1 in the comments are written as
         * {@code \u005Cu}<i>xxxx</i> for their appropriate unicode hexadecimal
         * value <i>xxxx</i>.
         *
         * <li>Characters less than {@code \u005Cu0020} and characters greater
         * than {@code \u005Cu007E} in property keys or values are written as
         * {@code \u005Cu}<i>xxxx</i> for the appropriate hexadecimal value
         * <i>xxxx</i>.
         * </ul>
         * <p>
         * After the entries have been written, the output stream is flushed.
         * The output stream remains open after this method returns.
         * <p>
         * @param out an output stream.
         * @param comments a description of the property list.
         * @exception IOException if writing this property list to the specified
         * output stream throws an <tt>IOException</tt>.
         * @exception ClassCastException if this {@code Properties} object
         * contains any keys or values that are not {@code Strings}.
         * @exception NullPointerException if {@code out} is null.
         * @since 1.2
         */
        public void store(OutputStream out, String comments)
                throws IOException {
            store0(new BufferedWriter(new OutputStreamWriter(out, "8859_1")),
                    comments,
                    true);
        }

        private void store0(BufferedWriter bw, String comments, boolean escUnicode)
                throws IOException {
            if (comments != null) {
                writeComments(bw, comments);
            }
            //bw.write("#" + new Date().toString());
            //bw.newLine();
            synchronized (this) {
                for (Enumeration<?> e = keys(); e.hasMoreElements();) {
                    String key = (String) e.nextElement();
                    String val = (String) get(key);
                    key = saveConvert(key, true, escUnicode);
                    /* No need to escape embedded and trailing spaces for value, hence
                 * pass false to flag.
                     */
                    val = saveConvert(val, false, escUnicode);
                    bw.write(key + "=" + val);
                    bw.newLine();
                }
            }
            bw.flush();
        }

        /**
         * Searches for the property with the specified key in this property
         * list. If the key is not found in this property list, the default
         * property list, and its defaults, recursively, are then checked. The
         * method returns {@code null} if the property is not found.
         *
         * @param key the property key.
         * @return the value in this property list with the specified key value.
         * @see #setProperty
         * @see #defaults
         */
        public String getProperty(String key) {
            Object oval = super.get(key);
            String sval = (oval instanceof String) ? (String) oval : null;
            return ((sval == null) && (defaults != null)) ? defaults.getProperty(key) : sval;
        }

        /**
         * Searches for the property with the specified key in this property
         * list. If the key is not found in this property list, the default
         * property list, and its defaults, recursively, are then checked. The
         * method returns the default value argument if the property is not
         * found.
         *
         * @param key the hashtable key.
         * @param defaultValue a default value.
         *
         * @return the value in this property list with the specified key value.
         * @see #setProperty
         * @see #defaults
         */
        public String getProperty(String key, String defaultValue) {
            String val = getProperty(key);
            return (val == null) ? defaultValue : val;
        }

        /**
         * Returns an enumeration of all the keys in this property list,
         * including distinct keys in the default property list if a key of the
         * same name has not already been found from the main properties list.
         *
         * @return an enumeration of all the keys in this property list,
         * including the keys in the default property list.
         * @throws ClassCastException if any key in this property list is not a
         * string.
         * @see Enumeration
         * @see java.util.Properties#defaults
         * @see #stringPropertyNames
         */
        public Enumeration<?> propertyNames() {
            Hashtable<String, Object> h = new Hashtable<>();
            enumerate(h);
            return h.keys();
        }

        /**
         * Returns a set of keys in this property list where the key and its
         * corresponding value are strings, including distinct keys in the
         * default property list if a key of the same name has not already been
         * found from the main properties list. Properties whose key or value is
         * not of type <tt>String</tt> are omitted.
         * <p>
         * The returned set is not backed by the <tt>Properties</tt> object.
         * Changes to this <tt>Properties</tt> are not reflected in the set, or
         * vice versa.
         *
         * @return a set of keys in this property list where the key and its
         * corresponding value are strings, including the keys in the default
         * property list.
         * @see java.util.Properties#defaults
         * @since 1.6
         */
        public Set<String> stringPropertyNames() {
            Hashtable<String, String> h = new Hashtable<>();
            enumerateStringProperties(h);
            return h.keySet();
        }

        /**
         * Prints this property list out to the specified output stream. This
         * method is useful for debugging.
         *
         * @param out an output stream.
         * @throws ClassCastException if any key in this property list is not a
         * string.
         */
        public void list(PrintStream out) {
            out.println("-- listing properties --");
            Hashtable<String, Object> h = new Hashtable<>();
            enumerate(h);
            for (Enumeration<String> e = h.keys(); e.hasMoreElements();) {
                String key = e.nextElement();
                String val = (String) h.get(key);
                if (val.length() > 40) {
                    val = val.substring(0, 37) + "...";
                }
                out.println(key + "=" + val);
            }
        }

        /**
         * Prints this property list out to the specified output stream. This
         * method is useful for debugging.
         *
         * @param out an output stream.
         * @throws ClassCastException if any key in this property list is not a
         * string.
         * @since JDK1.1
         */
        /*
     * Rather than use an anonymous inner class to share common code, this
     * method is duplicated in order to ensure that a non-1.1 compiler can
     * compile this file.
         */
        public void list(PrintWriter out) {
            out.println("-- listing properties --");
            Hashtable<String, Object> h = new Hashtable<>();
            enumerate(h);
            for (Enumeration<String> e = h.keys(); e.hasMoreElements();) {
                String key = e.nextElement();
                String val = (String) h.get(key);
                if (val.length() > 40) {
                    val = val.substring(0, 37) + "...";
                }
                out.println(key + "=" + val);
            }
        }

        /**
         * Enumerates all key/value pairs in the specified hashtable.
         *
         * @param h the hashtable
         * @throws ClassCastException if any of the property keys is not of
         * String type.
         */
        private synchronized void enumerate(Hashtable<String, Object> h) {
            if (defaults != null) {
                defaults.enumerate(h);
            }
            for (Enumeration<?> e = keys(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                h.put(key, get(key));
            }
        }

        /**
         * Enumerates all key/value pairs in the specified hashtable and omits
         * the property if the key or value is not a string.
         *
         * @param h the hashtable
         */
        private synchronized void enumerateStringProperties(Hashtable<String, String> h) {
            if (defaults != null) {
                defaults.enumerateStringProperties(h);
            }
            for (Enumeration<?> e = keys(); e.hasMoreElements();) {
                Object k = e.nextElement();
                Object v = get(k);
                if (k instanceof String && v instanceof String) {
                    h.put((String) k, (String) v);
                }
            }
        }

        /**
         * Convert a nibble to a hex character
         *
         * @param nibble the nibble to convert.
         */
        private static char toHex(int nibble) {
            return hexDigit[(nibble & 0xF)];
        }

        /**
         * A table of hex digits
         */
        private static final char[] hexDigit = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
    }
}
