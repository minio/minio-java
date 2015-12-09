/*
 * Minio Java Library for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
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

import io.minio.acl.Acl;
import io.minio.errors.*;
import io.minio.messages.*;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.Headers;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.google.api.client.xml.Xml;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.common.io.BaseEncoding;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
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
 * <li> Setting canned ACLs on buckets</li>
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
  private static final DateTimeFormatter amzDateFormat = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'")
      .withZoneUTC();
  // the current client instance's base URL.
  private HttpUrl url;
  // access key to sign all requests with
  private String accessKey;
  // Secret key to sign all requests with
  private String secretKey;

  // default Transport
  private final OkHttpClient transport = new OkHttpClient();

  // default multipart upload size is 5MB, maximum is 5GB
  private static final int minimumPartSize = 5 * 1024 * 1024;
  private static final int maximumPartSize = 5 * 1024 * 1024 * 1024;

  // logger which is set only on enableLogger. Atomic reference is used to prevent multiple loggers
  // from being instantiated
  private final AtomicReference<Logger> logger = new AtomicReference<Logger>();

  // default expiration for a presigned URL is 7 days in seconds
  private static final int expiresDefault = 7 * 24 * 3600;

  // user agent to tag all requests with
  private String userAgent = "minio-java/"
      + MinioProperties.INSTANCE.getVersion()
      + " ("
      + System.getProperty("os.name")
      + "; "
      + System.getProperty("os.arch")
      + ")";


  public MinioClient(String endpoint) throws MinioException {
    this(endpoint, 0, null, null, null);
  }


  public MinioClient(URL url) throws NullPointerException, MinioException {
    this(url.toString(), 0, null, null, null);
  }


  public MinioClient(String endpoint, String accessKey, String secretKey) throws MinioException {
    this(endpoint, 0, null, accessKey, secretKey);
  }


  public MinioClient(URL url, String accessKey, String secretKey) throws NullPointerException, MinioException {
    this(url.toString(), 0, null, accessKey, secretKey);
  }


  public MinioClient(String endpoint, int port, String accessKey, String secretKey) throws MinioException {
    this(endpoint, port, null, accessKey, secretKey);
  }


  public MinioClient(String endpoint, HttpScheme scheme, String accessKey, String secretKey) throws MinioException {
    this(endpoint, 0, scheme, accessKey, secretKey);
  }

  /**
   * Create a new client.
   *
   * @param endpoint  request endpoint.  Valid endpoint is an URL, domain name, IPv4 or IPv6 address.
   *                  Valid endpoints:
   *                  * https://s3.amazonaws.com
   *                  * https://s3.amazonaws.com/
   *                  * https://play.minio.io:9000
   *                  * https://play.minio.io:9000/
   *                  * localhost
   *                  * localhost.localdomain
   *                  * play.minio.io
   *                  * 127.0.0.1
   *                  * 192.168.1.60
   *                  * ::1
   * @param port      valid port.  It should be in between 1 and 65535.  Unused if endpoint is an URL.
   * @param scheme    valid HttpScheme.  Unused if endpoint is an URL.
   * @param accessKey access key to access service in endpoint.
   * @param secretKey secret key to access service in endpoint.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, HttpScheme scheme, String accessKey, String secretKey)
   */
  public MinioClient(String endpoint, int port, HttpScheme scheme, String accessKey, String secretKey)
    throws MinioException {
    if (endpoint == null) {
      throw new MinioException("null endpoint");
    }

    // for valid URL endpoint, port and scheme are ignored
    HttpUrl url = HttpUrl.parse(endpoint);
    if (url != null) {
      if (!"/".equals(url.encodedPath())) {
        throw new MinioException("no path allowed in endpoint '" + endpoint + "'");
      }

      // treat Amazon S3 host as special case
      String amzHost = url.host();
      if (amzHost.endsWith(".amazonaws.com") && !amzHost.equals("s3.amazonaws.com")) {
        throw new MinioException("for Amazon S3, host should be 's3.amazonaws.com' in endpoint '" + endpoint + "'");
      }

      this.url = url;
      this.accessKey = accessKey;
      this.secretKey = secretKey;

      return;
    }

    // endpoint may be a valid hostname, IPv4 or IPv6 address
    if (!this.isValidEndpoint(endpoint)) {
      throw new MinioException("invalid host '" + endpoint + "'");
    }

    // treat Amazon S3 host as special case
    if (endpoint.endsWith(".amazonaws.com") && !endpoint.equals("s3.amazonaws.com")) {
      throw new MinioException("unsupported host '" + endpoint
                                 + "'.  For amazon S3, host should be 's3.amazonaws.com'");
    }

    if (port < 0 || port > 65535) {
      throw new MinioException("port must be in range of 1 to 65535");
    }

    HttpScheme httpScheme;
    if (scheme == null) {
      httpScheme = HttpScheme.HTTPS;
    } else {
      httpScheme = scheme;
    }

    if (port == 0) {
      this.url = new HttpUrl.Builder()
          .scheme(httpScheme.toString())
          .host(endpoint)
          .build();
    } else {
      this.url = new HttpUrl.Builder()
          .scheme(httpScheme.toString())
          .host(endpoint)
          .port(port)
          .build();
    }
    this.accessKey = accessKey;
    this.secretKey = secretKey;
  }


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

      if (!(label.matches("^[a-zA-Z0-9][a-zA-Z0-9-]*") && endpoint.matches(".*[a-zA-Z0-9]$"))) {
        return false;
      }
    }

    return true;
  }


  /**
   * Set user agent string of the app - http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
   *
   * @param name     name of your application
   * @param version  version of your application
   * @param comments optional list of comments
   *
   * @throws IOException attempt to overwrite an already set useragent
   */
  @SuppressWarnings("unused")
  public void setUserAgent(String name, String version, String... comments) throws IOException {
    if (name != null && version != null) {
      String newUserAgent = name.trim() + "/" + version.trim() + " (";
      StringBuilder sb = new StringBuilder();
      for (String comment : comments) {
        if (comment != null) {
          sb.append(comment.trim()).append("; ");
        }
      }
      this.userAgent = this.userAgent + newUserAgent + sb.toString() + ") ";
      return;
    }
    throw new IOException("User agent already set");
  }

  /**
   * Returns the URL this client uses
   *
   * @return the URL backed by this.
   */
  public URL getUrl() {
    return url.url();
  }

  /**
   * Returns metadata about the object
   *
   * @param bucket object's bucket
   * @param key    object's key
   *
   * @return Populated object metadata.
   *
   * @throws IOException     upon connection failure
   * @throws ClientException upon failure from server
   * @see ObjectStat
   */
  public ObjectStat statObject(String bucket, String key) throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket, key);
    Request request = getHeadRequest(url);
    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      try {
        if (response.isSuccessful()) {
          // all info we need is in the headers
          Headers responseHeaders = response.headers();
          SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
          Date lastModified = formatter.parse(responseHeaders.get("Last-Modified"));
          String contentType = responseHeaders.get("Content-Type");
          String etag = responseHeaders.get("ETag");
          Long contentLength = Long.valueOf(responseHeaders.get("Content-Length"));
          return new ObjectStat(bucket, key, lastModified, contentLength, etag, contentType);
        } else {
          parseError(response);
        }
      } catch (ParseException e) {
        // if a parse exception occurred, this indicates an error in the client.
        throw new InternalClientException(e);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  private void parseError(Response response) throws XmlPullParserException, IOException, MinioException {
    if (response == null) {
      throw new MinioException("null response");
    }

    if (response.isRedirect()) {
      throw new HttpRedirectException();
    }

    String resource = response.request().url().getPath();
    if (resource == null) {
      resource = "/";
    }
    String[] tokens = resource.split("/");
    int pathLength = tokens.length;
    String bucketName = tokens[1];
    String objectName = null;
    if (pathLength > 2) {
      objectName = tokens[2];
    }
    ErrorResponse errorResponse = new ErrorResponse();

    if (response.body().contentLength() == -1 || response.body().contentLength() == 0) {
      int statusCode = response.code();
      String hostId = String.valueOf(response.headers().get("x-amz-id-2"));
      if ("null".equals(hostId)) {
        hostId = null;
      }
      String requestId = String.valueOf(response.headers().get("x-amz-request-id"));
      if ("null".equals(requestId)) {
        requestId = null;
      }
      errorResponse.setHostId(hostId);
      errorResponse.setRequestId(requestId);
      errorResponse.setResource(resource);

      switch (statusCode) {
        case 404:
          if (pathLength > 2) {
            throw new ObjectNotFoundException(objectName, bucketName);
          } else if (pathLength == 2) {
            throw new BucketNotFoundException(bucketName);
          } else {
            throw new InternalClientException("404 without body resulted in path with less than two components");
          }
        case 501: case 405:
          throw new MethodNotAllowedException();
        case 409:
          throw new BucketNotEmptyException(bucketName);
        case 403:
          throw new AccessDeniedException();
        default:
          throw new InternalClientException("Unhandled error.  Please report this issue at "
                                              + "https://github.com/minio/minio-java/issues");
      }
    } else {
      parseXml(response, errorResponse);
      switch (errorResponse.getCode()) {
        case "NoSuchBucket":
          throw new BucketNotFoundException(bucketName);
        case "NoSuchKey":
          throw new ObjectNotFoundException(objectName, bucketName);
        case "InvalidBucketName":
          throw new InvalidBucketNameException(bucketName, "invalid bucket name");
        case "InvalidObjectName": case "KeyTooLong":
          throw new InvalidObjectNameException(objectName);
        case "AccessDenied":
          throw new AccessDeniedException();
        case "BucketAlreadyExists": case "BucketAlreadyOwnedByYou":
          throw new BucketAlreadyExistsException(bucketName);
        case "InternalError":
          throw new InternalServerException();
        case "TooManyBuckets":
          throw new MaxBucketsReachedException();
        case "PermanentRedirect": case "TemporaryRedirect":
          throw new HttpRedirectException();
        case "MethodNotAllowed":
          throw new ObjectAlreadyExistsException(objectName, bucketName);
        default:
          throw new InternalClientException(errorResponse.toString());
      }
    }
  }

  private void parseXml(Response response, Object objectToPopulate) throws XmlPullParserException, IOException,
                                                                           MinioException {
    if (response == null) {
      throw new MinioException("null response");
    }

    if (objectToPopulate == null) {
      throw new MinioException("objectToPopulate is null.  This should not happen.  "
                               + "Please file a bug at https://github.com/minio/minio-java/issues");
    }

    XmlPullParser parser = Xml.createParser();
    parser.setInput(response.body().charStream());
    XmlNamespaceDictionary dictionary = new XmlNamespaceDictionary();
    if (objectToPopulate instanceof ErrorResponse) {
      // Errors have no namespace, so we set a default empty alias and namespace
      dictionary.set("", "");
    }
    Xml.parseElement(parser, objectToPopulate, dictionary, null);
  }

  private Request getRequest(HttpMethod method, HttpUrl url, final byte[] data) {
    if (url == null) {
      return null;
    }

    DateTime date = new DateTime();
    this.transport.setFollowRedirects(false);
    this.transport.interceptors().add(new RequestSigner(data, accessKey, secretKey, date));

    RequestBody requestBody = null;
    if (data != null) {
      requestBody = RequestBody.create(null, data);
    }

    Request request = new Request.Builder()
        .url(url)
        .method(method.toString(), requestBody)
        .header("User-Agent", this.userAgent)
        .header("x-amz-date", date.toString(amzDateFormat))
        .build();

    return request;
  }

  private Request getGetRequest(HttpUrl url) {
    return getRequest(HttpMethod.GET, url, null);
  }

  private Request getHeadRequest(HttpUrl url) {
    return getRequest(HttpMethod.HEAD, url, null);
  }

  private Request getDeleteRequest(HttpUrl url) {
    return getRequest(HttpMethod.DELETE, url, null);
  }

  private Request getPutRequest(HttpUrl url, final byte[] data) {
    return getRequest(HttpMethod.PUT, url, data);
  }

  private Request getPostRequest(HttpUrl url, final byte[] data) {
    return getRequest(HttpMethod.POST, url, data);
  }

  private HttpUrl getRequestUrl(String bucket, String key) throws InvalidBucketNameException,
                                                                  InvalidObjectNameException,
                                                                  UnsupportedEncodingException {
    if (bucket == null || "".equals(bucket.trim())) {
      throw new InvalidBucketNameException(bucket, "invalid bucket name");
    }
    if (key == null || "".equals(key.trim())) {
      throw new InvalidObjectNameException(key);
    }
    HttpUrl.Builder urlBuilder = this.url.newBuilder();
    urlBuilder.addPathSegment(bucket);
    // URLEncoder.encode replaces space with + and / with %2F
    for (String tok: URLEncoder.encode(key, "UTF-8").replace("+", "%20").replace("%2F", "/").split("/")) {
      urlBuilder.addEncodedPathSegment(tok);
    }
    HttpUrl url = urlBuilder.build();
    return url;
  }

  private HttpUrl getRequestUrl(String bucket) throws InvalidBucketNameException {
    if (bucket == null || "".equals(bucket.trim())) {
      throw new InvalidBucketNameException(bucket, "invalid bucket name");
    }

    HttpUrl url = this.url.newBuilder()
        .addPathSegment(bucket)
        .build();

    return url;
  }

  /**
   * Returns an InputStream containing the object. The InputStream must be closed when
   * complete or the connection will remain open.
   *
   * @param bucket object's bucket
   * @param key    object's key
   *
   * @return an InputStream containing the object. Close the InputStream when done.
   *
   * @throws IOException     upon connection error
   * @throws ClientException upon failure from server
   */
  public InputStream getObject(String bucket, String key) throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket, key);

    Request request = getGetRequest(url);
    Response response = this.transport.newCall(request).execute();
    // we close the response only on failure or the user will be unable to retrieve the object
    // it is the user's responsibility to close the input stream
    if (response != null) {
      if (!(response.isSuccessful())) {
        try {
          parseError(response);
        } finally {
          response.body().close();
        }
      }
      return response.body().byteStream();
    }
    // TODO create a better exception
    throw new IOException();
  }

  /** Returns an presigned URL containing the object.
   *
   * @param bucket  object's bucket
   * @param key     object's key
   * @param expires object expiration
   *
   * @throws IOException     upon signature calculation failure
   * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedGetObject(String bucket, String key, Integer expires) throws IOException,
                                                                                      NoSuchAlgorithmException,
                                                                                      InvalidExpiresRangeException,
                                                                                      InvalidKeyException,
                                                                                      InvalidObjectNameException,
                                                                                      InternalClientException,
                                                                                      InvalidBucketNameException {
    if (expires < 1 || expires > expiresDefault) {
      throw new InvalidExpiresRangeException();
    }
    HttpUrl url = getRequestUrl(bucket, key);
    Request request = getGetRequest(url);
    DateTime date = new DateTime();

    RequestSigner signer = new RequestSigner(null, this.accessKey,
                                             this.secretKey, date);
    return signer.preSignV4(request, expires);
  }

  /** Returns an presigned URL containing the object.
   *
   * @param bucket  object's bucket
   * @param key     object's key
   *
   * @throws IOException     upon connection error
   * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedGetObject(String bucket, String key) throws IOException, NoSuchAlgorithmException,
                                                                     InvalidObjectNameException,
                                                                     InvalidExpiresRangeException,
                                                                     InvalidKeyException, InternalClientException,
                                                                     InvalidBucketNameException {
    return presignedGetObject(bucket, key, expiresDefault);
  }

  /** Returns an presigned URL for PUT.
   *
   * @param bucket  object's bucket
   * @param key     object's key
   * @param expires object expiration
   *
   * @throws IOException     upon signature calculation failure
   * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedPutObject(String bucket, String key, Integer expires) throws IOException,
                                                                                      NoSuchAlgorithmException,
                                                                                      InvalidExpiresRangeException,
                                                                                      InvalidKeyException,
                                                                                      InvalidObjectNameException,
                                                                                      InternalClientException,
                                                                                      InvalidBucketNameException {
    if (expires < 1 || expires > expiresDefault) {
      throw new InvalidExpiresRangeException();
    }
    // place holder data to avoid okhttp's request builder's exception
    byte[] dummy = "".getBytes("UTF-8");
    HttpUrl url = getRequestUrl(bucket, key);
    Request request = getPutRequest(url, dummy);
    DateTime date = new DateTime();

    RequestSigner signer = new RequestSigner(null, this.accessKey,
                                             this.secretKey, date);
    return signer.preSignV4(request, expires);
  }

  /** Returns an presigned URL for PUT.
   *
   * @param bucket  object's bucket
   * @param key     object's key
   *
   * @throws IOException     upon connection error
   * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedPutObject(String bucket, String key) throws IOException, NoSuchAlgorithmException,
                                                                     InvalidObjectNameException,
                                                                     InvalidExpiresRangeException, InvalidKeyException,
                                                                     InternalClientException,
                                                                     InvalidBucketNameException {
    return presignedPutObject(bucket, key, expiresDefault);
  }

  /** Returns an Policy for POST.
   */
  public PostPolicy newPostPolicy() {
    return new PostPolicy();
  }

  /** Returns an Map for POST form data.
   *
   * @param policy new PostPolicy
   *
   * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   * @throws UnsupportedEncodingException upon unsupported Encoding error
   */
  public Map<String, String> presignedPostPolicy(PostPolicy policy) throws UnsupportedEncodingException,
                                                                           NoSuchAlgorithmException,
                                                                           InvalidKeyException {
    DateTime date = new DateTime();
    RequestSigner signer = new RequestSigner(null, this.accessKey, this.secretKey, date);
    String region = Regions.INSTANCE.getRegion(this.url.uri().getHost());
    policy.setAlgorithm("AWS4-HMAC-SHA256");
    policy.setCredential(this.accessKey + "/" + signer.getScope(region, date));
    policy.setDate(date);

    String policybase64 = policy.base64();
    String signature = signer.postPreSignV4(policybase64, date, region);
    policy.setPolicy(policybase64);
    policy.setSignature(signature);
    return policy.getFormData();
  }

  /** Returns an InputStream containing a subset of the object. The InputStream must be
   *  closed or the connection will remain open.
   *
   * @param bucket      object's bucket
   * @param key         object's key
   * @param offsetStart Offset from the start of the object.
   *
   * @return an InputStream containing the object. Close the InputStream when done.
   *
   * @throws IOException     upon connection failure
   * @throws ClientException upon failure from server
   */
  public InputStream getPartialObject(String bucket, String key, long offsetStart)
    throws XmlPullParserException, IOException, MinioException {
    ObjectStat stat = statObject(bucket, key);
    long length = stat.getLength() - offsetStart;
    return getPartialObject(bucket, key, offsetStart, length);
  }

  /**
   * Returns an InputStream containing a subset of the object. The InputStream must be
   * closed or the connection will remain open.
   *
   * @param bucket      object's bucket
   * @param key         object's key
   * @param offsetStart Offset from the start of the object.
   * @param length      Length of bytes to retrieve.
   *
   * @return an InputStream containing the object. Close the InputStream when done.
   *
   * @throws IOException     upon connection failure
   * @throws ClientException upon failure from server
   */
  public InputStream getPartialObject(String bucket, String key, long offsetStart, long length)
    throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket, key);

    if (offsetStart < 0 || length <= 0) {
      throw new InvalidRangeException();
    }

    Request request = getGetRequest(url);
    long offsetEnd = offsetStart + length - 1;

    Request rangeRequest = request.newBuilder()
        .header("Range", "bytes=" + offsetStart + "-" + offsetEnd)
        .build();

    // we close the response only on failure or the user will be unable
    // to retrieve the object it is the user's responsibility to close
    // the input stream
    Response response = this.transport.newCall(rangeRequest).execute();
    if (response != null) {
      if (response.isSuccessful()) {
        return response.body().byteStream();
      }
      try {
        parseError(response);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  /** Remove an object from a bucket.
   *
   * @param bucket object's bucket
   * @param key    object's key
   *
   * @throws IOException     upon connection error
   * @throws ClientException upon failure from server
   */
  public void removeObject(String bucket, String key) throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket, key);

    Request request = getDeleteRequest(url);
    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      try {
        if (response.isSuccessful()) {
          return;
        }
        parseError(response);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  /**
   * listObjects is a wrapper around listObjects(bucket, null, true)
   *
   * @param bucket is the bucket to list objects from
   *
   * @return an iterator of Items.
   * @see #listObjects(String, String, boolean)
   */
  public Iterator<Result<Item>> listObjects(final String bucket) throws XmlPullParserException, MinioException {
    return listObjects(bucket, null);
  }

  /**
   * listObjects is a wrapper around listObjects(bucket, prefix, true)
   *
   * @param bucket to list objects of
   * @param prefix filters the list of objects to include only those that start with prefix
   *
   * @return an iterator of Items.
   * @see #listObjects(String, String, boolean)
   */
  public Iterator<Result<Item>> listObjects(final String bucket, final String prefix)
    throws XmlPullParserException, MinioException {
    // list all objects recursively
    return listObjects(bucket, prefix, true);
  }

  /**
   * @param bucket    bucket to list objects from
   * @param prefix    filters all objects returned where each object must begin with the given prefix
   * @param recursive when false, emulates a directory structure where each listing returned is either a full object
   *                  or part of the object's key up to the first '/'. All objects wit the same prefix up to the first
   *                  '/' will be merged into one entry.
   *
   * @return an iterator of Items.
   */
  public Iterator<Result<Item>> listObjects(final String bucket, final String prefix, final boolean recursive)
    throws XmlPullParserException {
    return new MinioIterator<Result<Item>>() {
      private String marker = null;
      private boolean isComplete = false;

      @Override
      protected List<Result<Item>> populate() throws XmlPullParserException {
        if (!isComplete) {
          String delimiter = null;
          // set delimiter  to '/' if not recursive to emulate directories
          if (!recursive) {
            delimiter = "/";
          }
          ListBucketResult listBucketResult;
          List<Result<Item>> items = new LinkedList<Result<Item>>();
          try {
            listBucketResult = listObjects(bucket, marker, prefix, delimiter, 1000);
            for (Item item : listBucketResult.getContents()) {
              items.add(new Result<Item>(item, null));
              if (listBucketResult.isTruncated()) {
                marker = item.getKey();
              }
            }
            for (Prefix prefix : listBucketResult.getCommonPrefixes()) {
              Item item = new Item();
              item.setKey(prefix.getPrefix());
              item.setIsDir(true);
              items.add(new Result<Item>(item, null));
            }
            if (listBucketResult.isTruncated() && delimiter != null) {
              marker = listBucketResult.getNextMarker();
            } else if (!listBucketResult.isTruncated()) {
              isComplete = true;
            }
          } catch (IOException e) {
            items.add(new Result<Item>(null, e));
            isComplete = true;
            return items;
          } catch (MinioException e) {
            items.add(new Result<Item>(null, e));
            isComplete = true;
            return items;
          }
          return items;
        }
        return new LinkedList<Result<Item>>();
      }
    };
  }

  private ListBucketResult listObjects(String bucket, String marker, String prefix, String delimiter, int maxKeys)
    throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket);

    // max keys limits the number of keys returned, max limit is 1000
    if (maxKeys >= 1000 || maxKeys < 0) {
      maxKeys = 1000;
    }
    url = url.newBuilder()
        .addQueryParameter("max-keys", Integer.toString(maxKeys))
        .addQueryParameter("marker", marker)
        .addQueryParameter("prefix", prefix)
        .addQueryParameter("delimiter", delimiter)
        .build();

    Request request = getGetRequest(url);
    Response response = this.transport.newCall(request).execute();

    if (response != null) {
      try {
        if (response.isSuccessful()) {
          ListBucketResult result = new ListBucketResult();
          parseXml(response, result);
          return result;
        }
        parseError(response);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  /**
   * List buckets owned by the current user.
   *
   * @return a list of buckets owned by the current user
   *
   * @throws IOException     upon connection failure
   * @throws ClientException upon failure from server
   */
  public Iterator<Bucket> listBuckets() throws XmlPullParserException, IOException, MinioException {
    Request request = getGetRequest(this.url);
    Response response = this.transport.newCall(request).execute();

    if (response == null) {
      throw new MinioException("no response from server");
    }

    try {
      if (response.isSuccessful()) {
        ListAllMyBucketsResult retrievedBuckets = new ListAllMyBucketsResult();
        parseXml(response, retrievedBuckets);
        return retrievedBuckets.getBuckets().iterator();
      } else {
        parseError(response);
        return null;
      }
    } finally {
      response.body().close();
    }
  }

  /**
   * Test whether a bucket exists and the user has at least read access.
   *
   * @param bucket bucket to test for existence and access
   *
   * @return true if the bucket exists and the user has at least read access
   *
   * @throws IOException     upon connection error
   * @throws ClientException upon failure from server
   */
  public boolean bucketExists(String bucket) throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket);

    Request request = getHeadRequest(url);
    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      if (response.isSuccessful()) {
        return true;
      }
      try {
        parseError(response);
      } catch (BucketNotFoundException ex) {
        return false;
      } finally {
        response.body().close();
      }
    }
    throw new IOException("No response from server");
  }

  /**
   * @param bucket bucket to create.
   *
   * @throws IOException     upon connection error
   * @throws ClientException upon failure from server
   */
  public void makeBucket(String bucket) throws XmlPullParserException, IOException, MinioException {
    this.makeBucket(bucket, Acl.PRIVATE);
  }

  /**
   * Create a bucket with a given name and ACL.
   *
   * @param bucket bucket to create
   * @param acl    canned acl
   *
   * @throws IOException     upon connection error
   * @throws ClientException upon failure from server
   */
  public void makeBucket(String bucket, Acl acl) throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket);
    Request request = null;

    CreateBucketConfiguration config = new CreateBucketConfiguration();
    String region = Regions.INSTANCE.getRegion(url.uri().getHost());

    // ``us-east-1`` is not a valid location constraint according to amazon, so we skip it
    // Valid constraints are
    // [ us-west-1 | us-west-2 | EU or eu-west-1 | eu-central-1 | ap-southeast-1 | ap-northeast-1 |
    // ap-southeast-2 | sa-east-1 ]
    if (!"us-east-1".equals(region)) {
      config.setLocationConstraint(region);
      byte[] data = config.toString().getBytes("UTF-8");
      byte[] md5sum = calculateMd5sum(data);
      String base64md5sum = "";
      if (md5sum != null) {
        base64md5sum = BaseEncoding.base64().encode(md5sum);
      }
      request = getPutRequest(url, data);
      request = request.newBuilder()
          .header("Content-MD5", base64md5sum)
          .build();
    } else {
      // okhttp requires PUT objects to have non-nil body, so we send a dummy not "null"
      byte[] dummy = "".getBytes("UTF-8");
      request = getPutRequest(url, dummy) ;
    }

    if (acl == null) {
      acl = Acl.PRIVATE;
    }

    request = request.newBuilder()
        .header("x-amz-acl", acl.toString())
        .build();

    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      if (response.isSuccessful()) {
        return;
      }
      parseError(response);
    }
    throw new IOException();
  }

  /**
   * Remove a bucket with a given name.
   * <p>
   * NOTE: -
   * All objects (including all object versions and delete markers) in the bucket
   * must be deleted prior, this API will not recursively delete objects
   * </p>
   *
   * @param bucket bucket to create
   *
   * @throws IOException     upon connection error
   * @throws ClientException upon failure from server
   */
  public void removeBucket(String bucket) throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket);

    Request request = getDeleteRequest(url);
    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      try {
        if (response.isSuccessful()) {
          return;
        }
        parseError(response);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  /**
   * Get the bucket's ACL.
   *
   * @param bucket bucket to get ACL on
   *
   * @return Acl type
   *
   * @throws IOException     upon connection error
   * @throws ClientException upon failure from server
   */
  public Acl getBucketAcl(String bucket) throws XmlPullParserException, IOException, MinioException {
    AccessControlPolicy policy = this.getAccessPolicy(bucket);
    if (policy == null) {
      throw new InvalidArgumentException();
    }
    Acl acl = Acl.PRIVATE;
    List<Grant> accessControlList = policy.getAccessControlList();
    switch (accessControlList.size()) {
      case 1:
        for (Grant grant : accessControlList) {
          if (grant.getGrantee().getUri() == null && "FULL_CONTROL".equals(grant.getPermission())) {
            acl = Acl.PRIVATE;
            break;
          }
        }
        break;
      case 2:
        for (Grant grant : accessControlList) {
          if ("http://acs.amazonaws.com/groups/global/AuthenticatedUsers".equals(grant.getGrantee().getUri())
              &&
              "READ".equals(grant.getPermission())) {
            acl = Acl.AUTHENTICATED_READ;
            break;
          }
          if ("http://acs.amazonaws.com/groups/global/AllUsers".equals(grant.getGrantee().getUri())
              &&
              "READ".equals(grant.getPermission())) {
            acl = Acl.PUBLIC_READ;
            break;
          }
        }
        break;
      case 3:
        for (Grant grant : accessControlList) {
          if ("http://acs.amazonaws.com/groups/global/AllUsers".equals(grant.getGrantee().getUri())
              &&
              "WRITE".equals(grant.getPermission())) {
            acl = Acl.PUBLIC_READ_WRITE;
            break;
          }
        }
        break;
      default:
        throw new InternalClientException("Invalid control flow.  Please report this issue at "
                                          + "https://github.com/minio/minio-java/issues");
    }
    return acl;
  }

  private AccessControlPolicy getAccessPolicy(String bucket)
    throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket);
    url = url.newBuilder()
        .addQueryParameter("acl", "")
        .build();

    Request request = getGetRequest(url);
    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      try {
        if (response.isSuccessful()) {
          AccessControlPolicy policy = new AccessControlPolicy();
          parseXml(response, policy);
          return policy;
        }
        parseError(response);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  /**
   * Set the bucket's ACL.
   *
   * @param bucket bucket to set ACL on
   * @param acl    canned acl
   *
   * @throws IOException     upon connection error
   * @throws ClientException upon failure from server
   */
  public void setBucketAcl(String bucket, Acl acl) throws XmlPullParserException, IOException, MinioException {
    if (acl == null) {
      throw new InvalidAclNameException();
    }

    HttpUrl url = getRequestUrl(bucket);
    // make sure to set this, otherwise it would convert this call into a regular makeBucket operation
    url = url.newBuilder()
        .addQueryParameter("acl", "")
        .build();

    // okhttp requires PUT objects to have non-nil body, so we send a dummy not "null"
    byte[] data = "".getBytes("UTF-8");
    Request request = getPutRequest(url, data);
    request = request.newBuilder()
        .header("x-amz-acl", acl.toString())
        .build();

    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      try {
        if (response.isSuccessful()) {
          return;
        }
        parseError(response);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  /**
   * Create an object.
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
   * @param bucket      Bucket to use
   * @param key         Key of object
   * @param contentType Content type to set this object to
   * @param size        Size of all the data that will be uploaded.
   * @param body        Data to upload
   *
   * @throws IOException     upon connection error
   * @throws ClientException upon failure from server
   */
  public void putObject(String bucket, String key, String contentType, long size, InputStream body)
    throws XmlPullParserException, IOException, MinioException {
    boolean isMultipart = false;
    boolean newUpload = true;
    int partSize = 0;
    String uploadId = null;

    if (contentType == null || "".equals(contentType.trim())) {
      contentType = "application/octet-stream";
    }

    if (size > minimumPartSize) {
      // check if multipart exists
      Iterator<Result<Upload>> multipartUploads = listIncompleteUploads(bucket, key);
      while (multipartUploads.hasNext()) {
        Upload upload = multipartUploads.next().getResult();
        if (upload.getKey().equals(key)) {
          uploadId = upload.getUploadId();
          newUpload = false;
        }
      }

      isMultipart = true;
      partSize = calculatePartSize(size);
    }

    if (!isMultipart) {
      Data data = readData((int) size, body);
      if (data.getData().length != size || destructiveHasMore(body)) {
        throw new UnexpectedShortReadException();
      }
      try {
        putObject(bucket, key, contentType, data.getData(), data.getMD5());
      } catch (MethodNotAllowedException ex) {
        throw new ObjectAlreadyExistsException(key, bucket);
      }
      return;
    }
    long totalSeen = 0;
    List<Part> parts = new LinkedList<Part>();
    int partNumber = 1;
    Iterator<Part> existingParts = new LinkedList<Part>().iterator();
    if (newUpload) {
      uploadId = newMultipartUpload(bucket, key);
    } else {
      existingParts = listObjectParts(bucket, key, uploadId);
    }
    while (true) {
      Data data = readData(partSize, body);
      if (data.getData().length == 0) {
        break;
      }
      if (data.getData().length < partSize) {
        long expectedSize = size - totalSeen;
        if (expectedSize != data.getData().length) {
          throw new UnexpectedShortReadException();
        }
      }
      if (!newUpload && existingParts.hasNext()) {
        Part existingPart = existingParts.next();
        if (existingPart.getPartNumber() == partNumber
            &&
            existingPart.getETag().toLowerCase().equals(BaseEncoding.base16().encode(data.getMD5()).toLowerCase())) {
          partNumber++;
          continue;
        }
      }
      String etag = putObject(bucket, key, contentType, data.getData(),
                              data.getMD5(), uploadId, partNumber);
      totalSeen += data.getData().length;

      Part part = new Part();
      part.setPartNumber(partNumber);
      part.setETag(etag);
      parts.add(part);
      partNumber++;
    }
    if (totalSeen != size) {
      throw new InputSizeMismatchException();
    }
    try {
      completeMultipart(bucket, key, uploadId, parts);
    } catch (MethodNotAllowedException ex) {
      throw new ObjectAlreadyExistsException(key, bucket);
    }
  }

  private void putObject(String bucket, String key, String contentType, byte[] data, byte[] md5sum)
    throws XmlPullParserException, IOException, MinioException {
    putObject(bucket, key, contentType, data, md5sum, "", 0);
  }

  private String putObject(String bucket, String key, String contentType, byte[] data, byte[] md5sum, String uploadId,
                           int partId) throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket, key);

    if (partId > 0 && uploadId != null && !"".equals(uploadId.trim())) {
      url = url.newBuilder()
          .addQueryParameter("partNumber", Integer.toString(partId))
          .addQueryParameter("uploadId", uploadId)
          .build();
    }

    String base64md5sum = "";
    if (md5sum != null) {
      base64md5sum = BaseEncoding.base64().encode(md5sum);
    }

    Request request = getPutRequest(url, data);
    if (md5sum != null) {
      request = request.newBuilder()
          .header("Content-MD5", base64md5sum)
          .build();
    }

    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      try {
        if (response.isSuccessful()) {
          return response.headers().get("ETag").replaceAll("\"", "");
        }
        parseError(response);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  /**
   * listIncompleteUploads is a wrapper around listIncompleteUploads(bucket, null, true)
   *
   * @param bucket is the bucket to list objects from
   *
   * @return an iterator of Upload.
   * @see #listIncompleteUploads(String, String, boolean)
   */
  public Iterator<Result<Upload>> listIncompleteUploads(String bucket) throws XmlPullParserException {
    return listIncompleteUploads(bucket, null, true);
  }

  /**
   * listIncompleteUploads is a wrapper around listIncompleteUploads(bucket, prefix, true)
   *
   * @param bucket is the bucket to list incomplete uploads from
   * @param prefix filters the list of uploads to include only those that start with prefix
   *
   * @return an iterator of Upload.
   * @see #listIncompleteUploads(String, String, boolean)
   */
  public Iterator<Result<Upload>> listIncompleteUploads(String bucket, String prefix) throws XmlPullParserException {
    return listIncompleteUploads(bucket, prefix, true);
  }

  /**
   * @param bucket    bucket to list incomplete uploads from
   * @param prefix    filters all uploads returned where each object must begin with the given prefix
   * @param recursive when false, emulates a directory structure where each listing returned is either a full object
   *                  or part of the object's key up to the first '/'. All uploads with the same prefix up to the first
   *                  '/' will be merged into one entry.
   *
   * @return an iterator of Upload.
   */
  public Iterator<Result<Upload>> listIncompleteUploads(final String bucket, final String prefix,
                                                        final boolean recursive) throws XmlPullParserException {

    return new MinioIterator<Result<Upload>>() {
      private boolean isComplete = false;
      private String keyMarker = null;
      private String uploadIdMarker;

      @Override
      protected List<Result<Upload>> populate() throws XmlPullParserException {
        List<Result<Upload>> ret = new LinkedList<Result<Upload>>();
        if (!isComplete) {
          ListMultipartUploadsResult uploadResult;
          String delimiter = null;
          // set delimiter  to '/' if not recursive to emulate directories
          if (!recursive) {
            delimiter = "/";
          }
          try {
            uploadResult = listIncompleteUploads(bucket, keyMarker,
                                                 uploadIdMarker, prefix,
                                                 delimiter, 1000);
            if (uploadResult.isTruncated()) {
              keyMarker = uploadResult.getNextKeyMarker();
              uploadIdMarker = uploadResult.getNextUploadIdMarker();
            } else {
              isComplete = true;
            }
            List<Upload> uploads = uploadResult.getUploads();
            for (Upload upload : uploads) {
              ret.add(new Result<Upload>(upload, null));
            }
          } catch (IOException e) {
            ret.add(new Result<Upload>(null, e));
            isComplete = true;
          } catch (MinioException e) {
            ret.add(new Result<Upload>(null, e));
            isComplete = true;
          }
        }
        return ret;
      }
    };
  }

  private ListMultipartUploadsResult listIncompleteUploads(String bucket, String keyMarker, String uploadIdMarker,
                                                           String prefix, String delimiter, int maxUploads)
    throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket);
    // max uploads limits the number of uploads returned, max limit is 1000
    if (maxUploads >= 1000 || maxUploads < 0) {
      maxUploads = 1000;
    }

    url = url.newBuilder()
        .addQueryParameter("uploads", "")
        .addQueryParameter("max-uploads", Integer.toString(maxUploads))
        .addQueryParameter("prefix", prefix)
        .addQueryParameter("key-marker", keyMarker)
        .addQueryParameter("upload-id-marker", uploadIdMarker)
        .addQueryParameter("delimiter", delimiter)
        .build();

    Request request = getGetRequest(url);
    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      try {
        if (response.isSuccessful()) {
          ListMultipartUploadsResult result = new ListMultipartUploadsResult();
          parseXml(response, result);
          return result;
        }
        parseError(response);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  private String newMultipartUpload(String bucket, String key)
    throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket, key);
    url = url.newBuilder()
        .addQueryParameter("uploads", "")
        .build();

    // okhttp requires POST to have non-nil body, so we send a dummy not "null"
    byte[] dummy = "".getBytes("UTF-8");
    Request request = getPostRequest(url, dummy);
    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      try {
        if (response.isSuccessful()) {
          InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
          parseXml(response, result);
          return result.getUploadId();
        }
        parseError(response);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  private void completeMultipart(String bucket, String key, String uploadId, List<Part> parts)
    throws XmlPullParserException, IOException, MinioException {
    HttpUrl url = getRequestUrl(bucket, key);
    url = url.newBuilder()
        .addQueryParameter("uploadId", uploadId)
        .build();

    CompleteMultipartUpload completeManifest = new CompleteMultipartUpload();
    completeManifest.setParts(parts);

    byte[] data = completeManifest.toString().getBytes("UTF-8");
    Request request = getPostRequest(url, data);

    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      try {
        if (response.isSuccessful()) {
          return;
        }
        parseError(response);
      } finally {
        response.body().close();
      }
    }
  }

  private Iterator<Part> listObjectParts(final String bucket, final String key,
                                         final String uploadId) throws XmlPullParserException {
    return new MinioIterator<Part>() {
      public int marker;
      private boolean isComplete = false;

      @Override
      protected List<Part> populate() throws XmlPullParserException, IOException, MinioException {
        if (!isComplete) {
          ListPartsResult result;
          result = listObjectParts(bucket, key, uploadId, marker);
          if (result.isTruncated()) {
            marker = result.getNextPartNumberMarker();
          } else {
            isComplete = true;
          }
          return result.getParts();
        }
        return new LinkedList<Part>();
      }
    };
  }

  private ListPartsResult listObjectParts(String bucket, String key, String uploadId, int partNumberMarker)
    throws XmlPullParserException, IOException, MinioException {
    if (partNumberMarker <= 0) {
      throw new InvalidArgumentException();
    }

    HttpUrl url = getRequestUrl(bucket, key);
    url = url.newBuilder()
        .addQueryParameter("uploadId", uploadId)
        .addQueryParameter("part-number-marker",
                           Integer.toString(partNumberMarker))
        .build();

    Request request = getGetRequest(url);
    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      try {
        if (response.isSuccessful()) {
          ListPartsResult result = new ListPartsResult();
          parseXml(response, result);
          return result;
        }
        parseError(response);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  private void abortMultipartUpload(String bucket, String key, String uploadId)
    throws XmlPullParserException, IOException, MinioException {
    if (bucket == null) {
      throw new InvalidBucketNameException("(null)", "null bucket name");
    }
    if (key == null) {
      throw new InvalidObjectNameException("null");
    }
    if (uploadId == null) {
      throw new InternalClientException("UploadId cannot be null");
    }
    HttpUrl url = getRequestUrl(bucket, key);
    url = url.newBuilder()
        .addQueryParameter("uploadId", uploadId)
        .build();

    Request request = getDeleteRequest(url);
    Response response = this.transport.newCall(request).execute();
    if (response != null) {
      try {
        if (response.isSuccessful()) {
          return;
        }
        parseError(response);
      } finally {
        response.body().close();
      }
    }
    throw new IOException();
  }

  /**
   * Remove active multipart uploads, starting from key.
   *
   * @param bucket of multipart upload to remove
   * @param key    of multipart upload to remove
   *
   * @throws IOException     upon connection failure
   * @throws ClientException upon failure from server
   */
  public void removeIncompleteUpload(String bucket, String key)
    throws XmlPullParserException, IOException, MinioException {
    Iterator<Result<Upload>> uploads = listIncompleteUploads(bucket, key);
    while (uploads.hasNext()) {
      Upload upload = uploads.next().getResult();
      if (key.equals(upload.getKey())) {
        abortMultipartUpload(bucket, key, upload.getUploadId());
        return;
      }
    }
  }

  private int calculatePartSize(long size) {
    // 9999 is used instead of 10000 to cater for the last part being too small
    int partSize = (int) (size / 9999);
    if (partSize > minimumPartSize) {
      if (partSize > maximumPartSize) {
        return maximumPartSize;
      }
      return partSize;
    }
    return minimumPartSize;
  }

  private byte[] calculateMd5sum(byte[] data) {
    byte[] md5sum;
    try {
      MessageDigest md5Digest = MessageDigest.getInstance("MD5");
      md5sum = md5Digest.digest(data);
    } catch (NoSuchAlgorithmException e) {
      // we should never see this, unless the underlying JVM is broken.
      // Throw a runtime exception if we run into this, the environment
      // is not sane
      System.err.println("MD5 message digest type not found, the current JVM is likely broken.");
      throw new RuntimeException(e);
    }
    return md5sum;
  }

  private Data readData(int size, InputStream data) throws IOException {
    int amountRead = 0;
    byte[] fullData = new byte[size];
    while (amountRead != size) {
      byte[] buf = new byte[size - amountRead];
      int curRead = data.read(buf);
      if (curRead == -1) {
        break;
      }
      buf = Arrays.copyOf(buf, curRead);
      System.arraycopy(buf, 0, fullData, amountRead, curRead);
      amountRead += curRead;
    }
    fullData = Arrays.copyOfRange(fullData, 0, amountRead);
    Data d = new Data();
    d.setData(fullData);
    d.setMD5(calculateMd5sum(fullData));
    return d;
  }

  private boolean destructiveHasMore(InputStream data) {
    try {
      return data.read() > -1;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Enable logging to a java logger for debugging purposes. This will enable logging for all http requests.
   */
  @SuppressWarnings("unused")
  public void enableLogging() {
    if (this.logger.get() == null) {
      this.logger.set(Logger.getLogger(OkHttpClient.class.getName()));
      this.logger.get().setLevel(Level.CONFIG);
      this.logger.get().addHandler(new Handler() {

        @Override
        public void close() throws SecurityException {
        }

        @Override
        public void flush() {
        }

        @Override
        public void publish(LogRecord record) {
          // default ConsoleHandler will print >= INFO to System.err
          if (record.getLevel().intValue() < Level.INFO.intValue()) {
            System.out.println(record.getMessage());
          }
        }
      });
    } else {
      this.logger.get().setLevel(Level.CONFIG);
    }
  }

  /**
   * Disable logging http requests.
   */
  @SuppressWarnings("unused")
  public void disableLogging() {
    if (this.logger.get() != null) {
      this.logger.get().setLevel(Level.OFF);
    }
  }
}
