/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2021 MinIO, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import okhttp3.OkHttpClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class MinioClientWithTLS {
    // your key store password
    private static final String KEY_STORE_PASSWORD = "123456";
    // your trust store password
    private static final String TRUST_STORE_PASSWORD = "123456";

    public static void main(String[] args) throws Exception {
        // build OkHttpClient
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(getSSLSocketFactoryWithJKS());
        // default skip hostname verifier
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint("https://MINIO-HOST:MINIO-PORT")
                        .credentials("YOUR-ACCESSKEY", "YOUR-SECRETACCESSKEY").httpClient(builder.build())
                        .build();
        // minioClient.ignoreCertCheck();
        // Get information of an object.
        StatObjectResponse stat =
                minioClient.statObject(
                        StatObjectArgs.builder().bucket("my-bucketname").object("my-objectname").build());
        System.out.println(stat);
    }

    /**
     * The client uses a certificate of the jks type.
     *
     * @return SSLSocketFactory
     */
    public static SSLSocketFactory getSSLSocketFactoryWithJKS() {
        try (InputStream trustInput = new FileInputStream("your truststore file path");
             InputStream keyInput = new FileInputStream("your keystore file path");) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(trustInput, TRUST_STORE_PASSWORD.toCharArray());

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(keyInput, KEY_STORE_PASSWORD.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, KEY_STORE_PASSWORD.toCharArray());

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
            SSLSocketFactory factory = sslContext.getSocketFactory();
            return factory;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * The client uses a certificate of the p12 type.
     *
     * @return SSLSocketFactory
     */
    public static SSLSocketFactory getSSLSocketFactoryWithP12() {
        try (InputStream trustInput = new FileInputStream("your truststore file path");
             InputStream keyInput = new FileInputStream("your keystore file path");) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(trustInput, TRUST_STORE_PASSWORD.toCharArray());

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(keyInput, KEY_STORE_PASSWORD.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, KEY_STORE_PASSWORD.toCharArray());

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
            SSLSocketFactory factory = sslContext.getSocketFactory();
            return factory;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Ignore all certificates equals minioClient.ignoreCertCheck()
     *
     * @return SSLSocketFactory
     */
    public static SSLSocketFactory getSSLSocketFactoryWithoutCertificate() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, getTrustManager(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TrustManager[] getTrustManager() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };
        return trustAllCerts;
    }
}
