/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015, 2016, 2017 Minio, Inc.
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

import com.google.common.io.ByteStreams;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidExpiresRangeException;
import io.minio.errors.InvalidObjectPrefixException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.NoResponseException;
import io.minio.errors.RegionConflictException;
import io.minio.http.HeaderParser;
import io.minio.http.Method;
import io.minio.http.Scheme;
import io.minio.messages.Bucket;
import io.minio.messages.CompleteMultipartUpload;
import io.minio.messages.CopyObjectResult;
import io.minio.messages.CreateBucketConfiguration;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import io.minio.messages.ErrorResponse;
import io.minio.messages.InitiateMultipartUploadResult;
import io.minio.messages.Item;
import io.minio.messages.ListAllMyBucketsResult;
import io.minio.messages.ListBucketResult;
import io.minio.messages.ListBucketResultV1;
import io.minio.messages.ListMultipartUploadsResult;
import io.minio.messages.ListPartsResult;
import io.minio.messages.Part;
import io.minio.messages.Prefix;
import io.minio.messages.Upload;
import io.minio.messages.NotificationConfiguration;
import io.minio.org.apache.commons.validator.routines.InetAddressValidator;
import io.minio.policy.PolicyType;
import io.minio.policy.BucketPolicy;
import okio.BufferedSink;
import okio.Okio;
import org.joda.time.DateTime;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * <p>
 * This class implements a simple cloud storage client. This client consists
 * of a useful subset of S3 compatible functionality.
 * </p>
 * <h2>Service</h2>
 * <ul>
 * <li>Creating a bucket</li>
 * <li>Listing buckets</li>
 * </ul>
 * <h2>Bucket</h2>
 * <ul>
 * <li> Creating an object, including automatic upload resuming for large objects.</li>
 * <li> Listing objects in a bucket</li>
 * <li> Listing active multipart uploads</li>
 * </ul>
 * <h2>Object</h2>
 * <ul>
 * <li>Removing an active multipart upload for a specific object and uploadId</li>
 * <li>Read object metadata</li>
 * <li>Reading an object</li>
 * <li>Reading a range of bytes of an object</li>
 * <li>Deleting an object</li>
 * </ul>
 * <p>
 * Optionally, users can also provide access/secret keys. If keys are provided, all requests by the
 * client will be signed using AWS Signature Version 4.
 * </p>
 * For examples on using this library, please see
 * <a href="https://github.com/minio/minio-java/tree/master/src/test/java/io/minio/examples"></a>.
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public final class MinioClient {
  private static final Logger LOGGER = Logger.getLogger(MinioClient.class.getName());
  // maximum allowed object size is 5TiB
  private static final long MAX_OBJECT_SIZE = 5L * 1024 * 1024 * 1024 * 1024;
  private static final int MAX_MULTIPART_COUNT = 10000;
  // minimum allowed multipart size is 5MiB
  private static final int MIN_MULTIPART_SIZE = 5 * 1024 * 1024;
  // default expiration for a presigned URL is 7 days in seconds
  private static final int DEFAULT_EXPIRY_TIME = 7 * 24 * 3600;
  private static final String DEFAULT_USER_AGENT = "Minio (" + System.getProperty("os.arch") + "; "
      + System.getProperty("os.arch") + ") minio-java/" + MinioProperties.INSTANCE.getVersion();
  private static final String NULL_STRING = "(null)";
  private static final String S3_AMAZONAWS_COM = "s3.amazonaws.com";
  private static final String AMAZONAWS_COM = ".amazonaws.com";
  private static final String END_HTTP = "----------END-HTTP----------";
  private static final String US_EAST_1 = "us-east-1";
  private static final String UPLOAD_ID = "uploadId";

  private static XmlPullParserFactory xmlPullParserFactory = null;

  static {
    try {
      xmlPullParserFactory = XmlPullParserFactory.newInstance();
      xmlPullParserFactory.setNamespaceAware(true);
    } catch (XmlPullParserException e) {
      throw new ExceptionInInitializerError(e);
    }
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

  private OkHttpClient httpClient = new OkHttpClient();


  /**
   * Creates Minio client object with given endpoint using anonymous access.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code MinioClient minioClient = new MinioClient("https://play.minio.io:9000"); }</pre>
   *
   * @param endpoint  Request endpoint. Endpoint is an URL, domain name, IPv4 or IPv6 address.<pre>
   *              Valid endpoints:
   *              * https://s3.amazonaws.com
   *              * https://s3.amazonaws.com/
   *              * https://play.minio.io:9000
   *              * http://play.minio.io:9010/
   *              * localhost
   *              * localhost.localdomain
   *              * play.minio.io
   *              * 127.0.0.1
   *              * 192.168.1.60
   *              * ::1</pre>
   *
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
   */
  public MinioClient(String endpoint) throws InvalidEndpointException, InvalidPortException {
    this(endpoint, 0, null, null);
  }


  /**
   * Creates Minio client object with given URL object using anonymous access.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code MinioClient minioClient = new MinioClient(new URL("https://play.minio.io:9000")); }</pre>
   *
   * @param url Endpoint URL object.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
   */
  public MinioClient(URL url) throws InvalidEndpointException, InvalidPortException {
    this(url.toString(), 0, null, null);
  }

  /**
   * Creates Minio client object with given HttpUrl object using anonymous access.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code MinioClient minioClient = new MinioClient(new HttpUrl.parse("https://play.minio.io:9000")); }</pre>
   *
   * @param url Endpoint HttpUrl object.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
   */
  public MinioClient(HttpUrl url) throws InvalidEndpointException, InvalidPortException {
    this(url.toString(), 0, null, null);
  }

  /**
   * Creates Minio client object with given endpoint, access key and secret key.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code MinioClient minioClient = new MinioClient("https://play.minio.io:9000",
   *                            "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY"); }</pre>
   * @param endpoint  Request endpoint. Endpoint is an URL, domain name, IPv4 or IPv6 address.<pre>
   *              Valid endpoints:
   *              * https://s3.amazonaws.com
   *              * https://s3.amazonaws.com/
   *              * https://play.minio.io:9000
   *              * http://play.minio.io:9010/
   *              * localhost
   *              * localhost.localdomain
   *              * play.minio.io
   *              * 127.0.0.1
   *              * 192.168.1.60
   *              * ::1</pre>
   * @param accessKey Access key to access service in endpoint.
   * @param secretKey Secret key to access service in endpoint.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
   */
  public MinioClient(String endpoint, String accessKey, String secretKey)
    throws InvalidEndpointException, InvalidPortException {
    this(endpoint, 0, accessKey, secretKey);
  }

  /**
   * Creates Minio client object with given endpoint, access key, secret key and region name
   *
   * </p><b>Example:</b><br>
   * <pre>{@code MinioClient minioClient = new MinioClient("https://play.minio.io:9000",
   *                            "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", "us-east-1"); }</pre>
   * @param endpoint  Request endpoint. Endpoint is an URL, domain name, IPv4 or IPv6 address.<pre>
   *              Valid endpoints:
   *              * https://s3.amazonaws.com
   *              * https://s3.amazonaws.com/
   *              * https://play.minio.io:9000
   *              * http://play.minio.io:9010/
   *              * localhost
   *              * localhost.localdomain
   *              * play.minio.io
   *              * 127.0.0.1
   *              * 192.168.1.60
   *              * ::1</pre>
   * @param accessKey Access key to access service in endpoint.
   * @param secretKey Secret key to access service in endpoint.
   * @param region Region name to access service in endpoint.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
   */
  public MinioClient(String endpoint, String accessKey, String secretKey, String region)
    throws InvalidEndpointException, InvalidPortException {
    this(endpoint, 0, accessKey, secretKey, region, !(endpoint != null && endpoint.startsWith("http://")));
  }

  /**
   * Creates Minio client object with given URL object, access key and secret key.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code MinioClient minioClient = new MinioClient(new URL("https://play.minio.io:9000"),
   *                            "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY"); }</pre>
   *
   * @param url Endpoint URL object.
   * @param accessKey Access key to access service in endpoint.
   * @param secretKey Secret key to access service in endpoint.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
   */
  public MinioClient(URL url, String accessKey, String secretKey)
    throws InvalidEndpointException, InvalidPortException {
    this(url.toString(), 0, accessKey, secretKey);
  }

  /**
   * Creates Minio client object with given URL object, access key and secret key.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code MinioClient minioClient = new MinioClient(HttpUrl.parse("https://play.minio.io:9000"),
   *                            "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY"); }</pre>
   *
   * @param url Endpoint HttpUrl object.
   * @param accessKey Access key to access service in endpoint.
   * @param secretKey Secret key to access service in endpoint.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
   */
  public MinioClient(HttpUrl url, String accessKey, String secretKey)
      throws InvalidEndpointException, InvalidPortException {
    this(url.toString(), 0, accessKey, secretKey);
  }

  /**
   * Creates Minio client object with given endpoint, port, access key and secret key using secure (HTTPS) connection.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code MinioClient minioClient =
   *                  new MinioClient("play.minio.io", 9000, "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY");
   * }</pre>
   *
   * @param endpoint  Request endpoint. Endpoint is an URL, domain name, IPv4 or IPv6 address.<pre>
   *              Valid endpoints:
   *              * https://s3.amazonaws.com
   *              * https://s3.amazonaws.com/
   *              * https://play.minio.io:9000
   *              * http://play.minio.io:9010/
   *              * localhost
   *              * localhost.localdomain
   *              * play.minio.io
   *              * 127.0.0.1
   *              * 192.168.1.60
   *              * ::1</pre>
   *
   * @param port      Valid port.  It should be in between 1 and 65535.  Unused if endpoint is an URL.
   * @param accessKey Access key to access service in endpoint.
   * @param secretKey Secret key to access service in endpoint.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
   */
  public MinioClient(String endpoint, int port, String accessKey, String secretKey)
    throws InvalidEndpointException, InvalidPortException {
    this(endpoint, port, accessKey, secretKey, !(endpoint != null && endpoint.startsWith("http://")));
  }

  /**
   * Creates Minio client object with given endpoint, access key and secret key using secure (HTTPS) connection.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code MinioClient minioClient =
   *                      new MinioClient("play.minio.io:9000", "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", true);
   * }</pre>
   *
   * @param endpoint  Request endpoint. Endpoint is an URL, domain name, IPv4 or IPv6 address.<pre>
   *              Valid endpoints:
   *              * https://s3.amazonaws.com
   *              * https://s3.amazonaws.com/
   *              * https://play.minio.io:9000
   *              * http://play.minio.io:9010/
   *              * localhost
   *              * localhost.localdomain
   *              * play.minio.io
   *              * 127.0.0.1
   *              * 192.168.1.60
   *              * ::1</pre>
   *
   * @param accessKey Access key to access service in endpoint.
   * @param secretKey Secret key to access service in endpoint.
   * @param secure If true, access endpoint using HTTPS else access it using HTTP.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
   */
  public MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
    throws InvalidEndpointException, InvalidPortException {
    this(endpoint, 0, accessKey, secretKey, secure);
  }

  /**
   * Creates Minio client object using given endpoint, port, access key, secret key and secure option.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code MinioClient minioClient =
   *          new MinioClient("play.minio.io", 9000, "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", false);
   * }</pre>
   *
   * @param endpoint  Request endpoint. Endpoint is an URL, domain name, IPv4 or IPv6 address.<pre>
   *              Valid endpoints:
   *              * https://s3.amazonaws.com
   *              * https://s3.amazonaws.com/
   *              * https://play.minio.io:9000
   *              * http://play.minio.io:9010/
   *              * localhost
   *              * localhost.localdomain
   *              * play.minio.io
   *              * 127.0.0.1
   *              * 192.168.1.60
   *              * ::1</pre>
   *
   * @param port      Valid port.  It should be in between 1 and 65535.  Unused if endpoint is an URL.
   * @param accessKey Access key to access service in endpoint.
   * @param secretKey Secret key to access service in endpoint.
   * @param secure    If true, access endpoint using HTTPS else access it using HTTP.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
   */
  public MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
    throws InvalidEndpointException, InvalidPortException {
    this(endpoint, port, accessKey, secretKey, null, secure);
  }

  /**
   * Creates Minio client object using given endpoint, port, access key, secret key, region and secure option.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code MinioClient minioClient =
   *          new MinioClient("play.minio.io", 9000, "YOUR-ACCESSKEYID", "YOUR-SECRETACCESSKEY", "us-east-1", false);
   * }</pre>
   *
   * @param endpoint  Request endpoint. Endpoint is an URL, domain name, IPv4 or IPv6 address.<pre>
   *              Valid endpoints:
   *              * https://s3.amazonaws.com
   *              * https://s3.amazonaws.com/
   *              * https://play.minio.io:9000
   *              * http://play.minio.io:9010/
   *              * localhost
   *              * localhost.localdomain
   *              * play.minio.io
   *              * 127.0.0.1
   *              * 192.168.1.60
   *              * ::1</pre>
   *
   * @param port      Valid port.  It should be in between 1 and 65535.  Unused if endpoint is an URL.
   * @param accessKey Access key to access service in endpoint.
   * @param secretKey Secret key to access service in endpoint.
   * @param region    Region name to access service in endpoint.
   * @param secure    If true, access endpoint using HTTPS else access it using HTTP.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, String region)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
   */
  public MinioClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
    throws InvalidEndpointException, InvalidPortException {
    if (endpoint == null) {
      throw new InvalidEndpointException(NULL_STRING, "null endpoint");
    }

    if (port < 0 || port > 65535) {
      throw new InvalidPortException(port, "port must be in range of 1 to 65535");
    }

    HttpUrl url = HttpUrl.parse(endpoint);
    if (url != null) {
      if (!"/".equals(url.encodedPath())) {
        throw new InvalidEndpointException(endpoint, "no path allowed in endpoint");
      }

      // treat Amazon S3 host as special case
      String amzHost = url.host();
      if (amzHost.endsWith(AMAZONAWS_COM) && !amzHost.equals(S3_AMAZONAWS_COM)) {
        throw new InvalidEndpointException(endpoint, "for Amazon S3, host should be 's3.amazonaws.com' in endpoint");
      }

      HttpUrl.Builder urlBuilder = url.newBuilder();
      Scheme scheme = Scheme.HTTP;
      if (secure) {
        scheme = Scheme.HTTPS;
      }

      urlBuilder.scheme(scheme.toString());

      if (port > 0) {
        urlBuilder.port(port);
      }

      this.baseUrl = urlBuilder.build();
      this.accessKey = accessKey;
      this.secretKey = secretKey;
      this.region = region;

      return;
    }

    // endpoint may be a valid hostname, IPv4 or IPv6 address
    if (!this.isValidEndpoint(endpoint)) {
      throw new InvalidEndpointException(endpoint, "invalid host");
    }

    // treat Amazon S3 host as special case
    if (endpoint.endsWith(AMAZONAWS_COM) && !endpoint.equals(S3_AMAZONAWS_COM)) {
      throw new InvalidEndpointException(endpoint, "for amazon S3, host should be 's3.amazonaws.com'");
    }

    Scheme scheme = Scheme.HTTP;
    if (secure) {
      scheme = Scheme.HTTPS;
    }

    if (port == 0) {
      this.baseUrl = new HttpUrl.Builder()
          .scheme(scheme.toString())
          .host(endpoint)
          .build();
    } else {
      this.baseUrl = new HttpUrl.Builder()
          .scheme(scheme.toString())
          .host(endpoint)
          .port(port)
          .build();
    }
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.region = region;
  }

  /**
   * Returns true if given endpoint is valid else false.
   */
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

  /**
   * Validates if given objectPrefix is valid.
   */
  private void checkObjectPrefix(String prefix) throws InvalidObjectPrefixException {
    // TODO(nl5887): what to do with wild-cards in objectPrefix?
    if (prefix.length() > 1024) {
      throw new InvalidObjectPrefixException(prefix, "Object prefix cannot be greater than 1024 characters.");
    }

    try {
      prefix.getBytes("UTF-8");
    } catch (java.io.UnsupportedEncodingException exc) {
      throw new InvalidObjectPrefixException(prefix, "Object prefix cannot be properly encoded to utf-8.");
    }
  }

  /**
   * Validates if given bucket name is DNS compatible.
   */
  private void checkBucketName(String name) throws InvalidBucketNameException {
    if (name == null) {
      throw new InvalidBucketNameException(NULL_STRING, "null bucket name");
    }

    // Bucket names cannot be no less than 3 and no more than 63 characters long.
    if (name.length() < 3 || name.length() > 63) {
      String msg = "bucket name must be at least 3 and no more than 63 characters long";
      throw new InvalidBucketNameException(name, msg);
    }
    // Successive periods in bucket names are not allowed.
    if (name.matches("\\.\\.")) {
      String msg = "bucket name cannot contain successive periods. For more information refer "
          + "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
      throw new InvalidBucketNameException(name, msg);
    }
    // Bucket names should be dns compatible.
    if (!name.matches("^[a-z0-9][a-z0-9\\.\\-]+[a-z0-9]$")) {
      String msg = "bucket name does not follow Amazon S3 standards. For more information refer "
          + "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
      throw new InvalidBucketNameException(name, msg);
    }
  }


  /**
   * Sets HTTP connect, write and read timeouts.  A value of 0 means no timeout, otherwise values must be between 1 and
   * Integer.MAX_VALUE when converted to milliseconds.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code minioClient.setTimeout(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10),
   *                            TimeUnit.SECONDS.toMillis(30)); }</pre>
   *
   * @param connectTimeout    HTTP connect timeout in milliseconds.
   * @param writeTimeout      HTTP write timeout in milliseconds.
   * @param readTimeout       HTTP read timeout in milliseconds.
   */
  public void setTimeout(long connectTimeout, long writeTimeout, long readTimeout) {
    httpClient.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
    httpClient.setWriteTimeout(writeTimeout, TimeUnit.MILLISECONDS);
    httpClient.setReadTimeout(readTimeout, TimeUnit.MILLISECONDS);
  }

  /**
   * Creates Request object for given request parameters.
   *
   * @param method         HTTP method.
   * @param bucketName     Bucket name.
   * @param objectName     Object name in the bucket.
   * @param region         Amazon S3 region of the bucket.
   * @param headerMap      Map of HTTP headers for the request.
   * @param queryParamMap  Map of HTTP query parameters of the request.
   * @param contentType    Content type of the request body.
   * @param body           HTTP request body.
   * @param length         Length of HTTP request body.
   */
  private Request createRequest(Method method, String bucketName, String objectName,
                                String region, Map<String,String> headerMap,
                                Map<String,String> queryParamMap, final String contentType,
                                final Object body, final int length)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException {
    if (bucketName == null && objectName != null) {
      throw new InvalidBucketNameException(NULL_STRING, "null bucket name for object '" + objectName + "'");
    }

    HttpUrl.Builder urlBuilder = this.baseUrl.newBuilder();

    if (bucketName != null) {
      checkBucketName(bucketName);

      String host = this.baseUrl.host();
      if (host.equals(S3_AMAZONAWS_COM)) {
        // special case: handle s3.amazonaws.com separately
        if (region != null) {
          host = AwsS3Endpoints.INSTANCE.endpoint(region);
        }

        boolean usePathStyle = false;
        if (method == Method.PUT && objectName == null && queryParamMap == null) {
          // use path style for make bucket to workaround "AuthorizationHeaderMalformed" error from s3.amazonaws.com
          usePathStyle = true;
        } else if (queryParamMap != null && queryParamMap.containsKey("location")) {
          // use path style for location query
          usePathStyle = true;
        } else if (bucketName.contains(".") && this.baseUrl.isHttps()) {
          // use path style where '.' in bucketName causes SSL certificate validation error
          usePathStyle = true;
        }

        if (usePathStyle) {
          urlBuilder.host(host);
          urlBuilder.addEncodedPathSegment(S3Escaper.encode(bucketName));
        } else {
          urlBuilder.host(bucketName + "." + host);
        }
      } else {
        urlBuilder.addEncodedPathSegment(S3Escaper.encode(bucketName));
      }
    }

    if (objectName != null) {
      // Limitation: OkHttp does not allow to add '.' and '..' as path segment.
      for (String pathSegment : objectName.split("/")) {
        urlBuilder.addEncodedPathSegment(S3Escaper.encode(pathSegment));
      }
    }

    if (queryParamMap != null) {
      for (Map.Entry<String,String> entry : queryParamMap.entrySet()) {
        urlBuilder.addEncodedQueryParameter(S3Escaper.encode(entry.getKey()), S3Escaper.encode(entry.getValue()));
      }
    }

    HttpUrl url = urlBuilder.build();

    RequestBody requestBody = null;
    if (body != null) {
      requestBody = new RequestBody() {
        @Override
        public MediaType contentType() {
          MediaType mediaType = null;

          if (contentType != null) {
            mediaType = MediaType.parse(contentType);
          }
          if (mediaType == null) {
            mediaType = MediaType.parse("application/octet-stream");
          }

          return mediaType;
        }

        @Override
        public long contentLength() {
          if (body instanceof InputStream || body instanceof RandomAccessFile || body instanceof byte[]) {
            return length;
          }

          if (length == 0) {
            return -1;
          } else {
            return length;
          }
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
          if (body instanceof InputStream) {
            InputStream stream = (InputStream) body;
            sink.write(Okio.source(stream), length);
          } else if (body instanceof RandomAccessFile) {
            RandomAccessFile file = (RandomAccessFile) body;
            sink.write(Okio.source(Channels.newInputStream(file.getChannel())), length);
          } else if (body instanceof byte[]) {
            byte[] data = (byte[]) body;
            sink.write(data, 0, length);
          } else {
            sink.writeUtf8(body.toString());
          }
        }
      };
    }

    Request.Builder requestBuilder = new Request.Builder();
    requestBuilder.url(url);
    requestBuilder.method(method.toString(), requestBody);
    if (headerMap != null) {
      for (Map.Entry<String,String> entry : headerMap.entrySet()) {
        requestBuilder.header(entry.getKey(), entry.getValue());
      }
    }

    String sha256Hash = null;
    String md5Hash = null;
    if (this.accessKey != null && this.secretKey != null) {
      // No need to compute sha256 if endpoint scheme is HTTPS. Issue #415.
      if (url.isHttps()) {
        sha256Hash = "UNSIGNED-PAYLOAD";
        if (body instanceof BufferedInputStream) {
          md5Hash = Digest.md5Hash((BufferedInputStream) body, length);
        } else if (body instanceof RandomAccessFile) {
          md5Hash = Digest.md5Hash((RandomAccessFile) body, length);
        } else if (body instanceof byte[]) {
          byte[] data = (byte[]) body;
          md5Hash = Digest.md5Hash(data, length);
        }
      } else {
        if (body == null) {
          sha256Hash = Digest.sha256Hash(new byte[0]);
        } else {
          if (body instanceof BufferedInputStream) {
            String[] hashes = Digest.sha256md5Hashes((BufferedInputStream) body, length);
            sha256Hash = hashes[0];
            md5Hash = hashes[1];
          } else if (body instanceof RandomAccessFile) {
            String[] hashes = Digest.sha256md5Hashes((RandomAccessFile) body, length);
            sha256Hash = hashes[0];
            md5Hash = hashes[1];
          } else if (body instanceof byte[]) {
            byte[] data = (byte[]) body;
            sha256Hash = Digest.sha256Hash(data, length);
            md5Hash = Digest.md5Hash(data, length);
          } else {
            sha256Hash = Digest.sha256Hash(body.toString());
          }
        }
      }
    }

    if (md5Hash != null) {
      requestBuilder.header("Content-MD5", md5Hash);
    }
    if (url.port() == 80 || url.port() == 443) {
      requestBuilder.header("Host", url.host());
    } else {
      requestBuilder.header("Host", url.host() + ":" + url.port());
    }
    requestBuilder.header("User-Agent", this.userAgent);
    if (sha256Hash != null) {
      requestBuilder.header("x-amz-content-sha256", sha256Hash);
    }
    DateTime date = new DateTime();
    requestBuilder.header("x-amz-date", date.toString(DateFormat.AMZ_DATE_FORMAT));

    return requestBuilder.build();
  }


  /**
   * Executes given request parameters.
   *
   * @param method         HTTP method.
   * @param region         Amazon S3 region of the bucket.
   * @param bucketName     Bucket name.
   * @param objectName     Object name in the bucket.
   * @param headerMap      Map of HTTP headers for the request.
   * @param queryParamMap  Map of HTTP query parameters of the request.
   * @param contentType    Content type of the request body.
   * @param body           HTTP request body.
   * @param length         Length of HTTP request body.
   */
  private HttpResponse execute(Method method, String region, String bucketName, String objectName,
                               Map<String,String> headerMap, Map<String,String> queryParamMap,
                               Object body, int length)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    String contentType = null;
    if (headerMap != null) {
      contentType = headerMap.get("Content-Type");
    }
    if (body != null && !(body instanceof InputStream || body instanceof RandomAccessFile || body instanceof byte[])) {
      byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
      body = bytes;
      length = bytes.length;
    }

    Request request = createRequest(method, bucketName, objectName, region,
                                    headerMap, queryParamMap,
                                    contentType, body, length);

    if (this.accessKey != null && this.secretKey != null) {
      request = Signer.signV4(request, region, accessKey, secretKey);
    }

    if (this.traceStream != null) {
      this.traceStream.println("---------START-HTTP---------");
      String encodedPath = request.httpUrl().encodedPath();
      String encodedQuery = request.httpUrl().encodedQuery();
      if (encodedQuery != null) {
        encodedPath += "?" + encodedQuery;
      }
      this.traceStream.println(request.method() + " " + encodedPath + " HTTP/1.1");
      String headers = request.headers().toString().replaceAll("Signature=([0-9a-f]+)", "Signature=*REDACTED*");
      this.traceStream.println(headers);
    }

    Response response = this.httpClient.newCall(request).execute();
    if (response == null) {
      if (this.traceStream != null) {
        this.traceStream.println("<NO RESPONSE>");
        this.traceStream.println(END_HTTP);
      }
      throw new NoResponseException();
    }

    if (this.traceStream != null) {
      this.traceStream.println(response.protocol().toString().toUpperCase() + " " + response.code());
      this.traceStream.println(response.headers());
    }

    ResponseHeader header = new ResponseHeader();
    HeaderParser.set(response.headers(), header);

    if (response.isSuccessful()) {
      if (this.traceStream != null) {
        this.traceStream.println(END_HTTP);
      }
      return new HttpResponse(header, response);
    }

    ErrorResponse errorResponse = null;

    // HEAD returns no body, and fails on parseXml
    if (!method.equals(Method.HEAD)) {
      Scanner scanner = new Scanner(response.body().charStream());
      try {
        scanner.useDelimiter("\\A");
        String errorXml = "";

        // read entire body stream to string.
        if (scanner.hasNext()) {
          errorXml = scanner.next();
        }

        errorResponse = new ErrorResponse(new StringReader(errorXml));
        if (this.traceStream != null) {
          this.traceStream.println(errorXml);
        }

      } finally {
        response.body().close();
        scanner.close();
      }
    }

    if (this.traceStream != null) {
      this.traceStream.println(END_HTTP);
    }

    if (errorResponse == null) {
      ErrorCode ec;
      switch (response.code()) {
        case 400:
          ec = ErrorCode.INVALID_URI;
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
          throw new InternalException("unhandled HTTP code " + response.code() + ".  Please report this issue at "
                                      + "https://github.com/minio/minio-java/issues");
      }

      errorResponse = new ErrorResponse(ec, bucketName, objectName, request.httpUrl().encodedPath(),
                                        header.xamzRequestId(), header.xamzId2());
    }

    // invalidate region cache if needed
    if (errorResponse.errorCode() == ErrorCode.NO_SUCH_BUCKET) {
      BucketRegionCache.INSTANCE.remove(bucketName);
      // TODO: handle for other cases as well
      // observation: on HEAD of a bucket with wrong region gives 400 without body
    }

    throw new ErrorResponseException(errorResponse, response);
  }

  /**
   * Updates Region cache for given bucket.
   */
  private void updateRegionCache(String bucketName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    if (bucketName != null && this.accessKey != null && this.secretKey != null
        && !BucketRegionCache.INSTANCE.exists(bucketName)) {
      Map<String,String> queryParamMap = new HashMap<>();
      queryParamMap.put("location", null);

      HttpResponse response = execute(Method.GET, US_EAST_1, bucketName, null,
          null, queryParamMap, null, 0);

      // existing XmlEntity does not work, so fallback to regular parsing.
      XmlPullParser xpp = xmlPullParserFactory.newPullParser();
      String location = null;

      xpp.setInput(response.body().charStream());
      while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
        if (xpp.getEventType() ==  XmlPullParser.START_TAG && xpp.getName() == "LocationConstraint") {
          xpp.next();
          location = getText(xpp, location);
          break;
        }
        xpp.next();
      }

      // close response body.
      response.body().close();

      String region;
      if (location == null) {
        region = US_EAST_1;
      } else {
        // eu-west-1 can be sometimes 'EU'.
        if ("EU".equals(location)) {
          region = "eu-west-1";
        } else {
          region = location;
        }
      }

      // Add the new location.
      BucketRegionCache.INSTANCE.set(bucketName, region);
    }
  }

  /**
   * Computes region of a given bucket name. If set, this.region is considered. Otherwise,
   * resort to the server location API.
   */
  private String getRegion(String bucketName) throws InvalidBucketNameException, NoSuchAlgorithmException,
          InsufficientDataException, IOException, InvalidKeyException, NoResponseException, XmlPullParserException,
          ErrorResponseException, InternalException {
    String region;
    if (this.region == null || "".equals(this.region)) {
      updateRegionCache(bucketName);
      region = BucketRegionCache.INSTANCE.region(bucketName);
    } else {
      region = this.region;
    }
    return region;
  }

  /**
   * Returns text of given XML element.
   */
  private String getText(XmlPullParser xpp, String location) throws XmlPullParserException {
    if (xpp.getEventType() == XmlPullParser.TEXT) {
      return xpp.getText();
    }
    return location;
  }


  /**
   * Executes GET method for given request parameters.
   *
   * @param bucketName     Bucket name.
   * @param objectName     Object name in the bucket.
   * @param headerMap      Map of HTTP headers for the request.
   * @param queryParamMap  Map of HTTP query parameters of the request.
   */
  private HttpResponse executeGet(String bucketName, String objectName, Map<String,String> headerMap,
                                  Map<String,String> queryParamMap)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    return execute(Method.GET, getRegion(bucketName), bucketName, objectName, headerMap, queryParamMap, null, 0);
  }


  /**
   * Executes HEAD method for given request parameters.
   *
   * @param bucketName     Bucket name.
   * @param objectName     Object name in the bucket.
   */
  private HttpResponse executeHead(String bucketName, String objectName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {

    HttpResponse response = execute(Method.HEAD, getRegion(bucketName), bucketName, objectName, null,
                                    null, null, 0);
    response.body().close();
    return response;
  }


  /**
   * Executes DELETE method for given request parameters.
   *
   * @param bucketName     Bucket name.
   * @param objectName     Object name in the bucket.
   * @param queryParamMap  Map of HTTP query parameters of the request.
   */
  private HttpResponse executeDelete(String bucketName, String objectName, Map<String,String> queryParamMap)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    HttpResponse response = execute(Method.DELETE, getRegion(bucketName), bucketName, objectName, null,
                                    queryParamMap, null, 0);
    response.body().close();
    return response;
  }


  /**
   * Executes POST method for given request parameters.
   *
   * @param bucketName     Bucket name.
   * @param objectName     Object name in the bucket.
   * @param headerMap      Map of HTTP headers for the request.
   * @param queryParamMap  Map of HTTP query parameters of the request.
   * @param data           HTTP request body data.
   */
  private HttpResponse executePost(String bucketName, String objectName, Map<String,String> headerMap,
                                   Map<String,String> queryParamMap, Object data)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    return execute(Method.POST, getRegion(bucketName), bucketName, objectName, headerMap, queryParamMap, data, 0);
  }


  /**
   * Executes PUT method for given request parameters.
   *
   * @param bucketName     Bucket name.
   * @param objectName     Object name in the bucket.
   * @param headerMap      Map of HTTP headers for the request.
   * @param queryParamMap  Map of HTTP query parameters of the request.
   * @param region         Amazon S3 region of the bucket.
   * @param data           HTTP request body data.
   * @param length         Length of HTTP request body data.
   */
  private HttpResponse executePut(String bucketName, String objectName, Map<String,String> headerMap,
                                  Map<String,String> queryParamMap, String region, Object data, int length)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    HttpResponse response = execute(Method.PUT, region, bucketName, objectName,
                                    headerMap, queryParamMap,
                                    data, length);
    return response;
  }


  /**
   * Executes PUT method for given request parameters.
   *
   * @param bucketName     Bucket name.
   * @param objectName     Object name in the bucket.
   * @param headerMap      Map of HTTP headers for the request.
   * @param queryParamMap  Map of HTTP query parameters of the request.
   * @param data           HTTP request body data.
   * @param length         Length of HTTP request body data.
   */
  private HttpResponse executePut(String bucketName, String objectName, Map<String,String> headerMap,
                                  Map<String,String> queryParamMap, Object data, int length)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    return executePut(bucketName, objectName, headerMap, queryParamMap, getRegion(bucketName), data, length);
  }


  /**
   * Sets application's name/version to user agent. For more information about user agent
   * refer <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">#rfc2616</a>.
   *
   * @param name     Your application name.
   * @param version  Your application version.
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
   * Returns meta data information of given object in given bucket.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code ObjectStat objectStat = minioClient.statObject("my-bucketname", "my-objectname");
   * System.out.println(objectStat); }</pre>
   *
   * @param bucketName Bucket name.
   * @param objectName Object name in the bucket.
   *
   * @return Populated object metadata.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   * @see ObjectStat
   */
  public ObjectStat statObject(String bucketName, String objectName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    HttpResponse response = executeHead(bucketName, objectName);
    ResponseHeader header = response.header();
    return new ObjectStat(bucketName, objectName, header.lastModified(), header.contentLength(),
                          header.etag(), header.contentType());
  }


  /**
   * Gets object's URL in given bucket.  The URL is ONLY useful to retrieve the object's data if the object has
   * public read permissions.
   *
   * <p><b>Example:</b>
   * <pre>{@code String url = minioClient.getObjectUrl("my-bucketname", "my-objectname");
   * System.out.println(url); }</pre>
   *
   * @param bucketName Bucket name.
   * @param objectName Object name in the bucket.
   *
   * @return string contains URL to download the object.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public String getObjectUrl(String bucketName, String objectName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    Request request = createRequest(Method.GET, bucketName, objectName, getRegion(bucketName),
        null, null, null, null, 0);
    HttpUrl url = request.httpUrl();
    return url.toString();
  }


  /**
   * Gets entire object's data as {@link InputStream} in given bucket. The InputStream must be closed
   * after use else the connection will remain open.
   *
   * <p><b>Example:</b>
   * <pre>{@code InputStream stream = minioClient.getObject("my-bucketname", "my-objectname");
   * byte[] buf = new byte[16384];
   * int bytesRead;
   * while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
   *   System.out.println(new String(buf, 0, bytesRead));
   * }
   * stream.close(); }</pre>
   *
   * @param bucketName Bucket name.
   * @param objectName Object name in the bucket.
   *
   * @return {@link InputStream} containing the object data.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public InputStream getObject(String bucketName, String objectName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException,
           InvalidArgumentException {
    return getObject(bucketName, objectName, 0, null);
  }


  /**
   * Gets object's data starting from given offset as {@link InputStream} in the given bucket. The InputStream must be
   * closed after use else the connection will remain open.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code InputStream stream = minioClient.getObject("my-bucketname", "my-objectname", 1024L);
   * byte[] buf = new byte[16384];
   * int bytesRead;
   * while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
   *   System.out.println(new String(buf, 0, bytesRead));
   * }
   * stream.close(); }</pre>
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name in the bucket.
   * @param offset      Offset to read at.
   *
   * @return {@link InputStream} containing the object's data.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public InputStream getObject(String bucketName, String objectName, long offset)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException,
           InvalidArgumentException {
    return getObject(bucketName, objectName, offset, null);
  }


  /**
   * Gets object's data of given offset and length as {@link InputStream} in the given bucket. The InputStream must be
   * closed after use else the connection will remain open.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code InputStream stream = minioClient.getObject("my-bucketname", "my-objectname", 1024L, 4096L);
   * byte[] buf = new byte[16384];
   * int bytesRead;
   * while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
   *   System.out.println(new String(buf, 0, bytesRead));
   * }
   * stream.close(); }</pre>
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name in the bucket.
   * @param offset      Offset to read at.
   * @param length      Length to read.
   *
   * @return {@link InputStream} containing the object's data.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public InputStream getObject(String bucketName, String objectName, long offset, Long length)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException,
           InvalidArgumentException {
    if (offset < 0) {
      throw new InvalidArgumentException("offset should be zero or greater");
    }

    if (length != null && length <= 0) {
      throw new InvalidArgumentException("length should be greater than zero");
    }

    Map<String,String> headerMap = new HashMap<>();
    if (length != null) {
      headerMap.put("Range", "bytes=" + offset + "-" + (offset + length - 1));
    } else {
      headerMap.put("Range", "bytes=" + offset + "-");
    }

    HttpResponse response = executeGet(bucketName, objectName, headerMap, null);
    return response.body().byteStream();
  }


  /**
   * Gets object's data in the given bucket and stores it to given file name.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code minioClient.getObject("my-bucketname", "my-objectname", "photo.jpg"); }</pre>
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name in the bucket.
   * @param fileName    file name.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void getObject(String bucketName, String objectName, String fileName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException, InvalidArgumentException {
    Path filePath = Paths.get(fileName);
    boolean fileExists = Files.exists(filePath);

    if (fileExists && !Files.isRegularFile(filePath)) {
      throw new InvalidArgumentException(fileName + ": not a regular file");
    }

    ObjectStat objectStat = statObject(bucketName, objectName);
    long length = objectStat.length();
    String etag = objectStat.etag();

    String tempFileName = fileName + "." + etag + ".part.minio";
    Path tempFilePath = Paths.get(tempFileName);
    boolean tempFileExists = Files.exists(tempFilePath);

    if (tempFileExists && !Files.isRegularFile(tempFilePath)) {
      throw new IOException(tempFileName + ": not a regular file");
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
        throw new InvalidArgumentException("'" + fileName + "': object size " + length + " is smaller than file size "
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
      is = getObject(bucketName, objectName, tempFileSize);
      os = Files.newOutputStream(tempFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      long bytesWritten = ByteStreams.copy(is, os);
      is.close();
      os.close();

      if (bytesWritten != length - tempFileSize) {
        throw new IOException(tempFileName + ": unexpected data written.  expected = " + (length - tempFileSize)
                                + ", written = " + bytesWritten);
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
   * Copy a source object into a new destination object with same object name.
   *
   * </p>
   * <b>Example:</b><br>
   *
   * <pre>
   * {@code minioClient.copyObject("my-bucketname", "my-objectname", "my-destbucketname");}
   * </pre>
   *
   * @param bucketName
   *          Bucket name where the object to be copied exists.
   * @param objectName
   *          Object name source to be copied.
   * @param destBucketName
   *          Bucket name where the object will be copied to.
   *
   * @throws InvalidBucketNameException
   *           upon an invalid bucket name
   * @throws NoSuchAlgorithmException
   *           upon requested algorithm was not found during signature calculation
   * @throws InvalidKeyException
   *           upon an invalid access key or secret key
   */
  public void copyObject(String bucketName, String objectName, String destBucketName)
      throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
      NoResponseException, ErrorResponseException, InternalException, IOException, XmlPullParserException,
      InvalidArgumentException {

    copyObject(bucketName, objectName, destBucketName, null, null);
  }

  /**
   * Copy a source object into a new destination object.
   *
   * </p>
   * <b>Example:</b><br>
   *
   * <pre>
   * {@code minioClient.copyObject("my-bucketname", "my-objectname", "my-destbucketname", "my-destobjname");}
   * </pre>
   *
   * @param bucketName
   *          Bucket name where the object to be copied exists.
   * @param objectName
   *          Object name source to be copied.
   * @param destBucketName
   *          Bucket name where the object will be copied to.
   * @param destObjectName
   *          Object name to be created, if not provided uses source object name
   *          as destination object name.
   *
   * @throws InvalidBucketNameException
   *           upon an invalid bucket name
   * @throws NoSuchAlgorithmException
   *           upon requested algorithm was not found during signature calculation
   * @throws InvalidKeyException
   *           upon an invalid access key or secret key
   */
  public void copyObject(String bucketName, String objectName, String destBucketName, String destObjectName)
      throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
      NoResponseException, ErrorResponseException, InternalException, IOException, XmlPullParserException,
      InvalidArgumentException {

    copyObject(bucketName, objectName, destBucketName, destObjectName, null);
  }

  /**
   * Copy a source object into a new object with the provided name in the provided bucket.
   * optionally can take a key value CopyConditions as well for conditionally attempting
   * copyObject.
   *
   * </p>
   * <b>Example:</b><br>
   *
   * <pre>
   * {@code minioClient.copyObject("my-bucketname", "my-objectname", "my-destbucketname",
   * copyConditions);}
   * </pre>
   *
   * @param bucketName
   *          Bucket name where the object to be copied exists.
   * @param objectName
   *          Object name source to be copied.
   * @param destBucketName
   *          Bucket name where the object will be copied to.
   * @param copyConditions
   *          CopyConditions object with collection of supported CopyObject conditions.
   *
   * @throws InvalidBucketNameException
   *           upon an invalid bucket name
   * @throws NoSuchAlgorithmException
   *           upon requested algorithm was not found during signature calculation
   * @throws InvalidKeyException
   *           upon an invalid access key or secret key
   */
  public void copyObject(String bucketName, String objectName, String destBucketName,
                         CopyConditions copyConditions)
        throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
        NoResponseException, ErrorResponseException, InternalException, IOException, XmlPullParserException,
        InvalidArgumentException {

    copyObject(bucketName, objectName, destBucketName, null, copyConditions);
  }

  /**
   * Copy a source object into a new object with the provided name in the provided bucket.
   * optionally can take a key value CopyConditions as well for conditionally attempting
   * copyObject.
   *
   * </p>
   * <b>Example:</b><br>
   *
   * <pre>
   * {@code minioClient.copyObject("my-bucketname", "my-objectname", "my-destbucketname",
   * "my-destobjname", copyConditions);}
   * </pre>
   *
   * @param bucketName
   *          Bucket name where the object to be copied exists.
   * @param objectName
   *          Object name source to be copied.
   * @param destBucketName
   *          Bucket name where the object will be copied to.
   * @param destObjectName
   *          Object name to be created, if not provided uses source object name
   *          as destination object name.
   * @param copyConditions
   *          CopyConditions object with collection of supported CopyObject conditions.
   *
   * @throws InvalidBucketNameException
   *           upon an invalid bucket name, invalid object name.
   * @throws NoSuchAlgorithmException
   *           upon requested algorithm was not found during signature calculation
   * @throws InvalidKeyException
   *           upon an invalid access key or secret key
   */
  public void copyObject(String bucketName, String objectName, String destBucketName,
                         String destObjectName, CopyConditions copyConditions)
      throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
      NoResponseException, ErrorResponseException, InternalException, IOException, XmlPullParserException,
      InvalidArgumentException {

    if (bucketName == null) {
      throw new InvalidArgumentException("Source bucket name cannot be empty");
    }
    if (objectName == null) {
      throw new InvalidArgumentException("Source object name cannot be empty");
    }
    if (destBucketName == null) {
      throw new InvalidArgumentException("Destination bucket name cannot be empty");
    }

    // Escape source object path.
    String sourceObjectPath = S3Escaper.encode(Paths.get(bucketName, objectName).toString());

    // Destination object name is optional, if empty default to source object name.
    if (destObjectName == null) {
      destObjectName = objectName;
    }

    Map<String, String> headerMap = new HashMap<>();

    // Set the object source
    headerMap.put("x-amz-copy-source", sourceObjectPath);

    // If no conditions available, skip addition else add the conditions to the header
    if (copyConditions != null) {
      headerMap.putAll(copyConditions.getConditions());
    }

    HttpResponse response = executePut(destBucketName, destObjectName, headerMap,
        null, "", 0);

    // For now ignore the copyObjectResult, just read and parse it.
    CopyObjectResult result = new CopyObjectResult();
    result.parseXml(response.body().charStream());
    response.body().close();
  }

  /**
   * Returns an presigned URL to download the object in the bucket with given expiry time with custom request params.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code String url = minioClient.presignedGetObject("my-bucketname", "my-objectname", 60 * 60 * 24, reqParams);
   * System.out.println(url); }</pre>
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name in the bucket.
   * @param expires     Expiration time in seconds of presigned URL.
   * @param reqParams   Override values for set of response headers. Currently supported request parameters are
   *                    [response-expires, response-content-type, response-cache-control, response-content-disposition]
   *
   * @return string contains URL to download the object.
   *
   * @throws InvalidBucketNameException   upon an invalid bucket name
   * @throws InvalidKeyException          upon an invalid access key or secret key
   * @throws IOException                  upon signature calculation failure
   * @throws NoSuchAlgorithmException     upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedGetObject(String bucketName, String objectName, Integer expires,
                                   Map<String, String> reqParams)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException, InvalidExpiresRangeException {
    // Validate input.
    if (expires < 1 || expires > DEFAULT_EXPIRY_TIME) {
      throw new InvalidExpiresRangeException(expires, "expires must be in range of 1 to " + DEFAULT_EXPIRY_TIME);
    }

    String region = getRegion(bucketName);
    Request request = createRequest(Method.GET, bucketName, objectName, region,
                                    null, reqParams, null, null, 0);
    HttpUrl url = Signer.presignV4(request, region, accessKey, secretKey, expires);
    return url.toString();
  }

  /**
   * Returns an presigned URL to download the object in the bucket with given expiry time.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code String url = minioClient.presignedGetObject("my-bucketname", "my-objectname", 60 * 60 * 24);
   * System.out.println(url); }</pre>
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name in the bucket.
   * @param expires     Expiration time in seconds of presigned URL.
   *
   * @return string contains URL to download the object.
   *
   * @throws InvalidBucketNameException   upon an invalid bucket name
   * @throws InvalidKeyException          upon an invalid access key or secret key
   * @throws IOException                  upon signature calculation failure
   * @throws NoSuchAlgorithmException     upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedGetObject(String bucketName, String objectName, Integer expires)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException, InvalidExpiresRangeException {
    return presignedGetObject(bucketName, objectName, expires, null);
  }


  /**
   * Returns an presigned URL to download the object in the bucket with default expiry time.
   * Default expiry time is 7 days in seconds.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code String url = minioClient.presignedGetObject("my-bucketname", "my-objectname");
   * System.out.println(url); }</pre>
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name in the bucket.
   *
   * @return string contains URL to download the object
   *
   * @throws IOException     upon connection error
   * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedGetObject(String bucketName, String objectName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException, InvalidExpiresRangeException {
    return presignedGetObject(bucketName, objectName, DEFAULT_EXPIRY_TIME, null);
  }


  /**
   * Returns a presigned URL to upload an object in the bucket with given expiry time.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code String url = minioClient.presignedPutObject("my-bucketname", "my-objectname", 60 * 60 * 24);
   * System.out.println(url); }</pre>
   *
   * @param bucketName  Bucket name
   * @param objectName  Object name in the bucket
   * @param expires     Expiration time in seconds to presigned URL.
   *
   * @return string contains URL to upload the object.
   *
   * @throws InvalidBucketNameException   upon an invalid bucket name
   * @throws InvalidKeyException          upon an invalid access key or secret key
   * @throws IOException                  upon signature calculation failure
   * @throws NoSuchAlgorithmException     upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedPutObject(String bucketName, String objectName, Integer expires)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException, InvalidExpiresRangeException {
    if (expires < 1 || expires > DEFAULT_EXPIRY_TIME) {
      throw new InvalidExpiresRangeException(expires, "expires must be in range of 1 to " + DEFAULT_EXPIRY_TIME);
    }

    String region = getRegion(bucketName);
    Request request = createRequest(Method.PUT, bucketName, objectName, region, null, null, null, "", 0);
    HttpUrl url = Signer.presignV4(request, region, accessKey, secretKey, expires);
    return url.toString();
  }


  /**
   * Returns a presigned URL to upload an object in the bucket with default expiry time.
   * Default expiry time is 7 days in seconds.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code String url = minioClient.presignedPutObject("my-bucketname", "my-objectname");
   * System.out.println(url); }</pre>
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name in the bucket.
   *
   * @return string contains URL to upload the object.
   *
   * @throws IOException     upon connection error
   * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedPutObject(String bucketName, String objectName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException, InvalidExpiresRangeException {
    return presignedPutObject(bucketName, objectName, DEFAULT_EXPIRY_TIME);
  }


  /**
   * Returns string map for given {@link PostPolicy} to upload object with various post policy conditions.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code // Create new PostPolicy object for 'my-bucketname', 'my-objectname' and 7 days expire time from now.
   * PostPolicy policy = new PostPolicy("my-bucketname", "my-objectname", DateTime.now().plusDays(7));
   * // 'my-objectname' should be 'image/png' content type
   * policy.setContentType("image/png");
   * Map<String,String> formData = minioClient.presignedPostPolicy(policy);
   * // Print a curl command that can be executable with the file /tmp/userpic.png and the file will be uploaded.
   * System.out.print("curl -X POST ");
   * for (Map.Entry<String,String> entry : formData.entrySet()) {
   *   System.out.print(" -F " + entry.getKey() + "=" + entry.getValue());
   * }
   * System.out.println(" -F file=@/tmp/userpic.png https://play.minio.io:9000/my-bucketname"); }</pre>
   *
   * @param policy Post policy of an object.
   * @return Map of strings to construct form-data.
   *
   * @see PostPolicy
   */
  public Map<String, String> presignedPostPolicy(PostPolicy policy)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException, InvalidArgumentException {
    return policy.formData(this.accessKey, this.secretKey, getRegion(policy.bucketName()));
  }


  /**
   * Removes an object from a bucket.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code minioClient.removeObject("my-bucketname", "my-objectname"); }</pre>
   *
   * @param bucketName Bucket name.
   * @param objectName Object name in the bucket.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void removeObject(String bucketName, String objectName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    executeDelete(bucketName, objectName, null);
  }


  private List<DeleteError> removeObject(String bucketName, List<DeleteObject> objectList)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    Map<String,String> queryParamMap = new HashMap<>();
    queryParamMap.put("delete", "");

    DeleteRequest request = new DeleteRequest(objectList);
    HttpResponse response = executePost(bucketName, null, null, queryParamMap, request);

    String bodyContent = "";
    // Use scanner to read entire body stream to string.
    Scanner scanner = new Scanner(response.body().charStream());
    try {
      scanner.useDelimiter("\\A");
      if (scanner.hasNext()) {
        bodyContent = scanner.next();
      }
    } finally {
      response.body().close();
      scanner.close();
    }

    List<DeleteError> errorList = null;

    bodyContent = bodyContent.trim();
    // Check if body content is <Error> message.
    DeleteError error = new DeleteError(new StringReader(bodyContent));
    if (error.code() != null) {
      // As it is <Error> message, add to error list.
      errorList = new LinkedList<DeleteError>();
      errorList.add(error);
    } else {
      // As it is not <Error> message, parse it as <DeleteResult> message.
      DeleteResult result = new DeleteResult(new StringReader(bodyContent));
      errorList = result.errorList();
    }

    return errorList;
  }


  /**
   * Removes multiple objects from a bucket.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code // Create object list for removal.
   * List<String> objectNames = new LinkedList<String>();
   * objectNames.add("my-objectname1");
   * objectNames.add("my-objectname2");
   * objectNames.add("my-objectname3");
   * for (Result<DeleteError> errorResult: minioClient.removeObject("my-bucketname", objectNames)) {
   *     DeleteError error = errorResult.get();
   *     System.out.println("Failed to remove '" + error.objectName() + "'. Error:" + error.message());
   * } }</pre>
   *
   * @param bucketName Bucket name.
   * @param objectNames List of Object names in the bucket.
   */
  public Iterable<Result<DeleteError>> removeObject(final String bucketName, final Iterable<String> objectNames) {
    return new Iterable<Result<DeleteError>>() {
      @Override
      public Iterator<Result<DeleteError>> iterator() {
        return new Iterator<Result<DeleteError>>() {
          private Result<DeleteError> error;
          private Iterator<DeleteError> errorIterator;
          private boolean completed = false;

          private synchronized void populate() {
            List<DeleteError> errorList = null;
            try {
              List<DeleteObject> objectList = new LinkedList<DeleteObject>();
              int i = 0;
              for (String objectName: objectNames) {
                objectList.add(new DeleteObject(objectName));
                i++;
                // Maximum 1000 objects are allowed in a request
                if (i == 1000) {
                  break;
                }
              }

              if (i == 0) {
                return;
              }

              errorList = removeObject(bucketName, objectList);
            } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException
                     | InvalidKeyException | NoResponseException | XmlPullParserException | ErrorResponseException
                     | InternalException e) {
              this.error = new Result<>(null, e);
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

            if (this.error != null) {
              this.completed = true;
              return this.error;
            }

            if (this.errorIterator.hasNext()) {
              return new Result<>(this.errorIterator.next(), null);
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
   * Lists object information in given bucket.
   *
   * @param bucketName Bucket name.
   *
   * @return an iterator of Result Items.
   */
  public Iterable<Result<Item>> listObjects(final String bucketName) throws XmlPullParserException {
    return listObjects(bucketName, null);
  }


  /**
   * Lists object information in given bucket and prefix.
   *
   * @param bucketName Bucket name.
   * @param prefix     Prefix string.  List objects whose name starts with `prefix`.
   *
   * @return an iterator of Result Items.
   */
  public Iterable<Result<Item>> listObjects(final String bucketName, final String prefix)
    throws XmlPullParserException {
    // list all objects recursively
    return listObjects(bucketName, prefix, true);
  }


  /**
   * Lists object information as {@code Iterable<Result><Item>>} in given bucket, prefix and recursive flag.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code Iterable<Result<Item>> myObjects = minioClient.listObjects("my-bucketname");
   * for (Result<Item> result : myObjects) {
   *   Item item = result.get();
   *   System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
   * } }</pre>
   *
   * @param bucketName Bucket name.
   * @param prefix     Prefix string.  List objects whose name starts with `prefix`.
   * @param recursive when false, emulates a directory structure where each listing returned is either a full object
   *                  or part of the object's key up to the first '/'. All objects wit the same prefix up to the first
   *                  '/' will be merged into one entry.
   *
   * @return an iterator of Result Items.
   *
   * @see #listObjects(String bucketName)
   * @see #listObjects(String bucketName, String prefix)
   * @see #listObjects(String bucketName, String prefix, boolean recursive, boolean useVersion1)
   */
  public Iterable<Result<Item>> listObjects(final String bucketName, final String prefix, final boolean recursive) {
    return listObjects(bucketName, prefix, recursive, false);
  }


  /**
   * Lists object information as {@code Iterable<Result><Item>>} in given bucket, prefix, recursive flag and S3 API
   * version to use.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code Iterable<Result<Item>> myObjects = minioClient.listObjects("my-bucketname", "my-object-prefix", true,
   *                                    false);
   * for (Result<Item> result : myObjects) {
   *   Item item = result.get();
   *   System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
   * } }</pre>
   *
   * @param bucketName Bucket name.
   * @param prefix     Prefix string.  List objects whose name starts with `prefix`.
   * @param recursive when false, emulates a directory structure where each listing returned is either a full object
   *                  or part of the object's key up to the first '/'. All objects wit the same prefix up to the first
   *                  '/' will be merged into one entry.
   * @param useVersion1 If set, Amazon AWS S3 List Object V1 is used, else List Object V2 is used as default.
   *
   * @return an iterator of Result Items.
   *
   * @see #listObjects(String bucketName)
   * @see #listObjects(String bucketName, String prefix)
   * @see #listObjects(String bucketName, String prefix, boolean recursive)
   */
  public Iterable<Result<Item>> listObjects(final String bucketName, final String prefix, final boolean recursive,
                                            final boolean useVersion1) {
    if (useVersion1) {
      return listObjectsV1(bucketName, prefix, recursive);
    }

    return listObjectsV2(bucketName, prefix, recursive);
  }


  private Iterable<Result<Item>> listObjectsV2(final String bucketName, final String prefix, final boolean recursive) {
    return new Iterable<Result<Item>>() {
      @Override
      public Iterator<Result<Item>> iterator() {
        return new Iterator<Result<Item>>() {
          private ListBucketResult listBucketResult;
          private Result<Item> error;
          private Iterator<Item> itemIterator;
          private Iterator<Prefix> prefixIterator;
          private boolean completed = false;

          private synchronized void populate() {
            String delimiter = "/";
            if (recursive) {
              delimiter = null;
            }

            String continuationToken = null;
            if (this.listBucketResult != null && delimiter != null) {
              continuationToken = listBucketResult.nextContinuationToken();
            }

            this.listBucketResult = null;
            this.itemIterator = null;
            this.prefixIterator = null;

            try {
              this.listBucketResult = listObjectsV2(bucketName, continuationToken, prefix, delimiter);
            } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException
                     | InvalidKeyException | NoResponseException | XmlPullParserException | ErrorResponseException
                     | InternalException e) {
              this.error = new Result<>(null, e);
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

            if (this.error == null && !this.itemIterator.hasNext() && !this.prefixIterator.hasNext()
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

            if (this.error == null && !this.itemIterator.hasNext() && !this.prefixIterator.hasNext()
                && this.listBucketResult.isTruncated()) {
              populate();
            }

            if (this.error != null) {
              this.completed = true;
              return this.error;
            }

            if (this.itemIterator.hasNext()) {
              Item item = this.itemIterator.next();
              return new Result<>(item, null);
            }

            if (this.prefixIterator.hasNext()) {
              Prefix prefix = this.prefixIterator.next();
              Item item;
              try {
                item = new Item(prefix.prefix(), true);
              } catch (XmlPullParserException e) {
                // special case: ignore the error as we can't propagate the exception in next()
                item = null;
              }

              return new Result<>(item, null);
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
   * Returns {@link ListBucketResult} of given bucket, marker, prefix and delimiter.
   *
   * @param bucketName Bucket name.
   * @param marker     Marker string.  List objects whose name is greater than `marker`.
   * @param prefix     Prefix string.  List objects whose name starts with `prefix`.
   * @param delimiter  delimiter string.  Group objects whose name contains `delimiter`.
   */
  private ListBucketResult listObjectsV2(String bucketName, String continuationToken, String prefix, String delimiter)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    Map<String,String> queryParamMap = new HashMap<>();
    queryParamMap.put("list-type", "2");

    if (continuationToken != null) {
      queryParamMap.put("continuation-token", continuationToken);
    }

    if (prefix != null) {
      queryParamMap.put("prefix", prefix);
    }

    if (delimiter != null) {
      queryParamMap.put("delimiter", delimiter);
    }

    HttpResponse response = executeGet(bucketName, null, null, queryParamMap);

    ListBucketResult result = new ListBucketResult();
    result.parseXml(response.body().charStream());
    response.body().close();
    return result;
  }


  private Iterable<Result<Item>> listObjectsV1(final String bucketName, final String prefix, final boolean recursive) {
    return new Iterable<Result<Item>>() {
      @Override
      public Iterator<Result<Item>> iterator() {
        return new Iterator<Result<Item>>() {
          private String lastObjectName;
          private ListBucketResultV1 listBucketResult;
          private Result<Item> error;
          private Iterator<Item> itemIterator;
          private Iterator<Prefix> prefixIterator;
          private boolean completed = false;

          private synchronized void populate() {
            String delimiter = "/";
            if (recursive) {
              delimiter = null;
            }

            String marker = null;
            if (this.listBucketResult != null) {
              if (delimiter != null) {
                marker = listBucketResult.nextMarker();
              } else {
                marker = this.lastObjectName;
              }
            }

            this.listBucketResult = null;
            this.itemIterator = null;
            this.prefixIterator = null;

            try {
              this.listBucketResult = listObjectsV1(bucketName, marker, prefix, delimiter);
            } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException
                     | InvalidKeyException | NoResponseException | XmlPullParserException | ErrorResponseException
                     | InternalException e) {
              this.error = new Result<>(null, e);
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

            if (this.error == null && !this.itemIterator.hasNext() && !this.prefixIterator.hasNext()
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

            if (this.error == null && !this.itemIterator.hasNext() && !this.prefixIterator.hasNext()
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
              return new Result<>(item, null);
            }

            if (this.prefixIterator.hasNext()) {
              Prefix prefix = this.prefixIterator.next();
              Item item;
              try {
                item = new Item(prefix.prefix(), true);
              } catch (XmlPullParserException e) {
                // special case: ignore the error as we can't propagate the exception in next()
                item = null;
              }

              return new Result<>(item, null);
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
   * Returns {@link ListBucketResultV1} of given bucket, marker, prefix and delimiter.
   *
   * @param bucketName Bucket name.
   * @param marker     Marker string.  List objects whose name is greater than `marker`.
   * @param prefix     Prefix string.  List objects whose name starts with `prefix`.
   * @param delimiter  delimiter string.  Group objects whose name contains `delimiter`.
   * @param maxKeys    Maximum number of entries to be returned.
   */
  private ListBucketResultV1 listObjectsV1(String bucketName, String marker, String prefix, String delimiter)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    Map<String,String> queryParamMap = new HashMap<>();

    if (marker != null) {
      queryParamMap.put("marker", marker);
    }

    if (prefix != null) {
      queryParamMap.put("prefix", prefix);
    }

    if (delimiter != null) {
      queryParamMap.put("delimiter", delimiter);
    }

    HttpResponse response = executeGet(bucketName, null, null, queryParamMap);

    ListBucketResultV1 result = new ListBucketResultV1();
    result.parseXml(response.body().charStream());
    response.body().close();
    return result;
  }


  /**
   * Returns all bucket information owned by the current user.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code List<Bucket> bucketList = minioClient.listBuckets();
   * for (Bucket bucket : bucketList) {
   *   System.out.println(bucket.creationDate() + ", " + bucket.name());
   * } }</pre>
   *
   * @return List of bucket type.
   *
   * @throws NoResponseException     upon no response from server
   * @throws IOException             upon connection error
   * @throws XmlPullParserException  upon parsing response xml
   * @throws ErrorResponseException  upon unsuccessful execution
   * @throws InternalException       upon internal library error
   */
  public List<Bucket> listBuckets()
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    HttpResponse response = executeGet(null, null, null, null);
    ListAllMyBucketsResult result = new ListAllMyBucketsResult();
    result.parseXml(response.body().charStream());
    response.body().close();
    return result.buckets();
  }


  /**
   * Checks if given bucket exist and is having read access.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code boolean found = minioClient.bucketExists("my-bucketname");
   * if (found) {
   *   System.out.println("my-bucketname exists");
   * } else {
   *   System.out.println("my-bucketname does not exist");
   * } }</pre>
   *
   * @param bucketName Bucket name.
   *
   * @return True if the bucket exists and the user has at least read access.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public boolean bucketExists(String bucketName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    try {
      executeHead(bucketName, null);
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
   * @param bucketName Bucket name.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void makeBucket(String bucketName)
    throws InvalidBucketNameException, RegionConflictException, NoSuchAlgorithmException, InsufficientDataException,
                IOException, InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
                InternalException {
    this.makeBucket(bucketName, null);
  }


  /**
   * Creates a bucket with given region.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code minioClient.makeBucket("my-bucketname");
   * System.out.println("my-bucketname is created successfully"); }</pre>
   *
   * @param bucketName Bucket name.
   * @param region     region in which the bucket will be created.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void makeBucket(String bucketName, String region)
    throws InvalidBucketNameException, RegionConflictException, NoSuchAlgorithmException, InsufficientDataException,
                IOException, InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
                InternalException {
    // If region param is not provided, set it with the one provided by constructor
    if (region == null) {
      region = this.region;
    }
    // If constructor already sets a region, check if it is equal to region param if provided
    if (this.region != null && !this.region.equals(region)) {
      throw new RegionConflictException("passed region conflicts with the one previously specified");
    }
    String configString;
    if (region == null || US_EAST_1.equals(region)) {
      // for 'us-east-1', location constraint is not required.  for more info
      // http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region
      configString = "";
    } else {
      CreateBucketConfiguration config = new CreateBucketConfiguration(region);
      configString = config.toString();
    }

    HttpResponse response = executePut(bucketName, null, null, null, US_EAST_1, configString, 0);
    response.body().close();
  }


  /**
   * Removes a bucket.
   * <p>
   * NOTE: -
   * All objects (including all object versions and delete markers) in the bucket
   * must be deleted prior, this API will not recursively delete objects
   * </p>
   *
   * </p><b>Example:</b><br>
   * <pre>{@code minioClient.removeBucket("my-bucketname");
   * System.out.println("my-bucketname is removed successfully"); }</pre>
   *
   * @param bucketName Bucket name.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void removeBucket(String bucketName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    executeDelete(bucketName, null, null);
  }


  /**
   * Uploads given file as object in given bucket.
   * <p>
   * If the object is larger than 5MB, the client will automatically use a multipart session.
   * </p>
   * <p>
   * If the session fails, the user may attempt to re-upload the object by attempting to create
   * the exact same object again. The client will examine all parts of any current upload session
   * and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail
   * before uploading any more data. Otherwise, it will resume uploading where the session left off.
   * </p>
   * <p>
   * If the multipart session fails, the user is responsible for resuming or removing the session.
   * </p>
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name to create in the bucket.
   * @param fileName    File name to upload.
   * @param contentType File content type of the object, user supplied.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void putObject(String bucketName, String objectName, String fileName, String contentType)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException,
           InvalidArgumentException, InsufficientDataException {
    if (fileName == null || "".equals(fileName)) {
      throw new InvalidArgumentException("empty file name is not allowed");
    }

    Path filePath = Paths.get(fileName);
    if (!Files.isRegularFile(filePath)) {
      throw new InvalidArgumentException("'" + fileName + "': not a regular file");
    }

    if (contentType == null) {
      contentType = Files.probeContentType(filePath);
    }

    long size = Files.size(filePath);

    RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r");
    try {
      putObject(bucketName, objectName, contentType, size, file);
    } finally {
      file.close();
    }
  }

  /**
   * Uploads given file as object in given bucket.
   * <p>
   * If the object is larger than 5MB, the client will automatically use a multipart session.
   * </p>
   * <p>
   * If the session fails, the user may attempt to re-upload the object by attempting to create
   * the exact same object again. The client will examine all parts of any current upload session
   * and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail
   * before uploading any more data. Otherwise, it will resume uploading where the session left off.
   * </p>
   * <p>
   * If the multipart session fails, the user is responsible for resuming or removing the session.
   * </p>
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name to create in the bucket.
   * @param fileName    File name to upload.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void putObject(String bucketName, String objectName, String fileName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException,
           InvalidArgumentException, InsufficientDataException {
    putObject(bucketName, objectName, fileName, null);
  }


  /**
   * Uploads data from given stream as object to given bucket.
   * <p>
   * If the object is larger than 5MB, the client will automatically use a multipart session.
   * </p>
   * <p>
   * If the session fails, the user may attempt to re-upload the object by attempting to create
   * the exact same object again. The client will examine all parts of any current upload session
   * and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail
   * before uploading any more data. Otherwise, it will resume uploading where the session left off.
   * </p>
   * <p>
   * If the multipart session fails, the user is responsible for resuming or removing the session.
   * </p>
   *
   * </p><b>Example:</b><br>
   * <pre>{@code StringBuilder builder = new StringBuilder();
   * for (int i = 0; i < 1000; i++) {
   *   builder.append("Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
   *   builder.append("(29 letters)\n");
   *   builder.append("Jackdaws love my big sphinx of quartz: Similarly, used by Windows XP for some fonts. ");
   *   builder.append("(31 letters)\n");
   *   builder.append("Pack my box with five dozen liquor jugs: According to Wikipedia, this one is used on ");
   *   builder.append("NASAs Space Shuttle. (32 letters)\n");
   *   builder.append("The quick onyx goblin jumps over the lazy dwarf: Flavor text from an Unhinged Magic Card. ");
   *   builder.append("(39 letters)\n");
   *   builder.append("How razorback-jumping frogs can level six piqued gymnasts!: Not going to win any brevity ");
   *   builder.append("awards at 49 letters long, but old-time Mac users may recognize it.\n");
   *   builder.append("Cozy lummox gives smart squid who asks for job pen: A 41-letter tester sentence for Mac ");
   *   builder.append("computers after System 7.\n");
   *   builder.append("A few others we like: Amazingly few discotheques provide jukeboxes; Now fax quiz Jack! my ");
   *   builder.append("brave ghost pled; Watch Jeopardy!, Alex Trebeks fun TV quiz game.\n");
   *   builder.append("---\n");
   * }
   * ByteArrayInputStream bais = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
   * // create object
   * minioClient.putObject("my-bucketname", "my-objectname", bais, bais.available(), "application/octet-stream");
   * bais.close();
   * System.out.println("my-bucketname is uploaded successfully"); }</pre>
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name to create in the bucket.
   * @param stream      stream to upload.
   * @param size        Size of all the data that will be uploaded.
   * @param contentType Content type of the stream.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   *
   * @see #putObject(String bucketName, String objectName, String fileName)
   */
  public void putObject(String bucketName, String objectName, InputStream stream, long size, String contentType)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException,
           InvalidArgumentException, InsufficientDataException {
    putObject(bucketName, objectName, contentType, size, new BufferedInputStream(stream));
  }


  /**
   * Executes put object and returns ETag of the object.
   *
   * @param bucketName   Bucket name.
   * @param objectName   Object name in the bucket.
   * @param length       Length of object data.
   * @param data         Object data.
   * @param uploadId     Upload ID of multipart put object.
   * @param partNumber   Part number of multipart put object.
   */
  private String putObject(String bucketName, String objectName, int length,
                           Object data, String uploadId, String contentType,
                           int partNumber)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    Map<String,String> headerMap = new HashMap<>();
    if (contentType != null && !contentType.isEmpty()) {
      headerMap.put("Content-Type", contentType);
    } else {
      headerMap.put("Content-Type", "application/octet-stream");
    }

    Map<String,String> queryParamMap = null;
    if (partNumber > 0 && uploadId != null && !"".equals(uploadId)) {
      queryParamMap = new HashMap<>();
      queryParamMap.put("partNumber", Integer.toString(partNumber));
      queryParamMap.put(UPLOAD_ID, uploadId);
    }

    HttpResponse response = executePut(bucketName, objectName, headerMap, queryParamMap, data, length);
    response.body().close();
    return response.header().etag();
  }


  /**
   * Executes put object. If size of object data is <= 5MiB, single put object is used else multipart put object is
   * used.  This method also resumes if previous multipart put is found.
   *
   * @param bucketName   Bucket name.
   * @param objectName   Object name in the bucket.
   * @param contentType  Content type of object data.
   * @param size         Size of object data.
   * @param data         Object data.
   */
  private void putObject(String bucketName, String objectName, String contentType, long size, Object data)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException,
           InvalidArgumentException, InsufficientDataException {
    if (size <= MIN_MULTIPART_SIZE) {
      // Single put object.
      putObject(bucketName, objectName, (int) size, data, null, contentType, 0);
      return;
    }

    /* Multipart upload */

    int[] rv = calculateMultipartSize(size);
    int partSize = rv[0];
    int partCount = rv[1];
    int lastPartSize = rv[2];
    Part[] totalParts = new Part[partCount];

    // check whether there is incomplete multipart upload or not
    String uploadId = getLatestIncompleteUploadId(bucketName, objectName);
    Iterator<Result<Part>> existingParts = null;
    Part part = null;
    if (uploadId != null) {
      // resume previous multipart upload
      existingParts = listObjectParts(bucketName, objectName, uploadId).iterator();
      if (existingParts.hasNext()) {
        part = existingParts.next().get();
      }
    } else {
      // initiate new multipart upload ie no previous multipart found or no previous valid parts for
      // multipart found
      uploadId = initMultipartUpload(bucketName, objectName, contentType);
    }

    int expectedReadSize = partSize;
    for (int partNumber = 1; partNumber <= partCount; partNumber++) {
      if (partNumber == partCount) {
        expectedReadSize = lastPartSize;
      }

      if (part != null && partNumber == part.partNumber() && expectedReadSize == part.partSize()) {
        String md5Hash = Digest.md5Hash(data, expectedReadSize);
        if (md5Hash.equals(part.etag())) {
          // this part is already uploaded
          totalParts[partNumber - 1] = new Part(part.partNumber(), part.etag());
          skipStream(data, expectedReadSize);

          part = getPart(existingParts);

          continue;
        }
      }

      String etag = putObject(bucketName, objectName, expectedReadSize, data, uploadId, null, partNumber);
      totalParts[partNumber - 1] = new Part(partNumber, etag);
    }

    completeMultipart(bucketName, objectName, uploadId, totalParts);
  }


  /**
   * Returns the parsed current bucket access policy.
   */
  private BucketPolicy getBucketPolicy(String bucketName)
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException {
    Map<String,String> queryParamMap = new HashMap<>();
    queryParamMap.put("policy", "");

    BucketPolicy policy = null;
    HttpResponse response = null;

    try {
      response = executeGet(bucketName, null, null, queryParamMap);
      policy = BucketPolicy.parseJson(response.body().charStream(), bucketName);
      response.body().close();
    } catch (ErrorResponseException e) {
      if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_BUCKET_POLICY) {
        throw e;
      }
    }

    if (policy == null) {
      policy = new BucketPolicy(bucketName);
    }

    return policy;
  }


  /**
   * Get bucket policy at given objectPrefix
   *
   * @param bucketName   Bucket name.
   * @param objectPrefix name of the object prefix
   *
   * </p><b>Example:</b><br>
   * <pre>{@code String policy = minioClient.getBucketPolicy("my-bucketname", "my-objectname");
   * System.out.println(policy); }</pre>
   */
  public PolicyType getBucketPolicy(String bucketName, String objectPrefix)
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException {
    checkObjectPrefix(objectPrefix);

    BucketPolicy policy = getBucketPolicy(bucketName);
    return policy.getPolicy(objectPrefix);
  }


  /**
   * Sets the bucket access policy.
   */
  private void setBucketPolicy(String bucketName, BucketPolicy policy)
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException {
    Map<String,String> headerMap = new HashMap<>();
    headerMap.put("Content-Type", "application/json");

    Map<String,String> queryParamMap = new HashMap<>();
    queryParamMap.put("policy", "");

    String policyJson = policy.getJson();

    HttpResponse response = executePut(bucketName, null, headerMap, queryParamMap, policyJson, 0);
    response.body().close();
  }


  /**
   * Set policy on bucket and object prefix.
   *
   * @param bucketName   Bucket name.
   * @param objectPrefix Name of the object prefix.
   * @param policyType   Enum of {@link PolicyType}.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code setBucketPolicy("my-bucketname", "my-objectname", PolicyType.READ_ONLY); }</pre>
   */
  public void setBucketPolicy(String bucketName, String objectPrefix, PolicyType policyType)
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException {
    checkObjectPrefix(objectPrefix);

    BucketPolicy policy = getBucketPolicy(bucketName);

    if (policyType == PolicyType.NONE && policy.statements() == null) {
      // As the request is for removing policy and the bucket
      // has empty policy statements, just return success.
      return;
    }

    policy.setPolicy(policyType, objectPrefix);

    setBucketPolicy(bucketName, policy);
  }


  /**
   * Get bucket notification configuration
   *
   * @param bucketName   Bucket name.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code NotificationConfiguration notificationConfig = minioClient.getBucketNotification("my-bucketname");
   * }</pre>
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   *
   */
  public NotificationConfiguration getBucketNotification(String bucketName)
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException {
    Map<String,String> queryParamMap = new HashMap<>();
    queryParamMap.put("notification", "");

    HttpResponse response = executeGet(bucketName, null, null, queryParamMap);
    NotificationConfiguration result = new NotificationConfiguration();
    try {
      result.parseXml(response.body().charStream());
    } finally {
      response.body().close();
    }

    return result;
  }


  /**
   * Set bucket notification configuration
   *
   * @param bucketName   Bucket name.
   * @param notificationConfiguration   Notification configuration to be set.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code minioClient.setBucketNotification("my-bucketname", notificationConfiguration);
   * }</pre>
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   *
   */
  public void setBucketNotification(String bucketName, NotificationConfiguration notificationConfiguration)
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException {
    Map<String,String> queryParamMap = new HashMap<>();
    queryParamMap.put("notification", "");
    HttpResponse response = executePut(bucketName, null, null, queryParamMap, notificationConfiguration.toString(), 0);
    response.body().close();
  }


  /**
   * Remove all bucket notification.
   *
   * @param bucketName   Bucket name.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code minioClient.removeAllBucketNotification("my-bucketname");
   * }</pre>
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   *
   */
  public void removeAllBucketNotification(String bucketName)
    throws InvalidBucketNameException, InvalidObjectPrefixException, NoSuchAlgorithmException,
           InsufficientDataException, IOException, InvalidKeyException, NoResponseException,
           XmlPullParserException, ErrorResponseException, InternalException {
    NotificationConfiguration notificationConfiguration = new NotificationConfiguration();
    setBucketNotification(bucketName, notificationConfiguration);
  }


  /**
   * Returns next part if exists.
   */
  private Part getPart(Iterator<Result<Part>> existingParts)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    Part part;
    part = null;
    if (existingParts.hasNext()) {
      part = existingParts.next().get();
    }
    return part;
  }


  /**
   * Returns latest upload ID of incomplete multipart upload of given bucket name and object name.
   */
  private String getLatestIncompleteUploadId(String bucketName, String objectName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    Upload latestUpload = null;
    for (Result<Upload> result : listIncompleteUploads(bucketName, objectName, true, false)) {
      Upload upload = result.get();
      if (upload.objectName().equals(objectName)
            && (latestUpload == null || latestUpload.initiated().compareTo(upload.initiated()) < 0)) {
        latestUpload = upload;
      }
    }

    if (latestUpload != null) {
      return latestUpload.uploadId();
    } else {
      return null;
    }
  }


  /**
   * Lists incomplete uploads of objects in given bucket.
   *
   * @param bucketName Bucket name.
   *
   * @return an iterator of Upload.
   * @see #listIncompleteUploads(String, String, boolean)
   */
  public Iterable<Result<Upload>> listIncompleteUploads(String bucketName) throws XmlPullParserException {
    return listIncompleteUploads(bucketName, null, true, true);
  }


  /**
   * Lists incomplete uploads of objects in given bucket and prefix.
   *
   * @param bucketName Bucket name.
   * @param prefix filters the list of uploads to include only those that start with prefix.
   *
   * @return an iterator of Upload.
   * @see #listIncompleteUploads(String, String, boolean)
   */
  public Iterable<Result<Upload>> listIncompleteUploads(String bucketName, String prefix)
    throws XmlPullParserException {
    return listIncompleteUploads(bucketName, prefix, true, true);
  }


  /**
   * Lists incomplete uploads of objects in given bucket, prefix and recursive flag.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code Iterable<Result<Upload>> myObjects = minioClient.listIncompleteUploads("my-bucketname");
   * for (Result<Upload> result : myObjects) {
   *   Upload upload = result.get();
   *   System.out.println(upload.uploadId() + ", " + upload.objectName());
   * } }</pre>
   *
   * @param bucketName  Bucket name.
   * @param prefix      Prefix string.  List objects whose name starts with `prefix`.
   * @param recursive when false, emulates a directory structure where each listing returned is either a full object
   *                  or part of the object's key up to the first '/'. All uploads with the same prefix up to the first
   *                  '/' will be merged into one entry.
   *
   * @return an iterator of Upload.
   *
   * @see #listIncompleteUploads(String bucketName)
   * @see #listIncompleteUploads(String bucketName, String prefix)
   */
  public Iterable<Result<Upload>> listIncompleteUploads(String bucketName, String prefix, boolean recursive) {
    return listIncompleteUploads(bucketName, prefix, recursive, true);
  }


  /**
   * Returns {@code Iterable<Result<Upload>>} of given bucket name, prefix and recursive flag.
   * All parts size are aggregated when aggregatePartSize is true.
   */
  private Iterable<Result<Upload>> listIncompleteUploads(final String bucketName, final String prefix,
                                                         final boolean recursive, final boolean aggregatePartSize) {
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
              this.listMultipartUploadsResult = listIncompleteUploads(bucketName, nextKeyMarker, nextUploadIdMarker,
                                                                      prefix, delimiter, 1000);
            } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException
                     | InvalidKeyException | NoResponseException | XmlPullParserException | ErrorResponseException
                     | InternalException e) {
              this.error = new Result<>(null, e);
            } finally {
              if (this.listMultipartUploadsResult != null) {
                this.uploadIterator = this.listMultipartUploadsResult.uploads().iterator();
              } else {
                this.uploadIterator = new LinkedList<Upload>().iterator();
              }
            }
          }

          private synchronized long getAggregatedPartSize(String objectName, String uploadId)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
                   InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
                   InternalException {
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

            if (this.error == null && !this.uploadIterator.hasNext()
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

            if (this.error == null && !this.uploadIterator.hasNext()
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
                  aggregatedPartSize = getAggregatedPartSize(upload.objectName(), upload.uploadId());
                } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException
                         | InvalidKeyException | NoResponseException | XmlPullParserException | ErrorResponseException
                         | InternalException e) {
                  // special case: ignore the error as we can't propagate the exception in next()
                  aggregatedPartSize = -1;
                }

                upload.setAggregatedPartSize(aggregatedPartSize);
              }

              return new Result<>(upload, null);
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
   * Executes List Incomplete uploads S3 call for given bucket name, key marker, upload id marker, prefix,
   * delimiter and maxUploads and returns {@link ListMultipartUploadsResult}.
   */
  private ListMultipartUploadsResult listIncompleteUploads(String bucketName, String keyMarker, String uploadIdMarker,
                                                           String prefix, String delimiter, int maxUploads)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    if (maxUploads < 0 || maxUploads > 1000) {
      maxUploads = 1000;
    }

    Map<String,String> queryParamMap = new HashMap<>();
    queryParamMap.put("uploads", "");
    queryParamMap.put("max-uploads", Integer.toString(maxUploads));
    queryParamMap.put("prefix", prefix);
    queryParamMap.put("key-marker", keyMarker);
    queryParamMap.put("upload-id-marker", uploadIdMarker);
    queryParamMap.put("delimiter", delimiter);

    HttpResponse response = executeGet(bucketName, null, null, queryParamMap);

    ListMultipartUploadsResult result = new ListMultipartUploadsResult();
    result.parseXml(response.body().charStream());
    response.body().close();
    return result;
  }


  /**
   * Initializes new multipart upload for given bucket name, object name and content type.
   */
  private String initMultipartUpload(String bucketName, String objectName, String contentType)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    Map<String,String> headerMap = new HashMap<>();
    if (contentType != null && !contentType.isEmpty()) {
      headerMap.put("Content-Type", contentType);
    } else {
      headerMap.put("Content-Type", "application/octet-stream");
    }

    Map<String,String> queryParamMap = new HashMap<>();
    queryParamMap.put("uploads", "");

    HttpResponse response = executePost(bucketName, objectName, headerMap, queryParamMap, "");

    InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
    result.parseXml(response.body().charStream());
    response.body().close();
    return result.uploadId();
  }


  /**
   * Executes complete multipart upload of given bucket name, object name, upload ID and parts.
   */
  private void completeMultipart(String bucketName, String objectName, String uploadId, Part[] parts)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    Map<String,String> queryParamMap = new HashMap<>();
    queryParamMap.put(UPLOAD_ID, uploadId);

    CompleteMultipartUpload completeManifest = new CompleteMultipartUpload(parts);

    HttpResponse response = executePost(bucketName, objectName, null, queryParamMap, completeManifest);

    // Fixing issue https://github.com/minio/minio-java/issues/391
    String bodyContent = "";
    Scanner scanner = new Scanner(response.body().charStream());
    try {
      // read entire body stream to string.
      scanner.useDelimiter("\\A");
      if (scanner.hasNext()) {
        bodyContent = scanner.next();
      }
    } finally {
      response.body().close();
      scanner.close();
    }

    bodyContent = bodyContent.trim();
    if (!bodyContent.isEmpty()) {
      ErrorResponse errorResponse = new ErrorResponse(new StringReader(bodyContent));
      if (errorResponse.code() != null) {
        throw new ErrorResponseException(errorResponse, response.response());
      }
    }
  }


  /**
   * Executes List object parts of multipart upload for given bucket name, object name and upload ID and
   * returns {@code Iterable<Result<Part>>}.
   */
  private Iterable<Result<Part>> listObjectParts(final String bucketName, final String objectName,
                                                 final String uploadId) {
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
              this.listPartsResult = listObjectParts(bucketName, objectName, uploadId, nextPartNumberMarker);
            } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException
                     | InvalidKeyException | NoResponseException | XmlPullParserException | ErrorResponseException
                     | InternalException e) {
              this.error = new Result<>(null, e);
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

            if (this.error == null && !this.partIterator.hasNext() && this.listPartsResult.isTruncated()) {
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

            if (this.error == null && !this.partIterator.hasNext() && this.listPartsResult.isTruncated()) {
              this.nextPartNumberMarker = this.listPartsResult.nextPartNumberMarker();
              populate();
            }

            if (this.error != null) {
              this.completed = true;
              return this.error;
            }

            if (this.partIterator.hasNext()) {
              return new Result<>(this.partIterator.next(), null);
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
   * Executes list object parts for given bucket name, object name, upload ID and part number marker and
   * returns {@link ListPartsResult}.
   */
  private ListPartsResult listObjectParts(String bucketName, String objectName, String uploadId, int partNumberMarker)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    Map<String,String> queryParamMap = new HashMap<>();
    queryParamMap.put(UPLOAD_ID, uploadId);
    if (partNumberMarker > 0) {
      queryParamMap.put("part-number-marker", Integer.toString(partNumberMarker));
    }

    HttpResponse response = executeGet(bucketName, objectName, null, queryParamMap);

    ListPartsResult result = new ListPartsResult();
    result.parseXml(response.body().charStream());
    response.body().close();
    return result;
  }


  /**
   * Aborts multipart upload of given bucket name, object name and upload ID.
   */
  private void abortMultipartUpload(String bucketName, String objectName, String uploadId)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    Map<String,String> queryParamMap = new HashMap<>();
    queryParamMap.put(UPLOAD_ID, uploadId);
    executeDelete(bucketName, objectName, queryParamMap);
  }


  /**
   * Removes incomplete multipart upload of given object.
   *
   * </p><b>Example:</b><br>
   * <pre>{@code minioClient.removeIncompleteUpload("my-bucketname", "my-objectname");
   * System.out.println("successfully removed all incomplete upload session of my-bucketname/my-objectname"); }</pre>
   *
   * @param bucketName Bucket name.
   * @param objectName Object name in the bucket.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void removeIncompleteUpload(String bucketName, String objectName)
    throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException,
           InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException,
           InternalException {
    for (Result<Upload> r : listIncompleteUploads(bucketName, objectName, true, false)) {
      Upload upload = r.get();
      if (objectName.equals(upload.objectName())) {
        abortMultipartUpload(bucketName, objectName, upload.uploadId());
        return;
      }
    }
  }


  /**
   * Skips data of up to given length in given input stream.
   *
   * @param inputStream  Input stream which is intance of {@link RandomAccessFile} or {@link BufferedInputStream}.
   * @param n            Length of bytes to skip.
   */
  private void skipStream(Object inputStream, long n)
    throws IOException, InsufficientDataException {
    RandomAccessFile file = null;
    BufferedInputStream stream = null;
    if (inputStream instanceof RandomAccessFile) {
      file = (RandomAccessFile) inputStream;
    } else if (inputStream instanceof BufferedInputStream) {
      stream = (BufferedInputStream) inputStream;
    } else {
      throw new IllegalArgumentException("unsupported input stream object");
    }

    if (file != null) {
      file.seek(file.getFilePointer() + n);
      return;
    }

    long bytesSkipped;
    long totalBytesSkipped = 0;

    while ((bytesSkipped = stream.skip(n - totalBytesSkipped)) >= 0) {
      totalBytesSkipped += bytesSkipped;
      if (totalBytesSkipped == n) {
        return;
      }
    }

    throw new InsufficientDataException("Insufficient data.  bytes skipped " + totalBytesSkipped + " expected " + n);
  }


  /**
   * Calculates multipart size of given size and returns three element array contains part size, part count
   * and last part size.
   */
  private static int[] calculateMultipartSize(long size)
    throws InvalidArgumentException {
    if (size > MAX_OBJECT_SIZE) {
      throw new InvalidArgumentException("size " + size + " is greater than allowed size 5TiB");
    }

    double partSize = Math.ceil((double) size / MAX_MULTIPART_COUNT);
    partSize = Math.ceil(partSize / MIN_MULTIPART_SIZE) * MIN_MULTIPART_SIZE;

    double partCount = Math.ceil(size / partSize);

    double lastPartSize = partSize - (partSize * partCount - size);
    if (lastPartSize == 0.0) {
      lastPartSize = partSize;
    }

    return new int[] { (int) partSize, (int) partCount, (int) lastPartSize };
  }


  /**
   * Enables HTTP call tracing and written to traceStream.
   *
   * @param traceStream {@link OutputStream} for writing HTTP call tracing.
   *
   * @see #traceOff
   */
  public void traceOn(OutputStream traceStream) {
    if (traceStream == null) {
      throw new NullPointerException();
    } else {
      this.traceStream = new PrintWriter(new OutputStreamWriter(traceStream, StandardCharsets.UTF_8), true);
    }
  }


  /**
   * Disables HTTP call tracing previously enabled.
   *
   * @see #traceOn
   */
  public void traceOff() throws IOException {
    this.traceStream = null;
  }
}
