/**
 * Copyright 2014-2017 Functional Genomics Development Team, European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * @author Andrew Tikhonov <andrew.tikhonov@gmail.com>
 **/

package uk.ac.ebi.biostd.webapp.server.endpoint.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostd.webapp.server.config.BackendConfig;

/**
 * Created by andrew on 07/06/2017.
 */
public class SSOSupport {

    public static JWTVerifier instance = null;
    private static Logger log = LoggerFactory.getLogger(SSOSupport.class);

    public SSOSupport() {
    }

    public static String getSSOServerPublicKeyText() {
        try {
            String inputLine;
            StringBuffer response = new StringBuffer();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    (new URL(BackendConfig.getSSOPublicCertificatePemURL()).openStream())));
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } catch (Exception e) {
            log.error("Error!", e);
        }
        return null;
    }

    public static byte[] getSSOServerPublicKeyBinary() {
        try {
            URL keyURL = new URL(BackendConfig.getSSOPublicCertificateDerURL());
            InputStream is = null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                is = keyURL.openStream();
                byte[] byteChunk = new byte[2048];
                int n;

                while ((n = is.read(byteChunk)) > 0) {
                    baos.write(byteChunk, 0, n);
                }
            } catch (IOException e) {
                log.error("Error!", e);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error!", e);
        }
        return null;
    }

    public static PublicKey convertToPublicKeyFromBinary(byte[] publicKeyBinary) throws Exception {
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        X509Certificate cer = (X509Certificate) fact.generateCertificate(
                new ByteArrayInputStream(publicKeyBinary));
        PublicKey key = cer.getPublicKey();
        return key;
    }

    public static PublicKey convertToPublicKey(String publicKey) throws Exception {
        publicKey = publicKey.replaceAll("-----BEGIN CERTIFICATE-----", "");
        publicKey = publicKey.replaceAll("-----END CERTIFICATE-----", "");
        return convertToPublicKeyFromBinary(Base64.decodeBase64(publicKey.getBytes()));
    }

    public static JWTVerifier getVerifier() throws Exception {
        if (instance == null) {
            PublicKey key = convertToPublicKeyFromBinary(getSSOServerPublicKeyBinary());
            instance = JWT.require(Algorithm.RSA256((RSAPublicKey) key)).build();
        }
        return instance;
    }

    public static String authenticateUsingSSOServer(String username, String password) throws Exception {
        URL authUrl = new URL(BackendConfig.getSSOAuthURL());
        HttpsURLConnection con = (HttpsURLConnection) authUrl.openConnection();
        con.setRequestProperty("Authorization", "Basic " +
                Base64.encodeBase64String((username + ":" + password).getBytes()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public static DecodedJWT verifyToken(String token) throws Exception {
        JWTVerifier verifier = getVerifier();
        if (verifier != null) {
            DecodedJWT tokenDecoded = verifier.verify(token);
            return tokenDecoded;
        }

        return null;
    }

}
