/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015, 2016, 2017, 2018, 2019 MinIO, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.minio;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.errors.BucketPolicyTooLargeException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidExpiresRangeException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.RegionConflictException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.CompleteMultipartUpload;
import io.minio.messages.CompleteMultipartUploadOutput;
import io.minio.messages.CopyObjectResult;
import io.minio.messages.CopyPartResult;
import io.minio.messages.CreateBucketConfiguration;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import io.minio.messages.ErrorResponse;
import io.minio.messages.InitiateMultipartUploadResult;
import io.minio.messages.InputSerialization;
import io.minio.messages.Item;
import io.minio.messages.LegalHold;
import io.minio.messages.ListAllMyBucketsResult;
import io.minio.messages.ListBucketResult;
import io.minio.messages.ListBucketResultV1;
import io.minio.messages.ListBucketResultV2;
import io.minio.messages.ListMultipartUploadsResult;
import io.minio.messages.ListPartsResult;
import io.minio.messages.LocationConstraint;
import io.minio.messages.NotificationConfiguration;
import io.minio.messages.NotificationRecords;
import io.minio.messages.ObjectLockConfiguration;
import io.minio.messages.OutputSerialization;
import io.minio.messages.Part;
import io.minio.messages.Prefix;
import io.minio.messages.Retention;
import io.minio.messages.SelectObjectContentRequest;
import io.minio.messages.SseConfiguration;
import io.minio.messages.Tags;
import io.minio.messages.Upload;
import io.minio.messages.VersioningConfiguration;
import io.minio.org.apache.commons.validator.routines.InetAddressValidator;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Simple Storage Service (aka S3) client to perform bucket and object operations.
 *
 * <h2>Bucket operations</h2>
 *
 * <ul>
 *   <li>Create, list and delete buckets.
 *   <li>Put, get and delete bucket lifecycle configuration.
 *   <li>Put, get and delete bucket policy configuration.
 *   <li>Put, get and delete bucket encryption configuration.
 *   <li>Put and get bucket default retention configuration.
 *   <li>Put and get bucket notification configuration.
 *   <li>Enable and disable bucket versioning.
 * </ul>
 *
 * <h2>Object operations</h2>
 *
 * <ul>
 *   <li>Put, get, delete and list objects.
 *   <li>Create objects by combining existing objects.
 *   <li>Put and get object retention and legal hold.
 *   <li>Filter object content by SQL statement.
 * </ul>
 *
 * <p>If access/secret keys are provided, all S3 operation requests are signed using AWS Signature
 * Version 4; else they are performed anonymously.
 *
 * <p>Examples on using this library are available <a
 * href="https://github.com/minio/minio-java/tree/master/src/test/java/io/minio/examples">here</a>.
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class MinioClient {
  private static final byte[] EMPTY_BODY = new byte[] {};
  // default network I/O timeout is 5 minutes
  private static final long DEFAULT_CONNECTION_TIMEOUT = 5;
  // maximum allowed bucket policy size is 12KiB
  private static final int MAX_BUCKET_POLICY_SIZE = 12 * 1024;
  // default expiration for a presigned URL is 7 days in seconds
  private static final int DEFAULT_EXPIRY_TIME = 7 * 24 * 3600;
  private static final String DEFAULT_USER_AGENT =
      "MinIO ("
          + System.getProperty("os.arch")
          + "; "
          + System.getProperty("os.arch")
          + ") minio-java/"
          + MinioProperties.INSTANCE.getVersion();
  private static final String END_HTTP = "----------END-HTTP----------";
  private static final String US_EAST_1 = "us-east-1";
  private static final String UPLOAD_ID = "uploadId";

  private static final Set<String> amzHeaders = new HashSet<>();

  static {
    amzHeaders.add("server-side-encryption");
    amzHeaders.add("server-side-encryption-aws-kms-key-id");
    amzHeaders.add("server-side-encryption-context");
    amzHeaders.add("server-side-encryption-customer-algorithm");
    amzHeaders.add("server-side-encryption-customer-key");
    amzHeaders.add("server-side-encryption-customer-key-md5");
    amzHeaders.add("website-redirect-location");
    amzHeaders.add("storage-class");
  }

  private static final Set<String> standardHeaders = new HashSet<>();

  static {
    standardHeaders.add("content-type");
    standardHeaders.add("cache-control");
    standardHeaders.add("content-encoding");
    standardHeaders.add("content-disposition");
    standardHeaders.add("content-language");
    standardHeaders.add("expires");
    standardHeaders.add("range");
  }

  private PrintWriter traceStream;

  // the current client instance's base URL.
  private HttpUrl baseUrl;
  // access key to sign all requests with
  private String accessKey;
  // Secret key to sign all requests with
  private String secretKey;
  // Region to sign all requests with
  private String region;

  private String userAgent = DEFAULT_USER_AGENT;

  private OkHttpClient httpClient;

  private boolean isAwsHost = false;
  private boolean isAcceleratedHost = false;
  private boolean isDualStackHost = false;
  private boolean useVirtualStyle = false;

  /**
   * Creates MinIO client object with given endpoint using anonymous access.
   *
   * <pre>Example:{@code
   * MinioClient minioClient = new MinioClient("https://play.min.io");
   * }</pre>
   *
   * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
   *     <pre>           Examples:
   *             * https://s3.amazonaws.com
   *             * https://s3.amazonaws.com/
   *             * https://play.min.io
   *             * http://play.min.io:9010/
   *             * localhost
   *             * localhost.localdomain
   *             * play.min.io
   *             * 127.0.0.1
   *             * 192.168.1.60
   *             * ::1</pre>
   *
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
   *     secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
   *     boolean secure)
   * @see #MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String
   *     region, Boolean secure, OkHttpClient httpClient)
   */
  public MinioClient(String endpoint) throws IllegalArgumentException {
    this(endpoint, null, null, null, null, null, null);
  }

  /**
   * Creates MinIO client object with given URL object using anonymous access.
   *
   * <pre>Example:{@code
   * MinioClient minioClient = new MinioClient(new URL("https://play.min.io"));
   * }</pre>
   *
   * @param url Endpoint as {@link URL} object.
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
   *     secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
   *     boolean secure)
   * @see #MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String
   *     region, Boolean secure, OkHttpClient httpClient)
   */
  public MinioClient(URL url) throws InvalidEndpointException, InvalidPortException {
    this(url.toString(), null, null, null, null, null, null);
  }

  /**
   * Creates MinIO client object with given HttpUrl object using anonymous access.
   *
   * <pre>Example:{@code
   * MinioClient minioClient = new MinioClient(new HttpUrl.parse("https://play.min.io"));
   * }</pre>
   *
   * @param url Endpoint as {@link HttpUrl} object.
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
   *     secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
   *     boolean secure)
   * @see #MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String
   *     region, Boolean secure, OkHttpClient httpClient)
   */
  public MinioClient(HttpUrl url) throws IllegalArgumentException {
    this(url.toString(), null, null, null, null, null, null);
  }

  /**
   * Creates MinIO client object with given endpoint, access key and secret key.
   *
   * <pre>Example:{@code
   * MinioClient minioClient = new MinioClient("https://play.min.io",
   *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
   * }</pre>
   *
   * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
   *     <pre>           Examples:
   *             * https://s3.amazonaws.com
   *             * https://s3.amazonaws.com/
   *             * https://play.min.io
   *             * http://play.min.io:9010/
   *             * localhost
   *             * localhost.localdomain
   *             * play.min.io
   *             * 127.0.0.1
   *             * 192.168.1.60
   *             * ::1</pre>
   *
   * @param accessKey Access key (aka user ID) of your account in S3 service.
   * @param secretKey Secret Key (aka password) of your account in S3 service.
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
   *     secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
   *     boolean secure)
   * @see #MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String
   *     region, Boolean secure, OkHttpClient httpClient)
   */
  public MinioClient(String endpoint, String accessKey, String secretKey)
      throws IllegalArgumentException {
    this(endpoint, null, accessKey, secretKey, null, null, null);
  }

  /**
   * Creates MinIO client object with given endpoint, access key, secret key and region name.
   *
   * <pre>Example:{@code
   * MinioClient minioClient = new MinioClient("https://play.min.io",
   *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", "us-west-1");
   * }</pre>
   *
   * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
   *     <pre>           Examples:
   *             * https://s3.amazonaws.com
   *             * https://s3.amazonaws.com/
   *             * https://play.min.io
   *             * http://play.min.io:9010/
   *             * localhost
   *             * localhost.localdomain
   *             * play.min.io
   *             * 127.0.0.1
   *             * 192.168.1.60
   *             * ::1</pre>
   *
   * @param accessKey Access key (aka user ID) of your account in S3 service.
   * @param secretKey Secret Key (aka password) of your account in S3 service.
   * @param region Region name of buckets in S3 service.
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
   *     secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
   *     boolean secure)
   * @see #MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String
   *     region, Boolean secure, OkHttpClient httpClient)
   */
  public MinioClient(String endpoint, String accessKey, String secretKey, String region)
      throws IllegalArgumentException {
    this(endpoint, null, accessKey, secretKey, region, null, null);
  }

  /**
   * Creates MinIO client object with given URL object, access key and secret key.
   *
   * <pre>{@code MinioClient minioClient = new MinioClient(new URL("https://play.min.io"),
   *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");}</pre>
   *
   * @param url Endpoint as {@link URL} object.
   * @param accessKey Access key (aka user ID) of your account in S3 service.
   * @param secretKey Secret Key (aka password) of your account in S3 service.
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
   *     secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
   *     boolean secure)
   * @see #MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String
   *     region, Boolean secure, OkHttpClient httpClient)
   */
  public MinioClient(URL url, String accessKey, String secretKey) throws IllegalArgumentException {
    this(url.toString(), null, accessKey, secretKey, null, null, null);
  }

  /**
   * Creates MinIO client object with given URL object, access key and secret key.
   *
   * <pre>Example:{@code
   * MinioClient minioClient = new MinioClient(HttpUrl.parse("https://play.min.io"),
   *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
   * }</pre>
   *
   * @param url Endpoint as {@link HttpUrl} object.
   * @param accessKey Access key (aka user ID) of your account in S3 service.
   * @param secretKey Secret Key (aka password) of your account in S3 service.
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
   *     secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
   *     boolean secure)
   * @see #MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String
   *     region, Boolean secure, OkHttpClient httpClient)
   */
  public MinioClient(HttpUrl url, String accessKey, String secretKey)
      throws IllegalArgumentException {
    this(url.toString(), null, accessKey, secretKey, null, null, null);
  }

  /**
   * Creates MinIO client object with given endpoint, port, access key and secret key.
   *
   * <pre>Example:{@code
   * MinioClient minioClient = new MinioClient("play.min.io", 9000,
   *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
   * }</pre>
   *
   * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
   *     <pre>           Examples:
   *             * https://s3.amazonaws.com
   *             * https://s3.amazonaws.com/
   *             * https://play.min.io
   *             * http://play.min.io:9010/
   *             * localhost
   *             * localhost.localdomain
   *             * play.min.io
   *             * 127.0.0.1
   *             * 192.168.1.60
   *             * ::1</pre>
   *
   * @param port TCP/IP port number between 1 and 65535. Unused if endpoint is an URL.
   * @param accessKey Access key (aka user ID) of your account in S3 service.
   * @param secretKey Secret Key (aka password) of your account in S3 service.
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
   *     secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
   *     boolean secure)
   * @see #MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String
   *     region, Boolean secure, OkHttpClient httpClient)
   */
  public MinioClient(String endpoint, int port, String accessKey, String secretKey)
      throws IllegalArgumentException {
    this(endpoint, port, accessKey, secretKey, null, null, null);
  }

  /**
   * Creates MinIO client object with given endpoint, access key and secret key using secure (TLS)
   * connection.
   *
   * <pre>Example:{@code
   * MinioClient minioClient = new MinioClient("play.min.io",
   *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);
   * }</pre>
   *
   * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
   *     <pre>           Examples:
   *             * https://s3.amazonaws.com
   *             * https://s3.amazonaws.com/
   *             * https://play.min.io
   *             * http://play.min.io:9010/
   *             * localhost
   *             * localhost.localdomain
   *             * play.min.io
   *             * 127.0.0.1
   *             * 192.168.1.60
   *             * ::1</pre>
   *
   * @param accessKey Access key (aka user ID) of your account in S3 service.
   * @param secretKey Secret Key (aka password) of your account in S3 service.
   * @param secure Flag to indicate to use secure (TLS) connection to S3 service or not.
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
   *     secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
   *     boolean secure)
   * @see #MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String
   *     region, Boolean secure, OkHttpClient httpClient)
   */
  public MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
      throws IllegalArgumentException {
    this(endpoint, null, accessKey, secretKey, null, secure, null);
  }

  /**
   * Creates MinIO client object using given endpoint, port, access key, secret key and secure (TLS)
   * connection.
   *
   * <pre>Example:{@code
   * MinioClient minioClient = new MinioClient("play.min.io", 9000,
   *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);
   * }</pre>
   *
   * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
   *     <pre>           Examples:
   *             * https://s3.amazonaws.com
   *             * https://s3.amazonaws.com/
   *             * https://play.min.io
   *             * http://play.min.io:9010/
   *             * localhost
   *             * localhost.localdomain
   *             * play.min.io
   *             * 127.0.0.1
   *             * 192.168.1.60
   *             * ::1</pre>
   *
   * @param port TCP/IP port number between 1 and 65535. Unused if endpoint is an URL.
   * @param accessKey Access key (aka user ID) of your account in S3 service.
   * @param secretKey Secret Key (aka password) of your account in S3 service.
   * @param secure Flag to indicate to use secure (TLS) connection to S3 service or not.
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
   *     boolean secure)
   * @see #MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String
   *     region, Boolean secure, OkHttpClient httpClient)
   */
  public MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
      throws IllegalArgumentException {
    this(endpoint, port, accessKey, secretKey, null, secure, null);
  }

  /**
   * Creates MinIO client object using given endpoint, port, access key, secret key, region and
   * secure (TLS) connection.
   *
   * <pre>Example:{@code
   * MinioClient minioClient = new MinioClient("play.min.io", 9000,
   *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);
   * }</pre>
   *
   * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
   *     <pre>           Examples:
   *             * https://s3.amazonaws.com
   *             * https://s3.amazonaws.com/
   *             * https://play.min.io
   *             * http://play.min.io:9010/
   *             * localhost
   *             * localhost.localdomain
   *             * play.min.io
   *             * 127.0.0.1
   *             * 192.168.1.60
   *             * ::1</pre>
   *
   * @param port TCP/IP port number between 1 and 65535. Unused if endpoint is an URL.
   * @param accessKey Access key (aka user ID) of your account in S3 service.
   * @param secretKey Secret Key (aka password) of your account in S3 service.
   * @param region Region name of buckets in S3 service.
   * @param secure Flag to indicate to use secure (TLS) connection to S3 service or not.
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, Integer port, String accessKey, String secretKey, String
   *     region, Boolean secure, OkHttpClient httpClient)
   */
  public MinioClient(
      String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
      throws IllegalArgumentException {
    this(endpoint, port, accessKey, secretKey, region, secure, null);
  }

  /**
   * Creates MinIO client object using given endpoint, port, access key, secret key, region and
   * secure (TLS) connection.
   *
   * <pre>Example:{@code
   * MinioClient minioClient = new MinioClient("play.min.io", 9000,
   *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true,
   *     customHttpClient);
   * }</pre>
   *
   * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
   *     <pre>           Examples:
   *             * https://s3.amazonaws.com
   *             * https://s3.amazonaws.com/
   *             * https://play.min.io
   *             * http://play.min.io:9010/
   *             * localhost
   *             * localhost.localdomain
   *             * play.min.io
   *             * 127.0.0.1
   *             * 192.168.1.60
   *             * ::1</pre>
   *
   * @param port TCP/IP port number between 1 and 65535. Overrides if it is non-null.
   * @param accessKey Access key (aka user ID) of your account in S3 service.
   * @param secretKey Secret Key (aka password) of your account in S3 service.
   * @param region Region name of buckets in S3 service.
   * @param secure Flag to indicate to use secure (TLS) connection to S3 service or not. Overrides
   *     if it is non-null.
   * @param httpClient Customized HTTP client object.
   * @throws IllegalArgumentException Throws to indicate invalid argument passed.
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
   *     boolean secure)
   */
  public MinioClient(
      String endpoint,
      Integer port,
      String accessKey,
      String secretKey,
      String region,
      Boolean secure,
      OkHttpClient httpClient)
      throws IllegalArgumentException {
    if (endpoint == null) {
      throw new IllegalArgumentException("null endpoint");
    }

    if (region != null && region.equals("")) {
      region = null;
    }

    HttpUrl.Builder urlBuilder = null;
    HttpUrl url = HttpUrl.parse(endpoint);
    if (url != null) {
      if (!"/".equals(url.encodedPath())) {
        throw new IllegalArgumentException("no path allowed in endpoint '" + endpoint + "'");
      }

      urlBuilder = url.newBuilder();
    } else {
      // endpoint may be a valid hostname, IPv4 or IPv6 address
      if (!isValidEndpoint(endpoint)) {
        throw new IllegalArgumentException("invalid host '" + endpoint + "'");
      }

      urlBuilder = new HttpUrl.Builder().host(endpoint);

      if (secure == null) {
        secure = Boolean.TRUE;
      }
    }

    if (secure != null) {
      if (secure) {
        urlBuilder.scheme("https");
      } else {
        urlBuilder.scheme("http");
      }
    }

    if (port != null) {
      if (port < 1 || port > 65535) {
        throw new IllegalArgumentException("port " + port + " must be in range of 1 to 65535");
      }

      urlBuilder.port(port);
    }

    url = urlBuilder.build();

    String host = url.host();
    this.isAwsHost = isAwsEndpoint(host);
    boolean isAwsChinaHost = false;
    if (this.isAwsHost) {
      isAwsChinaHost = host.endsWith(".cn");
      if (isAwsChinaHost) {
        urlBuilder.host("amazonaws.com.cn");
      } else {
        urlBuilder.host("amazonaws.com");
      }
      url = urlBuilder.build();

      this.isAcceleratedHost = isAwsAccelerateEndpoint(host);
      this.isDualStackHost = isAwsDualStackEndpoint(host);
      if (region == null) {
        region = extractRegion(host);
      }
      this.useVirtualStyle = true;
    } else {
      this.useVirtualStyle = host.endsWith("aliyuncs.com");
    }

    if (isAwsChinaHost && region == null) {
      throw new IllegalArgumentException(
          "Region missing in Amazon S3 China endpoint '" + endpoint + "'");
    }

    this.region = region;
    this.baseUrl = url;
    this.accessKey = accessKey;
    this.secretKey = secretKey;

    if (httpClient == null) {
      this.httpClient =
          new OkHttpClient()
              .newBuilder()
              .connectTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MINUTES)
              .writeTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MINUTES)
              .readTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MINUTES)
              .protocols(Arrays.asList(Protocol.HTTP_1_1))
              .build();
      String filename = System.getenv("SSL_CERT_FILE");
      if (filename != null && !filename.equals("")) {
        try {
          this.httpClient = enableExternalCertificates(this.httpClient, filename);
        } catch (GeneralSecurityException | IOException e) {
          throw new RuntimeException(e);
        }
      }
    } else {
      this.httpClient = httpClient;
    }
  }

  private boolean isAwsEndpoint(String endpoint) {
    return (endpoint.startsWith("s3.") || isAwsAccelerateEndpoint(endpoint))
        && (endpoint.endsWith(".amazonaws.com") || endpoint.endsWith(".amazonaws.com.cn"));
  }

  private boolean isAwsAccelerateEndpoint(String endpoint) {
    return endpoint.startsWith("s3-accelerate.");
  }

  private boolean isAwsDualStackEndpoint(String endpoint) {
    return endpoint.contains(".dualstack.");
  }

  /**
   * Extracts region from AWS endpoint if available. Region is placed at second token normal
   * endpoints and third token for dualstack endpoints.
   *
   * <p>Region is marked in square brackets in below examples.
   * <pre>
   * https://s3.[us-east-2].amazonaws.com
   * https://s3.dualstack.[ca-central-1].amazonaws.com
   * https://s3.[cn-north-1].amazonaws.com.cn
   * https://s3.dualstack.[cn-northwest-1].amazonaws.com.cn
   */
  private String extractRegion(String endpoint) {
    String[] tokens = endpoint.split("\\.");
    String token = tokens[1];

    // If token is "dualstack", then region might be in next token.
    if (token.equals("dualstack")) {
      token = tokens[2];
    }

    // If token is equal to "amazonaws", region is not passed in the endpoint.
    if (token.equals("amazonaws")) {
      return null;
    }

    // Return token as region.
    return token;
  }

  /**
   * copied logic from
   * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/CustomTrust.java
   */
  private OkHttpClient enableExternalCertificates(OkHttpClient httpClient, String filename)
      throws GeneralSecurityException, IOException {
    Collection<? extends Certificate> certificates = null;
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(filename);
      certificates = CertificateFactory.getInstance("X.509").generateCertificates(fis);
    } finally {
      if (fis != null) {
        fis.close();
      }
    }

    if (certificates == null || certificates.isEmpty()) {
      throw new IllegalArgumentException("expected non-empty set of trusted certificates");
    }

    char[] password = "password".toCharArray(); // Any password will work.

    // Put the certificates a key store.
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    // By convention, 'null' creates an empty key store.
    keyStore.load(null, password);

    int index = 0;
    for (Certificate certificate : certificates) {
      String certificateAlias = Integer.toString(index++);
      keyStore.setCertificateEntry(certificateAlias, certificate);
    }

    // Use it to build an X509 trust manager.
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, password);
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(keyStore);

    final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
    final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagers, trustManagers, null);
    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

    return httpClient
        .newBuilder()
        .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManagers[0])
        .build();
  }

  /** Returns true if given endpoint is valid else false. */
  private boolean isValidEndpoint(String endpoint) {
    if (InetAddressValidator.getInstance().isValid(endpoint)) {
      return true;
    }

    // endpoint may be a hostname
    // refer https://en.wikipedia.org/wiki/Hostname#Restrictions_on_valid_host_names
    // why checks are done like below
    if (endpoint.length() < 1 || endpoint.length() > 253) {
      return false;
    }

    for (String label : endpoint.split("\\.")) {
      if (label.length() < 1 || label.length() > 63) {
        return false;
      }

      if (!(label.matches("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$"))) {
        return false;
      }
    }

    return true;
  }

  private void checkArgs(BaseArgs args) {
    if (args == null) {
      throw new IllegalArgumentException("null arguments");
    }
  }

  /** Validates if given bucket name is DNS compatible. */
  private void checkBucketName(String name) throws InvalidBucketNameException {
    if (name == null) {
      throw new InvalidBucketNameException("(null)", "null bucket name");
    }

    // Bucket names cannot be no less than 3 and no more than 63 characters long.
    if (name.length() < 3 || name.length() > 63) {
      String msg = "bucket name must be at least 3 and no more than 63 characters long";
      throw new InvalidBucketNameException(name, msg);
    }
    // Successive periods in bucket names are not allowed.
    if (name.contains("..")) {
      String msg =
          "bucket name cannot contain successive periods. For more information refer "
              + "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
      throw new InvalidBucketNameException(name, msg);
    }
    // Bucket names should be dns compatible.
    if (!name.matches("^[a-z0-9][a-z0-9\\.\\-]+[a-z0-9]$")) {
      String msg =
          "bucket name does not follow Amazon S3 standards. For more information refer "
              + "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
      throw new InvalidBucketNameException(name, msg);
    }
  }

  private void checkObjectName(String objectName) throws IllegalArgumentException {
    if ((objectName == null) || (objectName.isEmpty())) {
      throw new IllegalArgumentException("object name cannot be empty");
    }
  }

  private void checkReadRequestSse(ServerSideEncryption sse) throws IllegalArgumentException {
    if (sse == null) {
      return;
    }

    if (sse.type() != ServerSideEncryption.Type.SSE_C) {
      throw new IllegalArgumentException("only SSE_C is supported for all read requests.");
    }

    if (sse.type().requiresTls() && !this.baseUrl.isHttps()) {
      throw new IllegalArgumentException(
          sse.type().name() + "operations must be performed over a secure connection.");
    }
  }

  private void checkWriteRequestSse(ServerSideEncryption sse) throws IllegalArgumentException {
    if (sse == null) {
      return;
    }

    if (sse.type().requiresTls() && !this.baseUrl.isHttps()) {
      throw new IllegalArgumentException(
          sse.type().name() + " operations must be performed over a secure connection.");
    }
  }

  private Map<String, String> normalizeHeaders(Map<String, String> headerMap) {
    Map<String, String> normHeaderMap = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : headerMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      String keyLowerCased = key.toLowerCase(Locale.US);
      if (amzHeaders.contains(keyLowerCased)) {
        key = "x-amz-" + key;
      } else if (!standardHeaders.contains(keyLowerCased) && !keyLowerCased.startsWith("x-amz-")) {
        key = "x-amz-meta-" + key;
      }
      normHeaderMap.put(key, value);
    }
    return normHeaderMap;
  }

  private HttpUrl buildUrl(
      Method method,
      String bucketName,
      String objectName,
      String region,
      Multimap<String, String> queryParamMap)
      throws IllegalArgumentException, InvalidBucketNameException, NoSuchAlgorithmException {
    if (bucketName == null && objectName != null) {
      throw new IllegalArgumentException("null bucket name for object '" + objectName + "'");
    }

    HttpUrl.Builder urlBuilder = this.baseUrl.newBuilder();
    String host = this.baseUrl.host();
    if (bucketName != null) {
      checkBucketName(bucketName);

      boolean enforcePathStyle = false;
      if (method == Method.PUT && objectName == null && queryParamMap == null) {
        // use path style for make bucket to workaround "AuthorizationHeaderMalformed" error from
        // s3.amazonaws.com
        enforcePathStyle = true;
      } else if (queryParamMap != null && queryParamMap.containsKey("location")) {
        // use path style for location query
        enforcePathStyle = true;
      } else if (bucketName.contains(".") && this.baseUrl.isHttps()) {
        // use path style where '.' in bucketName causes SSL certificate validation error
        enforcePathStyle = true;
      }

      if (isAwsHost) {
        String s3Domain = "s3.";
        if (isAcceleratedHost) {
          if (bucketName.contains(".")) {
            throw new IllegalArgumentException(
                "bucket name '"
                    + bucketName
                    + "' with '.' is not allowed for accelerated endpoint");
          }

          if (!enforcePathStyle) {
            s3Domain = "s3-accelerate.";
          }
        }

        String dualStack = "";
        if (isDualStackHost) {
          dualStack = "dualstack.";
        }

        String endpoint = s3Domain + dualStack;
        if (enforcePathStyle || !isAcceleratedHost) {
          endpoint += region + ".";
        }

        host = endpoint + host;
      }

      if (enforcePathStyle || !useVirtualStyle) {
        urlBuilder.host(host);
        urlBuilder.addEncodedPathSegment(S3Escaper.encode(bucketName));
      } else {
        urlBuilder.host(bucketName + "." + host);
      }

      if (objectName != null) {
        // Limitation: OkHttp does not allow to add '.' and '..' as path segment.
        for (String token : objectName.split("/")) {
          if (token.equals(".") || token.equals("..")) {
            throw new IllegalArgumentException(
                "object name with '.' or '..' path segment is not supported");
          }
        }

        urlBuilder.addEncodedPathSegments(S3Escaper.encodePath(objectName));
      }
    } else {
      if (isAwsHost) {
        urlBuilder.host("s3." + region + "." + host);
      }
    }

    if (queryParamMap != null) {
      for (Map.Entry<String, String> entry : queryParamMap.entries()) {
        urlBuilder.addEncodedQueryParameter(
            S3Escaper.encode(entry.getKey()), S3Escaper.encode(entry.getValue()));
      }
    }

    return urlBuilder.build();
  }

  private String getHostHeader(HttpUrl url) {
    // ignore port when port and service matches i.e HTTP -> 80, HTTPS -> 443
    if ((url.scheme().equals("http") && url.port() == 80)
        || (url.scheme().equals("https") && url.port() == 443)) {
      return url.host();
    }

    return url.host() + ":" + url.port();
  }

  private Request createRequest(
      HttpUrl url, Method method, Multimap<String, String> headerMap, Object body, int length)
      throws IllegalArgumentException, InsufficientDataException, InternalException, IOException,
          NoSuchAlgorithmException {
    Request.Builder requestBuilder = new Request.Builder();
    requestBuilder.url(url);

    String contentType = null;
    String contentEncoding = null;
    if (headerMap != null) {
      contentEncoding =
          headerMap.get("Content-Encoding").stream()
              .distinct()
              .filter(encoding -> !encoding.isEmpty())
              .collect(Collectors.joining(","));
      for (Map.Entry<String, String> entry : headerMap.entries()) {
        if (entry.getKey().equals("Content-Type")) {
          contentType = entry.getValue();
        }

        if (!entry.getKey().equals("Content-Encoding")) {
          requestBuilder.header(entry.getKey(), entry.getValue());
        }
      }
    }

    if (!Strings.isNullOrEmpty(contentEncoding)) {
      requestBuilder.header("Content-Encoding", contentEncoding);
    }

    requestBuilder.header("Host", getHostHeader(url));
    // Disable default gzip compression by okhttp library.
    requestBuilder.header("Accept-Encoding", "identity");
    requestBuilder.header("User-Agent", this.userAgent);

    String sha256Hash = null;
    String md5Hash = null;
    if (this.accessKey != null && this.secretKey != null) {
      if (url.isHttps()) {
        // Fix issue #415: No need to compute sha256 if endpoint scheme is HTTPS.
        sha256Hash = "UNSIGNED-PAYLOAD";
        if (body != null) {
          md5Hash = Digest.md5Hash(body, length);
        }
      } else {
        Object data = body;
        int len = length;
        if (data == null) {
          data = new byte[0];
          len = 0;
        }

        String[] hashes = Digest.sha256Md5Hashes(data, len);
        sha256Hash = hashes[0];
        md5Hash = hashes[1];
      }
    } else {
      // Fix issue #567: Compute MD5 hash only for anonymous access.
      if (body != null) {
        md5Hash = Digest.md5Hash(body, length);
      }
    }

    if (md5Hash != null) {
      requestBuilder.header("Content-MD5", md5Hash);
    }

    if (sha256Hash != null) {
      requestBuilder.header("x-amz-content-sha256", sha256Hash);
    }

    ZonedDateTime date = ZonedDateTime.now();
    requestBuilder.header("x-amz-date", date.format(Time.AMZ_DATE_FORMAT));

    RequestBody requestBody = null;
    if (body != null) {
      if (body instanceof RandomAccessFile) {
        requestBody = new HttpRequestBody((RandomAccessFile) body, length, contentType);
      } else if (body instanceof BufferedInputStream) {
        requestBody = new HttpRequestBody((BufferedInputStream) body, length, contentType);
      } else {
        requestBody = new HttpRequestBody((byte[]) body, length, contentType);
      }
    }

    requestBuilder.method(method.toString(), requestBody);
    return requestBuilder.build();
  }

  private Response execute(
      Method method,
      String bucketName,
      String objectName,
      String region,
      Multimap<String, String> headerMap,
      Multimap<String, String> queryParamMap,
      Object body,
      int length)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    boolean traceRequestBody = false;
    if (body != null
        && !(body instanceof InputStream
            || body instanceof RandomAccessFile
            || body instanceof byte[])) {
      byte[] bytes;
      if (body instanceof CharSequence) {
        bytes = body.toString().getBytes(StandardCharsets.UTF_8);
      } else {
        bytes = Xml.marshal(body).getBytes(StandardCharsets.UTF_8);
      }

      body = bytes;
      length = bytes.length;
      traceRequestBody = true;
    }

    if (body == null && (method == Method.PUT || method == Method.POST)) {
      body = EMPTY_BODY;
    }

    HttpUrl url = buildUrl(method, bucketName, objectName, region, queryParamMap);
    Request request = createRequest(url, method, headerMap, body, length);

    if (this.accessKey != null && this.secretKey != null) {
      request = Signer.signV4(request, region, accessKey, secretKey);
    }

    if (this.traceStream != null) {
      this.traceStream.println("---------START-HTTP---------");
      String encodedPath = request.url().encodedPath();
      String encodedQuery = request.url().encodedQuery();
      if (encodedQuery != null) {
        encodedPath += "?" + encodedQuery;
      }
      this.traceStream.println(request.method() + " " + encodedPath + " HTTP/1.1");
      String headers =
          request
              .headers()
              .toString()
              .replaceAll("Signature=([0-9a-f]+)", "Signature=*REDACTED*")
              .replaceAll("Credential=([^/]+)", "Credential=*REDACTED*");
      this.traceStream.println(headers);
      if (traceRequestBody) {
        this.traceStream.println(new String((byte[]) body, StandardCharsets.UTF_8));
      }
    }

    OkHttpClient httpClient = this.httpClient;
    if (method == Method.PUT || method == Method.POST) {
      // Issue #924: disable connection retry for PUT and POST methods. Its safe to do
      // retry for other methods.
      httpClient = this.httpClient.newBuilder().retryOnConnectionFailure(false).build();
    }

    Response response = httpClient.newCall(request).execute();
    if (this.traceStream != null) {
      this.traceStream.println(
          response.protocol().toString().toUpperCase(Locale.US) + " " + response.code());
      this.traceStream.println(response.headers());
    }

    if (response.isSuccessful()) {
      if (this.traceStream != null) {
        this.traceStream.println(END_HTTP);
      }
      // response.headers().toMultimap();
      return response;
    }

    String errorXml = null;
    try (ResponseBody responseBody = response.body()) {
      errorXml = new String(responseBody.bytes(), StandardCharsets.UTF_8);
    }

    if (this.traceStream != null && !("".equals(errorXml) && method.equals(Method.HEAD))) {
      this.traceStream.println(errorXml);
    }

    // Error in case of Non-XML response from server for non-HEAD requests.
    String contentType = response.headers().get("content-type");
    if (!method.equals(Method.HEAD)
        && (contentType == null
            || !Arrays.asList(contentType.split(";")).contains("application/xml"))) {
      if (this.traceStream != null) {
        this.traceStream.println(END_HTTP);
      }
      throw new InvalidResponseException();
    }

    ErrorResponse errorResponse = null;
    if (!"".equals(errorXml)) {
      errorResponse = Xml.unmarshal(ErrorResponse.class, errorXml);
    } else if (!method.equals(Method.HEAD)) {
      if (this.traceStream != null) {
        this.traceStream.println(END_HTTP);
      }
      throw new InvalidResponseException();
    }

    if (this.traceStream != null) {
      this.traceStream.println(END_HTTP);
    }

    if (errorResponse == null) {
      ErrorCode ec;
      switch (response.code()) {
        case 307:
          ec = ErrorCode.REDIRECT;
          break;
        case 400:
          // HEAD bucket with wrong region gives 400 without body.
          if (method.equals(Method.HEAD)
              && bucketName != null
              && objectName == null
              && isAwsHost
              && AwsRegionCache.INSTANCE.get(bucketName) != null) {
            ec = ErrorCode.RETRY_HEAD_BUCKET;
          } else {
            ec = ErrorCode.INVALID_URI;
          }
          break;
        case 404:
          if (objectName != null) {
            ec = ErrorCode.NO_SUCH_KEY;
          } else if (bucketName != null) {
            ec = ErrorCode.NO_SUCH_BUCKET;
          } else {
            ec = ErrorCode.RESOURCE_NOT_FOUND;
          }
          break;
        case 501:
        case 405:
          ec = ErrorCode.METHOD_NOT_ALLOWED;
          break;
        case 409:
          if (bucketName != null) {
            ec = ErrorCode.NO_SUCH_BUCKET;
          } else {
            ec = ErrorCode.RESOURCE_CONFLICT;
          }
          break;
        case 403:
          ec = ErrorCode.ACCESS_DENIED;
          break;
        default:
          if (response.code() >= 500) {
            throw new ServerException("server failed with HTTP status code " + response.code());
          }

          throw new InternalException(
              "unhandled HTTP code "
                  + response.code()
                  + ".  Please report this issue at "
                  + "https://github.com/minio/minio-java/issues");
      }

      errorResponse =
          new ErrorResponse(
              ec,
              bucketName,
              objectName,
              request.url().encodedPath(),
              response.header("x-amz-request-id"),
              response.header("x-amz-id-2"));
    }

    // invalidate region cache if needed
    if (errorResponse.errorCode() == ErrorCode.NO_SUCH_BUCKET
        || errorResponse.errorCode() == ErrorCode.RETRY_HEAD_BUCKET) {
      if (isAwsHost) {
        AwsRegionCache.INSTANCE.remove(bucketName);
      }

      // TODO: handle for other cases as well
    }

    throw new ErrorResponseException(errorResponse, response);
  }

  private Response execute(
      Method method,
      String bucketName,
      String objectName,
      String region,
      Map<String, String> headerMap,
      Map<String, String> queryParamMap,
      Object body,
      int length)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    Multimap<String, String> headerMultiMap = null;
    if (headerMap != null) {
      headerMultiMap = Multimaps.forMap(normalizeHeaders(headerMap));
    }

    Multimap<String, String> queryParamMultiMap = null;
    if (queryParamMap != null) {
      queryParamMultiMap = Multimaps.forMap(queryParamMap);
    }

    return execute(
        method, bucketName, objectName, region, headerMultiMap, queryParamMultiMap, body, length);
  }

  /** Returns region of given bucket either from region cache or set in constructor. */
  private String getRegion(String bucketName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    if (this.region != null && !this.region.equals("")) {
      return this.region;
    }

    if (!isAwsHost || bucketName == null || this.accessKey == null) {
      return US_EAST_1;
    }

    String region = AwsRegionCache.INSTANCE.get(bucketName);
    if (region != null) {
      return region;
    }

    // Execute GetBucketLocation REST API to get region of the bucket.
    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("location", null);

    Response response =
        execute(Method.GET, bucketName, null, US_EAST_1, null, queryParamMap, null, 0);

    try (ResponseBody body = response.body()) {
      LocationConstraint lc = Xml.unmarshal(LocationConstraint.class, body.charStream());
      if (lc.location() == null || lc.location().equals("")) {
        region = US_EAST_1;
      } else if (lc.location().equals("EU")) {
        region = "eu-west-1"; // eu-west-1 is also referred as 'EU'.
      } else {
        region = lc.location();
      }
    }

    AwsRegionCache.INSTANCE.set(bucketName, region);
    return region;
  }

  private Response executeGet(
      String bucketName,
      String objectName,
      Map<String, String> headerMap,
      Map<String, String> queryParamMap)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return execute(
        Method.GET,
        bucketName,
        objectName,
        getRegion(bucketName),
        headerMap,
        queryParamMap,
        null,
        0);
  }

  private Response executeGet(
      String bucketName, String objectName, Multimap<String, String> queryParamMap)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return execute(
        Method.GET, bucketName, objectName, getRegion(bucketName), null, queryParamMap, null, 0);
  }

  private Response executeHead(
      String bucketName,
      String objectName,
      Multimap<String, String> headers,
      Multimap<String, String> queryParams)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    Response response =
        execute(
            Method.HEAD,
            bucketName,
            objectName,
            getRegion(bucketName),
            headers,
            queryParams,
            null,
            0);
    response.body().close();
    return response;
  }

  private Response executeHead(String bucketName, String objectName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    try {
      return executeHead(bucketName, objectName, null, null);
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode() != ErrorCode.RETRY_HEAD_BUCKET) {
        throw e;
      }
    }

    // Retry once for RETRY_HEAD_BUCKET error.
    return executeHead(bucketName, objectName, null, null);
  }

  private Response executeDelete(
      String bucketName,
      String objectName,
      Multimap<String, String> headers,
      Multimap<String, String> queryParams)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    Response response =
        execute(
            Method.DELETE,
            bucketName,
            objectName,
            getRegion(bucketName),
            headers,
            queryParams,
            null,
            0);
    response.body().close();
    return response;
  }

  private Response executeDelete(
      String bucketName, String objectName, Map<String, String> queryParamMap)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    Response response =
        execute(
            Method.DELETE,
            bucketName,
            objectName,
            getRegion(bucketName),
            null,
            queryParamMap,
            null,
            0);
    response.body().close();
    return response;
  }

  private Response executePost(
      String bucketName,
      String objectName,
      Map<String, String> headerMap,
      Map<String, String> queryParamMap,
      Object data)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return execute(
        Method.POST,
        bucketName,
        objectName,
        getRegion(bucketName),
        headerMap,
        queryParamMap,
        data,
        0);
  }

  private Response executePut(
      String bucketName,
      String objectName,
      String region,
      Map<String, String> headerMap,
      Map<String, String> queryParamMap,
      Object data,
      int length)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return execute(
        Method.PUT, bucketName, objectName, region, headerMap, queryParamMap, data, length);
  }

  private Response executePut(
      String bucketName,
      String objectName,
      Map<String, String> headerMap,
      Map<String, String> queryParamMap,
      Object data,
      int length)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return executePut(
        bucketName, objectName, getRegion(bucketName), headerMap, queryParamMap, data, length);
  }

  /**
   * Gets object information and metadata of an object.
   *
   * <pre>Example:{@code
   * ObjectStat objectStat = minioClient.statObject("my-bucketname", "my-objectname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @return {@link ObjectStat} - Populated object information and metadata.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #statObject(StatObjectArgs)}
   */
  @Deprecated
  public ObjectStat statObject(String bucketName, String objectName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
  }

  /**
   * Gets object information and metadata of a SSE-C encrypted object.
   *
   * <pre>Example:{@code
   * ObjectStat objectStat =
   *     minioClient.statObject("my-bucketname", "my-objectname", ssec);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param ssec SSE-C type server-side encryption.
   * @return {@link ObjectStat} - Populated object information and metadata.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #statObject(StatObjectArgs)}
   */
  @Deprecated
  public ObjectStat statObject(
      String bucketName, String objectName, ServerSideEncryptionCustomerKey ssec)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return statObject(
        StatObjectArgs.builder().bucket(bucketName).object(objectName).ssec(ssec).build());
  }

  /**
   * Gets information of an object.
   *
   * <pre>Example:{@code
   * // Get information of an object.
   * ObjectStat objectStat =
   *     minioClient.statObject(
   *         StatObjectArgs.builder().bucket("my-bucketname").object("my-objectname").build());
   *
   * // Get information of SSE-C encrypted object.
   * ObjectStat objectStat =
   *     minioClient.statObject(
   *         StatObjectArgs.builder()
   *             .bucket("my-bucketname")
   *             .object("my-objectname")
   *             .ssec(ssec)
   *             .build());
   *
   * // Get information of a versioned object.
   * ObjectStat objectStat =
   *     minioClient.statObject(
   *         StatObjectArgs.builder()
   *             .bucket("my-bucketname")
   *             .object("my-objectname")
   *             .versionId("version-id")
   *             .build());
   *
   * // Get information of a SSE-C encrypted versioned object.
   * ObjectStat objectStat =
   *     minioClient.statObject(
   *         StatObjectArgs.builder()
   *             .bucket("my-bucketname")
   *             .object("my-objectname")
   *             .versionId("version-id")
   *             .ssec(ssec)
   *             .build());
   * }</pre>
   *
   * @param args {@link StatObjectArgs} object.
   * @return {@link ObjectStat} - Populated object information and metadata.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @see ObjectStat
   */
  public ObjectStat statObject(StatObjectArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);
    args.validateSsec(baseUrl);

    Multimap<String, String> headers = HashMultimap.create();
    headers.putAll(args.extraHeaders());
    if (args.ssec() != null) {
      headers.putAll(Multimaps.forMap(args.ssec().headers()));
    }

    Multimap<String, String> queryParams = HashMultimap.create();
    queryParams.putAll(args.extraQueryParams());
    if (args.versionId() != null) {
      queryParams.put("versionId", args.versionId());
    }

    try (Response response = executeHead(args.bucket(), args.object(), headers, queryParams)) {
      return new ObjectStat(args.bucket(), args.object(), response.headers());
    }
  }

  /**
   * Gets URL of an object useful when this object has public read access.
   *
   * <pre>Example:{@code
   * String url = minioClient.getObjectUrl("my-bucketname", "my-objectname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @return String - URL string.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public String getObjectUrl(String bucketName, String objectName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkObjectName(objectName);
    HttpUrl url = buildUrl(Method.GET, bucketName, objectName, getRegion(bucketName), null);
    return url.toString();
  }

  /**
   * Gets data of an object. Returned {@link InputStream} must be closed after use to release
   * network resources.
   *
   * <pre>Example:{@code
   * try (InputStream stream =
   *     minioClient.getObject("my-bucketname", "my-objectname")) {
   *   // Read data from stream
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @return {@link InputStream} - Contains object data.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getObject(GetObjectArgs)}
   */
  @Deprecated
  public InputStream getObject(String bucketName, String objectName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
  }

  /**
   * Gets data of a SSE-C encrypted object. Returned {@link InputStream} must be closed after use to
   * release network resources.
   *
   * <pre>Example:{@code
   * try (InputStream stream =
   *     minioClient.getObject("my-bucketname", "my-objectname", ssec)) {
   *   // Read data from stream
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param ssec SSE-C type server-side encryption.
   * @return {@link InputStream} - Contains object data.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getObject(GetObjectArgs)}
   */
  @Deprecated
  public InputStream getObject(
      String bucketName, String objectName, ServerSideEncryptionCustomerKey ssec)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return getObject(
        GetObjectArgs.builder().bucket(bucketName).object(objectName).ssec(ssec).build());
  }

  /**
   * Gets data from offset of an object. Returned {@link InputStream} must be closed after use to
   * release network resources.
   *
   * <pre>Example:{@code
   * try (InputStream stream =
   *     minioClient.getObject("my-bucketname", "my-objectname", 1024L)) {
   *   // Read data from stream
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param offset Start byte position of object data.
   * @return {@link InputStream} - Contains object data.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getObject(GetObjectArgs)}
   */
  @Deprecated
  public InputStream getObject(String bucketName, String objectName, long offset)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return getObject(
        GetObjectArgs.builder().bucket(bucketName).object(objectName).offset(offset).build());
  }

  /**
   * Gets data from offset to length of an object. Returned {@link InputStream} must be closed after
   * use to release network resources.
   *
   * <pre>Example:{@code
   * try (InputStream stream =
   *     minioClient.getObject("my-bucketname", "my-objectname", 1024L, 4096L)) {
   *   // Read data from stream
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param offset Start byte position of object data.
   * @param length Number of bytes of object data from offset.
   * @return {@link InputStream} - Contains object data.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getObject(GetObjectArgs)}
   */
  @Deprecated
  public InputStream getObject(String bucketName, String objectName, long offset, Long length)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return getObject(
        GetObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .offset(offset)
            .length(length)
            .build());
  }

  /**
   * Gets data from offset to length of a SSE-C encrypted object. Returned {@link InputStream} must
   * be closed after use to release network resources.
   *
   * <pre>Example:{@code
   * try (InputStream stream =
   *     minioClient.getObject("my-bucketname", "my-objectname", 1024L, 4096L, ssec)) {
   *   // Read data from stream
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param offset Start byte position of object data.
   * @param length Number of bytes of object data from offset.
   * @param ssec SSE-C type server-side encryption.
   * @return {@link InputStream} - Contains object data.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getObject(GetObjectArgs)}
   */
  @Deprecated
  public InputStream getObject(
      String bucketName,
      String objectName,
      Long offset,
      Long length,
      ServerSideEncryptionCustomerKey ssec)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return getObject(
        GetObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .offset(offset)
            .length(length)
            .ssec(ssec)
            .build());
  }

  /**
   * Gets data from offset to length of a SSE-C encrypted object. Returned {@link InputStream} must
   * be closed after use to release network resources.
   *
   * <pre>Example:{@code
   * try (InputStream stream =
   *     minioClient.getObject(
   *   GetObjectArgs.builder()
   *     .bucket("my-bucketname")
   *     .object("my-objectname")
   *     .offset(offset)
   *     .length(len)
   *     .ssec(ssec)
   *     .build()
   * ) {
   *   // Read data from stream
   * }
   * }</pre>
   *
   * @param args Object of {@link GetObjectArgs}
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public InputStream getObject(GetObjectArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    Long offset = args.offset();
    Long length = args.length();
    ServerSideEncryptionCustomerKey ssec = args.ssec();
    args.validateSsec(this.baseUrl);

    if (length != null && offset == null) {
      offset = 0L;
    }

    Map<String, String> headerMap = null;
    if (offset != null || length != null || ssec != null) {
      headerMap = new HashMap<>();
    }

    if (length != null) {
      headerMap.put("Range", "bytes=" + offset + "-" + (offset + length - 1));
    } else if (offset != null) {
      headerMap.put("Range", "bytes=" + offset + "-");
    }

    if (ssec != null) {
      headerMap.putAll(ssec.headers());
    }

    Response response = executeGet(args.bucket(), args.object(), headerMap, null);
    return response.body().byteStream();
  }

  /**
   * Downloads data of an object to file.
   *
   * <pre>Example:{@code
   * minioClient.getObject("my-bucketname", "my-objectname", "my-object-file");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param fileName Name of the file.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getObject(GetObjectArgs)}
   */
  @Deprecated
  public void getObject(String bucketName, String objectName, String fileName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    downloadObject(
        DownloadObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .filename(fileName)
            .build());
  }

  /**
   * Downloads data of a SSE-C encrypted object to file.
   *
   * <pre>Example:{@code
   * minioClient.getObject("my-bucketname", "my-objectname", ssec, "my-object-file");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param ssec SSE-C type server-side encryption.
   * @param fileName Name of the file.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getObject(GetObjectArgs)}
   */
  @Deprecated
  public void getObject(
      String bucketName, String objectName, ServerSideEncryptionCustomerKey ssec, String fileName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    downloadObject(
        DownloadObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .ssec(ssec)
            .filename(fileName)
            .build());
  }

  /**
   * Downloads data of a SSE-C encrypted object to file.
   *
   * <pre>Example:{@code
   * minioClient.downloadObject(
   *   GetObjectArgs.builder()
   *     .bucket("my-bucketname")
   *     .object("my-objectname")
   *     .ssec(ssec)
   *     .fileName("my-filename")
   *     .build());
   * }</pre>
   *
   * @param args Object of {@link DownloadObjectArgs}
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void downloadObject(DownloadObjectArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    String filename = args.filename();
    Path filePath = Paths.get(filename);
    boolean fileExists = Files.exists(filePath);

    ObjectStat objectStat =
        statObject(
            StatObjectArgs.builder()
                .bucket(args.bucket())
                .object(args.object())
                .ssec(args.ssec())
                .build());
    long length = objectStat.length();
    String etag = objectStat.etag();

    String tempFilename = filename + "." + etag + ".part.minio";
    Path tempFilePath = Paths.get(tempFilename);
    boolean tempFileExists = Files.exists(tempFilePath);

    if (tempFileExists && !Files.isRegularFile(tempFilePath)) {
      throw new IOException(tempFilename + ": not a regular file");
    }

    long tempFileSize = 0;
    if (tempFileExists) {
      tempFileSize = Files.size(tempFilePath);
      if (tempFileSize > length) {
        Files.delete(tempFilePath);
        tempFileExists = false;
        tempFileSize = 0;
      }
    }

    if (fileExists) {
      long fileSize = Files.size(filePath);
      if (fileSize == length) {
        // already downloaded. nothing to do
        return;
      } else if (fileSize > length) {
        throw new IllegalArgumentException(
            "Source object, '"
                + args.object()
                + "', size:"
                + length
                + " is smaller than the destination file, '"
                + filename
                + "', size:"
                + fileSize);
      } else if (!tempFileExists) {
        // before resuming the download, copy filename to tempfilename
        Files.copy(filePath, tempFilePath);
        tempFileSize = fileSize;
        tempFileExists = true;
      }
    }

    InputStream is = null;
    OutputStream os = null;
    try {
      is =
          getObject(
              GetObjectArgs.builder()
                  .bucket(args.bucket())
                  .object(args.object())
                  .ssec(args.ssec())
                  .build());
      os =
          Files.newOutputStream(tempFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      long bytesWritten = ByteStreams.copy(is, os);
      is.close();
      os.close();

      if (bytesWritten != length - tempFileSize) {
        throw new IOException(
            tempFilename
                + ": unexpected data written.  expected = "
                + (length - tempFileSize)
                + ", written = "
                + bytesWritten);
      }
      Files.move(tempFilePath, filePath, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      if (is != null) {
        is.close();
      }
      if (os != null) {
        os.close();
      }
    }
  }

  /**
   * Creates an object by server-side copying data from another object.
   *
   * <pre>Example:{@code
   * // Copy data from my-source-bucketname/my-objectname to my-bucketname/my-objectname.
   * minioClient.copyObject("my-bucketname", "my-objectname", null, null,
   *     "my-source-bucketname", null, null, null);
   *
   * // Copy data from my-source-bucketname/my-source-objectname to
   * // my-bucketname/my-objectname.
   * minioClient.copyObject("my-bucketname", "my-objectname", null, null,
   *     "my-source-bucketname", "my-source-objectname", null, null);
   *
   * // Copy data from my-source-bucketname/my-objectname to my-bucketname/my-objectname
   * // by server-side encryption.
   * minioClient.copyObject("my-bucketname", "my-objectname", null, sse,
   *     "my-source-bucketname", null, null, null);
   *
   * // Copy data from SSE-C encrypted my-source-bucketname/my-objectname to
   * // my-bucketname/my-objectname.
   * minioClient.copyObject("my-bucketname", "my-objectname", null, null,
   *     "my-source-bucketname", null, srcSsec, null);
   *
   * // Copy data from my-source-bucketname/my-objectname to my-bucketname/my-objectname
   * // with user metadata and copy conditions.
   * minioClient.copyObject("my-bucketname", "my-objectname", headers, null,
   *     "my-source-bucketname", null, null, conditions);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name to be created.
   * @param headerMap (Optional) User metadata.
   * @param sse (Optional) Server-side encryption.
   * @param srcBucketName Source bucket name.
   * @param srcObjectName (Optional) Source object name.
   * @param srcSse (Optional) SSE-C type server-side encryption of source object.
   * @param copyConditions (Optional) Conditiions to be used in copy operation.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #copyObject(CopyObjectArgs)}
   */
  @Deprecated
  public void copyObject(
      String bucketName,
      String objectName,
      Map<String, String> headerMap,
      ServerSideEncryption sse,
      String srcBucketName,
      String srcObjectName,
      ServerSideEncryption srcSse,
      CopyConditions copyConditions)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    ServerSideEncryptionCustomerKey srcSsec = null;
    if (srcSse instanceof ServerSideEncryptionCustomerKey) {
      srcSsec = (ServerSideEncryptionCustomerKey) srcSse;
    }
    checkReadRequestSse(srcSse);

    CopyObjectArgs.Builder builder =
        CopyObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .headers(headerMap)
            .sse(sse)
            .srcBucket(srcBucketName)
            .srcObject(srcObjectName)
            .srcSsec(srcSsec);

    if (copyConditions != null) {
      Map<String, String> map = copyConditions.getConditions();
      String value;

      builder.srcMatchETag(map.get("x-amz-copy-source-if-match"));
      builder.srcNotMatchETag(map.get("x-amz-copy-source-if-none-match"));

      value = map.get("x-amz-copy-source-if-modified-since");
      if (value != null) {
        builder.srcModifiedSince(ZonedDateTime.parse(value, Time.HTTP_HEADER_DATE_FORMAT));
      }

      value = map.get("x-amz-copy-source-if-unmodified-since");
      if (value != null) {
        builder.srcUnmodifiedSince(ZonedDateTime.parse(value, Time.HTTP_HEADER_DATE_FORMAT));
      }

      value = map.get("x-amz-metadata-directive");
      if (value != null) {
        builder.metadataDirective(Directive.valueOf(value));
      }
    }

    copyObject(builder.build());
  }

  /**
   * Creates an object by server-side copying data from another object.
   *
   * <pre>Example:{@code
   * // Create object "my-objectname" in bucket "my-bucketname" by copying from object
   * // "my-objectname" in bucket "my-source-bucketname".
   * minioClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .srcBucket("my-source-bucketname")
   *         .build());
   *
   * // Create object "my-objectname" in bucket "my-bucketname" by copying from object
   * // "my-source-objectname" in bucket "my-source-bucketname".
   * minioClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .srcBucket("my-source-bucketname")
   *         .srcObject("my-source-objectname")
   *         .build());
   *
   * // Create object "my-objectname" in bucket "my-bucketname" with server-side encryption by
   * // copying from object "my-objectname" in bucket "my-source-bucketname".
   * minioClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .srcBucket("my-source-bucketname")
   *         .sse(sse)
   *         .build());
   *
   * // Create object "my-objectname" in bucket "my-bucketname" by copying from SSE-C encrypted
   * // object "my-source-objectname" in bucket "my-source-bucketname".
   * minioClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .srcBucket("my-source-bucketname")
   *         .srcObject("my-source-objectname")
   *         .srcSsec(ssec)
   *         .build());
   *
   * // Create object "my-objectname" in bucket "my-bucketname" with custom headers by copying from
   * // object "my-objectname" in bucket "my-source-bucketname" using conditions.
   * minioClient.copyObject(
   *     CopyObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .srcBucket("my-source-bucketname")
   *         .headers(headers)
   *         .srcMatchETag(etag)
   *         .build());
   * }</pre>
   *
   * @param args {@link CopyObjectArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void copyObject(CopyObjectArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);
    args.validateSse(this.baseUrl);

    Multimap<String, String> headers = args.genHeaders();

    // Source object name is optional, if empty default to object name.
    String srcObject = Optional.ofNullable(args.srcObject()).orElse(args.object());

    String copySource = S3Escaper.encodePath("/" + args.srcBucket() + "/" + srcObject);
    if (args.srcVersionId() != null) {
      copySource += "?versionId=" + S3Escaper.encode(args.srcVersionId());
    }

    headers.put("x-amz-copy-source", copySource);

    if (args.srcSsec() != null) {
      headers.putAll(Multimaps.forMap(args.srcSsec().copySourceHeaders()));
    }

    if (args.srcMatchETag() != null) {
      headers.put("x-amz-copy-source-if-match", args.srcMatchETag());
    }

    if (args.srcNotMatchETag() != null) {
      headers.put("x-amz-copy-source-if-none-match", args.srcNotMatchETag());
    }

    if (args.srcModifiedSince() != null) {
      headers.put(
          "x-amz-copy-source-if-modified-since",
          args.srcModifiedSince().format(Time.HTTP_HEADER_DATE_FORMAT));
    }

    if (args.srcUnmodifiedSince() != null) {
      headers.put(
          "x-amz-copy-source-if-unmodified-since",
          args.srcUnmodifiedSince().format(Time.HTTP_HEADER_DATE_FORMAT));
    }

    if (args.metadataDirective() != null) {
      headers.put("x-amz-metadata-directive", args.metadataDirective().name());
    }

    if (args.taggingDirective() != null) {
      headers.put("x-amz-tagging-directive", args.taggingDirective().name());
    }

    try (Response response =
        execute(
            Method.PUT,
            args.bucket(),
            args.object(),
            getRegion(args.bucket()),
            headers,
            null,
            null,
            0)) {
      // For now ignore the copyObjectResult, just read and parse it.
      Xml.unmarshal(CopyObjectResult.class, response.body().charStream());
    }
  }

  /**
   * Creates an object by combining data from different source objects using server-side copy.
   *
   * <pre>Example:{@code
   * List<ComposeSource> sourceObjectList = new ArrayList<ComposeSource>();
   * sourceObjectList.add(new ComposeSource("my-job-bucket", "my-objectname-part-one"));
   * sourceObjectList.add(new ComposeSource("my-job-bucket", "my-objectname-part-two"));
   * sourceObjectList.add(new ComposeSource("my-job-bucket", "my-objectname-part-three"));
   *
   * // Create my-bucketname/my-objectname by combining source object list.
   * minioClient.composeObject("my-bucketname", "my-objectname", sourceObjectList,
   *     null, null);
   *
   * // Create my-bucketname/my-objectname with user metadata by combining source object
   * // list.
   * minioClient.composeObject("my-bucketname", "my-objectname", sourceObjectList,
   *     userMetadata, null);
   *
   * // Create my-bucketname/my-objectname with user metadata and server-side encryption
   * // by combining source object list.
   * minioClient.composeObject("my-bucketname", "my-objectname", sourceObjectList,
   *     userMetadata, sse);
   * }</pre>
   *
   * @param bucketName Destination Bucket to be created upon compose.
   * @param objectName Destination Object to be created upon compose.
   * @param sources List of Source Objects used to compose Object.
   * @param headerMap User Meta data.
   * @param sse Server Side Encryption.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void composeObject(
      String bucketName,
      String objectName,
      List<ComposeSource> sources,
      Map<String, String> headerMap,
      ServerSideEncryption sse)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    if ((bucketName == null) || (bucketName.isEmpty())) {
      throw new IllegalArgumentException("bucket name cannot be empty");
    }

    checkObjectName(objectName);

    if (sources.isEmpty()) {
      throw new IllegalArgumentException("compose sources cannot be empty");
    }

    checkWriteRequestSse(sse);

    long objectSize = 0;
    int partsCount = 0;
    for (int i = 0; i < sources.size(); i++) {
      ComposeSource src = sources.get(i);

      checkReadRequestSse(src.ssec());

      ObjectStat stat = statObject(src.bucketName(), src.objectName(), src.ssec());
      src.buildHeaders(stat.length(), stat.etag());

      if (i != 0 && src.headers().containsKey("x-amz-meta-x-amz-key")) {
        throw new IllegalArgumentException(
            "Client side encryption is not supported for more than one source");
      }

      long size = stat.length();
      if (src.length() != null) {
        size = src.length();
      } else if (src.offset() != null) {
        size -= src.offset();
      }

      if (size < ObjectWriteArgs.MIN_MULTIPART_SIZE
          && sources.size() != 1
          && i != (sources.size() - 1)) {
        throw new IllegalArgumentException(
            "source "
                + src.bucketName()
                + "/"
                + src.objectName()
                + ": size "
                + size
                + " must be greater than "
                + ObjectWriteArgs.MIN_MULTIPART_SIZE);
      }

      objectSize += size;
      if (objectSize > ObjectWriteArgs.MAX_OBJECT_SIZE) {
        throw new IllegalArgumentException(
            "Destination object size must be less than " + ObjectWriteArgs.MAX_OBJECT_SIZE);
      }

      if (size > ObjectWriteArgs.MAX_PART_SIZE) {
        long count = size / ObjectWriteArgs.MAX_PART_SIZE;
        long lastPartSize = size - (count * ObjectWriteArgs.MAX_PART_SIZE);
        if (lastPartSize > 0) {
          count++;
        } else {
          lastPartSize = ObjectWriteArgs.MAX_PART_SIZE;
        }

        if (lastPartSize < ObjectWriteArgs.MIN_MULTIPART_SIZE
            && sources.size() != 1
            && i != (sources.size() - 1)) {
          throw new IllegalArgumentException(
              "source "
                  + src.bucketName()
                  + "/"
                  + src.objectName()
                  + ": "
                  + "for multipart split upload of "
                  + size
                  + ", last part size is less than "
                  + ObjectWriteArgs.MIN_MULTIPART_SIZE);
        }

        partsCount += (int) count;
      } else {
        partsCount++;
      }

      if (partsCount > ObjectWriteArgs.MAX_MULTIPART_COUNT) {
        throw new IllegalArgumentException(
            "Compose sources create more than allowed multipart count "
                + ObjectWriteArgs.MAX_MULTIPART_COUNT);
      }
    }

    if (partsCount == 1) {
      ComposeSource src = sources.get(0);
      if (headerMap == null) {
        headerMap = new HashMap<>();
      }
      if ((src.offset() != null) && (src.length() == null)) {
        headerMap.put("x-amz-copy-source-range", "bytes=" + src.offset() + "-");
      }

      if ((src.offset() != null) && (src.length() != null)) {
        headerMap.put(
            "x-amz-copy-source-range",
            "bytes=" + src.offset() + "-" + (src.offset() + src.length() - 1));
      }
      copyObject(
          bucketName,
          objectName,
          headerMap,
          sse,
          src.bucketName(),
          src.objectName(),
          src.ssec(),
          src.copyConditions());
      return;
    }

    Map<String, String> sseHeaders = null;
    if (sse != null) {
      sseHeaders = sse.headers();
      if (headerMap == null) {
        headerMap = new HashMap<>();
      }
      headerMap.putAll(sseHeaders);
    }

    String uploadId = createMultipartUpload(bucketName, objectName, headerMap);

    int partNumber = 0;
    Part[] totalParts = new Part[partsCount];
    try {
      for (int i = 0; i < sources.size(); i++) {
        ComposeSource src = sources.get(i);

        long size = src.objectSize();
        if (src.length() != null) {
          size = src.length();
        } else if (src.offset() != null) {
          size -= src.offset();
        }
        long offset = 0;
        if (src.offset() != null) {
          offset = src.offset();
        }

        if (size <= ObjectWriteArgs.MAX_PART_SIZE) {
          partNumber++;
          Map<String, String> headers = new HashMap<>();
          if (src.headers() != null) {
            headers.putAll(src.headers());
          }
          if (src.length() != null) {
            headers.put(
                "x-amz-copy-source-range", "bytes=" + offset + "-" + (offset + src.length() - 1));
          } else if (src.offset() != null) {
            headers.put("x-amz-copy-source-range", "bytes=" + offset + "-" + (offset + size - 1));
          }
          if (sseHeaders != null) {
            headers.putAll(sseHeaders);
          }
          String eTag = uploadPartCopy(bucketName, objectName, uploadId, partNumber, headers);

          totalParts[partNumber - 1] = new Part(partNumber, eTag);
          continue;
        }

        while (size > 0) {
          partNumber++;

          long startBytes = offset;
          long endBytes = startBytes + ObjectWriteArgs.MAX_PART_SIZE;
          if (size < ObjectWriteArgs.MAX_PART_SIZE) {
            endBytes = startBytes + size;
          }

          Map<String, String> headers = src.headers();
          headers.put("x-amz-copy-source-range", "bytes=" + startBytes + "-" + endBytes);
          if (sseHeaders != null) {
            headers.putAll(sseHeaders);
          }
          String eTag = uploadPartCopy(bucketName, objectName, uploadId, partNumber, headers);

          totalParts[partNumber - 1] = new Part(partNumber, eTag);

          offset = startBytes;
          size -= (endBytes - startBytes);
        }
      }

      completeMultipartUpload(bucketName, objectName, uploadId, totalParts);
    } catch (RuntimeException e) {
      abortMultipartUpload(bucketName, objectName, uploadId);
      throw e;
    } catch (Exception e) {
      abortMultipartUpload(bucketName, objectName, uploadId);
      throw e;
    }
  }

  /**
   * Gets presigned URL of an object for HTTP method, expiry time and custom request parameters.
   *
   * <pre>Example:{@code
   * String url = minioClient.getPresignedObjectUrl(Method.DELETE, "my-bucketname",
   *     "my-objectname", 24 * 60 * 60, reqParams);
   * }</pre>
   *
   * @param method HTTP {@link Method} to generate presigned URL.
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param expires Expiry in seconds; defaults to 7 days.
   * @param reqParams Request parameters to override. Supported headers are response-expires,
   *     response-content-type, response-cache-control and response-content-disposition.
   * @return String - URL string.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidExpiresRangeException thrown to indicate invalid expiry duration passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getPresignedObjectUrl(GetPresignedObjectUrlArgs)}
   */
  @Deprecated
  public String getPresignedObjectUrl(
      Method method,
      String bucketName,
      String objectName,
      Integer expires,
      Map<String, String> reqParams)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidExpiresRangeException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          ServerException, XmlParserException {

    return getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .method(method)
            .expiry(expires)
            .extraQueryParams(reqParams)
            .build());
  }

  /**
   * Gets presigned URL of an object for HTTP method, expiry time and custom request parameters.
   *
   * <pre>Example:{@code
   * // Get presigned URL string to delete 'my-objectname' in 'my-bucketname' and its life time
   * // is one day.
   * String url =
   *    minioClient.getPresignedObjectUrl(
   *        GetPresignedObjectUrlArgs.builder()
   *            .method(Method.DELETE)
   *            .bucket("my-bucketname")
   *            .object("my-objectname")
   *            .expiry(24 * 60 * 60)
   *            .build());
   * System.out.println(url);
   *
   * // Get presigned URL string to upload 'my-objectname' in 'my-bucketname'
   * // with response-content-type as application/json and life time as one day.
   * Map<String, String> reqParams = new HashMap<String, String>();
   * reqParams.put("response-content-type", "application/json");
   *
   * String url =
   *    minioClient.getPresignedObjectUrl(
   *        GetPresignedObjectUrlArgs.builder()
   *            .method(Method.PUT)
   *            .bucket("my-bucketname")
   *            .object("my-objectname")
   *            .expiry(1, TimeUnit.DAYS)
   *            .extraQueryParams(reqParams)
   *            .build());
   * System.out.println(url);
   *
   * // Get presigned URL string to download 'my-objectname' in 'my-bucketname' and its life time
   * // is 2 hours.
   * String url =
   *    minioClient.getPresignedObjectUrl(
   *        GetPresignedObjectUrlArgs.builder()
   *            .method(Method.GET)
   *            .bucket("my-bucketname")
   *            .object("my-objectname")
   *            .expiry(2, TimeUnit.HOURS)
   *            .build());
   * System.out.println(url);
   * }</pre>
   *
   * @param args {@link GetPresignedObjectUrlArgs} object.
   * @return String - URL string.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidExpiresRangeException thrown to indicate invalid expiry duration passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @throws ServerException
   */
  public String getPresignedObjectUrl(GetPresignedObjectUrlArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidExpiresRangeException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          XmlParserException, ServerException {
    checkArgs(args);

    byte[] body = null;
    if (args.method() == Method.PUT || args.method() == Method.POST) {
      body = EMPTY_BODY;
    }

    String region = getRegion(args.bucket());
    HttpUrl url =
        buildUrl(args.method(), args.bucket(), args.object(), region, args.extraQueryParams());
    Request request = createRequest(url, args.method(), null, body, 0);
    url = Signer.presignV4(request, region, accessKey, secretKey, args.expiry());
    return url.toString();
  }

  /**
   * Gets presigned URL of an object to download its data for expiry time and request parameters.
   *
   * <pre>Example:{@code
   * // Get presigned URL to download my-objectname data with one day expiry and request
   * // parameters.
   * String url = minioClient.presignedGetObject("my-bucketname", "my-objectname",
   *     24 * 60 * 60, reqParams);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param expires Expiry in seconds; defaults to 7 days.
   * @param reqParams Request parameters to override. Supported headers are response-expires,
   *     response-content-type, response-cache-control and response-content-disposition.
   * @return String - URL string to download the object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidExpiresRangeException thrown to indicate invalid expiry duration passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getPresignedObjectUrl(GetPresignedObjectUrlArgs)}
   */
  @Deprecated
  public String presignedGetObject(
      String bucketName, String objectName, Integer expires, Map<String, String> reqParams)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidExpiresRangeException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          ServerException, XmlParserException {

    return getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket(bucketName)
            .object(objectName)
            .expiry(expires)
            .extraQueryParams(reqParams)
            .build());
  }

  /**
   * Gets presigned URL of an object to download its data for expiry time.
   *
   * <pre>Example:{@code
   * // Get presigned URL to download my-objectname data with one day expiry.
   * String url = minioClient.presignedGetObject("my-bucketname", "my-objectname",
   *     24 * 60 * 60);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param expires Expiry in seconds; defaults to 7 days.
   * @return String - URL string to download the object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidExpiresRangeException thrown to indicate invalid expiry duration passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getPresignedObjectUrl(GetPresignedObjectUrlArgs)}
   */
  @Deprecated
  public String presignedGetObject(String bucketName, String objectName, Integer expires)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidExpiresRangeException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          ServerException, XmlParserException {
    return getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket(bucketName)
            .object(objectName)
            .expiry(expires)
            .build());
  }

  /**
   * Gets presigned URL of an object to download its data for 7 days.
   *
   * <pre>Example:{@code
   * String url = minioClient.presignedGetObject("my-bucketname", "my-objectname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @return String - URL string to download the object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidExpiresRangeException thrown to indicate invalid expiry duration passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getPresignedObjectUrl(GetPresignedObjectUrlArgs)}
   */
  @Deprecated
  public String presignedGetObject(String bucketName, String objectName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidExpiresRangeException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          ServerException, XmlParserException {

    return getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket(bucketName)
            .object(objectName)
            .build());
  }

  /**
   * Gets presigned URL of an object to upload data for expiry time.
   *
   * <pre>Example:{@code
   * // Get presigned URL to upload data to my-objectname with one day expiry.
   * String url =
   *     minioClient.presignedPutObject("my-bucketname", "my-objectname", 24 * 60 * 60);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param expires Expiry in seconds; defaults to 7 days.
   * @return String - URL string to upload an object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidExpiresRangeException thrown to indicate invalid expiry duration passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getPresignedObjectUrl(GetPresignedObjectUrlArgs)}
   */
  @Deprecated
  public String presignedPutObject(String bucketName, String objectName, Integer expires)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidExpiresRangeException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          ServerException, XmlParserException {

    return getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.PUT)
            .bucket(bucketName)
            .object(objectName)
            .expiry(expires)
            .build());
  }

  /**
   * Gets presigned URL of an object to upload data for 7 days.
   *
   * <pre>Example:{@code
   * String url = minioClient.presignedPutObject("my-bucketname", "my-objectname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @return String - URL string to upload an object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidExpiresRangeException thrown to indicate invalid expiry duration passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getPresignedObjectUrl(GetPresignedObjectUrlArgs)}
   */
  @Deprecated
  public String presignedPutObject(String bucketName, String objectName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidExpiresRangeException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          ServerException, XmlParserException {

    return getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.PUT)
            .bucket(bucketName)
            .object(objectName)
            .build());
  }

  /**
   * Gets form-data of {@link PostPolicy} of an object to upload its data using POST method.
   *
   * <pre>Example:{@code
   * PostPolicy policy = new PostPolicy("my-bucketname", "my-objectname",
   *     ZonedDateTime.now().plusDays(7));
   *
   * // 'my-objectname' should be 'image/png' content type
   * policy.setContentType("image/png");
   *
   * // set success action status to 201 to receive XML document
   * policy.setSuccessActionStatus(201);
   *
   * Map<String,String> formData = minioClient.presignedPostPolicy(policy);
   *
   * // Print curl command to be executed by anonymous user to upload /tmp/userpic.png.
   * System.out.print("curl -X POST ");
   * for (Map.Entry<String,String> entry : formData.entrySet()) {
   *   System.out.print(" -F " + entry.getKey() + "=" + entry.getValue());
   * }
   * System.out.println(" -F file=@/tmp/userpic.png https://play.min.io/my-bucketname");
   * }</pre>
   *
   * @param policy Post policy of an object.
   * @return Map&ltString, String&gt - Contains form-data to upload an object using POST method.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidExpiresRangeException thrown to indicate invalid expiry duration passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @see PostPolicy
   */
  public Map<String, String> presignedPostPolicy(PostPolicy policy)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidExpiresRangeException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          ServerException, XmlParserException {
    return policy.formData(this.accessKey, this.secretKey, getRegion(policy.bucketName()));
  }

  /**
   * Removes an object.
   *
   * <pre>Example:{@code
   * minioClient.removeObject("my-bucketname", "my-objectname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #removeObject(RemoveObjectArgs)}
   */
  @Deprecated
  public void removeObject(String bucketName, String objectName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
  }

  /**
   * Removes an object.
   *
   * <pre>Example:{@code
   * // Remove object.
   * minioClient.removeObject(
   *     RemoveObjectArgs.builder().bucket("my-bucketname").object("my-objectname").build());
   *
   * // Remove versioned object.
   * minioClient.removeObject(
   *     RemoveObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-versioned-objectname")
   *         .versionId("my-versionid")
   *         .build());
   *
   * // Remove versioned object bypassing Governance mode.
   * minioClient.removeObject(
   *     RemoveObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-versioned-objectname")
   *         .versionId("my-versionid")
   *         .bypassRetentionMode(true)
   *         .build());
   * }</pre>
   *
   * @param args {@link RemoveObjectArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void removeObject(RemoveObjectArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Multimap<String, String> headers = HashMultimap.create();
    headers.putAll(args.extraHeaders());
    if (args.bypassGovernanceMode()) {
      headers.put("x-amz-bypass-governance-retention", "true");
    }

    Multimap<String, String> queryParams = HashMultimap.create();
    queryParams.putAll(args.extraQueryParams());
    if (args.versionId() != null) {
      queryParams.put("versionId", args.versionId());
    }

    executeDelete(args.bucket(), args.object(), headers, queryParams);
  }

  /**
   * Removes multiple objects lazily. Its required to iterate the returned Iterable to perform
   * removal.
   *
   * <pre>Example:{@code
   * List<String> myObjectNames = new LinkedList<String>();
   * objectNames.add("my-objectname1");
   * objectNames.add("my-objectname2");
   * objectNames.add("my-objectname3");
   * Iterable<Result<DeleteError>> results =
   *     minioClient.removeObjects("my-bucketname", myObjectNames);
   * for (Result<DeleteError> result : results) {
   *   DeleteError error = errorResult.get();
   *   System.out.println(
   *       "Error in deleting object " + error.objectName() + "; " + error.message());
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectNames List of Object names in the bucket.
   * @return Iterable&ltResult&ltDeleteError&gt&gt - Lazy iterator contains object removal status.
   * @deprecated use {@link #removeObjects(RemoveObjectsArgs)}
   */
  @Deprecated
  public Iterable<Result<DeleteError>> removeObjects(
      final String bucketName, final Iterable<String> objectNames) {
    Stream<DeleteObject> stream =
        StreamSupport.stream(objectNames.spliterator(), false)
            .map(
                name -> {
                  return new DeleteObject(name);
                });
    return removeObjects(
        RemoveObjectsArgs.builder().bucket(bucketName).objects(stream::iterator).build());
  }

  /**
   * Removes multiple objects lazily. Its required to iterate the returned Iterable to perform
   * removal.
   *
   * <pre>Example:{@code
   * List<DeleteObject> objects = new LinkedList<>();
   * objects.add(new DeleteObject("my-objectname1"));
   * objects.add(new DeleteObject("my-objectname2"));
   * objects.add(new DeleteObject("my-objectname3"));
   * Iterable<Result<DeleteError>> results =
   *     minioClient.removeObjects(
   *         RemoveObjectsArgs.builder().bucket("my-bucketname").objects(objects).build());
   * for (Result<DeleteError> result : results) {
   *   DeleteError error = errorResult.get();
   *   System.out.println(
   *       "Error in deleting object " + error.objectName() + "; " + error.message());
   * }
   * }</pre>
   *
   * @param args {@link RemoveObjectsArgs} object.
   * @return Iterable&ltResult&ltDeleteError&gt&gt - Lazy iterator contains object removal status.
   */
  public Iterable<Result<DeleteError>> removeObjects(RemoveObjectsArgs args) {
    checkArgs(args);

    return new Iterable<Result<DeleteError>>() {
      @Override
      public Iterator<Result<DeleteError>> iterator() {
        return new Iterator<Result<DeleteError>>() {
          private Result<DeleteError> error;
          private Iterator<DeleteError> errorIterator;
          private boolean completed = false;
          private Iterator<DeleteObject> objectIter = args.objects().iterator();

          private synchronized void populate() {
            List<DeleteError> errorList = null;
            try {
              List<DeleteObject> objectList = new LinkedList<>();
              int i = 0;
              while (objectIter.hasNext() && i < 1000) {
                objectList.add(objectIter.next());
                i++;
              }

              if (objectList.size() > 0) {
                DeleteResult result =
                    deleteObjects(
                        args.bucket(), objectList, args.quiet(), args.bypassGovernanceMode());
                errorList = result.errorList();
              }
            } catch (ErrorResponseException
                | IllegalArgumentException
                | InsufficientDataException
                | InternalException
                | InvalidBucketNameException
                | InvalidKeyException
                | InvalidResponseException
                | IOException
                | NoSuchAlgorithmException
                | ServerException
                | XmlParserException e) {
              this.error = new Result<>(e);
            } finally {
              if (errorList != null) {
                this.errorIterator = errorList.iterator();
              } else {
                this.errorIterator = new LinkedList<DeleteError>().iterator();
              }
            }
          }

          @Override
          public boolean hasNext() {
            if (this.completed) {
              return false;
            }

            if (this.error == null && this.errorIterator == null) {
              populate();
            }

            if (this.error == null && this.errorIterator != null && !this.errorIterator.hasNext()) {
              populate();
            }

            if (this.error != null) {
              return true;
            }

            if (this.errorIterator.hasNext()) {
              return true;
            }

            this.completed = true;
            return false;
          }

          @Override
          public Result<DeleteError> next() {
            if (this.completed) {
              throw new NoSuchElementException();
            }

            if (this.error == null && this.errorIterator == null) {
              populate();
            }

            if (this.error == null && this.errorIterator != null && !this.errorIterator.hasNext()) {
              populate();
            }

            if (this.error != null) {
              this.completed = true;
              return this.error;
            }

            if (this.errorIterator.hasNext()) {
              return new Result<>(this.errorIterator.next());
            }

            this.completed = true;
            throw new NoSuchElementException();
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  /**
   * Lists object information of a bucket.
   *
   * <pre>Example:{@code
   * Iterable<Result<Item>> results = minioClient.listObjects("my-bucketname");
   * for (Result<Item> result : results) {
   *   Item item = result.get();
   *   System.out.println(
   *       item.lastModified() + ", " + item.size() + ", " + item.objectName());
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @return Iterable&ltResult&ltItem&gt&gt - Lazy iterator contains object information.
   * @throws XmlParserException upon parsing response xml
   * @deprecated use {@link #listObjects(ListObjectsArgs)}
   */
  @Deprecated
  public Iterable<Result<Item>> listObjects(final String bucketName) throws XmlParserException {
    return listObjects(bucketName, null);
  }

  /**
   * Lists object information of a bucket for prefix.
   *
   * <pre>Example:{@code
   * Iterable<Result<Item>> results = minioClient.listObjects("my-bucketname", "my-obj");
   * for (Result<Item> result : results) {
   *   Item item = result.get();
   *   System.out.println(
   *       item.lastModified() + ", " + item.size() + ", " + item.objectName());
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param prefix Object name starts with prefix.
   * @return Iterable&ltResult&ltItem&gt&gt - Lazy iterator contains object information.
   * @throws XmlParserException upon parsing response xml
   * @deprecated use {@link #listObjects(ListObjectsArgs)}
   */
  @Deprecated
  public Iterable<Result<Item>> listObjects(final String bucketName, final String prefix)
      throws XmlParserException {
    // list all objects recursively
    return listObjects(bucketName, prefix, true);
  }

  /**
   * Lists object information of a bucket for prefix recursively.
   *
   * <pre>Example:{@code
   * Iterable<Result<Item>> results =
   *     minioClient.listObjects("my-bucketname", "my-obj", true);
   * for (Result<Item> result : results) {
   *   Item item = result.get();
   *   System.out.println(
   *       item.lastModified() + ", " + item.size() + ", " + item.objectName());
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param prefix Object name starts with prefix.
   * @param recursive List recursively than directory structure emulation.
   * @return Iterable&ltResult&ltItem&gt&gt - Lazy iterator contains object information.
   * @see #listObjects(String bucketName)
   * @see #listObjects(String bucketName, String prefix)
   * @see #listObjects(String bucketName, String prefix, boolean recursive, boolean useVersion1)
   * @deprecated use {@link #listObjects(ListObjectsArgs)}
   */
  @Deprecated
  public Iterable<Result<Item>> listObjects(
      final String bucketName, final String prefix, final boolean recursive) {
    return listObjects(bucketName, prefix, recursive, false);
  }

  /**
   * Lists object information of a bucket for prefix recursively using S3 API version 1.
   *
   * <pre>Example:{@code
   * Iterable<Result<Item>> results =
   *     minioClient.listObjects("my-bucketname", "my-obj", true, true);
   * for (Result<Item> result : results) {
   *   Item item = result.get();
   *   System.out.println(
   *       item.lastModified() + ", " + item.size() + ", " + item.objectName());
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param prefix Object name starts with prefix.
   * @param recursive List recursively than directory structure emulation.
   * @param useVersion1 when true, version 1 of REST API is used.
   * @return Iterable&ltResult&ltItem&gt&gt - Lazy iterator contains object information.
   * @see #listObjects(String bucketName)
   * @see #listObjects(String bucketName, String prefix)
   * @see #listObjects(String bucketName, String prefix, boolean recursive)
   * @deprecated use {@link #listObjects(ListObjectsArgs)}
   */
  @Deprecated
  public Iterable<Result<Item>> listObjects(
      final String bucketName,
      final String prefix,
      final boolean recursive,
      final boolean useVersion1) {
    return listObjects(bucketName, prefix, recursive, false, false);
  }

  /**
   * Lists object information with user metadata of a bucket for prefix recursively using S3 API
   * version 2.
   *
   * <pre>Example:{@code
   * Iterable<Result<Item>> results =
   *     minioClient.listObjects("my-bucketname", "my-obj", true, true, false);
   * for (Result<Item> result : results) {
   *   Item item = result.get();
   *   System.out.println(
   *       item.lastModified() + ", " + item.size() + ", " + item.objectName());
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param prefix Object name starts with prefix.
   * @param recursive List recursively than directory structure emulation.
   * @param includeUserMetadata include user metadata of each object. This is MinIO specific
   *     extension to ListObjectsV2.
   * @param useVersion1 when true, version 1 of REST API is used.
   * @return Iterable&ltResult&ltItem&gt&gt - Lazy iterator contains object information.
   * @see #listObjects(String bucketName)
   * @see #listObjects(String bucketName, String prefix)
   * @see #listObjects(String bucketName, String prefix, boolean recursive)
   * @deprecated use {@link #listObjects(ListObjectsArgs)}
   */
  @Deprecated
  public Iterable<Result<Item>> listObjects(
      final String bucketName,
      final String prefix,
      final boolean recursive,
      final boolean includeUserMetadata,
      final boolean useVersion1) {
    return listObjects(
        ListObjectsArgs.builder()
            .bucket(bucketName)
            .prefix(prefix)
            .recursive(recursive)
            .includeUserMetadata(includeUserMetadata)
            .build());
  }

  /**
   * Lists objects information of a bucket. Supports both the versions 1 and 2 of the S3 API. By
   * default, the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html">
   * version 2</a> API is used. <br>
   * <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html">version 1</a>
   * can be used by passing the optional argument {@code useVersion1} as {@code true}.
   *
   * <pre>Example:
   * {@code
   *   Iterable<Result<Item>> results = minioClient.listObjects(
   *     ListObjectsArgs.builder()
   *       .bucket("my-bucketname")
   *       .includeUserMetadata(true)
   *       .startAfter("start-after-entry")
   *       .prefix("my-obj")
   *       .maxKeys(100)
   *       .fetchOwner(true)
   *   );
   *   for (Result<Item> result : results) {
   *     Item item = result.get();
   *     System.out.println(
   *       item.lastModified() + ", " + item.size() + ", " + item.objectName());
   *   }
   * }</pre>
   *
   * @param args Instance of {@link ListObjectsArgs} built using the builder
   * @return Iterable&lt;Result&lt;Item&gt;&gt; - Lazy iterator contains object information.
   * @throws XmlParserException upon parsing response xml
   */
  public Iterable<Result<Item>> listObjects(ListObjectsArgs args) {
    if (args.useVersion1()) {
      if (args.includeUserMetadata()) {
        throw new IllegalArgumentException(
            "include user metadata flag is not supported in version 1");
      }
      return listObjectsV1(args);
    } else {
      return listObjectsV2(args);
    }
  }

  private abstract class ObjectIterator implements Iterator<Result<Item>> {
    protected Result<Item> error;
    protected Iterator<Item> itemIterator;
    protected Iterator<Prefix> prefixIterator;
    protected boolean completed = false;
    protected ListBucketResult listBucketResult;
    protected String lastObjectName;

    protected String getDelimiter(ListObjectsArgs args) {
      if (args.recursive()) {
        return null;
      }
      String delimiter = args.delimiter();
      if (delimiter == null) {
        return "/";
      }
      return delimiter;
    }

    protected abstract void populateResult()
        throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
            InternalException, InvalidBucketNameException, InvalidKeyException,
            InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
            XmlParserException;

    protected synchronized void populate() {
      try {
        populateResult();
      } catch (ErrorResponseException
          | IllegalArgumentException
          | InsufficientDataException
          | InternalException
          | InvalidBucketNameException
          | InvalidKeyException
          | InvalidResponseException
          | IOException
          | NoSuchAlgorithmException
          | ServerException
          | XmlParserException e) {
        this.error = new Result<>(e);
      } finally {
        if (this.listBucketResult != null) {
          this.itemIterator = this.listBucketResult.contents().iterator();
          this.prefixIterator = this.listBucketResult.commonPrefixes().iterator();
        } else {
          this.itemIterator = new LinkedList<Item>().iterator();
          this.prefixIterator = new LinkedList<Prefix>().iterator();
        }
      }
    }

    @Override
    public boolean hasNext() {
      if (this.completed) {
        return false;
      }

      if (this.error == null && this.itemIterator == null && this.prefixIterator == null) {
        populate();
      }

      if (this.error == null
          && !this.itemIterator.hasNext()
          && !this.prefixIterator.hasNext()
          && this.listBucketResult.isTruncated()) {
        populate();
      }

      if (this.error != null) {
        return true;
      }

      if (this.itemIterator.hasNext()) {
        return true;
      }

      if (this.prefixIterator.hasNext()) {
        return true;
      }

      this.completed = true;
      return false;
    }

    @Override
    public Result<Item> next() {
      if (this.completed) {
        throw new NoSuchElementException();
      }

      if (this.error == null && this.itemIterator == null && this.prefixIterator == null) {
        populate();
      }

      if (this.error == null
          && !this.itemIterator.hasNext()
          && !this.prefixIterator.hasNext()
          && this.listBucketResult.isTruncated()) {
        populate();
      }

      if (this.error != null) {
        this.completed = true;
        return this.error;
      }

      if (this.itemIterator.hasNext()) {
        Item item = this.itemIterator.next();
        this.lastObjectName = item.objectName();
        return new Result<>(item);
      }
      if (this.prefixIterator.hasNext()) {
        return new Result<>(this.prefixIterator.next().toItem());
      }

      this.completed = true;
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private Iterable<Result<Item>> listObjectsV2(ListObjectsArgs args) {
    return new Iterable<Result<Item>>() {
      @Override
      public Iterator<Result<Item>> iterator() {
        return new ObjectIterator() {
          protected void populateResult()
              throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
                  InternalException, InvalidBucketNameException, InvalidKeyException,
                  InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
                  XmlParserException {
            String delimiter = getDelimiter(args);
            String continuationToken = null;
            if (this.listBucketResult != null) {
              continuationToken = listBucketResult.nextContinuationToken();
            }

            this.listBucketResult = null;
            this.itemIterator = null;
            this.prefixIterator = null;

            this.listBucketResult =
                invokeListObjectsV2(
                    ListObjectsArgs.builder()
                        .bucket(args.bucket())
                        .continuationToken(continuationToken)
                        .delimiter(delimiter)
                        .fetchOwner(args.fetchOwner())
                        .prefix(args.prefix())
                        .includeUserMetadata(args.includeUserMetadata())
                        .build());
          }
        };
      }
    };
  }

  private Iterable<Result<Item>> listObjectsV1(ListObjectsArgs args) {
    return new Iterable<Result<Item>>() {
      @Override
      public Iterator<Result<Item>> iterator() {
        return new ObjectIterator() {
          @Override
          protected void populateResult()
              throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
                  InternalException, InvalidBucketNameException, InvalidKeyException,
                  InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
                  XmlParserException {
            String delimiter = getDelimiter(args);
            String continuationToken = null;
            if (this.listBucketResult != null) {
              if (delimiter != null) {
                continuationToken = listBucketResult.nextContinuationToken();
              } else {
                continuationToken = this.lastObjectName;
              }
            }

            this.listBucketResult = null;
            this.itemIterator = null;
            this.prefixIterator = null;

            this.listBucketResult =
                invokeListObjectsV1(
                    ListObjectsArgs.builder()
                        .bucket(args.bucket())
                        .delimiter(args.delimiter())
                        .startAfter(continuationToken)
                        .prefix(args.prefix())
                        .build());
          }
        };
      }
    };
  }

  /**
   * Lists bucket information of all buckets.
   *
   * <pre>Example:{@code
   * List<Bucket> bucketList = minioClient.listBuckets();
   * for (Bucket bucket : bucketList) {
   *   System.out.println(bucket.creationDate() + ", " + bucket.name());
   * }
   * }</pre>
   *
   * @return List&ltBucket&gt - List of bucket information.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public List<Bucket> listBuckets()
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    Response response = executeGet(null, null, (Multimap<String, String>) null);
    try (ResponseBody body = response.body()) {
      ListAllMyBucketsResult result =
          Xml.unmarshal(ListAllMyBucketsResult.class, body.charStream());
      return result.buckets();
    }
  }

  /**
   * Checks if a bucket exists.
   *
   * <pre>Example:{@code
   * boolean found = minioClient.bucketExists("my-bucketname");
   * if (found) {
   *   System.out.println("my-bucketname exists");
   * } else {
   *   System.out.println("my-bucketname does not exist");
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @return boolean - True if the bucket exists.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #bucketExists(BucketExistsArgs)}
   */
  @Deprecated
  public boolean bucketExists(String bucketName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
  }

  /**
   * Checks if a bucket exists.
   *
   * <pre>Example:{@code
   * boolean found =
   *      minioClient.bucketExists(BucketExistsArgs.builder().bucket("my-bucketname").build());
   * if (found) {
   *   System.out.println("my-bucketname exists");
   * } else {
   *   System.out.println("my-bucketname does not exist");
   * }
   * }</pre>
   *
   * @param args {@link BucketExistsArgs} object.
   * @return boolean - True if the bucket exists.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public boolean bucketExists(BucketExistsArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    try {
      executeHead(args.bucket(), null);
      return true;
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_BUCKET) {
        throw e;
      }
    }
    return false;
  }

  /**
   * Creates a bucket with default region.
   *
   * <pre>Example:{@code
   * minioClient.makeBucket("my-bucketname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws RegionConflictException thrown to indicate passed region conflict with default region.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #makeBucket(MakeBucketArgs)}
   */
  @Deprecated
  public void makeBucket(String bucketName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, RegionConflictException,
          ServerException, XmlParserException {
    this.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
  }

  /**
   * Creates a bucket with given region.
   *
   * <pre>Example:{@code
   * minioClient.makeBucket("my-bucketname", "eu-west-1");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param region Region in which the bucket will be created.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws RegionConflictException thrown to indicate passed region conflict with default region.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #makeBucket(MakeBucketArgs)}
   */
  @Deprecated
  public void makeBucket(String bucketName, String region)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, RegionConflictException,
          ServerException, XmlParserException {
    this.makeBucket(MakeBucketArgs.builder().bucket(bucketName).region(region).build());
  }

  /**
   * Creates a bucket with object lock feature enabled.
   *
   * <pre>Example:{@code
   * minioClient.makeBucket("my-bucketname", "eu-west-2", true);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param region Region in which the bucket will be created.
   * @param objectLock Flag to enable object lock feature.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws RegionConflictException thrown to indicate passed region conflict with default region.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #makeBucket(MakeBucketArgs)}
   */
  @Deprecated
  public void makeBucket(String bucketName, String region, boolean objectLock)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, RegionConflictException,
          ServerException, XmlParserException {
    this.makeBucket(
        MakeBucketArgs.builder().bucket(bucketName).region(region).objectLock(objectLock).build());
  }

  /**
   * Creates a bucket with region and object lock.
   *
   * <pre>Example:{@code
   * // Create bucket with default region.
   * minioClient.makeBucket(
   *     MakeBucketArgs.builder()
   *         .bucket("my-bucketname")
   *         .build());
   *
   * // Create bucket with specific region.
   * minioClient.makeBucket(
   *     MakeBucketArgs.builder()
   *         .bucket("my-bucketname")
   *         .region("us-west-1")
   *         .build());
   *
   * // Create object-lock enabled bucket with specific region.
   * minioClient.makeBucket(
   *     MakeBucketArgs.builder()
   *         .bucket("my-bucketname")
   *         .region("us-west-1")
   *         .objectLock(true)
   *         .build());
   * }</pre>
   *
   * @param args Object with bucket name, region and lock functionality
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws RegionConflictException thrown to indicate passed region conflict with default region.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void makeBucket(MakeBucketArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, RegionConflictException,
          ServerException, XmlParserException {
    checkArgs(args);

    String region = US_EAST_1;
    if (args.region() != null && !args.region().isEmpty()) {
      region = args.region();
    } else if (this.region != null && !this.region.isEmpty()) {
      region = this.region;
    }

    // If constructor already sets a region, check if it is equal to region param if provided
    if (this.region != null && !this.region.equals(region)) {
      throw new RegionConflictException(
          "passed region conflicts with the one previously specified");
    }

    CreateBucketConfiguration config = null;
    if (!region.equals(US_EAST_1)) {
      config = new CreateBucketConfiguration(region);
    }

    Map<String, String> headerMap = null;
    if (args.objectLock()) {
      headerMap = new HashMap<>();
      headerMap.put("x-amz-bucket-object-lock-enabled", "true");
    }

    Response response = executePut(args.bucket(), null, region, headerMap, null, config, 0);
    if (isAwsHost) {
      AwsRegionCache.INSTANCE.set(args.bucket(), region);
    }
    response.close();
  }

  /**
   * Enables object versioning feature in a bucket.
   *
   * <pre>Example:{@code
   * minioClient.enableVersioning("my-bucketname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #enableVersioning(EnableVersioningArgs)}
   */
  @Deprecated
  public void enableVersioning(String bucketName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    this.enableVersioning(EnableVersioningArgs.builder().bucket(bucketName).build());
  }

  /**
   * Enables object versioning feature in a bucket.
   *
   * <pre>Example:{@code
   * minioClient.enableVersioning(EnableVersioningArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link EnableVersioningArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void enableVersioning(EnableVersioningArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("versioning", "");
    VersioningConfiguration config = new VersioningConfiguration(true);

    Response response = executePut(args.bucket(), null, null, queryParamMap, config, 0);
    response.body().close();
  }

  /**
   * Disables object versioning feature in a bucket.
   *
   * <pre>Example:{@code
   * minioClient.disableVersioning("my-bucketname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #disableVersioning(DisableVersioningArgs)}
   */
  @Deprecated
  public void disableVersioning(String bucketName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    this.disableVersioning(DisableVersioningArgs.builder().bucket(bucketName).build());
  }

  /**
   * Disables object versioning feature in a bucket.
   *
   * <pre>Example:{@code
   * minioClient.disableVersioning(
   *     DisableVersioningArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DisableVersioningArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void disableVersioning(DisableVersioningArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("versioning", "");
    VersioningConfiguration config = new VersioningConfiguration(false);
    Response response = executePut(args.bucket(), null, null, queryParamMap, config, 0);
    response.body().close();
  }

  /**
   * Returns true if versioning is enabled on the bucket.
   *
   * <pre>Example:{@code
   * boolean isVersioningEnabled =
   *  minioClient.isVersioningEnabled(
   *       IsVersioningEnabledArgs.builder().bucket("my-bucketname").build());
   * if (isVersioningEnabled) {
   *   System.out.println("Bucket versioning is enabled");
   * } else {
   *   System.out.println("Bucket versioning is disabled");
   * }
   * }</pre>
   *
   * @param args {@link IsVersioningEnabledArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public boolean isVersioningEnabled(IsVersioningEnabledArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("versioning", "");
    try (Response response = executeGet(args.bucket(), null, null, queryParamMap)) {
      VersioningConfiguration result =
          Xml.unmarshal(VersioningConfiguration.class, response.body().charStream());
      return result.status();
    }
  }

  /**
   * Sets default object retention in a bucket.
   *
   * <pre>Example:{@code
   * ObjectLockConfiguration config = new ObjectLockConfiguration(
   *     RetentionMode.COMPLIANCE, new RetentionDurationDays(100));
   * minioClient.setDefaultRetention("my-bucketname", config);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param config Object lock configuration.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #setDefaultRetention(SetDefaultRetentionArgs)}
   */
  @Deprecated
  public void setDefaultRetention(String bucketName, ObjectLockConfiguration config)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    setDefaultRetention(
        SetDefaultRetentionArgs.builder().bucket(bucketName).config(config).build());
  }

  /**
   * Sets default object retention in a bucket.
   *
   * <pre>Example:{@code
   * ObjectLockConfiguration config = new ObjectLockConfiguration(
   *     RetentionMode.COMPLIANCE, new RetentionDurationDays(100));
   * minioClient.setDefaultRetention(
   *     SetDefaultRetentionArgs.builder().bucket("my-bucketname").config(config).build());
   * }</pre>
   *
   * @param args {@link SetDefaultRetentionArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void setDefaultRetention(SetDefaultRetentionArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("object-lock", "");

    Response response = executePut(args.bucket(), null, null, queryParamMap, args.config(), 0);
    response.close();
  }

  /**
   * Deletes default object retention in a bucket.
   *
   * <pre>Example:{@code
   * minioClient.deleteDefaultRetention(
   *     DeleteDefaultRetentionArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DeleteDefaultRetentionArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void deleteDefaultRetention(DeleteDefaultRetentionArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("object-lock", "");

    Response response =
        executePut(args.bucket(), null, null, queryParamMap, new ObjectLockConfiguration(), 0);
    response.close();
  }

  /**
   * Gets default object retention in a bucket.
   *
   * <pre>Example:{@code
   * ObjectLockConfiguration config = minioClient.getDefaultRetention("my-bucketname");
   * System.out.println("Mode: " + config.mode());
   * System.out.println(
   *     "Duration: " + config.duration().duration() + " " + config.duration().unit());
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @return {@link ObjectLockConfiguration} - Default retention configuration.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getDefaultRetention(GetDefaultRetentionArgs)}
   */
  @Deprecated
  public ObjectLockConfiguration getDefaultRetention(String bucketName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return getDefaultRetention(GetDefaultRetentionArgs.builder().bucket(bucketName).build());
  }

  /**
   * Gets default object retention in a bucket.
   *
   * <pre>Example:{@code
   * ObjectLockConfiguration config =
   *     minioClient.getDefaultRetention(
   *         GetDefaultRetentionArgs.builder().bucket("my-bucketname").build());
   * System.out.println("Mode: " + config.mode());
   * System.out.println(
   *     "Duration: " + config.duration().duration() + " " + config.duration().unit());
   * }</pre>
   *
   * @param args {@link GetDefaultRetentionArgs} object.
   * @return {@link ObjectLockConfiguration} - Default retention configuration.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public ObjectLockConfiguration getDefaultRetention(GetDefaultRetentionArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("object-lock", "");

    try (Response response = executeGet(args.bucket(), null, null, queryParamMap)) {
      return Xml.unmarshal(ObjectLockConfiguration.class, response.body().charStream());
    }
  }

  /**
   * Sets retention configuration to an object.
   *
   * <pre>Example:{@code
   * Retention retention =
   *     new Retention(RetentionMode.COMPLIANCE, ZonedDateTime.now().plusYears(1));
   * minioClient.setObjectRetention(
   *     "my-bucketname", "my-objectname", null, retention, true);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param versionId Version ID of the object.
   * @param config Object retention configuration.
   * @param bypassGovernanceMode Bypass Governance retention.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #setObjectRetention(SetObjectRetentionArgs)}
   */
  @Deprecated
  public void setObjectRetention(
      String bucketName,
      String objectName,
      String versionId,
      Retention config,
      boolean bypassGovernanceMode)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {

    this.setObjectRetention(
        SetObjectRetentionArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .versionId(versionId)
            .config(config)
            .bypassGovernanceMode(bypassGovernanceMode)
            .build());
  }

  /**
   * Sets retention configuration to an object.
   *
   * <pre>Example:{@code
   *  Retention retention = new Retention(
   *       RetentionMode.COMPLIANCE, ZonedDateTime.now().plusYears(1));
   *  minioClient.setObjectRetention(
   *      SetObjectRetentionArgs.builder()
   *          .bucket("my-bucketname")
   *          .object("my-objectname")
   *          .config(config)
   *          .bypassGovernanceMode(true)
   *          .build());
   * }</pre>
   *
   * @param args {@link SetObjectRetentionArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void setObjectRetention(SetObjectRetentionArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("retention", "");

    if (args.versionId() != null) {
      queryParamMap.put("versionId", args.versionId());
    }

    Map<String, String> headerMap = new HashMap<>();
    if (args.bypassGovernanceMode()) {
      headerMap.put("x-amz-bypass-governance-retention", "True");
    }

    Response response =
        executePut(args.bucket(), args.object(), headerMap, queryParamMap, args.config(), 0);
    response.body().close();
  }

  /**
   * Gets retention configuration of an object.
   *
   * <pre>Example:{@code
   * Retention retention =
   *     minioClient.getObjectRetention("my-bucketname", "my-objectname", null);
   * System.out.println(
   *     "mode: " + retention.mode() + "until: " + retention.retainUntilDate());
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param versionId Version ID of the object.
   * @return {@link Retention} - Object retention configuration.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getObjectRetention(GetObjectRetentionArgs)}
   */
  @Deprecated
  public Retention getObjectRetention(String bucketName, String objectName, String versionId)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return this.getObjectRetention(
        GetObjectRetentionArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .versionId(versionId)
            .build());
  }

  /**
   * Gets retention configuration of an object.
   *
   * <pre>Example:{@code
   * Retention retention =
   *     minioClient.getObjectRetention(GetObjectRetentionArgs.builder()
   *        .bucket(bucketName)
   *        .object(objectName)
   *        .versionId(versionId)
   *        .build()););
   * System.out.println(
   *     "mode: " + retention.mode() + "until: " + retention.retainUntilDate());
   * }</pre>
   *
   * @param args {@link GetObjectRetentionArgs} object.
   * @return {@link Retention} - Object retention configuration.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public Retention getObjectRetention(GetObjectRetentionArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("retention", "");

    if (args.versionId() != null) {
      queryParamMap.put("versionId", args.versionId());
    }

    try (Response response = executeGet(args.bucket(), args.object(), null, queryParamMap)) {
      Retention retention = Xml.unmarshal(Retention.class, response.body().charStream());
      return retention;
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_OBJECT_LOCK_CONFIGURATION) {
        throw e;
      }
    }
    return null;
  }

  /**
   * Enables legal hold on an object.
   *
   * <pre>Example:{@code
   * minioClient.enableObjectLegalHold("my-bucketname", "my-object", null);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param versionId Version ID of the object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #enableObjectLegalHold(EnableObjectLegalHoldArgs)}
   */
  @Deprecated
  public void enableObjectLegalHold(String bucketName, String objectName, String versionId)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    enableObjectLegalHold(
        EnableObjectLegalHoldArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .versionId(versionId)
            .build());
  }

  /**
   * Enables legal hold on an object.
   *
   * <pre>Example:{@code
   * minioClient.enableObjectLegalHold(
   *    EnableObjectLegalHoldArgs.builder()
   *        .bucket("my-bucketname")
   *        .object("my-objectname")
   *        .versionId("object-versionId")
   *        .build());
   * }</pre>
   *
   * @param args {@link EnableObjectLegalHoldArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void enableObjectLegalHold(EnableObjectLegalHoldArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);
    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("legal-hold", "");

    if (args.versionId() != null) {
      queryParamMap.put("versionId", args.versionId());
    }

    LegalHold legalHold = new LegalHold(true);
    Response response = executePut(args.bucket(), args.object(), null, queryParamMap, legalHold, 0);
    response.body().close();
  }

  /**
   * Disables legal hold on an object.
   *
   * <pre>Example:{@code
   * minioClient.disableObjectLegalHold("my-bucketname", "my-object", null);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param versionId Version ID of the object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #disableObjectLegalHold(DisableObjectLegalHoldArgs)}
   */
  @Deprecated
  public void disableObjectLegalHold(String bucketName, String objectName, String versionId)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    disableObjectLegalHold(
        DisableObjectLegalHoldArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .versionId(versionId)
            .build());
  }

  /**
   * Disables legal hold on an object.
   *
   * <pre>Example:{@code
   * minioClient.disableObjectLegalHold(
   *    DisableObjectLegalHoldArgs.builder()
   *        .bucket("my-bucketname")
   *        .object("my-objectname")
   *        .versionId("object-versionId")
   *        .build());
   * }</pre>
   *
   * @param args {@link DisableObjectLegalHoldArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void disableObjectLegalHold(DisableObjectLegalHoldArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("legal-hold", "");

    if (args.versionId() != null) {
      queryParamMap.put("versionId", args.versionId());
    }

    LegalHold legalHold = new LegalHold(false);
    Response response = executePut(args.bucket(), args.object(), null, queryParamMap, legalHold, 0);
    response.body().close();
  }

  /**
   * Returns true if legal hold is enabled on an object.
   *
   * <pre>Example:{@code
   * boolean status =
   *     s3Client.isObjectLegalHoldEnabled("my-bucketname", "my-objectname", null);
   * if (status) {
   *   System.out.println("Legal hold is on");
   * } else {
   *   System.out.println("Legal hold is off");
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param versionId Version ID of the object.
   * @return boolean - True if legal hold is enabled.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #isObjectLegalHoldEnabled(IsObjectLegalHoldEnabledArgs)}
   */
  @Deprecated
  public boolean isObjectLegalHoldEnabled(String bucketName, String objectName, String versionId)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return isObjectLegalHoldEnabled(
        IsObjectLegalHoldEnabledArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .versionId(versionId)
            .build());
  }

  /**
   * Returns true if legal hold is enabled on an object.
   *
   * <pre>Example:{@code
   * boolean status =
   *     s3Client.isObjectLegalHoldEnabled(
   *        IsObjectLegalHoldEnabledArgs.builder()
   *             .bucket("my-bucketname")
   *             .object("my-objectname")
   *             .versionId("object-versionId")
   *             .build());
   * if (status) {
   *   System.out.println("Legal hold is on");
   *  } else {
   *   System.out.println("Legal hold is off");
   *  }
   * }</pre>
   *
   * args {@link IsObjectLegalHoldEnabledArgs} object.
   *
   * @return boolean - True if legal hold is enabled.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public boolean isObjectLegalHoldEnabled(IsObjectLegalHoldEnabledArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("legal-hold", "");

    if (args.versionId() != null) {
      queryParamMap.put("versionId", args.versionId());
    }

    try (Response response = executeGet(args.bucket(), args.object(), null, queryParamMap)) {
      LegalHold result = Xml.unmarshal(LegalHold.class, response.body().charStream());
      return result.status();
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_OBJECT_LOCK_CONFIGURATION) {
        throw e;
      }
    }
    return false;
  }

  /**
   * Removes an empty bucket.
   *
   * <pre>Example:{@code
   * minioClient.removeBucket("my-bucketname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #removeBucket(RemoveBucketArgs)}
   */
  @Deprecated
  public void removeBucket(String bucketName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    this.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
  }

  /**
   * Removes an empty bucket using arguments
   *
   * <pre>Example:{@code
   * minioClient.removeBucket(RemoveBucketArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link RemoveBucketArgs} bucket.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void removeBucket(RemoveBucketArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    executeDelete(args.bucket(), null, null);
  }

  private ObjectWriteResponse putObject(
      ObjectWriteArgs args,
      Object data,
      long objectSize,
      long partSize,
      int partCount,
      String contentType)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    Multimap<String, String> headers = HashMultimap.create();
    headers.putAll(args.extraHeaders());
    headers.putAll(args.genHeaders());
    if (!headers.containsKey("Content-Type")) {
      headers.put("Content-Type", contentType);
    }

    String uploadId = null;
    long uploadedSize = 0L;
    Part[] parts = null;

    try {
      for (int partNumber = 1; partNumber <= partCount || partCount < 0; partNumber++) {
        long availableSize = partSize;
        if (partCount > 0) {
          if (partNumber == partCount) {
            availableSize = objectSize - uploadedSize;
          }
        } else {
          availableSize = getAvailableSize(data, partSize + 1);

          // If availableSize is less or equal to partSize, then we have reached last
          // part.
          if (availableSize <= partSize) {
            partCount = partNumber;
          } else {
            availableSize = partSize;
          }
        }

        if (partCount == 1) {
          return putObject(
              args.bucket(),
              args.region(),
              args.object(),
              data,
              (int) availableSize,
              headers,
              args.extraQueryParams());
        }

        if (uploadId == null) {
          uploadId =
              createMultipartUpload(
                  args.bucket(), args.region(), args.object(), headers, args.extraQueryParams());
          parts = new Part[ObjectWriteArgs.MAX_MULTIPART_COUNT];
        }

        Map<String, String> ssecHeaders = null;
        // set encryption headers in the case of SSE-C.
        if (args.sse() != null && args.sse().type() == ServerSideEncryption.Type.SSE_C) {
          ssecHeaders = args.sse().headers();
        }

        String etag =
            uploadPart(
                args.bucket(),
                args.object(),
                data,
                (int) availableSize,
                uploadId,
                partNumber,
                ssecHeaders);
        parts[partNumber - 1] = new Part(partNumber, etag);
        uploadedSize += availableSize;
      }

      return completeMultipartUpload(
          args.bucket(), args.region(), args.object(), uploadId, parts, null, null);
    } catch (RuntimeException e) {
      if (uploadId != null) {
        abortMultipartUpload(args.bucket(), args.object(), uploadId);
      }
      throw e;
    } catch (Exception e) {
      if (uploadId != null) {
        abortMultipartUpload(args.bucket(), args.object(), uploadId);
      }
      throw e;
    }
  }

  /**
   * Uploads data from a file to an object using {@link PutObjectOptions}.
   *
   * <pre>Example:{@code
   * minioClient.putObject("my-bucketname", "my-objectname", "my-filename", null);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param filename Name of file to upload.
   * @param options {@link PutObjectOptions} to be used during upload.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #uploadObject(UploadObjectArgs)}
   */
  @Deprecated
  public void putObject(
      String bucketName, String objectName, String filename, PutObjectOptions options)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    UploadObjectArgs.Builder builder =
        UploadObjectArgs.builder().bucket(bucketName).object(objectName).filename(filename);
    if (!options.contentType().equals("application/octet-stream")) {
      builder.contentType(options.contentType());
    }
    uploadObject(builder.build());
  }

  /**
   * Uploads data from a stream to an object using {@link PutObjectOptions}.
   *
   * <pre>Example:{@code
   * PutObjectOptions options = new PutObjectOptions(7003256, -1);
   * minioClient.putObject("my-bucketname", "my-objectname", stream, options);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param stream Stream contains object data.
   * @param options {@link PutObjectOptions} to be used during upload.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #putObject(PutObjectArgs)}
   */
  @Deprecated
  public void putObject(
      String bucketName, String objectName, InputStream stream, PutObjectOptions options)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    putObject(
        PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                stream, options.objectSize(), options.partSize())
            .contentType(options.contentType())
            .headers(options.headers())
            .sse(options.sse())
            .build());
  }

  /**
   * Uploads data from a stream to an object.
   *
   * <pre>Example:{@code
   * // Upload known sized input stream.
   * minioClient.putObject(
   *     PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
   *             inputStream, size, -1)
   *         .contentType("video/mp4")
   *         .build());
   *
   * // Upload unknown sized input stream.
   * minioClient.putObject(
   *     PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
   *             inputStream, -1, 10485760)
   *         .contentType("video/mp4")
   *         .build());
   *
   * // Create object ends with '/' (also called as folder or directory).
   * minioClient.putObject(
   *     PutObjectArgs.builder().bucket("my-bucketname").object("path/to/").stream(
   *             new ByteArrayInputStream(new byte[] {}), 0, -1)
   *         .build());
   *
   * // Upload input stream with headers and user metadata.
   * Map<String, String> headers = new HashMap<>();
   * headers.put("X-Amz-Storage-Class", "REDUCED_REDUNDANCY");
   * Map<String, String> userMetadata = new HashMap<>();
   * userMetadata.put("My-Project", "Project One");
   * minioClient.putObject(
   *     PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
   *             inputStream, size, -1)
   *         .headers(headers)
   *         .userMetadata(userMetadata)
   *         .build());
   *
   * // Upload input stream with server-side encryption.
   * minioClient.putObject(
   *     PutObjectArgs.builder().bucket("my-bucketname").object("my-objectname").stream(
   *             inputStream, size, -1)
   *         .sse(sse)
   *         .build());
   * }</pre>
   *
   * @param args {@link PutObjectArgs} object.
   * @return {@link ObjectWriteResponse} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public ObjectWriteResponse putObject(PutObjectArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);
    args.validateSse(this.baseUrl);
    return putObject(
        args,
        args.stream(),
        args.objectSize(),
        args.partSize(),
        args.partCount(),
        args.contentType());
  }

  /**
   * Uploads data from a file to an object.
   *
   * <pre>Example:{@code
   * // Upload an JSON file.
   * minioClient.uploadObject(
   *     UploadObjectArgs.builder()
   *         .bucket("my-bucketname").object("my-objectname").filename("person.json").build());
   *
   * // Upload a video file.
   * minioClient.uploadObject(
   *     UploadObjectArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .filename("my-video.avi")
   *         .contentType("video/mp4")
   *         .build());
   * }</pre>
   *
   * @param args {@link UploadObjectArgs} object.
   * @return {@link ObjectWriteResponse} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public ObjectWriteResponse uploadObject(UploadObjectArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);
    args.validateSse(this.baseUrl);
    try (RandomAccessFile file = new RandomAccessFile(args.filename(), "r")) {
      return putObject(
          args, file, args.objectSize(), args.partSize(), args.partCount(), args.contentType());
    }
  }

  /**
   * Gets bucket policy configuration of a bucket.
   *
   * <pre>Example:{@code
   * String config = minioClient.getBucketPolicy("my-bucketname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @return String - Bucket policy configuration as JSON string.
   * @throws BucketPolicyTooLargeException thrown to indicate returned bucket policy is too large.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getBucketPolicy(GetBucketPolicyArgs)}
   */
  @Deprecated
  public String getBucketPolicy(String bucketName)
      throws BucketPolicyTooLargeException, ErrorResponseException, IllegalArgumentException,
          InsufficientDataException, InternalException, InvalidBucketNameException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          ServerException, XmlParserException {
    return getBucketPolicy(GetBucketPolicyArgs.builder().bucket(bucketName).build());
  }

  /**
   * Gets bucket policy configuration of a bucket.
   *
   * <pre>Example:{@code
   * String config =
   *     minioClient.getBucketPolicy(GetBucketPolicyArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketPolicyArgs} object.
   * @return String - Bucket policy configuration as JSON string.
   * @throws BucketPolicyTooLargeException thrown to indicate returned bucket policy is too large.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public String getBucketPolicy(GetBucketPolicyArgs args)
      throws BucketPolicyTooLargeException, ErrorResponseException, IllegalArgumentException,
          InsufficientDataException, InternalException, InvalidBucketNameException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          ServerException, XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("policy", "");

    try (Response response = executeGet(args.bucket(), null, null, queryParamMap)) {
      byte[] buf = new byte[MAX_BUCKET_POLICY_SIZE];
      int bytesRead = 0;
      bytesRead = response.body().byteStream().read(buf, 0, MAX_BUCKET_POLICY_SIZE);
      if (bytesRead < 0) {
        throw new IOException("unexpected EOF when reading bucket policy");
      }

      // Read one byte extra to ensure only MAX_BUCKET_POLICY_SIZE data is sent by the server.
      if (bytesRead == MAX_BUCKET_POLICY_SIZE) {
        int byteRead = 0;
        while (byteRead == 0) {
          byteRead = response.body().byteStream().read();
          if (byteRead < 0) {
            break; // reached EOF which is fine.
          }

          if (byteRead > 0) {
            throw new BucketPolicyTooLargeException(args.bucket());
          }
        }
      }

      return new String(buf, 0, bytesRead, StandardCharsets.UTF_8);
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_BUCKET_POLICY) {
        throw e;
      }
    }

    return "";
  }

  /**
   * Sets bucket policy configuration to a bucket.
   *
   * <pre>Example:{@code
   * // Assume policyJson contains below JSON string;
   * // {
   * //     "Statement": [
   * //         {
   * //             "Action": [
   * //                 "s3:GetBucketLocation",
   * //                 "s3:ListBucket"
   * //             ],
   * //             "Effect": "Allow",
   * //             "Principal": "*",
   * //             "Resource": "arn:aws:s3:::my-bucketname"
   * //         },
   * //         {
   * //             "Action": "s3:GetObject",
   * //             "Effect": "Allow",
   * //             "Principal": "*",
   * //             "Resource": "arn:aws:s3:::my-bucketname/myobject*"
   * //         }
   * //     ],
   * //     "Version": "2012-10-17"
   * // }
   * //
   * minioClient.setBucketPolicy("my-bucketname", policyJson);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param policy Bucket policy configuration as JSON string.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #setBucketPolicy(SetBucketPolicyArgs)}
   */
  @Deprecated
  public void setBucketPolicy(String bucketName, String policy)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
  }

  /**
   * Sets bucket policy configuration to a bucket.
   *
   * <pre>Example:{@code
   * // Assume policyJson contains below JSON string;
   * // {
   * //     "Statement": [
   * //         {
   * //             "Action": [
   * //                 "s3:GetBucketLocation",
   * //                 "s3:ListBucket"
   * //             ],
   * //             "Effect": "Allow",
   * //             "Principal": "*",
   * //             "Resource": "arn:aws:s3:::my-bucketname"
   * //         },
   * //         {
   * //             "Action": "s3:GetObject",
   * //             "Effect": "Allow",
   * //             "Principal": "*",
   * //             "Resource": "arn:aws:s3:::my-bucketname/myobject*"
   * //         }
   * //     ],
   * //     "Version": "2012-10-17"
   * // }
   * //
   * minioClient.setBucketPolicy(
   *     SetBucketPolicyArgs.builder().bucket("my-bucketname").config(policyJson).build());
   * }</pre>
   *
   * @param args {@link SetBucketPolicyArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void setBucketPolicy(SetBucketPolicyArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> headerMap = new HashMap<>();
    headerMap.put("Content-Type", "application/json");

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("policy", "");

    Response response = executePut(args.bucket(), null, headerMap, queryParamMap, args.config(), 0);
    response.close();
  }

  /**
   * Deletes bucket policy configuration to a bucket.
   *
   * <pre>Example:{@code
   * minioClient.deleteBucketPolicy(DeleteBucketPolicyArgs.builder().bucket("my-bucketname"));
   * }</pre>
   *
   * @param args {@link DeleteBucketPolicyArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void deleteBucketPolicy(DeleteBucketPolicyArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("policy", "");

    try {
      Response response = executeDelete(args.bucket(), "", queryParamMap);
      response.close();
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_BUCKET_POLICY) {
        throw e;
      }
    }
  }

  /**
   * Sets life-cycle configuration to a bucket.
   *
   * <pre>Example:{@code
   * // Lets consider variable 'lifeCycleXml' contains below XML String;
   * // <LifecycleConfiguration>
   * //   <Rule>
   * //     <ID>expire-bucket</ID>
   * //     <Prefix></Prefix>
   * //     <Status>Enabled</Status>
   * //     <Expiration>
   * //       <Days>365</Days>
   * //     </Expiration>
   * //   </Rule>
   * // </LifecycleConfiguration>
   * //
   * minioClient.setBucketLifeCycle("my-bucketname", lifeCycleXml);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param lifeCycle Life cycle configuraion as XML string.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #setBucketLifeCycle(SetBucketLifeCycleArgs)}
   */
  @Deprecated
  public void setBucketLifeCycle(String bucketName, String lifeCycle)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    setBucketLifeCycle(
        SetBucketLifeCycleArgs.builder().bucket(bucketName).config(lifeCycle).build());
  }

  /**
   * Sets life-cycle configuration to a bucket.
   *
   * <pre>Example:{@code
   * // Lets consider variable 'lifeCycleXml' contains below XML String;
   * // <LifecycleConfiguration>
   * //   <Rule>
   * //     <ID>expire-bucket</ID>
   * //     <Prefix></Prefix>
   * //     <Status>Enabled</Status>
   * //     <Expiration>
   * //       <Days>365</Days>
   * //     </Expiration>
   * //   </Rule>
   * // </LifecycleConfiguration>
   * //
   * minioClient.setBucketLifeCycle(
   *     SetBucketLifeCycleArgs.builder().bucket("my-bucketname").config(lifeCycleXml).build());
   * }</pre>
   *
   * @param args {@link SetBucketLifeCycleArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void setBucketLifeCycle(SetBucketLifeCycleArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("lifecycle", "");
    Response response = executePut(args.bucket(), null, null, queryParamMap, args.config(), 0);
    response.close();
  }

  /**
   * Deletes life-cycle configuration of a bucket.
   *
   * <pre>Example:{@code
   * deleteBucketLifeCycle("my-bucketname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #deleteBucketLifeCycle(DeleteBucketLifeCycleArgs)}
   */
  @Deprecated
  public void deleteBucketLifeCycle(String bucketName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    deleteBucketLifeCycle(DeleteBucketLifeCycleArgs.builder().bucket(bucketName).build());
  }

  /**
   * Deletes life-cycle configuration of a bucket.
   *
   * <pre>Example:{@code
   * deleteBucketLifeCycle(DeleteBucketLifeCycleArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DeleteBucketLifeCycleArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void deleteBucketLifeCycle(DeleteBucketLifeCycleArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("lifecycle", "");
    Response response = executeDelete(args.bucket(), "", queryParamMap);
    response.close();
  }

  /**
   * Gets life-cycle configuration of a bucket.
   *
   * <pre>Example:{@code
   * String lifecycle = minioClient.getBucketLifeCycle("my-bucketname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @return String - Life cycle configuration as XML string.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getBucketLifeCycle(GetBucketLifeCycleArgs)}
   */
  @Deprecated
  public String getBucketLifeCycle(String bucketName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return getBucketLifeCycle(GetBucketLifeCycleArgs.builder().bucket(bucketName).build());
  }

  /**
   * Gets life-cycle configuration of a bucket.
   *
   * <pre>Example:{@code
   * String lifecycle =
   *     minioClient.getBucketLifeCycle(
   *         GetBucketLifeCycleArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketLifeCycleArgs} object.
   * @return String - Life cycle configuration as XML string.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public String getBucketLifeCycle(GetBucketLifeCycleArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("lifecycle", "");
    try (Response response = executeGet(args.bucket(), null, null, queryParamMap)) {
      return new String(response.body().bytes(), StandardCharsets.UTF_8);
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_LIFECYCLE_CONFIGURATION) {
        throw e;
      }
    }

    return "";
  }

  /**
   * Gets notification configuration of a bucket.
   *
   * <pre>Example:{@code
   * NotificationConfiguration config =
   *     minioClient.getBucketNotification("my-bucketname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @return {@link NotificationConfiguration} - Notification configuration.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #getBucketNotification(GetBucketNotificationArgs)}
   */
  @Deprecated
  public NotificationConfiguration getBucketNotification(String bucketName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return getBucketNotification(GetBucketNotificationArgs.builder().bucket(bucketName).build());
  }

  /**
   * Gets notification configuration of a bucket.
   *
   * <pre>Example:{@code
   * NotificationConfiguration config =
   *     minioClient.getBucketNotification(
   *         GetBucketNotificationArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketNotificationArgs} object.
   * @return {@link NotificationConfiguration} - Notification configuration.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public NotificationConfiguration getBucketNotification(GetBucketNotificationArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("notification", "");

    try (Response response = executeGet(args.bucket(), null, null, queryParamMap)) {
      return Xml.unmarshal(NotificationConfiguration.class, response.body().charStream());
    }
  }

  /**
   * Sets notification configuration to a bucket.
   *
   * <pre>Example:{@code
   * List<EventType> eventList = new LinkedList<>();
   * eventList.add(EventType.OBJECT_CREATED_PUT);
   * eventList.add(EventType.OBJECT_CREATED_COPY);
   *
   * QueueConfiguration queueConfiguration = new QueueConfiguration();
   * queueConfiguration.setQueue("arn:minio:sqs::1:webhook");
   * queueConfiguration.setEvents(eventList);
   * queueConfiguration.setPrefixRule("images");
   * queueConfiguration.setSuffixRule("pg");
   *
   * List<QueueConfiguration> queueConfigurationList = new LinkedList<>();
   * queueConfigurationList.add(queueConfiguration);
   *
   * NotificationConfiguration config = new NotificationConfiguration();
   * config.setQueueConfigurationList(queueConfigurationList);
   *
   * minioClient.setBucketNotification("my-bucketname", config);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param notificationConfiguration {@link NotificationConfiguration} to be set.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #setBucketNotification(SetBucketNotificationArgs)}
   */
  @Deprecated
  public void setBucketNotification(
      String bucketName, NotificationConfiguration notificationConfiguration)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    setBucketNotification(
        SetBucketNotificationArgs.builder()
            .bucket(bucketName)
            .config(notificationConfiguration)
            .build());
  }

  /**
   * Sets notification configuration to a bucket.
   *
   * <pre>Example:{@code
   * List<EventType> eventList = new LinkedList<>();
   * eventList.add(EventType.OBJECT_CREATED_PUT);
   * eventList.add(EventType.OBJECT_CREATED_COPY);
   *
   * QueueConfiguration queueConfiguration = new QueueConfiguration();
   * queueConfiguration.setQueue("arn:minio:sqs::1:webhook");
   * queueConfiguration.setEvents(eventList);
   * queueConfiguration.setPrefixRule("images");
   * queueConfiguration.setSuffixRule("pg");
   *
   * List<QueueConfiguration> queueConfigurationList = new LinkedList<>();
   * queueConfigurationList.add(queueConfiguration);
   *
   * NotificationConfiguration config = new NotificationConfiguration();
   * config.setQueueConfigurationList(queueConfigurationList);
   *
   * minioClient.setBucketNotification(
   *     SetBucketNotificationArgs.builder().bucket("my-bucketname").config(config).build());
   * }</pre>
   *
   * @param args {@link SetBucketNotificationArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void setBucketNotification(SetBucketNotificationArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("notification", "");
    Response response = executePut(args.bucket(), null, null, queryParamMap, args.config(), 0);
    response.close();
  }

  /**
   * Removes notification configuration of a bucket.
   *
   * <pre>Example:{@code
   * minioClient.removeAllBucketNotification("my-bucketname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #deleteBucketNotification(DeleteBucketNotificationArgs)}
   */
  @Deprecated
  public void removeAllBucketNotification(String bucketName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    deleteBucketNotification(DeleteBucketNotificationArgs.builder().bucket(bucketName).build());
  }

  /**
   * Deletes notification configuration of a bucket.
   *
   * <pre>Example:{@code
   * minioClient.deleteBucketNotification(
   *     DeleteBucketNotificationArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DeleteBucketNotificationArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void deleteBucketNotification(DeleteBucketNotificationArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    setBucketNotification(
        SetBucketNotificationArgs.builder()
            .bucket(args.bucket())
            .config(new NotificationConfiguration())
            .build());
  }

  /**
   * Lists incomplete object upload information of a bucket.
   *
   * <pre>Example:{@code
   * Iterable<Result<Upload>> results =
   *     minioClient.listIncompleteUploads("my-bucketname");
   * for (Result<Upload> result : results) {
   *   Upload upload = result.get();
   *   System.out.println(upload.uploadId() + ", " + upload.objectName());
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @return Iterable&ltResult&ltUpload&gt;&gt; - Lazy iterator contains object upload information.
   * @deprecated use {@link #listIncompleteUploads(ListIncompleteUploadsArgs)}
   */
  @Deprecated
  public Iterable<Result<Upload>> listIncompleteUploads(String bucketName)
      throws XmlParserException {
    return listIncompleteUploads(
        ListIncompleteUploadsArgs.builder().bucket(bucketName).recursive(true).build());
  }

  /**
   * Lists incomplete object upload information of a bucket for prefix.
   *
   * <pre>Example:{@code
   * Iterable<Result<Upload>> results =
   *     minioClient.listIncompleteUploads("my-bucketname", "my-obj");
   * for (Result<Upload> result : results) {
   *   Upload upload = result.get();
   *   System.out.println(upload.uploadId() + ", " + upload.objectName());
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param prefix Object name starts with prefix.
   * @return Iterable&ltResult&ltUpload&gt;&gt; - Lazy iterator contains object upload information.
   * @throws XmlParserException upon parsing response xml
   * @deprecated use {@link #listIncompleteUploads(ListIncompleteUploadsArgs)}
   */
  @Deprecated
  public Iterable<Result<Upload>> listIncompleteUploads(String bucketName, String prefix)
      throws XmlParserException {
    return listIncompleteUploads(
        ListIncompleteUploadsArgs.builder()
            .bucket(bucketName)
            .prefix(prefix)
            .recursive(true)
            .build());
  }

  /**
   * Lists incomplete object upload information of a bucket for prefix recursively.
   *
   * <pre>Example:{@code
   * Iterable<Result<Upload>> results =
   *     minioClient.listIncompleteUploads("my-bucketname", "my-obj", true);
   * for (Result<Upload> result : results) {
   *   Upload upload = result.get();
   *   System.out.println(upload.uploadId() + ", " + upload.objectName());
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param prefix Object name starts with prefix.
   * @param recursive List recursively than directory structure emulation.
   * @return Iterable&ltResult&ltUpload&gt;&gt; - Lazy iterator contains object upload information.
   * @deprecated use {@link #listIncompleteUploads(ListIncompleteUploadsArgs)}
   */
  @Deprecated
  public Iterable<Result<Upload>> listIncompleteUploads(
      String bucketName, String prefix, boolean recursive) {
    return listIncompleteUploads(
        ListIncompleteUploadsArgs.builder()
            .bucket(bucketName)
            .prefix(prefix)
            .recursive(recursive)
            .build());
  }

  /**
   * Lists incomplete object upload information of a bucket for prefix recursively.
   *
   * <pre>Example:{@code
   *  // Lists incomplete object upload information of a bucket.
   *   Iterable<Result<Upload>> results =
   *       minioClient.listIncompleteUploads(
   *           ListIncompleteUploadsArgs.builder().bucket("my-bucketname").build());
   *   for (Result<Upload> result : results) {
   *     Upload upload = result.get();
   *     System.out.println(upload.uploadId() + ", " + upload.objectName());
   *   }
   *
   *   // Lists incomplete object upload information of a bucket for prefix.
   *   Iterable<Result<Upload>> results =
   *       minioClient.listIncompleteUploads(
   *           ListIncompleteUploadsArgs.builder()
   *               .bucket("my-bucketname")
   *               .prefix("my-obj")
   *               .build());
   *   for (Result<Upload> result : results) {
   *     Upload upload = result.get();
   *     System.out.println(upload.uploadId() + ", " + upload.objectName());
   *   }
   *
   *   // Lists incomplete object upload information of a bucket for prefix recursively.
   *   Iterable<Result<Upload>> results =
   *       minioClient.listIncompleteUploads(
   *           ListIncompleteUploadsArgs.builder()
   *               .bucket("my-bucketname")
   *               .prefix("my-obj")
   *               .recursive(true)
   *               .build());
   *   for (Result<Upload> result : results) {
   *    Upload upload = result.get();
   *    System.out.println(upload.uploadId() + ", " + upload.objectName());
   *   }
   *
   *   // Lists incomplete object upload information of a bucket for prefix, delimiter.
   *   //  keyMarker, uploadIdMarker and maxUpload to 500
   *   Iterable<Result<Upload>> results =
   *       minioClient.listIncompleteUploads(
   *           ListIncompleteUploadsArgs.builder()
   *               .bucket("my-bucketname")
   *               .prefix("my-obj")
   *               .delimiter("-")
   *               .keyMarker("b")
   *               .maxUploads(500)
   *               .uploadIdMarker("k")
   *               .build());
   *   for (Result<Upload> result : results) {
   *    Upload upload = result.get();
   *    System.out.println(upload.uploadId() + ", " + upload.objectName());
   *   }
   * }</pre>
   *
   * @param args {@link ListIncompleteUploadsArgs} objects.
   * @return Iterable&lt;Result&lt;Upload&gt;&gt; - Lazy iterator contains object upload
   *     information.
   */
  public Iterable<Result<Upload>> listIncompleteUploads(ListIncompleteUploadsArgs args) {
    checkArgs(args);
    return this.listIncompleteUploads(args, true);
  }

  /**
   * Returns Iterable<Result<Upload>> of given ListIncompleteUploadsArgs argumentsr. All parts size
   * are aggregated when aggregatePartSize is true.
   */
  private Iterable<Result<Upload>> listIncompleteUploads(
      ListIncompleteUploadsArgs args, final boolean aggregatePartSize) {
    return new Iterable<Result<Upload>>() {
      @Override
      public Iterator<Result<Upload>> iterator() {
        return new Iterator<Result<Upload>>() {
          private String nextKeyMarker = args.keyMarker();
          private String nextUploadIdMarker = args.uploadIdMarker();
          private ListMultipartUploadsResult listMultipartUploadsResult;
          private Result<Upload> error;
          private Iterator<Upload> uploadIterator;
          private boolean completed = false;

          private synchronized void populate() {
            String delimiter = args.delimiter();
            if (args.recursive()) {
              delimiter = null;
            }

            this.listMultipartUploadsResult = null;
            this.uploadIterator = null;

            try {
              this.listMultipartUploadsResult =
                  listMultipartUploads(
                      args.bucket(),
                      delimiter,
                      nextKeyMarker,
                      args.maxUploads(),
                      args.prefix(),
                      nextUploadIdMarker);
            } catch (ErrorResponseException
                | IllegalArgumentException
                | InsufficientDataException
                | InternalException
                | InvalidBucketNameException
                | InvalidKeyException
                | InvalidResponseException
                | IOException
                | NoSuchAlgorithmException
                | ServerException
                | XmlParserException e) {
              this.error = new Result<>(e);
            } finally {
              if (this.listMultipartUploadsResult != null) {
                this.uploadIterator = this.listMultipartUploadsResult.uploads().iterator();
              } else {
                this.uploadIterator = new LinkedList<Upload>().iterator();
              }
            }
          }

          private synchronized long getAggregatedPartSize(String objectName, String uploadId)
              throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
                  InternalException, InvalidBucketNameException, InvalidKeyException,
                  InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
                  XmlParserException {
            long aggregatedPartSize = 0;

            for (Result<Part> result : listObjectParts(args.bucket(), objectName, uploadId)) {
              aggregatedPartSize += result.get().partSize();
            }

            return aggregatedPartSize;
          }

          @Override
          public boolean hasNext() {
            if (this.completed) {
              return false;
            }

            if (this.error == null && this.uploadIterator == null) {
              populate();
            }

            if (this.error == null
                && !this.uploadIterator.hasNext()
                && this.listMultipartUploadsResult.isTruncated()) {
              this.nextKeyMarker = this.listMultipartUploadsResult.nextKeyMarker();
              this.nextUploadIdMarker = this.listMultipartUploadsResult.nextUploadIdMarker();
              populate();
            }

            if (this.error != null) {
              return true;
            }

            if (this.uploadIterator.hasNext()) {
              return true;
            }

            this.completed = true;
            return false;
          }

          @Override
          public Result<Upload> next() {
            if (this.completed) {
              throw new NoSuchElementException();
            }

            if (this.error == null && this.uploadIterator == null) {
              populate();
            }

            if (this.error == null
                && !this.uploadIterator.hasNext()
                && this.listMultipartUploadsResult.isTruncated()) {
              this.nextKeyMarker = this.listMultipartUploadsResult.nextKeyMarker();
              this.nextUploadIdMarker = this.listMultipartUploadsResult.nextUploadIdMarker();
              populate();
            }

            if (this.error != null) {
              this.completed = true;
              return this.error;
            }

            if (this.uploadIterator.hasNext()) {
              Upload upload = this.uploadIterator.next();

              if (aggregatePartSize) {
                long aggregatedPartSize;

                try {
                  aggregatedPartSize =
                      getAggregatedPartSize(upload.objectName(), upload.uploadId());
                } catch (ErrorResponseException
                    | IllegalArgumentException
                    | InsufficientDataException
                    | InternalException
                    | InvalidBucketNameException
                    | InvalidKeyException
                    | InvalidResponseException
                    | IOException
                    | NoSuchAlgorithmException
                    | ServerException
                    | XmlParserException e) {
                  // special case: ignore the error as we can't propagate the exception in next()
                  aggregatedPartSize = -1;
                }

                upload.setAggregatedPartSize(aggregatedPartSize);
              }

              return new Result<>(upload);
            }

            this.completed = true;
            throw new NoSuchElementException();
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  /**
   * Executes List object parts of multipart upload for given bucket name, object name and upload ID
   * and returns Iterable<Result<Part>>.
   */
  private Iterable<Result<Part>> listObjectParts(
      final String bucketName, final String objectName, final String uploadId) {
    return new Iterable<Result<Part>>() {
      @Override
      public Iterator<Result<Part>> iterator() {
        return new Iterator<Result<Part>>() {
          private int nextPartNumberMarker;
          private ListPartsResult listPartsResult;
          private Result<Part> error;
          private Iterator<Part> partIterator;
          private boolean completed = false;

          private synchronized void populate() {
            this.listPartsResult = null;
            this.partIterator = null;

            try {
              this.listPartsResult =
                  listParts(bucketName, objectName, null, nextPartNumberMarker, uploadId);
            } catch (ErrorResponseException
                | IllegalArgumentException
                | InsufficientDataException
                | InternalException
                | InvalidBucketNameException
                | InvalidKeyException
                | InvalidResponseException
                | IOException
                | NoSuchAlgorithmException
                | ServerException
                | XmlParserException e) {
              this.error = new Result<>(e);
            } finally {
              if (this.listPartsResult != null) {
                this.partIterator = this.listPartsResult.partList().iterator();
              } else {
                this.partIterator = new LinkedList<Part>().iterator();
              }
            }
          }

          @Override
          public boolean hasNext() {
            if (this.completed) {
              return false;
            }

            if (this.error == null && this.partIterator == null) {
              populate();
            }

            if (this.error == null
                && !this.partIterator.hasNext()
                && this.listPartsResult.isTruncated()) {
              this.nextPartNumberMarker = this.listPartsResult.nextPartNumberMarker();
              populate();
            }

            if (this.error != null) {
              return true;
            }

            if (this.partIterator.hasNext()) {
              return true;
            }

            this.completed = true;
            return false;
          }

          @Override
          public Result<Part> next() {
            if (this.completed) {
              throw new NoSuchElementException();
            }

            if (this.error == null && this.partIterator == null) {
              populate();
            }

            if (this.error == null
                && !this.partIterator.hasNext()
                && this.listPartsResult.isTruncated()) {
              this.nextPartNumberMarker = this.listPartsResult.nextPartNumberMarker();
              populate();
            }

            if (this.error != null) {
              this.completed = true;
              return this.error;
            }

            if (this.partIterator.hasNext()) {
              return new Result<>(this.partIterator.next());
            }

            this.completed = true;
            throw new NoSuchElementException();
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  /**
   * Removes incomplete uploads of an object.
   *
   * <pre>Example:{@code
   * minioClient.removeIncompleteUpload("my-bucketname", "my-objectname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #removeIncompleteUpload(RemoveIncompleteUploadArgs)}
   */
  @Deprecated
  public void removeIncompleteUpload(String bucketName, String objectName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    removeIncompleteUpload(
        RemoveIncompleteUploadArgs.builder().bucket(bucketName).object(objectName).build());
  }

  /**
   * Removes incomplete uploads of an object.
   *
   * <pre>Example:{@code
   * minioClient.removeIncompleteUpload(
   *     RemoveIncompleteUploadArgs.builder()
   *     .bucket("my-bucketname")
   *     .object("my-objectname")
   *     .build());
   * }</pre>
   *
   * @param args instance of {@link RemoveIncompleteUploadArgs}
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void removeIncompleteUpload(RemoveIncompleteUploadArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    for (Result<Upload> r :
        listIncompleteUploads(
            ListIncompleteUploadsArgs.builder()
                .bucket(args.bucket())
                .prefix(args.object())
                .recursive(true)
                .build(),
            false)) {

      // args.bucket(), args.object(), true, false)) {
      Upload upload = r.get();
      if (args.object().equals(upload.objectName())) {
        abortMultipartUpload(args.bucket(), args.object(), upload.uploadId());
        return;
      }
    }
  }

  /**
   * Listens events of object prefix and suffix of a bucket. The returned closable iterator is
   * lazily evaluated hence its required to iterate to get new records and must be used with
   * try-with-resource to release underneath network resources.
   *
   * <pre>Example:{@code
   * String[] events = {"s3:ObjectCreated:*", "s3:ObjectAccessed:*"};
   * try (CloseableIterator<Result<NotificationInfo>> ci =
   *     minioClient.listenBucketNotification("bucketName", "", "", events)) {
   *   while (ci.hasNext()) {
   *     NotificationRecords records = ci.next().get();
   *     for (Event event : records.events()) {
   *       System.out.println("Event " + event.eventType() + " occurred at "
   *           + event.eventTime() + " for " + event.bucketName() + "/"
   *           + event.objectName());
   *     }
   *   }
   * }
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param prefix Listen events of object starts with prefix.
   * @param suffix Listen events of object ends with suffix.
   * @param events Events to listen.
   * @return CloseableIterator&ltResult&ltNotificationRecords&gt&gt - Lazy closable iterator
   *     contains event records.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #listenBucketNotification(ListenBucketNotificationArgs)}
   */
  @Deprecated
  public CloseableIterator<Result<NotificationRecords>> listenBucketNotification(
      String bucketName, String prefix, String suffix, String[] events)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return listenBucketNotification(
        ListenBucketNotificationArgs.builder()
            .bucket(bucketName)
            .prefix(prefix)
            .suffix(suffix)
            .events(events)
            .build());
  }

  /**
   * Listens events of object prefix and suffix of a bucket. The returned closable iterator is
   * lazily evaluated hence its required to iterate to get new records and must be used with
   * try-with-resource to release underneath network resources.
   *
   * <pre>Example:{@code
   * String[] events = {"s3:ObjectCreated:*", "s3:ObjectAccessed:*"};
   * try (CloseableIterator<Result<NotificationRecords>> ci =
   *     minioClient.listenBucketNotification(
   *         ListenBucketNotificationArgs.builder()
   *             .bucket("bucketName")
   *             .prefix("")
   *             .suffix("")
   *             .events(events)
   *             .build())) {
   *   while (ci.hasNext()) {
   *     NotificationRecords records = ci.next().get();
   *     for (Event event : records.events()) {
   *       System.out.println("Event " + event.eventType() + " occurred at "
   *           + event.eventTime() + " for " + event.bucketName() + "/"
   *           + event.objectName());
   *     }
   *   }
   * }
   * }</pre>
   *
   * @param args {@link ListenBucketNotificationArgs} object.
   * @return CloseableIterator&ltResult&ltNotificationRecords&gt&gt - Lazy closable iterator
   *     contains event records.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public CloseableIterator<Result<NotificationRecords>> listenBucketNotification(
      ListenBucketNotificationArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Multimap<String, String> queryParamMap = HashMultimap.create();
    queryParamMap.put("prefix", args.prefix());
    queryParamMap.put("suffix", args.suffix());
    for (String event : args.events()) {
      queryParamMap.put("events", event);
    }

    Response response = executeGet(args.bucket(), "", queryParamMap);
    NotificationResultRecords result = new NotificationResultRecords(response);
    return result.closeableIterator();
  }

  /**
   * Selects content of an object by SQL expression.
   *
   * <pre>Example:{@code
   * String sqlExpression = "select * from S3Object";
   * InputSerialization is =
   *     new InputSerialization(null, false, null, null, FileHeaderInfo.USE, null, null,
   *         null);
   * OutputSerialization os =
   *     new OutputSerialization(null, null, null, QuoteFields.ASNEEDED, null);
   * SelectResponseStream stream =
   *     minioClient.selectObjectContent("my-bucketname", "my-objectName", sqlExpression,
   *         is, os, true, null, null, null);
   *
   * byte[] buf = new byte[512];
   * int bytesRead = stream.read(buf, 0, buf.length);
   * System.out.println(new String(buf, 0, bytesRead, StandardCharsets.UTF_8));
   *
   * Stats stats = stream.stats();
   * System.out.println("bytes scanned: " + stats.bytesScanned());
   * System.out.println("bytes processed: " + stats.bytesProcessed());
   * System.out.println("bytes returned: " + stats.bytesReturned());
   *
   * stream.close();
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param sqlExpression SQL expression.
   * @param is Input specification of object data.
   * @param os Output specification of result.
   * @param requestProgress Flag to request progress information.
   * @param scanStartRange scan start range of the object.
   * @param scanEndRange scan end range of the object.
   * @param ssec SSE-C type server-side encryption.
   * @return {@link SelectResponseStream} - Contains filtered records and progress.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   * @deprecated use {@link #selectObjectContent(SelectObjectContentArgs)}
   */
  @Deprecated
  public SelectResponseStream selectObjectContent(
      String bucketName,
      String objectName,
      String sqlExpression,
      InputSerialization is,
      OutputSerialization os,
      boolean requestProgress,
      Long scanStartRange,
      Long scanEndRange,
      ServerSideEncryptionCustomerKey ssec)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return selectObjectContent(
        SelectObjectContentArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .sqlExpression(sqlExpression)
            .inputSerialization(is)
            .outputSerialization(os)
            .requestProgress(requestProgress)
            .scanStartRange(scanStartRange)
            .scanEndRange(scanEndRange)
            .build());
  }

  /**
   * Selects content of an object by SQL expression.
   *
   * <pre>Example:{@code
   * String sqlExpression = "select * from S3Object";
   * InputSerialization is =
   *     new InputSerialization(null, false, null, null, FileHeaderInfo.USE, null, null,
   *         null);
   * OutputSerialization os =
   *     new OutputSerialization(null, null, null, QuoteFields.ASNEEDED, null);
   * SelectResponseStream stream =
   *     minioClient.selectObjectContent(
   *       SelectObjectContentArgs.builder()
   *       .bucket("my-bucketname")
   *       .object("my-objectname")
   *       .sqlExpression(sqlExpression)
   *       .inputSerialization(is)
   *       .outputSerialization(os)
   *       .requestProgress(true)
   *       .build());
   *
   * byte[] buf = new byte[512];
   * int bytesRead = stream.read(buf, 0, buf.length);
   * System.out.println(new String(buf, 0, bytesRead, StandardCharsets.UTF_8));
   *
   * Stats stats = stream.stats();
   * System.out.println("bytes scanned: " + stats.bytesScanned());
   * System.out.println("bytes processed: " + stats.bytesProcessed());
   * System.out.println("bytes returned: " + stats.bytesReturned());
   *
   * stream.close();
   * }</pre>
   *
   * @param args instance of {@link SelectObjectContentArgs}
   * @return {@link SelectResponseStream} - Contains filtered records and progress.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public SelectResponseStream selectObjectContent(SelectObjectContentArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    args.validateSsec(this.baseUrl);

    Map<String, String> headerMap = null;
    if (args.ssec() != null) {
      headerMap = args.ssec().headers();
    }

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("select", "");
    queryParamMap.put("select-type", "2");

    SelectObjectContentRequest request =
        new SelectObjectContentRequest(
            args.sqlExpression(),
            args.requestProgress(),
            args.inputSerialization(),
            args.outputSerialization(),
            args.scanStartRange(),
            args.scanEndRange());
    Response response =
        executePost(args.bucket(), args.object(), headerMap, queryParamMap, request);
    return new SelectResponseStream(response.body().byteStream());
  }

  /**
   * Sets encryption configuration of a bucket.
   *
   * <pre>Example:{@code
   * minioClient.setBucketEncryption(
   *     SetBucketEncryptionArgs.builder().bucket("my-bucketname").config(config).build());
   * }</pre>
   *
   * @param args {@link SetBucketEncryptionArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void setBucketEncryption(SetBucketEncryptionArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("encryption", "");
    Response response = executePut(args.bucket(), null, null, queryParamMap, args.config(), 0);
    response.close();
  }

  /**
   * Gets encryption configuration of a bucket.
   *
   * <pre>Example:{@code
   * SseConfiguration config =
   *     minioClient.getBucketEncryption(
   *         GetBucketEncryptionArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketEncryptionArgs} object.
   * @return {@link SseConfiguration} - Server-side encryption configuration.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public SseConfiguration getBucketEncryption(GetBucketEncryptionArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("encryption", "");
    try (Response response = executeGet(args.bucket(), null, null, queryParamMap)) {
      return Xml.unmarshal(SseConfiguration.class, response.body().charStream());
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode()
          != ErrorCode.SERVER_SIDE_ENCRYPTION_CONFIGURATION_NOT_FOUND_ERROR) {
        throw e;
      }
    }

    return new SseConfiguration();
  }

  /**
   * Deletes encryption configuration of a bucket.
   *
   * <pre>Example:{@code
   * minioClient.deleteBucketEncryption(
   *     DeleteBucketEncryptionArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DeleteBucketEncryptionArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void deleteBucketEncryption(DeleteBucketEncryptionArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("encryption", "");
    try {
      Response response = executeDelete(args.bucket(), "", queryParamMap);
      response.close();
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode()
          != ErrorCode.SERVER_SIDE_ENCRYPTION_CONFIGURATION_NOT_FOUND_ERROR) {
        throw e;
      }
    }
  }

  /**
   * Gets tags of a bucket.
   *
   * <pre>Example:{@code
   * Tags tags =
   *     minioClient.getBucketTags(GetBucketTagsArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link GetBucketTagsArgs} object.
   * @return {@link Tags} - Tags.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public Tags getBucketTags(GetBucketTagsArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("tagging", "");

    try (Response response = executeGet(args.bucket(), null, null, queryParamMap)) {
      return Xml.unmarshal(Tags.class, response.body().charStream());
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_TAG_SET) {
        throw e;
      }
    }

    return new Tags();
  }

  /**
   * Sets tags to a bucket.
   *
   * <pre>Example:{@code
   * Map<String, String> map = new HashMap<>();
   * map.put("Project", "Project One");
   * map.put("User", "jsmith");
   * minioClient.setBucketTags(
   *     SetBucketTagsArgs.builder().bucket("my-bucketname").tags(map).build());
   * }</pre>
   *
   * @param args {@link SetBucketTagsArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void setBucketTags(SetBucketTagsArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("tagging", "");
    Response response = executePut(args.bucket(), null, null, queryParamMap, args.tags(), 0);
    response.close();
  }

  /**
   * Deletes tags of a bucket.
   *
   * <pre>Example:{@code
   * minioClient.deleteBucketTags(DeleteBucketTagsArgs.builder().bucket("my-bucketname").build());
   * }</pre>
   *
   * @param args {@link DeleteBucketTagsArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void deleteBucketTags(DeleteBucketTagsArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("tagging", "");
    Response response = executeDelete(args.bucket(), null, queryParamMap);
    response.close();
  }

  /**
   * Gets tags of an object.
   *
   * <pre>Example:{@code
   * Tags tags =
   *     minioClient.getObjectTags(
   *         GetObjectTagsArgs.builder().bucket("my-bucketname").object("my-objectname").build());
   * }</pre>
   *
   * @param args {@link GetObjectTagsArgs} object.
   * @return {@link Tags} - Tags.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public Tags getObjectTags(GetObjectTagsArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("tagging", "");

    try (Response response = executeGet(args.bucket(), args.object(), null, queryParamMap)) {
      return Xml.unmarshal(Tags.class, response.body().charStream());
    }
  }

  /**
   * Sets tags to an object.
   *
   * <pre>Example:{@code
   * Map<String, String> map = new HashMap<>();
   * map.put("Project", "Project One");
   * map.put("User", "jsmith");
   * minioClient.setObjectTags(
   *     SetObjectTagsArgs.builder()
   *         .bucket("my-bucketname")
   *         .object("my-objectname")
   *         .tags((map)
   *         .build());
   * }</pre>
   *
   * @param args {@link SetObjectTagsArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void setObjectTags(SetObjectTagsArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("tagging", "");
    Response response =
        executePut(args.bucket(), args.object(), null, queryParamMap, args.tags(), 0);
    response.close();
  }

  /**
   * Deletes tags of an object.
   *
   * <pre>Example:{@code
   * minioClient.deleteObjectTags(
   *     DeleteObjectTags.builder().bucket("my-bucketname").object("my-objectname").build());
   * }</pre>
   *
   * @param args {@link DeleteObjectTagsArgs} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  public void deleteObjectTags(DeleteObjectTagsArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("tagging", "");
    Response response = executeDelete(args.bucket(), args.object(), queryParamMap);
    response.close();
  }

  private long getAvailableSize(Object data, long expectedReadSize)
      throws IOException, InternalException {
    if (!(data instanceof BufferedInputStream)) {
      throw new InternalException(
          "data must be BufferedInputStream. This should not happen.  "
              + "Please report to https://github.com/minio/minio-java/issues/");
    }

    BufferedInputStream stream = (BufferedInputStream) data;
    stream.mark((int) expectedReadSize);

    byte[] buf = new byte[16384]; // 16KiB buffer for optimization
    long totalBytesRead = 0;
    while (totalBytesRead < expectedReadSize) {
      long bytesToRead = expectedReadSize - totalBytesRead;
      if (bytesToRead > buf.length) {
        bytesToRead = buf.length;
      }

      int bytesRead = stream.read(buf, 0, (int) bytesToRead);
      if (bytesRead < 0) {
        break; // reached EOF
      }

      totalBytesRead += bytesRead;
    }

    stream.reset();
    return totalBytesRead;
  }

  /**
   * Sets HTTP connect, write and read timeouts. A value of 0 means no timeout, otherwise values
   * must be between 1 and Integer.MAX_VALUE when converted to milliseconds.
   *
   * <pre>Example:{@code
   * minioClient.setTimeout(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10),
   *     TimeUnit.SECONDS.toMillis(30));
   * }</pre>
   *
   * @param connectTimeout HTTP connect timeout in milliseconds.
   * @param writeTimeout HTTP write timeout in milliseconds.
   * @param readTimeout HTTP read timeout in milliseconds.
   */
  public void setTimeout(long connectTimeout, long writeTimeout, long readTimeout) {
    this.httpClient =
        this.httpClient
            .newBuilder()
            .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
            .build();
  }

  /**
   * Ignores check on server certificate for HTTPS connection.
   *
   * <pre>Example:{@code
   * minioClient.ignoreCertCheck();
   * }</pre>
   *
   * @throws KeyManagementException thrown to indicate key management error.
   * @throws NoSuchAlgorithmException thrown to indicate missing of SSL library.
   */
  @SuppressFBWarnings(value = "SIC", justification = "Should not be used in production anyways.")
  public void ignoreCertCheck() throws KeyManagementException, NoSuchAlgorithmException {
    final TrustManager[] trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {}

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[] {};
            }
          }
        };

    final SSLContext sslContext = SSLContext.getInstance("SSL");
    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

    this.httpClient =
        this.httpClient
            .newBuilder()
            .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
            .hostnameVerifier(
                new HostnameVerifier() {
                  @Override
                  public boolean verify(String hostname, SSLSession session) {
                    return true;
                  }
                })
            .build();
  }

  /**
   * Sets application's name/version to user agent. For more information about user agent refer <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">#rfc2616</a>.
   *
   * @param name Your application name.
   * @param version Your application version.
   */
  @SuppressWarnings("unused")
  public void setAppInfo(String name, String version) {
    if (name == null || version == null) {
      // nothing to do
      return;
    }

    this.userAgent = DEFAULT_USER_AGENT + " " + name.trim() + "/" + version.trim();
  }

  /**
   * Enables HTTP call tracing and written to traceStream.
   *
   * @param traceStream {@link OutputStream} for writing HTTP call tracing.
   * @see #traceOff
   */
  public void traceOn(OutputStream traceStream) {
    if (traceStream == null) {
      throw new NullPointerException();
    } else {
      this.traceStream =
          new PrintWriter(new OutputStreamWriter(traceStream, StandardCharsets.UTF_8), true);
    }
  }

  /**
   * Disables HTTP call tracing previously enabled.
   *
   * @see #traceOn
   * @throws IOException upon connection error
   */
  public void traceOff() throws IOException {
    this.traceStream = null;
  }

  /** Enables accelerate endpoint for Amazon S3 endpoint. */
  public void enableAccelerateEndpoint() {
    this.isAcceleratedHost = true;
  }

  /** Disables accelerate endpoint for Amazon S3 endpoint. */
  public void disableAccelerateEndpoint() {
    this.isAcceleratedHost = false;
  }

  /** Enables dual-stack endpoint for Amazon S3 endpoint. */
  public void enableDualStackEndpoint() {
    this.isDualStackHost = true;
  }

  /** Disables dual-stack endpoint for Amazon S3 endpoint. */
  public void disableDualStackEndpoint() {
    this.isDualStackHost = false;
  }

  /** Enables virtual-style endpoint. */
  public void enableVirtualStyleEndpoint() {
    this.useVirtualStyle = true;
  }

  /** Disables virtual-style endpoint. */
  public void disableVirtualStyleEndpoint() {
    this.useVirtualStyle = false;
  }

  private static class NotificationResultRecords {
    Response response = null;
    Scanner scanner = null;
    ObjectMapper mapper = null;

    public NotificationResultRecords(Response response) {
      this.response = response;
      this.scanner = new Scanner(response.body().charStream()).useDelimiter("\n");
      this.mapper = new ObjectMapper();
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    }

    /** returns closeable iterator of result of notification records. */
    public CloseableIterator<Result<NotificationRecords>> closeableIterator() {
      return new CloseableIterator<Result<NotificationRecords>>() {
        String recordsString = null;
        NotificationRecords records = null;
        boolean isClosed = false;

        @Override
        public void close() throws IOException {
          if (!isClosed) {
            try {
              response.body().close();
              scanner.close();
            } finally {
              isClosed = true;
            }
          }
        }

        public boolean populate() {
          if (isClosed) {
            return false;
          }

          if (recordsString != null) {
            return true;
          }

          while (scanner.hasNext()) {
            recordsString = scanner.next().trim();
            if (!recordsString.equals("")) {
              break;
            }
          }

          if (recordsString == null || recordsString.equals("")) {
            try {
              close();
            } catch (IOException e) {
              isClosed = true;
            }
            return false;
          }
          return true;
        }

        @Override
        public boolean hasNext() {
          return populate();
        }

        @Override
        public Result<NotificationRecords> next() {
          if (isClosed) {
            throw new NoSuchElementException();
          }
          if ((recordsString == null || recordsString.equals("")) && !populate()) {
            throw new NoSuchElementException();
          }

          try {
            records = mapper.readValue(recordsString, NotificationRecords.class);
            return new Result<>(records);
          } catch (JsonMappingException e) {
            return new Result<>(e);
          } catch (JsonParseException e) {
            return new Result<>(e);
          } catch (IOException e) {
            return new Result<>(e);
          } finally {
            recordsString = null;
            records = null;
          }
        }
      };
    }
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html">AbortMultipartUpload
   * S3 API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param uploadId Upload ID.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  protected void abortMultipartUpload(String bucketName, String objectName, String uploadId)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put(UPLOAD_ID, uploadId);
    executeDelete(bucketName, objectName, queryParamMap);
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html">CompleteMultipartUpload
   * S3 API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param parts List of parts.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  @Deprecated
  protected void completeMultipartUpload(
      String bucketName, String objectName, String uploadId, Part[] parts)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    completeMultipartUpload(bucketName, null, objectName, uploadId, parts, null, null);
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html">CompleteMultipartUpload
   * S3 API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param region Region of the bucket.
   * @param objectName Object name in the bucket.
   * @param uploadId Upload ID.
   * @param parts List of parts.
   * @param extraHeaders Extra headers.
   * @param extraQueryParams Extra query parameters.
   * @return {@link ObjectWriteResponse} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  protected ObjectWriteResponse completeMultipartUpload(
      String bucketName,
      String region,
      String objectName,
      String uploadId,
      Part[] parts,
      Multimap<String, String> extraHeaders,
      Multimap<String, String> extraQueryParams)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Multimap<String, String> queryParams = HashMultimap.create();
    if (extraQueryParams != null) {
      queryParams.putAll(extraQueryParams);
    }
    queryParams.put(UPLOAD_ID, uploadId);

    try (Response response =
        execute(
            Method.POST,
            bucketName,
            objectName,
            (region != null) ? region : getRegion(bucketName),
            extraHeaders,
            queryParams,
            new CompleteMultipartUpload(parts),
            0)) {
      String etag = null;

      String bodyContent = new String(response.body().bytes(), StandardCharsets.UTF_8);
      bodyContent = bodyContent.trim();
      if (!bodyContent.isEmpty()) {
        try {
          if (Xml.validate(ErrorResponse.class, bodyContent)) {
            ErrorResponse errorResponse = Xml.unmarshal(ErrorResponse.class, bodyContent);
            throw new ErrorResponseException(errorResponse, response);
          }
        } catch (XmlParserException e) {
          // As it is not <Error> message, fall-back to parse CompleteMultipartUploadOutput XML.
        }

        try {
          CompleteMultipartUploadOutput result =
              Xml.unmarshal(CompleteMultipartUploadOutput.class, bodyContent);
          etag = result.etag();
        } catch (XmlParserException e) {
          // As this CompleteMultipartUpload REST call succeeded, just log it.
          Logger.getLogger(MinioClient.class.getName())
              .warning(
                  "S3 service returned unknown XML for CompleteMultipartUpload REST API. "
                      + bodyContent);
        }
      }

      return new ObjectWriteResponse(response.headers(), etag, response.header("x-amz-version-id"));
    }
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload
   * S3 API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param headerMap Additional headers.
   * @return String - Contains upload ID.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  @Deprecated
  protected String createMultipartUpload(
      String bucketName, String objectName, Map<String, String> headerMap)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    return createMultipartUpload(
        bucketName,
        null,
        objectName,
        (headerMap != null) ? Multimaps.forMap(headerMap) : null,
        null);
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload
   * S3 API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param headers Request headers.
   * @return String - Contains upload ID.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  protected String createMultipartUpload(
      String bucketName,
      String region,
      String objectName,
      Multimap<String, String> headers,
      Multimap<String, String> extraQueryParams)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Multimap<String, String> queryParams = HashMultimap.create();
    if (extraQueryParams != null) {
      queryParams.putAll(extraQueryParams);
    }
    queryParams.put("uploads", "");

    Multimap<String, String> headersCopy = HashMultimap.create();
    if (headers != null) {
      headersCopy.putAll(headers);
    }
    // set content type if not set already
    if (!headersCopy.containsKey("Content-Type")) {
      headersCopy.put("Content-Type", "application/octet-stream");
    }

    try (Response response =
        execute(
            Method.POST,
            bucketName,
            objectName,
            (region != null) ? region : getRegion(bucketName),
            headersCopy,
            queryParams,
            null,
            0)) {
      InitiateMultipartUploadResult result =
          Xml.unmarshal(InitiateMultipartUploadResult.class, response.body().charStream());
      return result.uploadId();
    }
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjects.html">DeleteObjects S3
   * API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param objectList List of object names.
   * @param quiet Quiet flag.
   * @return {@link DeleteResult} - Contains delete result.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  protected DeleteResult deleteObjects(
      String bucketName, List<DeleteObject> objectList, boolean quiet, boolean bypassGovernanceMode)
      throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
          IOException, InvalidKeyException, ServerException, XmlParserException,
          ErrorResponseException, InternalException, InvalidResponseException {
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("delete", "");

    Map<String, String> headers = null;
    if (bypassGovernanceMode) {
      headers = new HashMap<>();
      headers.put("x-amz-bypass-governance-retention", "true");
    }

    DeleteRequest request = new DeleteRequest(objectList, quiet);
    try (Response response = executePost(bucketName, null, headers, queryParams, request)) {
      String bodyContent = new String(response.body().bytes(), StandardCharsets.UTF_8);
      try {
        if (Xml.validate(DeleteError.class, bodyContent)) {
          DeleteError error = Xml.unmarshal(DeleteError.class, bodyContent);
          return new DeleteResult(error);
        }
      } catch (XmlParserException e) {
        // As it is not <Error> message, parse it as <DeleteResult> message.
        // Ignore this exception
      }

      return Xml.unmarshal(DeleteResult.class, bodyContent);
    }
  }

  private Map<String, String> getCommonListObjectsQueryParams(ListObjectsArgs args) {
    Map<String, String> queryParamMap = new HashMap<>();

    if (args.delimiter() != null) {
      queryParamMap.put("delimiter", args.delimiter());
    } else {
      queryParamMap.put("delimiter", "");
    }

    if (args.maxKeys() != null) {
      queryParamMap.put("max-keys", args.maxKeys().toString());
    }

    if (args.prefix() != null) {
      queryParamMap.put("prefix", args.prefix());
    } else {
      queryParamMap.put("prefix", "");
    }

    return queryParamMap;
  }

  private ListBucketResultV2 invokeListObjectsV2(ListObjectsArgs args)
      throws InvalidKeyException, InvalidBucketNameException, IllegalArgumentException,
          NoSuchAlgorithmException, InsufficientDataException, ServerException, XmlParserException,
          ErrorResponseException, InternalException, InvalidResponseException, IOException {
    Map<String, String> queryParamMap = getCommonListObjectsQueryParams(args);
    queryParamMap.put("list-type", "2");

    if (args.continuationToken() != null) {
      queryParamMap.put("continuation-token", args.continuationToken());
    }

    if (args.fetchOwner()) {
      queryParamMap.put("fetch-owner", "true");
    }

    if (args.startAfter() != null) {
      queryParamMap.put("start-after", args.startAfter());
    }

    if (args.includeUserMetadata()) {
      queryParamMap.put("metadata", "true");
    }

    Response response = executeGet(args.bucket(), null, null, queryParamMap);

    try (ResponseBody body = response.body()) {
      return Xml.unmarshal(ListBucketResultV2.class, body.charStream());
    }
  }

  private ListBucketResultV1 invokeListObjectsV1(ListObjectsArgs args)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Map<String, String> queryParamMap = getCommonListObjectsQueryParams(args);

    if (args.startAfter() != null) {
      queryParamMap.put("marker", args.startAfter());
    }

    Response response = executeGet(args.bucket(), null, null, queryParamMap);

    try (ResponseBody body = response.body()) {
      return Xml.unmarshal(ListBucketResultV1.class, body.charStream());
    }
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject S3
   * API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param data Object data must be BufferedInputStream, RandomAccessFile, byte[] or String.
   * @param length Length of object data.
   * @param headerMap Additional headers.
   * @return String - Contains ETag.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  @Deprecated
  protected String putObject(
      String bucketName, String objectName, Object data, int length, Map<String, String> headerMap)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    ObjectWriteResponse reply =
        putObject(
            bucketName,
            null,
            objectName,
            data,
            length,
            (headerMap != null) ? Multimaps.forMap(headerMap) : null,
            null);
    return reply.etag();
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject S3
   * API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param data Object data must be BufferedInputStream, RandomAccessFile, byte[] or String.
   * @param length Length of object data.
   * @param headers Additional headers.
   * @param extraQueryParams Additional query parameters if any.
   * @return {@link ObjectWriteResponse} object.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  protected ObjectWriteResponse putObject(
      String bucketName,
      String region,
      String objectName,
      Object data,
      int length,
      Multimap<String, String> headers,
      Multimap<String, String> extraQueryParams)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    if (!(data instanceof BufferedInputStream
        || data instanceof RandomAccessFile
        || data instanceof byte[]
        || data instanceof CharSequence)) {
      throw new IllegalArgumentException(
          "data must be BufferedInputStream, RandomAccessFile, byte[] or String");
    }

    try (Response response =
        execute(
            Method.PUT,
            bucketName,
            objectName,
            (region != null) ? region : getRegion(bucketName),
            headers,
            extraQueryParams,
            data,
            length)) {
      return new ObjectWriteResponse(
          response.headers(),
          response.header("ETag").replaceAll("\"", ""),
          response.header("x-amz-version-id"));
    }
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListMultipartUploads.html">ListMultipartUploads
   * S3 API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param delimiter Delimiter.
   * @param keyMarker Key marker.
   * @param maxUploads Maximum upload information to fetch.
   * @param prefix Prefix.
   * @param uploadIdMarker Upload ID marker.
   * @return {@link ListMultipartUploadsResult} - Contains uploads information.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  protected ListMultipartUploadsResult listMultipartUploads(
      String bucketName,
      String delimiter,
      String keyMarker,
      Integer maxUploads,
      String prefix,
      String uploadIdMarker)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("uploads", "");

    if (delimiter != null) {
      queryParamMap.put("delimiter", delimiter);
    } else {
      queryParamMap.put("delimiter", "");
    }

    if (keyMarker != null) {
      queryParamMap.put("key-marker", keyMarker);
    }

    if (maxUploads != null) {
      queryParamMap.put("max-uploads", Integer.toString(maxUploads));
    }

    if (prefix != null) {
      queryParamMap.put("prefix", prefix);
    } else {
      queryParamMap.put("prefix", "");
    }

    if (uploadIdMarker != null) {
      queryParamMap.put("upload-id-marker", uploadIdMarker);
    }

    // Setting it as default to encode the object keys in the response
    queryParamMap.put("encoding-type", "url");

    Response response = executeGet(bucketName, null, null, queryParamMap);

    try (ResponseBody body = response.body()) {
      return Xml.unmarshal(ListMultipartUploadsResult.class, body.charStream());
    }
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListParts.html">ListParts S3
   * API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param maxParts Maximum parts information to fetch.
   * @param partNumberMarker Part number marker.
   * @param uploadId Upload ID.
   * @return {@link ListPartsResult} - Contains parts information.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  protected ListPartsResult listParts(
      String bucketName,
      String objectName,
      Integer maxParts,
      Integer partNumberMarker,
      String uploadId)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Map<String, String> queryParamMap = new HashMap<>();

    if (maxParts != null) {
      queryParamMap.put("max-parts", Integer.toString(maxParts));
    }

    if (partNumberMarker != null) {
      queryParamMap.put("part-number-marker", Integer.toString(partNumberMarker));
    }

    queryParamMap.put(UPLOAD_ID, uploadId);

    Response response = executeGet(bucketName, objectName, null, queryParamMap);

    try (ResponseBody body = response.body()) {
      return Xml.unmarshal(ListPartsResult.class, body.charStream());
    }
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart S3
   * API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param data Object data must be BufferedInputStream, RandomAccessFile, byte[] or String.
   * @param length Length of object data.
   * @param uploadId Upload ID.
   * @param partNumber Part number.
   * @param headerMap Additional headers.
   * @return String - Contains ETag.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  protected String uploadPart(
      String bucketName,
      String objectName,
      Object data,
      int length,
      String uploadId,
      int partNumber,
      Map<String, String> headerMap)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    if (!(data instanceof BufferedInputStream
        || data instanceof RandomAccessFile
        || data instanceof byte[]
        || data instanceof CharSequence)) {
      throw new IllegalArgumentException(
          "data must be BufferedInputStream, RandomAccessFile, byte[] or String");
    }

    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("partNumber", Integer.toString(partNumber));
    queryParamMap.put(UPLOAD_ID, uploadId);

    Response response = executePut(bucketName, objectName, headerMap, queryParamMap, data, length);
    response.close();
    return response.header("ETag").replaceAll("\"", "");
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy
   * S3 API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param uploadId Upload ID.
   * @param partNumber Part number.
   * @param headerMap Source object definitions.
   * @return String - Contains ETag.
   * @throws ErrorResponseException thrown to indicate S3 service returned an error response.
   * @throws IllegalArgumentException throws to indicate invalid argument passed.
   * @throws InsufficientDataException thrown to indicate not enough data available in InputStream.
   * @throws InternalException thrown to indicate internal library error.
   * @throws InvalidBucketNameException thrown to indicate invalid bucket name passed.
   * @throws InvalidKeyException thrown to indicate missing of HMAC SHA-256 library.
   * @throws InvalidResponseException thrown to indicate S3 service returned invalid or no error
   *     response.
   * @throws IOException thrown to indicate I/O error on S3 operation.
   * @throws NoSuchAlgorithmException thrown to indicate missing of MD5 or SHA-256 digest library.
   * @throws XmlParserException thrown to indicate XML parsing error.
   */
  protected String uploadPartCopy(
      String bucketName,
      String objectName,
      String uploadId,
      int partNumber,
      Map<String, String> headerMap)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Map<String, String> queryParamMap = new HashMap<>();
    queryParamMap.put("partNumber", Integer.toString(partNumber));
    queryParamMap.put("uploadId", uploadId);
    Response response = executePut(bucketName, objectName, headerMap, queryParamMap, "", 0);
    try (ResponseBody body = response.body()) {
      CopyPartResult result = Xml.unmarshal(CopyPartResult.class, body.charStream());
      return result.etag();
    }
  }
}
