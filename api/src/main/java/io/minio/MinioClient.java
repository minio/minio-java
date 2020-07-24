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
import io.minio.credentials.Provider;
import io.minio.credentials.StaticProvider;
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
import io.minio.messages.Credentials;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteMarker;
import io.minio.messages.DeleteObject;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import io.minio.messages.ErrorResponse;
import io.minio.messages.InitiateMultipartUploadResult;
import io.minio.messages.InputSerialization;
import io.minio.messages.Item;
import io.minio.messages.LegalHold;
import io.minio.messages.ListAllMyBucketsResult;
import io.minio.messages.ListBucketResultV1;
import io.minio.messages.ListBucketResultV2;
import io.minio.messages.ListMultipartUploadsResult;
import io.minio.messages.ListObjectsResult;
import io.minio.messages.ListPartsResult;
import io.minio.messages.ListVersionsResult;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
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
 *
 * <p>Use {@code MinioClient.builder()} to create S3 client.
 *
 * <pre>{@code
 * // Create client with anonymous access.
 * MinioClient minioClient = MinioClient.builder().endpoint("https://play.min.io").build();
 *
 * // Create client with credentials.
 * MinioClient minioClient =
 *     MinioClient.builder()
 *         .endpoint("https://play.min.io")
 *         .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
 *         .build();
 * }</pre>
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class MinioClient {
  private static final String NO_SUCH_BUCKET_MESSAGE = "Bucket does not exist";
  private static final String NO_SUCH_BUCKET = "NoSuchBucket";
  private static final String NO_SUCH_BUCKET_POLICY = "NoSuchBucketPolicy";
  private static final String NO_SUCH_OBJECT_LOCK_CONFIGURATION = "NoSuchObjectLockConfiguration";
  private static final String RETRY_HEAD_BUCKET = "RetryHeadBucket";
  private static final String SERVER_SIDE_ENCRYPTION_CONFIGURATION_NOT_FOUND_ERROR =
      "ServerSideEncryptionConfigurationNotFoundError";

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

  private String userAgent = DEFAULT_USER_AGENT;
  private PrintWriter traceStream;

  private HttpUrl baseUrl;
  private String region;
  private boolean isAwsHost;
  private boolean isAcceleratedHost;
  private boolean isDualStackHost;
  private boolean useVirtualStyle;
  private Provider provider;
  private OkHttpClient httpClient;

  private MinioClient(
      HttpUrl baseUrl,
      String region,
      boolean isAwsHost,
      boolean isAcceleratedHost,
      boolean isDualStackHost,
      boolean useVirtualStyle,
      Provider provider,
      OkHttpClient httpClient) {
    this.baseUrl = baseUrl;
    this.region = region;
    this.isAwsHost = isAwsHost;
    this.isAcceleratedHost = isAcceleratedHost;
    this.isDualStackHost = isDualStackHost;
    this.useVirtualStyle = useVirtualStyle;
    this.provider = provider;
    this.httpClient = httpClient;
  }

  /** Remove this constructor when all deprecated contructors are removed. */
  private MinioClient(MinioClient client) {
    this.baseUrl = client.baseUrl;
    this.region = client.region;
    this.isAwsHost = client.isAwsHost;
    this.isAcceleratedHost = client.isAcceleratedHost;
    this.isDualStackHost = client.isDualStackHost;
    this.useVirtualStyle = client.useVirtualStyle;
    this.provider = client.provider;
    this.httpClient = client.httpClient;
  }

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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
   * @see #builder()
   */
  @Deprecated
  public MinioClient(String endpoint) throws IllegalArgumentException {
    this(builder().endpoint(endpoint).build());
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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
   * @see #builder()
   */
  @Deprecated
  public MinioClient(URL url) throws InvalidEndpointException, InvalidPortException {
    this(builder().endpoint(url).build());
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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
   * @see #builder()
   */
  @Deprecated
  public MinioClient(HttpUrl url) throws IllegalArgumentException {
    this(builder().endpoint(url).build());
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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
   * @see #builder()
   */
  @Deprecated
  public MinioClient(String endpoint, String accessKey, String secretKey)
      throws IllegalArgumentException {
    this(builder().endpoint(endpoint).credentials(accessKey, secretKey).build());
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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
   * @see #builder()
   */
  @Deprecated
  public MinioClient(String endpoint, String accessKey, String secretKey, String region)
      throws IllegalArgumentException {
    this(builder().endpoint(endpoint).region(region).credentials(accessKey, secretKey).build());
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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
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
  @Deprecated
  public MinioClient(URL url, String accessKey, String secretKey) throws IllegalArgumentException {
    this(builder().endpoint(url).credentials(accessKey, secretKey).build());
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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
   * @see #builder()
   */
  @Deprecated
  public MinioClient(HttpUrl url, String accessKey, String secretKey)
      throws IllegalArgumentException {
    this(builder().endpoint(url).credentials(accessKey, secretKey).build());
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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
   * @see #builder()
   */
  @Deprecated
  public MinioClient(String endpoint, int port, String accessKey, String secretKey)
      throws IllegalArgumentException {
    this(
        builder()
            .endpoint(endpoint, Integer.valueOf(port), null)
            .credentials(accessKey, secretKey)
            .build());
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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
   * @see #builder()
   */
  @Deprecated
  public MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
      throws IllegalArgumentException {
    this(
        builder()
            .endpoint(endpoint, null, Boolean.valueOf(secure))
            .credentials(accessKey, secretKey)
            .build());
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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
   * @see #builder()
   */
  @Deprecated
  public MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
      throws IllegalArgumentException {
    this(
        builder()
            .endpoint(endpoint, Integer.valueOf(port), Boolean.valueOf(secure))
            .credentials(accessKey, secretKey)
            .build());
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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
   * @see #builder()
   */
  @Deprecated
  public MinioClient(
      String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
      throws IllegalArgumentException {
    this(
        builder()
            .endpoint(endpoint, Integer.valueOf(port), Boolean.valueOf(secure))
            .region(region)
            .credentials(accessKey, secretKey)
            .build());
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
   * @deprecated As of 7.1.0, use {@link MinioClient.Builder} instead
   * @see #builder()
   */
  @Deprecated
  public MinioClient(
      String endpoint,
      Integer port,
      String accessKey,
      String secretKey,
      String region,
      Boolean secure,
      OkHttpClient httpClient)
      throws IllegalArgumentException {
    this(
        builder()
            .endpoint(endpoint, port, secure)
            .region(region)
            .credentials(accessKey, secretKey)
            .httpClient(httpClient)
            .build());
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

    if (!(sse instanceof ServerSideEncryptionCustomerKey)) {
      throw new IllegalArgumentException("only SSE_C is supported for all read requests.");
    }

    if (sse.tlsRequired() && !this.baseUrl.isHttps()) {
      throw new IllegalArgumentException(
          sse + "operations must be performed over a secure connection.");
    }
  }

  private Multimap<String, String> merge(Multimap<String, String> m1, Multimap<String, String> m2) {
    Multimap<String, String> map = HashMultimap.create();
    if (m1 != null) map.putAll(m1);
    if (m2 != null) map.putAll(m2);
    return map;
  }

  /** Create new HashMultimap by alternating keys and values. */
  private Multimap<String, String> newMultimap(String... keysAndValues) {
    if (keysAndValues.length % 2 != 0) {
      throw new IllegalArgumentException("Expected alternating keys and values");
    }

    Multimap<String, String> map = HashMultimap.create();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      map.put(keysAndValues[i], keysAndValues[i + 1]);
    }

    return map;
  }

  /** Create new HashMultimap with copy of Map. */
  private Multimap<String, String> newMultimap(Map<String, String> map) {
    return (map != null) ? Multimaps.forMap(map) : HashMultimap.create();
  }

  /** Create new HashMultimap with copy of Multimap. */
  private Multimap<String, String> newMultimap(Multimap<String, String> map) {
    return (map != null) ? HashMultimap.create(map) : HashMultimap.create();
  }

  /** Build URL for given parameters. */
  protected HttpUrl buildUrl(
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

  /** Create HTTP request for given paramaters. */
  protected Request createRequest(
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
    Credentials creds = provider != null ? provider.fetch() : null;
    if (creds != null && !creds.isEmpty()) {
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

    if (creds != null && creds.sessionToken() != null) {
      requestBuilder.header("X-Amz-Security-Token", creds.sessionToken());
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

  /** Execute HTTP request for given args and parameters. */
  protected Response execute(
      Method method,
      BaseArgs args,
      Multimap<String, String> headers,
      Multimap<String, String> queryParams,
      Object body,
      int length)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    String bucketName = null;
    String region = null;
    String objectName = null;

    if (args instanceof BucketArgs) {
      bucketName = ((BucketArgs) args).bucket();
      region = ((BucketArgs) args).region();
    }

    if (args instanceof ObjectArgs) {
      objectName = ((ObjectArgs) args).object();
    }

    return execute(
        method,
        bucketName,
        objectName,
        getRegion(bucketName, region),
        merge(args.extraHeaders(), headers),
        merge(args.extraQueryParams(), queryParams),
        body,
        length);
  }

  /** Execute HTTP request for given parameters. */
  protected Response execute(
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

    Credentials creds = provider != null ? provider.fetch() : null;
    if (creds != null && !creds.isEmpty()) {
      request = Signer.signV4(request, region, creds.accessKey(), creds.secretKey());
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
      throw new InvalidResponseException(
          response.code(),
          contentType,
          errorXml.substring(0, errorXml.length() > 1024 ? 1024 : errorXml.length()));
    }

    ErrorResponse errorResponse = null;
    if (!"".equals(errorXml)) {
      errorResponse = Xml.unmarshal(ErrorResponse.class, errorXml);
    } else if (!method.equals(Method.HEAD)) {
      if (this.traceStream != null) {
        this.traceStream.println(END_HTTP);
      }
      throw new InvalidResponseException(response.code(), contentType, errorXml);
    }

    if (this.traceStream != null) {
      this.traceStream.println(END_HTTP);
    }

    if (errorResponse == null) {
      String code = null;
      String message = null;
      switch (response.code()) {
        case 307:
          code = "Redirect";
          message = "Temporary redirect";
          break;
        case 400:
          // HEAD bucket with wrong region gives 400 without body.
          if (method.equals(Method.HEAD)
              && bucketName != null
              && objectName == null
              && isAwsHost
              && AwsRegionCache.INSTANCE.get(bucketName) != null) {
            code = RETRY_HEAD_BUCKET;
          } else {
            code = "BadRequest";
            message = "Bad request";
          }
          break;
        case 404:
          if (objectName != null) {
            code = "NoSuchKey";
            message = "Object does not exist";
          } else if (bucketName != null) {
            code = NO_SUCH_BUCKET;
            message = NO_SUCH_BUCKET_MESSAGE;
          } else {
            code = "ResourceNotFound";
            message = "Request resource not found";
          }
          break;
        case 501:
        case 405:
          code = "MethodNotAllowed";
          message = "The specified method is not allowed against this resource";
          break;
        case 409:
          if (bucketName != null) {
            code = NO_SUCH_BUCKET;
            message = NO_SUCH_BUCKET_MESSAGE;
          } else {
            code = "ResourceConflict";
            message = "Request resource conflicts";
          }
          break;
        case 403:
          code = "AccessDenied";
          message = "Access denied";
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
              code,
              message,
              bucketName,
              objectName,
              request.url().encodedPath(),
              response.header("x-amz-request-id"),
              response.header("x-amz-id-2"));
    }

    // invalidate region cache if needed
    if (errorResponse.code().equals(NO_SUCH_BUCKET)
        || errorResponse.code().equals(RETRY_HEAD_BUCKET)) {
      if (isAwsHost) {
        AwsRegionCache.INSTANCE.remove(bucketName);
      }

      // TODO: handle for other cases as well
    }

    throw new ErrorResponseException(errorResponse, response);
  }

  /** Returns region of given bucket either from region cache or set in constructor. */
  protected String getRegion(String bucketName, String region)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    if (region != null) {
      // Error out if region does not match with region passed via constructor.
      if (this.region != null && !this.region.equals(region)) {
        throw new IllegalArgumentException(
            "region must be " + this.region + ", but passed " + region);
      }
      return region;
    }

    if (this.region != null && !this.region.equals("")) {
      return this.region;
    }

    if (!isAwsHost || bucketName == null || this.provider == null) {
      return US_EAST_1;
    }

    region = AwsRegionCache.INSTANCE.get(bucketName);
    if (region != null) {
      return region;
    }

    // Execute GetBucketLocation REST API to get region of the bucket.
    Response response =
        execute(
            Method.GET, bucketName, null, US_EAST_1, null, newMultimap("location", null), null, 0);

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
      BaseArgs args, Multimap<String, String> headers, Multimap<String, String> queryParams)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return execute(Method.GET, args, headers, queryParams, null, 0);
  }

  private Response executeHead(
      BaseArgs args, Multimap<String, String> headers, Multimap<String, String> queryParams)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    try {
      Response response = execute(Method.HEAD, args, headers, queryParams, null, 0);
      response.body().close();
      return response;
    } catch (ErrorResponseException e) {
      if (!e.errorResponse().code().equals(RETRY_HEAD_BUCKET)) {
        throw e;
      }
    }

    // Retry once for RETRY_HEAD_BUCKET error.
    Response response = execute(Method.HEAD, args, headers, queryParams, null, 0);
    response.body().close();
    return response;
  }

  private Response executeDelete(
      BaseArgs args, Multimap<String, String> headers, Multimap<String, String> queryParams)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    Response response = execute(Method.DELETE, args, headers, queryParams, null, 0);
    response.body().close();
    return response;
  }

  private Response executePost(
      BaseArgs args,
      Multimap<String, String> headers,
      Multimap<String, String> queryParams,
      Object data)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return execute(Method.POST, args, headers, queryParams, data, 0);
  }

  private Response executePut(
      BaseArgs args,
      Multimap<String, String> headers,
      Multimap<String, String> queryParams,
      Object data,
      int length)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    return execute(Method.PUT, args, headers, queryParams, data, length);
  }

  /**
   * Gets object information and metadata of an object.
   *
   * <pre>Example:{@code
   * StatObjectResponse stat = minioClient.statObject("my-bucketname", "my-objectname");
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @return {@link StatObjectResponse} - Populated object information and metadata.
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
  public StatObjectResponse statObject(String bucketName, String objectName)
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
   * StatObjectResponse stat = minioClient.statObject("my-bucketname", "my-objectname", ssec);
   * }</pre>
   *
   * @param bucketName Name of the bucket.
   * @param objectName Object name in the bucket.
   * @param ssec SSE-C type server-side encryption.
   * @return {@link StatObjectResponse} - Populated object information and metadata.
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
  public StatObjectResponse statObject(
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
   * StatObjectResponse stat =
   *     minioClient.statObject(
   *         StatObjectArgs.builder().bucket("my-bucketname").object("my-objectname").build());
   *
   * // Get information of SSE-C encrypted object.
   * StatObjectResponse stat =
   *     minioClient.statObject(
   *         StatObjectArgs.builder()
   *             .bucket("my-bucketname")
   *             .object("my-objectname")
   *             .ssec(ssec)
   *             .build());
   *
   * // Get information of a versioned object.
   * StatObjectResponse stat =
   *     minioClient.statObject(
   *         StatObjectArgs.builder()
   *             .bucket("my-bucketname")
   *             .object("my-objectname")
   *             .versionId("version-id")
   *             .build());
   *
   * // Get information of a SSE-C encrypted versioned object.
   * StatObjectResponse stat =
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
   * @return {@link StatObjectResponse} - Populated object information and metadata.
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
   * @see StatObjectResponse
   */
  public StatObjectResponse statObject(StatObjectArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);
    args.validateSsec(baseUrl);
    Response response =
        executeHead(
            args,
            (args.ssec() != null) ? newMultimap(args.ssec().headers()) : null,
            (args.versionId() != null) ? newMultimap("versionId", args.versionId()) : null);
    return new StatObjectResponse(response.headers(), args.bucket(), args.region(), args.object());
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
   * @see #buildUrl
   */
  @Deprecated
  public String getObjectUrl(String bucketName, String objectName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkObjectName(objectName);
    HttpUrl url =
        buildUrl(Method.GET, bucketName, objectName, getRegion(bucketName, this.region), null);
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
  public GetObjectResponse getObject(GetObjectArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);
    args.validateSsec(this.baseUrl);

    Long offset = args.offset();
    Long length = args.length();
    if (length != null && offset == null) {
      offset = 0L;
    }

    String range = null;
    if (offset != null) {
      range = "bytes=" + offset + "-";
      if (length != null) {
        range = range + (offset + length - 1);
      }
    }

    Multimap<String, String> headers = HashMultimap.create();
    if (range != null) headers.put("Range", range);
    if (args.ssec() != null) headers.putAll(newMultimap(args.ssec().headers()));

    Multimap<String, String> queryParams = HashMultimap.create();
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());

    Response response = executeGet(args, headers, queryParams);
    return new GetObjectResponse(
        response.headers(),
        args.bucket(),
        args.region(),
        args.object(),
        response.body().byteStream());
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

    StatObjectResponse stat = statObject(new StatObjectArgs(args));

    String tempFilename = filename + "." + stat.etag() + ".part.minio";
    Path tempFilePath = Paths.get(tempFilename);
    boolean tempFileExists = Files.exists(tempFilePath);

    if (tempFileExists && !Files.isRegularFile(tempFilePath)) {
      throw new IOException(tempFilename + ": not a regular file");
    }

    long tempFileSize = 0;
    if (tempFileExists) {
      tempFileSize = Files.size(tempFilePath);
      if (tempFileSize > stat.size()) {
        Files.delete(tempFilePath);
        tempFileExists = false;
        tempFileSize = 0;
      }
    }

    if (fileExists) {
      long fileSize = Files.size(filePath);
      if (fileSize == stat.size()) {
        // already downloaded. nothing to do
        return;
      } else if (fileSize > stat.size()) {
        throw new IllegalArgumentException(
            "Source object, '"
                + args.object()
                + "', size:"
                + stat.size()
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
      is = getObject(new GetObjectArgs(args));
      os =
          Files.newOutputStream(tempFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      long bytesWritten = ByteStreams.copy(is, os);
      is.close();
      os.close();

      if (bytesWritten != stat.size() - tempFileSize) {
        throw new IOException(
            tempFilename
                + ": unexpected data written.  expected = "
                + (stat.size() - tempFileSize)
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

    CopyObjectArgs.Builder argBuilder =
        CopyObjectArgs.builder().bucket(bucketName).object(objectName).headers(headerMap).sse(sse);

    CopySource.Builder sourceBuilder =
        CopySource.builder().bucket(srcBucketName).object(srcObjectName).ssec(srcSsec);

    if (copyConditions != null) {
      Map<String, String> map = copyConditions.getConditions();
      String value;

      sourceBuilder.matchETag(map.get("x-amz-copy-source-if-match"));
      sourceBuilder.notMatchETag(map.get("x-amz-copy-source-if-none-match"));

      value = map.get("x-amz-copy-source-if-modified-since");
      if (value != null) {
        sourceBuilder.modifiedSince(ZonedDateTime.parse(value, Time.HTTP_HEADER_DATE_FORMAT));
      }

      value = map.get("x-amz-copy-source-if-unmodified-since");
      if (value != null) {
        sourceBuilder.unmodifiedSince(ZonedDateTime.parse(value, Time.HTTP_HEADER_DATE_FORMAT));
      }

      value = map.get("x-amz-metadata-directive");
      if (value != null) {
        argBuilder.metadataDirective(Directive.valueOf(value));
      }
    }

    copyObject(argBuilder.source(sourceBuilder.build()).build());
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
  public ObjectWriteResponse copyObject(CopyObjectArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);
    args.validateSse(this.baseUrl);
    if (args.source().offset() != null || args.source().length() != null) {
      return composeObject(new ComposeObjectArgs(args));
    }

    StatObjectResponse stat = statObject(new StatObjectArgs(args.source()));
    if (stat.size() > ObjectWriteArgs.MAX_PART_SIZE) {
      if (args.metadataDirective() != null && args.metadataDirective() == Directive.COPY) {
        throw new IllegalArgumentException(
            "COPY metadata directive is not applicable to source object size greater than 5 GiB");
      }
      if (args.taggingDirective() != null && args.taggingDirective() == Directive.COPY) {
        throw new IllegalArgumentException(
            "COPY tagging directive is not applicable to source object size greater than 5 GiB");
      }

      return composeObject(new ComposeObjectArgs(args));
    }

    Multimap<String, String> headers = args.genHeaders();

    if (args.metadataDirective() != null) {
      headers.put("x-amz-metadata-directive", args.metadataDirective().name());
    }

    if (args.taggingDirective() != null) {
      headers.put("x-amz-tagging-directive", args.taggingDirective().name());
    }

    headers.putAll(args.source().genCopyHeaders());

    try (Response response = executePut(args, headers, null, null, 0)) {
      CopyObjectResult result = Xml.unmarshal(CopyObjectResult.class, response.body().charStream());
      return new ObjectWriteResponse(
          response.headers(),
          args.bucket(),
          args.region(),
          args.object(),
          result.etag(),
          response.header("x-amz-version-id"));
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
   * @deprecated use {@link #composeObject(ComposeObjectArgs)}
   */
  @Deprecated
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
    composeObject(
        ComposeObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .sources(sources)
            .sse(sse)
            .headers(newMultimap(headerMap))
            .build());
  }

  private int calculatePartCount(List<ComposeSource> sources)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    long objectSize = 0;
    int partCount = 0;
    int i = 0;
    for (ComposeSource src : sources) {
      i++;
      StatObjectResponse stat = statObject(new StatObjectArgs(src));

      src.buildHeaders(stat.size(), stat.etag());

      long size = stat.size();
      if (src.length() != null) {
        size = src.length();
      } else if (src.offset() != null) {
        size -= src.offset();
      }

      if (size < ObjectWriteArgs.MIN_MULTIPART_SIZE && sources.size() != 1 && i != sources.size()) {
        throw new IllegalArgumentException(
            "source "
                + src.bucket()
                + "/"
                + src.object()
                + ": size "
                + size
                + " must be greater than "
                + ObjectWriteArgs.MIN_MULTIPART_SIZE);
      }

      objectSize += size;
      if (objectSize > ObjectWriteArgs.MAX_OBJECT_SIZE) {
        throw new IllegalArgumentException(
            "destination object size must be less than " + ObjectWriteArgs.MAX_OBJECT_SIZE);
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
            && i != sources.size()) {
          throw new IllegalArgumentException(
              "source "
                  + src.bucket()
                  + "/"
                  + src.object()
                  + ": "
                  + "for multipart split upload of "
                  + size
                  + ", last part size is less than "
                  + ObjectWriteArgs.MIN_MULTIPART_SIZE);
        }
        partCount += (int) count;
      } else {
        partCount++;
      }

      if (partCount > ObjectWriteArgs.MAX_MULTIPART_COUNT) {
        throw new IllegalArgumentException(
            "Compose sources create more than allowed multipart count "
                + ObjectWriteArgs.MAX_MULTIPART_COUNT);
      }
    }

    return partCount;
  }

  /**
   * Creates an object by combining data from different source objects using server-side copy.
   *
   * <pre>Example:{@code
   * List<ComposeSource> sourceObjectList = new ArrayList<ComposeSource>();
   *
   * sourceObjectList.add(
   *    ComposeSource.builder().bucket("my-job-bucket").object("my-objectname-part-one").build());
   * sourceObjectList.add(
   *    ComposeSource.builder().bucket("my-job-bucket").object("my-objectname-part-two").build());
   * sourceObjectList.add(
   *    ComposeSource.builder().bucket("my-job-bucket").object("my-objectname-part-three").build());
   *
   * // Create my-bucketname/my-objectname by combining source object list.
   * minioClient.composeObject(
   *    ComposeObjectArgs.builder()
   *        .bucket("my-bucketname")
   *        .object("my-objectname")
   *        .sources(sourceObjectList)
   *        .build());
   *
   * // Create my-bucketname/my-objectname with user metadata by combining source object
   * // list.
   * minioClient.composeObject(
   *     ComposeObjectArgs.builder()
   *        .bucket("my-bucketname")
   *        .object("my-objectname")
   *        .sources(sourceObjectList)
   *        .build());
   *
   * // Create my-bucketname/my-objectname with user metadata and server-side encryption
   * // by combining source object list.
   * minioClient.composeObject(
   *   ComposeObjectArgs.builder()
   *        .bucket("my-bucketname")
   *        .object("my-objectname")
   *        .sources(sourceObjectList)
   *        .ssec(sse)
   *        .build());
   *
   * }</pre>
   *
   * @param args {@link ComposeObjectArgs} object.
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
  public ObjectWriteResponse composeObject(ComposeObjectArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    checkArgs(args);
    args.validateSse(this.baseUrl);
    List<ComposeSource> sources = args.sources();
    int partCount = calculatePartCount(sources);
    if (partCount == 1
        && args.sources().get(0).offset() == null
        && args.sources().get(0).length() == null) {
      return copyObject(new CopyObjectArgs(args));
    }

    Multimap<String, String> headers = newMultimap(args.extraHeaders());
    headers.putAll(args.genHeaders());
    CreateMultipartUploadResponse createMultipartUploadResponse =
        createMultipartUpload(
            args.bucket(), args.region(), args.object(), headers, args.extraQueryParams());
    String uploadId = createMultipartUploadResponse.result().uploadId();

    Multimap<String, String> ssecHeaders = HashMultimap.create();
    if (args.sse() != null && args.sse() instanceof ServerSideEncryptionCustomerKey) {
      ssecHeaders.putAll(newMultimap(args.sse().headers()));
    }

    try {
      int partNumber = 0;
      Part[] totalParts = new Part[partCount];
      for (ComposeSource src : sources) {
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

        headers = newMultimap(src.headers());
        headers.putAll(ssecHeaders);

        if (size <= ObjectWriteArgs.MAX_PART_SIZE) {
          partNumber++;
          if (src.length() != null) {
            headers.put(
                "x-amz-copy-source-range", "bytes=" + offset + "-" + (offset + src.length() - 1));
          } else if (src.offset() != null) {
            headers.put("x-amz-copy-source-range", "bytes=" + offset + "-" + (offset + size - 1));
          }

          UploadPartCopyResponse response =
              uploadPartCopy(
                  args.bucket(), args.region(), args.object(), uploadId, partNumber, headers, null);
          String eTag = response.result().etag();

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

          Multimap<String, String> headersCopy = newMultimap(headers);
          headersCopy.put("x-amz-copy-source-range", "bytes=" + startBytes + "-" + endBytes);

          UploadPartCopyResponse response =
              uploadPartCopy(
                  args.bucket(),
                  args.region(),
                  args.object(),
                  uploadId,
                  partNumber,
                  headersCopy,
                  null);
          String eTag = response.result().etag();
          totalParts[partNumber - 1] = new Part(partNumber, eTag);
          offset = startBytes;
          size -= (endBytes - startBytes);
        }
      }

      return completeMultipartUpload(
          args.bucket(),
          getRegion(args.bucket(), args.region()),
          args.object(),
          uploadId,
          totalParts,
          null,
          null);
    } catch (RuntimeException e) {
      abortMultipartUpload(args.bucket(), args.region(), args.object(), uploadId, null, null);
      throw e;
    } catch (Exception e) {
      abortMultipartUpload(args.bucket(), args.region(), args.object(), uploadId, null, null);
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

    Multimap<String, String> queryParams = newMultimap(args.extraQueryParams());
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());

    String region = getRegion(args.bucket(), args.region());
    HttpUrl url = buildUrl(args.method(), args.bucket(), args.object(), region, queryParams);
    Request request = createRequest(url, args.method(), null, body, 0);
    Credentials creds = provider != null ? provider.fetch() : null;
    String accessKey = creds != null ? creds.accessKey() : null;
    String secretKey = creds != null ? creds.secretKey() : null;
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
   * // Create new post policy for 'my-bucketname' with 7 days expiry from now.
   * PostPolicy policy = new PostPolicy("my-bucketname", ZonedDateTime.now().plusDays(7));
   *
   * // Add condition that 'key' (object name) equals to 'my-objectname'.
   * policy.addEqualsCondition("key", "my-objectname");
   *
   * // Add condition that 'Content-Type' starts with 'image/'.
   * policy.addStartsWithCondition("Content-Type", "image/");
   *
   * // Add condition that 'content-length-range' is between 64kiB to 10MiB.
   * policy.addContentLengthRangeCondition(64 * 1024, 10 * 1024 * 1024);
   *
   * Map<String, String> formData = minioClient.getPresignedPostFormData(policy);
   *
   * // Upload an image using POST object with form-data.
   * MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
   * multipartBuilder.setType(MultipartBody.FORM);
   * for (Map.Entry<String, String> entry : formData.entrySet()) {
   *   multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
   * }
   * multipartBuilder.addFormDataPart("key", "my-objectname");
   * multipartBuilder.addFormDataPart("Content-Type", "image/png");
   *
   * // "file" must be added at last.
   * multipartBuilder.addFormDataPart(
   *     "file", "my-objectname", RequestBody.create(null, new File("Pictures/avatar.png")));
   *
   * Request request =
   *     new Request.Builder()
   *         .url("https://play.min.io/my-bucketname")
   *         .post(multipartBuilder.build())
   *         .build();
   * OkHttpClient httpClient = new OkHttpClient().newBuilder().build();
   * Response response = httpClient.newCall(request).execute();
   * if (response.isSuccessful()) {
   *   System.out.println("Pictures/avatar.png is uploaded successfully using POST object");
   * } else {
   *   System.out.println("Failed to upload Pictures/avatar.png");
   * }
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
  public Map<String, String> getPresignedPostFormData(PostPolicy policy)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidExpiresRangeException,
          InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
          ServerException, XmlParserException {

    if (provider == null) {
      throw new IllegalArgumentException("credentials provider cannot be null");
    }

    Credentials creds = provider.fetch();
    return policy.formData(creds.accessKey(), creds.secretKey(), getRegion(policy.bucket(), null));
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
    executeDelete(
        args,
        args.bypassGovernanceMode()
            ? newMultimap("x-amz-bypass-governance-retention", "true")
            : null,
        (args.versionId() != null) ? newMultimap("versionId", args.versionId()) : null);
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
                DeleteObjectsResponse response =
                    deleteObjects(
                        args.bucket(),
                        args.region(),
                        objectList,
                        args.quiet(),
                        args.bypassGovernanceMode(),
                        args.extraHeaders(),
                        args.extraQueryParams());
                errorList = response.result().errorList();
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
    return listObjects(bucketName, prefix, recursive, false, useVersion1);
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
            .useApiVersion1(useVersion1)
            .build());
  }

  /**
   * Lists objects information optionally with versions of a bucket. Supports both the versions 1
   * and 2 of the S3 API. By default, the <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html">version 2</a> API
   * is used. <br>
   * <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html">Version 1</a>
   * can be used by passing the optional argument {@code useVersion1} as {@code true}.
   *
   * <pre>Example:{@code
   * // Lists objects information.
   * Iterable<Result<Item>> results = minioClient.listObjects(
   *     ListObjectsArgs.builder().bucket("my-bucketname").build());
   *
   * // Lists objects information recursively.
   * Iterable<Result<Item>> results = minioClient.listObjects(
   *     ListObjectsArgs.builder().bucket("my-bucketname").recursive(true).build());
   *
   * // Lists maximum 100 objects information those names starts with 'E' and after
   * // 'ExampleGuide.pdf'.
   * Iterable<Result<Item>> results = minioClient.listObjects(
   *     ListObjectsArgs.builder()
   *         .bucket("my-bucketname")
   *         .startAfter("ExampleGuide.pdf")
   *         .prefix("E")
   *         .maxKeys(100)
   *         .build());
   *
   * // Lists maximum 100 objects information with version those names starts with 'E' and after
   * // 'ExampleGuide.pdf'.
   * Iterable<Result<Item>> results = minioClient.listObjects(
   *     ListObjectsArgs.builder()
   *         .bucket("my-bucketname")
   *         .startAfter("ExampleGuide.pdf")
   *         .prefix("E")
   *         .maxKeys(100)
   *         .includeVersions(true)
   *         .build());
   * }</pre>
   *
   * @param args Instance of {@link ListObjectsArgs} built using the builder
   * @return Iterable&lt;Result&lt;Item&gt;&gt; - Lazy iterator contains object information.
   * @throws XmlParserException upon parsing response xml
   */
  public Iterable<Result<Item>> listObjects(ListObjectsArgs args) {
    if (args.includeVersions() || args.versionIdMarker() != null) {
      return listObjectVersions(args);
    }

    if (args.useApiVersion1()) {
      return listObjectsV1(args);
    }

    return listObjectsV2(args);
  }

  private abstract class ObjectIterator implements Iterator<Result<Item>> {
    protected Result<Item> error;
    protected Iterator<? extends Item> itemIterator;
    protected Iterator<DeleteMarker> deleteMarkerIterator;
    protected Iterator<Prefix> prefixIterator;
    protected boolean completed = false;
    protected ListObjectsResult listObjectsResult;
    protected String lastObjectName;

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
      }

      if (this.listObjectsResult != null) {
        this.itemIterator = this.listObjectsResult.contents().iterator();
        this.deleteMarkerIterator = this.listObjectsResult.deleteMarkers().iterator();
        this.prefixIterator = this.listObjectsResult.commonPrefixes().iterator();
      } else {
        this.itemIterator = new LinkedList<Item>().iterator();
        this.deleteMarkerIterator = new LinkedList<DeleteMarker>().iterator();
        this.prefixIterator = new LinkedList<Prefix>().iterator();
      }
    }

    @Override
    public boolean hasNext() {
      if (this.completed) {
        return false;
      }

      if (this.error == null
          && this.itemIterator == null
          && this.deleteMarkerIterator == null
          && this.prefixIterator == null) {
        populate();
      }

      if (this.error == null
          && !this.itemIterator.hasNext()
          && !this.deleteMarkerIterator.hasNext()
          && !this.prefixIterator.hasNext()
          && this.listObjectsResult.isTruncated()) {
        populate();
      }

      if (this.error != null) {
        return true;
      }

      if (this.itemIterator.hasNext()) {
        return true;
      }

      if (this.deleteMarkerIterator.hasNext()) {
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

      if (this.error == null
          && this.itemIterator == null
          && this.deleteMarkerIterator == null
          && this.prefixIterator == null) {
        populate();
      }

      if (this.error == null
          && !this.itemIterator.hasNext()
          && !this.deleteMarkerIterator.hasNext()
          && !this.prefixIterator.hasNext()
          && this.listObjectsResult.isTruncated()) {
        populate();
      }

      if (this.error != null) {
        this.completed = true;
        return this.error;
      }

      Item item = null;
      if (this.itemIterator.hasNext()) {
        item = this.itemIterator.next();
        item.setEncodingType(this.listObjectsResult.encodingType());
        this.lastObjectName = item.objectName();
      } else if (this.deleteMarkerIterator.hasNext()) {
        item = this.deleteMarkerIterator.next();
      } else if (this.prefixIterator.hasNext()) {
        item = this.prefixIterator.next().toItem();
      }

      if (item != null) {
        item.setEncodingType(this.listObjectsResult.encodingType());
        return new Result<>(item);
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
          private ListBucketResultV2 result = null;

          @Override
          protected void populateResult()
              throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
                  InternalException, InvalidBucketNameException, InvalidKeyException,
                  InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
                  XmlParserException {
            this.listObjectsResult = null;
            this.itemIterator = null;
            this.prefixIterator = null;

            ListObjectsV2Response response =
                listObjectsV2(
                    args.bucket(),
                    args.region(),
                    args.delimiter(),
                    args.useUrlEncodingType() ? "url" : null,
                    args.startAfter(),
                    args.maxKeys(),
                    args.prefix(),
                    (result == null) ? args.continuationToken() : result.nextContinuationToken(),
                    args.fetchOwner(),
                    args.includeUserMetadata(),
                    args.extraHeaders(),
                    args.extraQueryParams());
            result = response.result();
            this.listObjectsResult = response.result();
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
          private ListBucketResultV1 result = null;

          @Override
          protected void populateResult()
              throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
                  InternalException, InvalidBucketNameException, InvalidKeyException,
                  InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
                  XmlParserException {
            this.listObjectsResult = null;
            this.itemIterator = null;
            this.prefixIterator = null;

            String nextMarker = (result == null) ? args.marker() : result.nextMarker();
            if (nextMarker == null) {
              nextMarker = this.lastObjectName;
            }

            ListObjectsV1Response response =
                listObjectsV1(
                    args.bucket(),
                    args.region(),
                    args.delimiter(),
                    args.useUrlEncodingType() ? "url" : null,
                    nextMarker,
                    args.maxKeys(),
                    args.prefix(),
                    args.extraHeaders(),
                    args.extraQueryParams());
            result = response.result();
            this.listObjectsResult = response.result();
          }
        };
      }
    };
  }

  private Iterable<Result<Item>> listObjectVersions(ListObjectsArgs args) {
    return new Iterable<Result<Item>>() {
      @Override
      public Iterator<Result<Item>> iterator() {
        return new ObjectIterator() {
          private ListVersionsResult result = null;

          @Override
          protected void populateResult()
              throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
                  InternalException, InvalidBucketNameException, InvalidKeyException,
                  InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
                  XmlParserException {
            this.listObjectsResult = null;
            this.itemIterator = null;
            this.prefixIterator = null;

            ListObjectVersionsResponse response =
                listObjectVersions(
                    args.bucket(),
                    args.region(),
                    args.delimiter(),
                    args.useUrlEncodingType() ? "url" : null,
                    (result == null) ? args.keyMarker() : result.nextKeyMarker(),
                    args.maxKeys(),
                    args.prefix(),
                    (result == null) ? args.versionIdMarker() : result.nextVersionIdMarker(),
                    args.extraHeaders(),
                    args.extraQueryParams());
            result = response.result();
            this.listObjectsResult = response.result();
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
    return listBuckets(ListBucketsArgs.builder().build());
  }

  /**
   * Lists bucket information of all buckets.
   *
   * <pre>Example:{@code
   * List<Bucket> bucketList =
   *     minioClient.listBuckets(ListBucketsArgs.builder().extraHeaders(headers).build());
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
  public List<Bucket> listBuckets(ListBucketsArgs args)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    try (Response response = executeGet(args, null, null)) {
      ListAllMyBucketsResult result =
          Xml.unmarshal(ListAllMyBucketsResult.class, response.body().charStream());
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
      executeHead(args, null, null);
      return true;
    } catch (ErrorResponseException e) {
      if (!e.errorResponse().code().equals(NO_SUCH_BUCKET)) {
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

    String region = args.region();
    if (this.region != null && !this.region.isEmpty()) {
      // Error out if region does not match with region passed via constructor.
      if (region != null && !region.equals(this.region)) {
        throw new IllegalArgumentException(
            "region must be " + this.region + ", but passed " + region);
      }

      region = this.region;
    }

    if (region == null) {
      region = US_EAST_1;
    }

    Multimap<String, String> headers =
        args.objectLock() ? newMultimap("x-amz-bucket-object-lock-enabled", "true") : null;

    try (Response response =
        execute(
            Method.PUT,
            args.bucket(),
            null,
            region,
            merge(args.extraHeaders(), headers),
            args.extraQueryParams(),
            region.equals(US_EAST_1) ? null : new CreateBucketConfiguration(region),
            0)) {
      if (isAwsHost) {
        AwsRegionCache.INSTANCE.set(args.bucket(), region);
      }
    }
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
    Response response =
        executePut(args, null, newMultimap("versioning", ""), new VersioningConfiguration(true), 0);
    response.close();
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
    Response response =
        executePut(
            args, null, newMultimap("versioning", ""), new VersioningConfiguration(false), 0);
    response.close();
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
    try (Response response = executeGet(args, null, newMultimap("versioning", ""))) {
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
    Response response = executePut(args, null, newMultimap("object-lock", ""), args.config(), 0);
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
    Response response =
        executePut(args, null, newMultimap("object-lock", ""), new ObjectLockConfiguration(), 0);
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
    try (Response response = executeGet(args, null, newMultimap("object-lock", ""))) {
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
    Multimap<String, String> queryParams = newMultimap("retention", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    Response response =
        executePut(
            args,
            args.bypassGovernanceMode()
                ? newMultimap("x-amz-bypass-governance-retention", "True")
                : null,
            queryParams,
            args.config(),
            0);
    response.close();
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
    Multimap<String, String> queryParams = newMultimap("retention", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    try (Response response = executeGet(args, null, queryParams)) {
      return Xml.unmarshal(Retention.class, response.body().charStream());
    } catch (ErrorResponseException e) {
      if (!e.errorResponse().code().equals(NO_SUCH_OBJECT_LOCK_CONFIGURATION)) {
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
    Multimap<String, String> queryParams = newMultimap("legal-hold", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    Response response = executePut(args, null, queryParams, new LegalHold(true), 0);
    response.close();
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
    Multimap<String, String> queryParams = newMultimap("legal-hold", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    Response response = executePut(args, null, queryParams, new LegalHold(false), 0);
    response.close();
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
    Multimap<String, String> queryParams = newMultimap("legal-hold", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    try (Response response = executeGet(args, null, queryParams)) {
      LegalHold result = Xml.unmarshal(LegalHold.class, response.body().charStream());
      return result.status();
    } catch (ErrorResponseException e) {
      if (!e.errorResponse().code().equals(NO_SUCH_OBJECT_LOCK_CONFIGURATION)) {
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
    executeDelete(args, null, null);
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
    Multimap<String, String> headers = newMultimap(args.extraHeaders());
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
          CreateMultipartUploadResponse response =
              createMultipartUpload(
                  args.bucket(), args.region(), args.object(), headers, args.extraQueryParams());
          uploadId = response.result().uploadId();
          parts = new Part[ObjectWriteArgs.MAX_MULTIPART_COUNT];
        }

        Map<String, String> ssecHeaders = null;
        // set encryption headers in the case of SSE-C.
        if (args.sse() != null && args.sse() instanceof ServerSideEncryptionCustomerKey) {
          ssecHeaders = args.sse().headers();
        }

        UploadPartResponse response =
            uploadPart(
                args.bucket(),
                args.region(),
                args.object(),
                data,
                (int) availableSize,
                uploadId,
                partNumber,
                (ssecHeaders != null) ? Multimaps.forMap(ssecHeaders) : null,
                null);
        String etag = response.etag();
        parts[partNumber - 1] = new Part(partNumber, etag);
        uploadedSize += availableSize;
      }

      return completeMultipartUpload(
          args.bucket(), args.region(), args.object(), uploadId, parts, null, null);
    } catch (RuntimeException e) {
      if (uploadId != null) {
        abortMultipartUpload(args.bucket(), args.region(), args.object(), uploadId, null, null);
      }
      throw e;
    } catch (Exception e) {
      if (uploadId != null) {
        abortMultipartUpload(args.bucket(), args.region(), args.object(), uploadId, null, null);
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
    if (options != null) {
      builder.sse(options.sse());
      if (!options.contentType().equals("application/octet-stream")) {
        builder.contentType(options.contentType());
      }
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
    try (Response response = executeGet(args, null, newMultimap("policy", ""))) {
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
      if (!e.errorResponse().code().equals(NO_SUCH_BUCKET_POLICY)) {
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
    Response response =
        executePut(
            args,
            newMultimap("Content-Type", "application/json"),
            newMultimap("policy", ""),
            args.config(),
            0);
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
    try {
      executeDelete(args, null, newMultimap("policy", ""));
    } catch (ErrorResponseException e) {
      if (!e.errorResponse().code().equals(NO_SUCH_BUCKET_POLICY)) {
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
    Response response = executePut(args, null, newMultimap("lifecycle", ""), args.config(), 0);
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
    executeDelete(args, null, newMultimap("lifecycle", ""));
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
    try (Response response = executeGet(args, null, newMultimap("lifecycle", ""))) {
      return new String(response.body().bytes(), StandardCharsets.UTF_8);
    } catch (ErrorResponseException e) {
      if (!e.errorResponse().code().equals("NoSuchLifecycleConfiguration")) {
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
    try (Response response = executeGet(args, null, newMultimap("notification", ""))) {
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
    Response response = executePut(args, null, newMultimap("notification", ""), args.config(), 0);
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
    Response response =
        executePut(args, null, newMultimap("notification", ""), new NotificationConfiguration(), 0);
    response.close();
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
   * @return Iterable&ltResult&ltUpload&gt&gt - Lazy iterator contains object upload information.
   * @see #listIncompleteUploads(String, String, boolean)
   */
  @Deprecated
  public Iterable<Result<Upload>> listIncompleteUploads(String bucketName)
      throws XmlParserException {
    return listIncompleteUploads(bucketName, null, true, true);
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
   * @return Iterable&ltResult&ltUpload&gt&gt - Lazy iterator contains object upload information.
   * @throws XmlParserException upon parsing response xml
   * @see #listIncompleteUploads(String, String, boolean)
   */
  @Deprecated
  public Iterable<Result<Upload>> listIncompleteUploads(String bucketName, String prefix)
      throws XmlParserException {
    return listIncompleteUploads(bucketName, prefix, true, true);
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
   * @return Iterable&ltResult&ltUpload&gt&gt - Lazy iterator contains object upload information.
   * @see #listIncompleteUploads(String bucketName)
   * @see #listIncompleteUploads(String bucketName, String prefix)
   */
  @Deprecated
  public Iterable<Result<Upload>> listIncompleteUploads(
      String bucketName, String prefix, boolean recursive) {
    return listIncompleteUploads(bucketName, prefix, recursive, true);
  }

  /**
   * Returns Iterable<Result<Upload>> of given bucket name, prefix and recursive flag. All parts
   * size are aggregated when aggregatePartSize is true.
   */
  private Iterable<Result<Upload>> listIncompleteUploads(
      final String bucketName,
      final String prefix,
      final boolean recursive,
      final boolean aggregatePartSize) {
    return new Iterable<Result<Upload>>() {
      @Override
      public Iterator<Result<Upload>> iterator() {
        return new Iterator<Result<Upload>>() {
          private String nextKeyMarker;
          private String nextUploadIdMarker;
          private ListMultipartUploadsResult listMultipartUploadsResult;
          private Result<Upload> error;
          private Iterator<Upload> uploadIterator;
          private boolean completed = false;

          private synchronized void populate() {
            String delimiter = "/";
            if (recursive) {
              delimiter = null;
            }

            this.listMultipartUploadsResult = null;
            this.uploadIterator = null;

            try {
              ListMultipartUploadsResponse response =
                  listMultipartUploads(
                      bucketName,
                      null,
                      delimiter,
                      "url",
                      nextKeyMarker,
                      null,
                      prefix,
                      nextUploadIdMarker,
                      null,
                      null);
              this.listMultipartUploadsResult = response.result();
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

            for (Result<Part> result : listObjectParts(bucketName, objectName, uploadId)) {
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
              ListPartsResponse response =
                  listParts(
                      bucketName,
                      null,
                      objectName,
                      null,
                      nextPartNumberMarker,
                      uploadId,
                      null,
                      null);
              this.listPartsResult = response.result();
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
   */
  @Deprecated
  public void removeIncompleteUpload(String bucketName, String objectName)
      throws ErrorResponseException, IllegalArgumentException, InsufficientDataException,
          InternalException, InvalidBucketNameException, InvalidKeyException,
          InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException,
          XmlParserException {
    for (Result<Upload> r : listIncompleteUploads(bucketName, objectName, true, false)) {
      Upload upload = r.get();
      if (objectName.equals(upload.objectName())) {
        abortMultipartUpload(bucketName, null, objectName, upload.uploadId(), null, null);
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

    Multimap<String, String> queryParams =
        newMultimap("prefix", args.prefix(), "suffix", args.suffix());
    for (String event : args.events()) {
      queryParams.put("events", event);
    }

    Response response = executeGet(args, null, queryParams);
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
    checkArgs(args);
    args.validateSsec(this.baseUrl);
    Response response =
        executePost(
            args,
            (args.ssec() != null) ? newMultimap(args.ssec().headers()) : null,
            newMultimap("select", "", "select-type", "2"),
            new SelectObjectContentRequest(
                args.sqlExpression(),
                args.requestProgress(),
                args.inputSerialization(),
                args.outputSerialization(),
                args.scanStartRange(),
                args.scanEndRange()));
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
    Response response = executePut(args, null, newMultimap("encryption", ""), args.config(), 0);
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
    try (Response response = executeGet(args, null, newMultimap("encryption", ""))) {
      return Xml.unmarshal(SseConfiguration.class, response.body().charStream());
    } catch (ErrorResponseException e) {
      if (!e.errorResponse().code().equals(SERVER_SIDE_ENCRYPTION_CONFIGURATION_NOT_FOUND_ERROR)) {
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
    try {
      executeDelete(args, null, newMultimap("encryption", ""));
    } catch (ErrorResponseException e) {
      if (!e.errorResponse().code().equals(SERVER_SIDE_ENCRYPTION_CONFIGURATION_NOT_FOUND_ERROR)) {
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
    try (Response response = executeGet(args, null, newMultimap("tagging", ""))) {
      return Xml.unmarshal(Tags.class, response.body().charStream());
    } catch (ErrorResponseException e) {
      if (!e.errorResponse().code().equals("NoSuchTagSet")) {
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
    Response response = executePut(args, null, newMultimap("tagging", ""), args.tags(), 0);
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
    executeDelete(args, null, newMultimap("tagging", ""));
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
    Multimap<String, String> queryParams = newMultimap("tagging", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    try (Response response = executeGet(args, null, queryParams)) {
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
    Multimap<String, String> queryParams = newMultimap("tagging", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    Response response = executePut(args, null, queryParams, args.tags(), 0);
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
    Multimap<String, String> queryParams = newMultimap("tagging", "");
    if (args.versionId() != null) queryParams.put("versionId", args.versionId());
    executeDelete(args, null, queryParams);
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
   * @param region Region of the bucket.
   * @param objectName Object name in the bucket.
   * @param uploadId Upload ID.
   * @param extraHeaders Extra headers (Optional).
   * @param extraQueryParams Extra query parameters (Optional).
   * @return {@link AbortMultipartUploadResponse} object.
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
  protected AbortMultipartUploadResponse abortMultipartUpload(
      String bucketName,
      String region,
      String objectName,
      String uploadId,
      Multimap<String, String> extraHeaders,
      Multimap<String, String> extraQueryParams)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    try (Response response =
        execute(
            Method.DELETE,
            bucketName,
            objectName,
            getRegion(bucketName, region),
            extraHeaders,
            merge(extraQueryParams, newMultimap(UPLOAD_ID, uploadId)),
            null,
            0)) {
      return new AbortMultipartUploadResponse(
          response.headers(), bucketName, region, objectName, uploadId);
    }
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
   * @param extraHeaders Extra headers (Optional).
   * @param extraQueryParams Extra query parameters (Optional).
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
    Multimap<String, String> queryParams = newMultimap(extraQueryParams);
    queryParams.put(UPLOAD_ID, uploadId);

    try (Response response =
        execute(
            Method.POST,
            bucketName,
            objectName,
            getRegion(bucketName, region),
            extraHeaders,
            queryParams,
            new CompleteMultipartUpload(parts),
            0)) {
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
          return new ObjectWriteResponse(
              response.headers(),
              result.bucket(),
              result.location(),
              result.object(),
              result.etag(),
              response.header("x-amz-version-id"));
        } catch (XmlParserException e) {
          // As this CompleteMultipartUpload REST call succeeded, just log it.
          Logger.getLogger(MinioClient.class.getName())
              .warning(
                  "S3 service returned unknown XML for CompleteMultipartUpload REST API. "
                      + bodyContent);
        }
      }

      return new ObjectWriteResponse(
          response.headers(),
          bucketName,
          region,
          objectName,
          null,
          response.header("x-amz-version-id"));
    }
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload
   * S3 API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param region Region name of buckets in S3 service.
   * @param objectName Object name in the bucket.
   * @param headers Request headers.
   * @param extraQueryParams Extra query parameters for request (Optional).
   * @return {@link CreateMultipartUploadResponse} object.
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
  protected CreateMultipartUploadResponse createMultipartUpload(
      String bucketName,
      String region,
      String objectName,
      Multimap<String, String> headers,
      Multimap<String, String> extraQueryParams)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Multimap<String, String> queryParams = newMultimap(extraQueryParams);
    queryParams.put("uploads", "");

    Multimap<String, String> headersCopy = newMultimap(headers);
    // set content type if not set already
    if (!headersCopy.containsKey("Content-Type")) {
      headersCopy.put("Content-Type", "application/octet-stream");
    }

    try (Response response =
        execute(
            Method.POST,
            bucketName,
            objectName,
            getRegion(bucketName, region),
            headersCopy,
            queryParams,
            null,
            0)) {
      InitiateMultipartUploadResult result =
          Xml.unmarshal(InitiateMultipartUploadResult.class, response.body().charStream());
      return new CreateMultipartUploadResponse(
          response.headers(), bucketName, region, objectName, result);
    }
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjects.html">DeleteObjects S3
   * API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param region Region of the bucket (Optional).
   * @param objectList List of object names.
   * @param quiet Quiet flag.
   * @param bypassGovernanceMode Bypass Governance retention mode.
   * @param extraHeaders Extra headers for request (Optional).
   * @param extraQueryParams Extra query parameters for request (Optional).
   * @return {@link DeleteObjectsResponse} object.
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
  protected DeleteObjectsResponse deleteObjects(
      String bucketName,
      String region,
      List<DeleteObject> objectList,
      boolean quiet,
      boolean bypassGovernanceMode,
      Multimap<String, String> extraHeaders,
      Multimap<String, String> extraQueryParams)
      throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
          IOException, InvalidKeyException, ServerException, XmlParserException,
          ErrorResponseException, InternalException, InvalidResponseException {
    if (objectList == null) objectList = new LinkedList<>();

    if (objectList.size() > 1000) {
      throw new IllegalArgumentException("list of objects must not be more than 1000");
    }

    Multimap<String, String> headers =
        merge(
            extraHeaders,
            bypassGovernanceMode ? newMultimap("x-amz-bypass-governance-retention", "true") : null);
    try (Response response =
        execute(
            Method.POST,
            bucketName,
            null,
            getRegion(bucketName, region),
            headers,
            merge(extraQueryParams, newMultimap("delete", "")),
            new DeleteRequest(objectList, quiet),
            0)) {
      String bodyContent = new String(response.body().bytes(), StandardCharsets.UTF_8);
      try {
        if (Xml.validate(DeleteError.class, bodyContent)) {
          DeleteError error = Xml.unmarshal(DeleteError.class, bodyContent);
          DeleteResult result = new DeleteResult(error);
          return new DeleteObjectsResponse(response.headers(), bucketName, region, result);
        }
      } catch (XmlParserException e) {
        // Ignore this exception as it is not <Error> message,
        // but parse it as <DeleteResult> message below.
      }

      DeleteResult result = Xml.unmarshal(DeleteResult.class, bodyContent);
      return new DeleteObjectsResponse(response.headers(), bucketName, region, result);
    }
  }

  private Multimap<String, String> getCommonListObjectsQueryParams(
      String delimiter, String encodingType, Integer maxKeys, String prefix) {
    Multimap<String, String> queryParams =
        newMultimap(
            "delimiter",
            (delimiter == null) ? "" : delimiter,
            "max-keys",
            Integer.toString(maxKeys > 0 ? maxKeys : 1000),
            "prefix",
            (prefix == null) ? "" : prefix);
    if (encodingType != null) queryParams.put("encoding-type", encodingType);
    return queryParams;
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html">ListObjects
   * version 1 S3 API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param region Region of the bucket (Optional).
   * @param delimiter Delimiter (Optional).
   * @param encodingType Encoding type (Optional).
   * @param startAfter Fetch listing after this key (Optional).
   * @param maxKeys Maximum object information to fetch (Optional).
   * @param prefix Prefix (Optional).
   * @param continuationToken Continuation token (Optional).
   * @param fetchOwner Flag to fetch owner information (Optional).
   * @param includeUserMetadata MinIO extension flag to include user metadata (Optional).
   * @param extraHeaders Extra headers for request (Optional).
   * @param extraQueryParams Extra query parameters for request (Optional).
   * @return {@link ListObjectsV2Response} object.
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
  protected ListObjectsV2Response listObjectsV2(
      String bucketName,
      String region,
      String delimiter,
      String encodingType,
      String startAfter,
      Integer maxKeys,
      String prefix,
      String continuationToken,
      boolean fetchOwner,
      boolean includeUserMetadata,
      Multimap<String, String> extraHeaders,
      Multimap<String, String> extraQueryParams)
      throws InvalidKeyException, InvalidBucketNameException, IllegalArgumentException,
          NoSuchAlgorithmException, InsufficientDataException, ServerException, XmlParserException,
          ErrorResponseException, InternalException, InvalidResponseException, IOException {
    Multimap<String, String> queryParams =
        merge(
            extraQueryParams,
            getCommonListObjectsQueryParams(delimiter, encodingType, maxKeys, prefix));
    queryParams.put("list-type", "2");
    if (continuationToken != null) queryParams.put("continuation-token", continuationToken);
    if (fetchOwner) queryParams.put("fetch-owner", "true");
    if (startAfter != null) queryParams.put("start-after", startAfter);
    if (includeUserMetadata) queryParams.put("metadata", "true");

    try (Response response =
        execute(
            Method.GET,
            bucketName,
            null,
            getRegion(bucketName, region),
            extraHeaders,
            queryParams,
            null,
            0)) {
      ListBucketResultV2 result =
          Xml.unmarshal(ListBucketResultV2.class, response.body().charStream());
      return new ListObjectsV2Response(response.headers(), bucketName, region, result);
    }
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html">ListObjects
   * version 1 S3 API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param region Region of the bucket (Optional).
   * @param delimiter Delimiter (Optional).
   * @param encodingType Encoding type (Optional).
   * @param marker Marker (Optional).
   * @param maxKeys Maximum object information to fetch (Optional).
   * @param prefix Prefix (Optional).
   * @param extraHeaders Extra headers for request (Optional).
   * @param extraQueryParams Extra query parameters for request (Optional).
   * @return {@link ListObjectsV1Response} object.
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
  protected ListObjectsV1Response listObjectsV1(
      String bucketName,
      String region,
      String delimiter,
      String encodingType,
      String marker,
      Integer maxKeys,
      String prefix,
      Multimap<String, String> extraHeaders,
      Multimap<String, String> extraQueryParams)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Multimap<String, String> queryParams =
        merge(
            extraQueryParams,
            getCommonListObjectsQueryParams(delimiter, encodingType, maxKeys, prefix));
    if (marker != null) queryParams.put("marker", marker);

    try (Response response =
        execute(
            Method.GET,
            bucketName,
            null,
            getRegion(bucketName, region),
            extraHeaders,
            queryParams,
            null,
            0)) {
      ListBucketResultV1 result =
          Xml.unmarshal(ListBucketResultV1.class, response.body().charStream());
      return new ListObjectsV1Response(response.headers(), bucketName, region, result);
    }
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectVersions.html">ListObjectVersions
   * API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param region Region of the bucket (Optional).
   * @param delimiter Delimiter (Optional).
   * @param encodingType Encoding type (Optional).
   * @param keyMarker Key marker (Optional).
   * @param maxKeys Maximum object information to fetch (Optional).
   * @param prefix Prefix (Optional).
   * @param versionIdMarker Version ID marker (Optional).
   * @param extraHeaders Extra headers for request (Optional).
   * @param extraQueryParams Extra query parameters for request (Optional).
   * @return {@link ListObjectVersionsResponse} object.
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
  protected ListObjectVersionsResponse listObjectVersions(
      String bucketName,
      String region,
      String delimiter,
      String encodingType,
      String keyMarker,
      Integer maxKeys,
      String prefix,
      String versionIdMarker,
      Multimap<String, String> extraHeaders,
      Multimap<String, String> extraQueryParams)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Multimap<String, String> queryParams =
        merge(
            extraQueryParams,
            getCommonListObjectsQueryParams(delimiter, encodingType, maxKeys, prefix));
    if (keyMarker != null) queryParams.put("key-marker", keyMarker);
    if (versionIdMarker != null) queryParams.put("version-id-marker", versionIdMarker);
    queryParams.put("versions", "");

    try (Response response =
        execute(
            Method.GET,
            bucketName,
            null,
            getRegion(bucketName, region),
            extraHeaders,
            queryParams,
            null,
            0)) {
      ListVersionsResult result =
          Xml.unmarshal(ListVersionsResult.class, response.body().charStream());
      return new ListObjectVersionsResponse(response.headers(), bucketName, region, result);
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
            getRegion(bucketName, region),
            headers,
            extraQueryParams,
            data,
            length)) {
      return new ObjectWriteResponse(
          response.headers(),
          bucketName,
          region,
          objectName,
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
   * @param region Region of the bucket (Optional).
   * @param delimiter Delimiter (Optional).
   * @param encodingType Encoding type (Optional).
   * @param keyMarker Key marker (Optional).
   * @param maxUploads Maximum upload information to fetch (Optional).
   * @param prefix Prefix (Optional).
   * @param uploadIdMarker Upload ID marker (Optional).
   * @param extraHeaders Extra headers for request (Optional).
   * @param extraQueryParams Extra query parameters for request (Optional).
   * @return {@link ListMultipartUploadsResponse} object.
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
  protected ListMultipartUploadsResponse listMultipartUploads(
      String bucketName,
      String region,
      String delimiter,
      String encodingType,
      String keyMarker,
      Integer maxUploads,
      String prefix,
      String uploadIdMarker,
      Multimap<String, String> extraHeaders,
      Multimap<String, String> extraQueryParams)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Multimap<String, String> queryParams =
        merge(
            extraQueryParams,
            newMultimap(
                "uploads",
                "",
                "delimiter",
                (delimiter != null) ? delimiter : "",
                "max-uploads",
                (maxUploads != null) ? maxUploads.toString() : "1000",
                "prefix",
                (prefix != null) ? prefix : "",
                "encoding-type",
                "url"));
    if (encodingType != null) queryParams.put("encoding-type", encodingType);
    if (keyMarker != null) queryParams.put("key-marker", keyMarker);
    if (uploadIdMarker != null) queryParams.put("upload-id-marker", uploadIdMarker);

    try (Response response =
        execute(
            Method.GET,
            bucketName,
            null,
            getRegion(bucketName, region),
            extraHeaders,
            queryParams,
            null,
            0)) {
      ListMultipartUploadsResult result =
          Xml.unmarshal(ListMultipartUploadsResult.class, response.body().charStream());
      return new ListMultipartUploadsResponse(response.headers(), bucketName, region, result);
    }
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListParts.html">ListParts S3
   * API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param region Name of the bucket (Optional).
   * @param objectName Object name in the bucket.
   * @param maxParts Maximum parts information to fetch (Optional).
   * @param partNumberMarker Part number marker (Optional).
   * @param uploadId Upload ID.
   * @param extraHeaders Extra headers for request (Optional).
   * @param extraQueryParams Extra query parameters for request (Optional).
   * @return {@link ListPartsResponse} object.
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
  protected ListPartsResponse listParts(
      String bucketName,
      String region,
      String objectName,
      Integer maxParts,
      Integer partNumberMarker,
      String uploadId,
      Multimap<String, String> extraHeaders,
      Multimap<String, String> extraQueryParams)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    Multimap<String, String> queryParams =
        merge(
            extraQueryParams,
            newMultimap(
                UPLOAD_ID,
                uploadId,
                "max-parts",
                (maxParts != null) ? maxParts.toString() : "1000"));
    if (partNumberMarker != null)
      queryParams.put("part-number-marker", partNumberMarker.toString());

    try (Response response =
        execute(
            Method.GET,
            bucketName,
            objectName,
            getRegion(bucketName, region),
            extraHeaders,
            queryParams,
            null,
            0)) {
      ListPartsResult result = Xml.unmarshal(ListPartsResult.class, response.body().charStream());
      return new ListPartsResponse(response.headers(), bucketName, region, objectName, result);
    }
  }

  /**
   * Do <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart S3
   * API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param region Region of the bucket (Optional).
   * @param objectName Object name in the bucket.
   * @param data Object data must be BufferedInputStream, RandomAccessFile, byte[] or String.
   * @param length Length of object data.
   * @param uploadId Upload ID.
   * @param partNumber Part number.
   * @param extraHeaders Extra headers for request (Optional).
   * @param extraQueryParams Extra query parameters for request (Optional).
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
  protected UploadPartResponse uploadPart(
      String bucketName,
      String region,
      String objectName,
      Object data,
      int length,
      String uploadId,
      int partNumber,
      Multimap<String, String> extraHeaders,
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
            getRegion(bucketName, region),
            extraHeaders,
            merge(
                extraQueryParams,
                newMultimap("partNumber", Integer.toString(partNumber), UPLOAD_ID, uploadId)),
            data,
            length)) {
      return new UploadPartResponse(
          response.headers(),
          bucketName,
          region,
          objectName,
          uploadId,
          partNumber,
          response.header("ETag").replaceAll("\"", ""));
    }
  }

  /**
   * Do <a
   * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy
   * S3 API</a>.
   *
   * @param bucketName Name of the bucket.
   * @param region Region of the bucket (Optional).
   * @param objectName Object name in the bucket.
   * @param uploadId Upload ID.
   * @param partNumber Part number.
   * @param headers Request headers with source object definitions.
   * @param extraQueryParams Extra query parameters for request (Optional).
   * @return {@link UploadPartCopyResponse} object.
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
  protected UploadPartCopyResponse uploadPartCopy(
      String bucketName,
      String region,
      String objectName,
      String uploadId,
      int partNumber,
      Multimap<String, String> headers,
      Multimap<String, String> extraQueryParams)
      throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, ServerException,
          XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
    try (Response response =
        execute(
            Method.PUT,
            bucketName,
            objectName,
            getRegion(bucketName, region),
            headers,
            merge(
                extraQueryParams,
                newMultimap("partNumber", Integer.toString(partNumber), "uploadId", uploadId)),
            null,
            0)) {
      CopyPartResult result = Xml.unmarshal(CopyPartResult.class, response.body().charStream());
      return new UploadPartCopyResponse(
          response.headers(), bucketName, region, objectName, uploadId, partNumber, result);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    HttpUrl baseUrl;
    String region;
    OkHttpClient httpClient;
    boolean isAwsHost;
    boolean isAwsChinaHost;
    boolean isAcceleratedHost;
    boolean isDualStackHost;
    boolean useVirtualStyle;
    String regionInUrl;
    Provider provider;

    public Builder() {}

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

    private void setBaseUrl(HttpUrl url) {
      String host = url.host();
      this.isAwsHost = isAwsEndpoint(host);
      this.isAwsChinaHost = false;
      if (this.isAwsHost) {
        this.isAwsChinaHost = host.endsWith(".cn");
        url =
            url.newBuilder()
                .host(this.isAwsChinaHost ? "amazonaws.com.cn" : "amazonaws.com")
                .build();
        this.isAcceleratedHost = isAwsAccelerateEndpoint(host);
        this.isDualStackHost = isAwsDualStackEndpoint(host);
        this.regionInUrl = extractRegion(host);
        this.useVirtualStyle = true;
      } else {
        this.useVirtualStyle = host.endsWith("aliyuncs.com");
      }

      this.baseUrl = url;
    }

    /**
     * copied logic from
     * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/CustomTrust.java
     */
    private OkHttpClient enableExternalCertificates(OkHttpClient httpClient, String filename)
        throws GeneralSecurityException, IOException {
      Collection<? extends Certificate> certificates = null;
      try (FileInputStream fis = new FileInputStream(filename)) {
        certificates = CertificateFactory.getInstance("X.509").generateCertificates(fis);
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

    protected void validateNotNull(Object arg, String argName) {
      if (arg == null) {
        throw new IllegalArgumentException(argName + " must not be null.");
      }
    }

    protected void validateNotEmptyString(String arg, String argName) {
      validateNotNull(arg, argName);
      if (arg.isEmpty()) {
        throw new IllegalArgumentException(argName + " must be a non-empty string.");
      }
    }

    protected void validateNullOrNotEmptyString(String arg, String argName) {
      if (arg != null && arg.isEmpty()) {
        throw new IllegalArgumentException(argName + " must be a non-empty string.");
      }
    }

    private void validateUrl(HttpUrl url) {
      if (!url.encodedPath().equals("/")) {
        throw new IllegalArgumentException("no path allowed in endpoint " + url);
      }
    }

    private void validateHostnameOrIPAddress(String endpoint) {
      // Check endpoint is IPv4 or IPv6.
      if (InetAddressValidator.getInstance().isValid(endpoint)) {
        return;
      }

      // Check endpoint is a hostname.

      // Refer https://en.wikipedia.org/wiki/Hostname#Restrictions_on_valid_host_names
      // why checks are done like below
      if (endpoint.length() < 1 || endpoint.length() > 253) {
        throw new IllegalArgumentException("invalid hostname");
      }

      for (String label : endpoint.split("\\.")) {
        if (label.length() < 1 || label.length() > 63) {
          throw new IllegalArgumentException("invalid hostname");
        }

        if (!(label.matches("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$"))) {
          throw new IllegalArgumentException("invalid hostname");
        }
      }
    }

    private HttpUrl getBaseUrl(String endpoint) {
      validateNotEmptyString(endpoint, "endpoint");
      HttpUrl url = HttpUrl.parse(endpoint);
      if (url == null) {
        validateHostnameOrIPAddress(endpoint);
        url = new HttpUrl.Builder().scheme("https").host(endpoint).build();
      } else {
        validateUrl(url);
      }

      return url;
    }

    public Builder endpoint(String endpoint) {
      setBaseUrl(getBaseUrl(endpoint));
      return this;
    }

    public Builder endpoint(String endpoint, int port, boolean secure) {
      HttpUrl url = getBaseUrl(endpoint);
      if (port < 1 || port > 65535) {
        throw new IllegalArgumentException("port must be in range of 1 to 65535");
      }
      url = url.newBuilder().port(port).scheme(secure ? "https" : "http").build();

      setBaseUrl(url);
      return this;
    }

    /** Remove this method when all deprecated MinioClient constructors are removed. */
    private Builder endpoint(String endpoint, Integer port, Boolean secure) {
      HttpUrl url = getBaseUrl(endpoint);
      if (port != null) {
        if (port < 1 || port > 65535) {
          throw new IllegalArgumentException("port must be in range of 1 to 65535");
        }

        url = url.newBuilder().port(port).build();
      }

      if (secure != null) {
        url = url.newBuilder().scheme(secure ? "https" : "http").build();
      }

      setBaseUrl(url);
      return this;
    }

    public Builder endpoint(URL url) {
      validateNotNull(url, "url");
      return endpoint(HttpUrl.get(url));
    }

    public Builder endpoint(HttpUrl url) {
      validateNotNull(url, "url");
      validateUrl(url);
      setBaseUrl(url);
      return this;
    }

    public Builder region(String region) {
      validateNullOrNotEmptyString(region, "region");
      this.region = region;
      this.regionInUrl = region;
      return this;
    }

    public Builder credentials(String accessKey, String secretKey) {
      this.provider = new StaticProvider(accessKey, secretKey);
      return this;
    }

    public Builder credentialsProvider(Provider provider) {
      this.provider = provider;
      return this;
    }

    public Builder httpClient(OkHttpClient httpClient) {
      validateNotNull(httpClient, "http client");
      this.httpClient = httpClient;
      return this;
    }

    public MinioClient build() {
      validateNotNull(baseUrl, "endpoint");
      if (isAwsChinaHost && regionInUrl == null && region == null) {
        throw new IllegalArgumentException("Region missing in Amazon S3 China endpoint " + baseUrl);
      }

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
        if (filename != null && !filename.isEmpty()) {
          try {
            this.httpClient = enableExternalCertificates(this.httpClient, filename);
          } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
          }
        }
      }

      return new MinioClient(
          baseUrl,
          (region != null) ? region : regionInUrl,
          isAwsHost,
          isAcceleratedHost,
          isDualStackHost,
          useVirtualStyle,
          provider,
          httpClient);
    }
  }
}
