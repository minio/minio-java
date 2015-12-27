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

import io.minio.errors.*;
import io.minio.messages.*;
import io.minio.http.*;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;
import com.google.common.io.BaseEncoding;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.EOFException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import com.google.common.io.ByteStreams;
import java.nio.file.StandardCopyOption;


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
  // maximum allowed object size is 5TiB
  private static final long MAX_OBJECT_SIZE = 5L * 1024 * 1024 * 1024 * 1024;
  private static final int MAX_MULTIPART_COUNT = 10000;
  // minimum allowed multipart size is 5MiB
  private static final int MIN_MULTIPART_SIZE = 5 * 1024 * 1024;
  // maximum possible multipart size is 535MiB = MAX_OBJECT_SIZE / MAX_MULTIPART_COUNT
  private static final int MAX_MULTIPART_SIZE = 535 * 1024 * 1024;
  // default expiration for a presigned URL is 7 days in seconds
  private static final int DEFAULT_EXPIRY_TIME = 7 * 24 * 3600;
  private static final String DEFAULT_USER_AGENT = "Minio (" + System.getProperty("os.arch") + "; "
      + System.getProperty("os.arch") + ") minio-java/" + MinioProperties.INSTANCE.getVersion();

  // the current client instance's base URL.
  private HttpUrl baseUrl;
  // access key to sign all requests with
  private String accessKey;
  // Secret key to sign all requests with
  private String secretKey;

  // logger which is set only on enableLogger. Atomic reference is used to prevent multiple loggers
  // from being instantiated
  private final AtomicReference<Logger> logger = new AtomicReference<Logger>();
  private String userAgent = DEFAULT_USER_AGENT;


  public MinioClient(String endpoint) throws InvalidEndpointException, InvalidPortException {
    this(endpoint, 0, null, null, false);
  }


  public MinioClient(URL url) throws NullPointerException, InvalidEndpointException, InvalidPortException {
    this(url.toString(), 0, null, null, false);
  }


  public MinioClient(String endpoint, String accessKey, String secretKey)
    throws InvalidEndpointException, InvalidPortException {
    this(endpoint, 0, accessKey, secretKey, false);
  }


  public MinioClient(URL url, String accessKey, String secretKey)
    throws NullPointerException, InvalidEndpointException, InvalidPortException {
    this(url.toString(), 0, accessKey, secretKey, false);
  }


  public MinioClient(String endpoint, int port, String accessKey, String secretKey)
    throws InvalidEndpointException, InvalidPortException {
    this(endpoint, port, accessKey, secretKey, false);
  }


  public MinioClient(String endpoint, String accessKey, String secretKey, boolean insecure)
    throws InvalidEndpointException, InvalidPortException {
    this(endpoint, 0, accessKey, secretKey, insecure);
  }


  /**
   * Create a new client.
   *
   * @param endpoint  request endpoint.  Valid endpoint is an URL, domain name, IPv4 or IPv6 address.
   *                  Valid endpoints:
   *                  * https://s3.amazonaws.com
   *                  * https://s3.amazonaws.com/
   *                  * https://play.minio.io:9000
   *                  * http://play.minio.io:9010/
   *                  * localhost
   *                  * localhost.localdomain
   *                  * play.minio.io
   *                  * 127.0.0.1
   *                  * 192.168.1.60
   *                  * ::1
   * @param port      valid port.  It should be in between 1 and 65535.  Unused if endpoint is an URL.
   * @param accessKey access key to access service in endpoint.
   * @param secretKey secret key to access service in endpoint.
   * @param insecure  to access endpoint, use HTTP if true else HTTPS.
   *
   * @see #MinioClient(String endpoint)
   * @see #MinioClient(URL url)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey)
   * @see #MinioClient(URL url, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, int port, String accessKey, String secretKey)
   * @see #MinioClient(String endpoint, String accessKey, String secretKey, boolean insecure)
   */
  public MinioClient(String endpoint, int port, String accessKey, String secretKey, boolean insecure)
    throws InvalidEndpointException, InvalidPortException {
    if (endpoint == null) {
      throw new InvalidEndpointException("(null)", "null endpoint");
    }

    // for valid URL endpoint, port and insecure are ignored
    HttpUrl url = HttpUrl.parse(endpoint);
    if (url != null) {
      if (!"/".equals(url.encodedPath())) {
        throw new InvalidEndpointException(endpoint, "no path allowed in endpoint");
      }

      // treat Amazon S3 host as special case
      String amzHost = url.host();
      if (amzHost.endsWith(".amazonaws.com") && !amzHost.equals("s3.amazonaws.com")) {
        throw new InvalidEndpointException(endpoint, "for Amazon S3, host should be 's3.amazonaws.com' in endpoint");
      }

      this.baseUrl = url;
      this.accessKey = accessKey;
      this.secretKey = secretKey;

      return;
    }

    // endpoint may be a valid hostname, IPv4 or IPv6 address
    if (!this.isValidEndpoint(endpoint)) {
      throw new InvalidEndpointException(endpoint, "invalid host");
    }

    // treat Amazon S3 host as special case
    if (endpoint.endsWith(".amazonaws.com") && !endpoint.equals("s3.amazonaws.com")) {
      throw new InvalidEndpointException(endpoint, "for amazon S3, host should be 's3.amazonaws.com'");
    }

    if (port < 0 || port > 65535) {
      throw new InvalidPortException(port, "port must be in range of 1 to 65535");
    }

    Scheme scheme = Scheme.HTTPS;
    if (insecure) {
      scheme = Scheme.HTTP;
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


  private void checkBucketName(String name) throws InvalidBucketNameException {
    if (name == null) {
      throw new InvalidBucketNameException("(null)", "null bucket name");
    }

    if (name.length() < 3 || name.length() > 63) {
      String msg = "bucket name must be at least 3 and no more than 63 characters long";
      throw new InvalidBucketNameException(name, msg);
    }

    if (name.indexOf(".") != -1) {
      String msg = "bucket name with '.' is not allowed due to SSL cerificate verification error.  "
          + "For more information refer http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
      throw new InvalidBucketNameException(name, msg);
    }

    if (!name.matches("^[a-z0-9][a-z0-9\\-]+[a-z0-9]$")) {
      String msg = "bucket name does not follow Amazon S3 standards.  For more information refer "
          + "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
      throw new InvalidBucketNameException(name, msg);
    }
  }


  private Request getRequest(Method method, String bucketName, String objectName,
                             Map<String,String> headerMap, Map<String,String> queryParamMap,
                             byte[] data, int length)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    if (bucketName == null && objectName != null) {
      throw new InvalidBucketNameException("(null)", "null bucket name for object '" + objectName + "'");
    }

    HttpUrl.Builder urlBuilder = this.baseUrl.newBuilder();

    if (bucketName != null) {
      checkBucketName(bucketName);

      // special case:
      // if the request is for s3.amazonaws.com and location query,
      // use s3.amazonaws.com/BUCKETNAME
      if (baseUrl.host().equals("s3.amazonaws.com")) {
        if (queryParamMap != null && queryParamMap.containsKey("location")) {
          urlBuilder.addPathSegment(bucketName);
        } else {
          urlBuilder.host(bucketName + ".s3.amazonaws.com");
        }
      } else {
        urlBuilder.addPathSegment(bucketName);
      }
    }

    if (objectName != null) {
      urlBuilder.addPathSegment(objectName);
    }

    if (queryParamMap != null) {
      for (Map.Entry<String,String> entry : queryParamMap.entrySet()) {
        urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
      }
    }

    RequestBody requestBody = null;
    if (data != null) {
      requestBody = RequestBody.create(null, data, 0, length);
    }

    Request.Builder requestBuilder = new Request.Builder();
    requestBuilder.url(urlBuilder.build());
    requestBuilder.method(method.toString(), requestBody);
    if (headerMap != null) {
      for (Map.Entry<String,String> entry : headerMap.entrySet()) {
        requestBuilder.header(entry.getKey(), entry.getValue());
      }
    }
    requestBuilder.header("User-Agent", this.userAgent);

    return requestBuilder.build();
  }


  private HttpResponse execute(Method method, String region, String bucketName, String objectName,
                               Map<String,String> headerMap, Map<String,String> queryParamMap,
                               byte[] data, int length)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    Request request = getRequest(method, bucketName, objectName, headerMap, queryParamMap, data, length);

    OkHttpClient transport = new OkHttpClient();
    transport.interceptors().add(new RequestSigner(accessKey, secretKey, region, data, length));

    Response response = transport.newCall(request).execute();
    if (response == null) {
      throw new NoResponseException();
    }

    ResponseHeader header = new ResponseHeader();
    HeaderParser.set(response.headers(), header);

    if (response.isSuccessful()) {
      return new HttpResponse(header, response.body());
    }

    ErrorResponse errorResponse = null;
    boolean emptyBody = false;

    try {
      errorResponse = new ErrorResponse(response.body().charStream());
    } catch (EOFException e) {
      emptyBody = true;
    } finally {
      response.body().close();
    }

    if (emptyBody || errorResponse == null) {
      ErrorCode ec;
      switch (response.code()) {
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
                                        header.getXamzRequestId(), header.getXamzId2());
    }

    // invalidate region cache if needed
    if (errorResponse.getErrorCode() == ErrorCode.NO_SUCH_BUCKET) {
      Regions.INSTANCE.remove(bucketName);
      // TODO: handle for other cases as well
      // observation: on HEAD of a bucket with wrong region gives 400 without body
    }

    throw new ErrorResponseException(errorResponse);
  }


  private void updateRegionMap(String bucketName)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    if (bucketName != null && "s3.amazonaws.com".equals(this.baseUrl.host()) && this.accessKey != null
          && this.secretKey != null && Regions.INSTANCE.exists(bucketName) == false) {
      Map<String,String> queryParamMap = new HashMap<String,String>();
      queryParamMap.put("location", null);

      HttpResponse response = execute(Method.GET, "us-east-1", bucketName, null, null, queryParamMap, null, 0);

      // existing XmlEntity does not work, so fallback to regular parsing.
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      XmlPullParser xpp = factory.newPullParser();
      String location = null;

      xpp.setInput(response.body().charStream());
      while (xpp.getEventType() != xpp.END_DOCUMENT) {
        if (xpp.getEventType() ==  xpp.START_TAG && xpp.getName() == "LocationConstraint") {
          xpp.next();
          if (xpp.getEventType() == xpp.TEXT) {
            location = xpp.getText();
          }
          break;
        }

        xpp.next();
      }

      response.body().close();

      String region;
      if (location == null) {
        region = "us-east-1";
      } else {
        if ("EU".equals(location)) {
          region = "eu-west-1";
        } else {
          region = location;
        }
      }

      Regions.INSTANCE.add(bucketName, region);
    }
  }


  private HttpResponse executeGet(String bucketName, String objectName, Map<String,String> headerMap,
                                  Map<String,String> queryParamMap)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    updateRegionMap(bucketName);
    return execute(Method.GET, Regions.INSTANCE.region(bucketName), bucketName, objectName, headerMap,
                   queryParamMap, null, 0);
  }


  private HttpResponse executeHead(String bucketName, String objectName)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    updateRegionMap(bucketName);
    return execute(Method.HEAD, Regions.INSTANCE.region(bucketName), bucketName, objectName, null, null, null, 0);
  }


  private HttpResponse executeDelete(String bucketName, String objectName, Map<String,String> queryParamMap)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    updateRegionMap(bucketName);
    return execute(Method.DELETE, Regions.INSTANCE.region(bucketName), bucketName, objectName, null,
                   queryParamMap, null, 0);
  }


  private HttpResponse executePost(String bucketName, String objectName, Map<String,String> queryParamMap, byte[] data)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    updateRegionMap(bucketName);
    return execute(Method.POST, Regions.INSTANCE.region(bucketName), bucketName, objectName, null,
                   queryParamMap, data, data.length);
  }


  private HttpResponse executePut(String bucketName, String objectName, Map<String,String> headerMap,
                                  Map<String,String> queryParamMap, String region, byte[] data, int length)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    return execute(Method.PUT, region, bucketName, objectName, headerMap, queryParamMap, data, length);
  }


  private HttpResponse executePut(String bucketName, String objectName, Map<String,String> headerMap,
                                  Map<String,String> queryParamMap, byte[] data, int length)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    updateRegionMap(bucketName);
    return executePut(bucketName, objectName, headerMap, queryParamMap, Regions.INSTANCE.region(bucketName),
                      data, length);
  }


  /**
   * Set application info to user agent - see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
   *
   * @param name     your application name
   * @param version  your application version
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
   * Returns metadata of given object.
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
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    HttpResponse response = executeHead(bucketName, objectName);
    ResponseHeader header = response.header();
    return new ObjectStat(bucketName, objectName, header.getLastModified(), header.getContentLength(),
                          header.getEtag(), header.getContentType());
  }


  /**
   * Returns an InputStream containing the object. The InputStream must be closed when
   * complete or the connection will remain open.
   *
   * @param bucketName Bucket name
   * @param objectName Object name in the bucket
   *
   * @return an InputStream containing the object. Close the InputStream when done.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public InputStream getObject(String bucketName, String objectName)
    throws InvalidArgumentException, InvalidBucketNameException, NoResponseException, IOException,
           XmlPullParserException, ErrorResponseException, InternalException {
    return getObject(bucketName, objectName, 0, null);
  }


  /** Returns an InputStream containing a subset of the object. The InputStream must be
   *  closed or the connection will remain open.
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name in the bucket.
   * @param offset      Offset to read at.
   *
   * @return an InputStream containing the object. Close the InputStream when done.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public InputStream getObject(String bucketName, String objectName, long offset)
    throws InvalidArgumentException, InvalidBucketNameException, NoResponseException, IOException,
           XmlPullParserException, ErrorResponseException, InternalException {
    return getObject(bucketName, objectName, offset, null);
  }


  /**
   * Returns an InputStream containing a subset of the object. The InputStream must be
   * closed or the connection will remain open.
   *
   * @param bucketName  Bucket name.
   * @param objectName  Object name in the bucket.
   * @param offset      Offset to read at.
   * @param length      Length to read.
   *
   * @return an InputStream containing the object. Close the InputStream when done.
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public InputStream getObject(String bucketName, String objectName, long offset, Long length)
    throws InvalidArgumentException, InvalidBucketNameException, NoResponseException, IOException,
           XmlPullParserException, ErrorResponseException, InternalException {
    if (offset < 0) {
      throw new InvalidArgumentException("offset should be zero or greater");
    }

    if (length != null && length <= 0) {
      throw new InvalidArgumentException("length should be greater than zero");
    }

    Map<String,String> headerMap = new Hashtable<String,String>();
    if (length != null) {
      headerMap.put("Range", "bytes=" + offset + "-" + (offset + length - 1));
    } else {
      headerMap.put("Range", "bytes=" + offset + "-");
    }

    HttpResponse response = executeGet(bucketName, objectName, headerMap, null);
    return response.body().byteStream();
  }


  /**
   * Get object and store it to given file name.
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
    throws InvalidArgumentException, InvalidBucketNameException, NoResponseException,
           IOException, XmlPullParserException, ErrorResponseException, InternalException {
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
    boolean tempFileExists = Files.exists(filePath);

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


  /** Returns an presigned URL containing the object.
   *
   * @param bucketName  Bucket name
   * @param objectName  Object name in the bucket
   * @param expires     object expiration
   *
   * @throws InvalidBucketNameException   upon an invalid bucket name
   * @throws InvalidKeyException          upon an invalid access key or secret key
   * @throws IOException                  upon signature calculation failure
   * @throws NoSuchAlgorithmException     upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedGetObject(String bucketName, String objectName, Integer expires)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException, InvalidKeyException, NoSuchAlgorithmException,
           InvalidExpiresRangeException {
    updateRegionMap(bucketName);

    if (expires < 1 || expires > DEFAULT_EXPIRY_TIME) {
      throw new InvalidExpiresRangeException(expires, "expires must be in range of 1 to " + DEFAULT_EXPIRY_TIME);
    }

    Request request = getRequest(Method.GET, bucketName, objectName, null, null, null, 0);
    RequestSigner signer = new RequestSigner(this.accessKey, this.secretKey, Regions.INSTANCE.region(bucketName));
    return signer.preSignV4(request, expires);
  }


  /** Returns an presigned URL containing the object.
   *
   * @param bucketName  Bucket name
   * @param objectName  Object name in the bucket
   *
   * @throws IOException     upon connection error
   * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedGetObject(String bucketName, String objectName)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException, InvalidKeyException, NoSuchAlgorithmException,
           InvalidExpiresRangeException {
    return presignedGetObject(bucketName, objectName, DEFAULT_EXPIRY_TIME);
  }


  /** Returns an presigned URL for PUT.
   *
   * @param bucketName  Bucket name
   * @param objectName  Object name in the bucket
   * @param expires     object expiration
   *
   * @throws InvalidBucketNameException   upon an invalid bucket name
   * @throws InvalidKeyException          upon an invalid access key or secret key
   * @throws IOException                  upon signature calculation failure
   * @throws NoSuchAlgorithmException     upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedPutObject(String bucketName, String objectName, Integer expires)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException, InvalidKeyException, NoSuchAlgorithmException,
           InvalidExpiresRangeException {
    updateRegionMap(bucketName);

    if (expires < 1 || expires > DEFAULT_EXPIRY_TIME) {
      throw new InvalidExpiresRangeException(expires, "expires must be in range of 1 to " + DEFAULT_EXPIRY_TIME);
    }

    Request request = getRequest(Method.PUT, bucketName, objectName, null, null, "".getBytes("UTF-8"), 0);
    RequestSigner signer = new RequestSigner(this.accessKey, this.secretKey, Regions.INSTANCE.region(bucketName));
    return signer.preSignV4(request, expires);
  }


  /** Returns an presigned URL for PUT.
   *
   * @param bucketName  Bucket name
   * @param objectName  Object name in the bucket
   *
   * @throws IOException     upon connection error
   * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature calculation
   * @throws InvalidExpiresRangeException upon input expires is out of range
   */
  public String presignedPutObject(String bucketName, String objectName)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException, InvalidKeyException, NoSuchAlgorithmException,
           InvalidExpiresRangeException {
    return presignedPutObject(bucketName, objectName, DEFAULT_EXPIRY_TIME);
  }


  /** Returns an Map for POST form data.
   *
   * @param policy new PostPolicy
   *
   */
  public Map<String, String> presignedPostPolicy(PostPolicy policy)
    throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException,
           InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    updateRegionMap(policy.bucketName());
    return policy.formData(this.accessKey, this.secretKey);
  }


  /** Remove an object from a bucket.
   *
   * @param bucketName Bucket name
   * @param objectName Object name in the bucket
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void removeObject(String bucketName, String objectName)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    executeDelete(bucketName, objectName, null);
  }


  /**
   * listObjects is a wrapper around listObjects(bucketName, null)
   *
   * @param bucketName Bucket name
   *
   * @return an iterator of Items.
   * @see #listObjects(String, String, boolean)
   */
  public Iterator<Result<Item>> listObjects(final String bucketName) throws XmlPullParserException {
    return listObjects(bucketName, null);
  }


  /**
   * listObjects is a wrapper around listObjects(bucketName, prefix, true)
   *
   * @param bucketName Bucket name
   * @param prefix     Prefix string.  List objects whose name starts with `prefix`
   *
   * @return an iterator of Items.
   * @see #listObjects(String, String, boolean)
   */
  public Iterator<Result<Item>> listObjects(final String bucketName, final String prefix)
    throws XmlPullParserException {
    // list all objects recursively
    return listObjects(bucketName, prefix, true);
  }


  /**
   * @param bucketName Bucket name
   * @param prefix     Prefix string.  List objects whose name starts with `prefix`
   * @param recursive when false, emulates a directory structure where each listing returned is either a full object
   *                  or part of the object's key up to the first '/'. All objects wit the same prefix up to the first
   *                  '/' will be merged into one entry.
   *
   * @return an iterator of Items.
   */
  public Iterator<Result<Item>> listObjects(final String bucketName, final String prefix, final boolean recursive)
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
            listBucketResult = listObjects(bucketName, marker, prefix, delimiter, 1000);
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


  private ListBucketResult listObjects(String bucketName, String marker, String prefix, String delimiter, int maxKeys)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    if (maxKeys < 0 || maxKeys > 1000) {
      maxKeys = 1000;
    }

    Map<String,String> queryParamMap = new HashMap<String,String>();
    queryParamMap.put("max-keys", Integer.toString(maxKeys));
    queryParamMap.put("marker", marker);
    queryParamMap.put("prefix", prefix);
    queryParamMap.put("delimiter", delimiter);

    HttpResponse response = executeGet(bucketName, null, null, queryParamMap);

    ListBucketResult result = new ListBucketResult();
    result.parseXml(response.body().charStream());
    return result;
  }


  /**
   * List buckets owned by the current user.
   *
   * @return an iterator of Bucket type.
   *
   * @throws NoResponseException     upon no response from server
   * @throws IOException             upon connection error
   * @throws XmlPullParserException  upon parsing response xml
   * @throws ErrorResponseException  upon unsuccessful execution
   * @throws InternalException       upon internal library error
   */
  public Iterator<Bucket> listBuckets()
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    HttpResponse response = executeGet(null, null, null, null);
    ListAllMyBucketsResult result = new ListAllMyBucketsResult();
    result.parseXml(response.body().charStream());
    return result.buckets().iterator();
  }


  /**
   * Test whether a bucket exists and the user has at least read access.
   *
   * @param bucketName Bucket name
   *
   * @return true if the bucket exists and the user has at least read access
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public boolean bucketExists(String bucketName)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    try {
      executeHead(bucketName, null);
      return true;
    } catch (ErrorResponseException e) {
      if (e.getErrorCode() != ErrorCode.NO_SUCH_BUCKET) {
        throw e;
      }
    }

    return false;
  }


  /**
   * Create a bucket with default region and ACL.
   *
   * @param bucketName Bucket name
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void makeBucket(String bucketName)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, NoSuchAlgorithmException, InternalException {
    this.makeBucket(bucketName, null, null);
  }


  /**
   * Create a bucket with given region and default ACL.
   *
   * @param bucketName Bucket name
   * @param region     region in which the bucket will be created
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void makeBucket(String bucketName, String region)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, NoSuchAlgorithmException, InternalException {
    this.makeBucket(bucketName, region, null);
  }


  /**
   * Create a bucket with given ACL and default region.
   *
   * @param bucketName Bucket name
   * @param acl        Canned ACL
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void makeBucket(String bucketName, Acl acl)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, NoSuchAlgorithmException, InternalException {
    this.makeBucket(bucketName, null, acl);
  }


  /**
   * Create a bucket with given region and ACL.
   *
   * @param bucketName Bucket name
   * @param region     region in which the bucket will be created
   * @param acl        Canned ACL
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void makeBucket(String bucketName, String region, Acl acl)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, NoSuchAlgorithmException, InternalException {
    byte[] data = null;
    Map<String,String> headerMap = new HashMap<String,String>();

    if (region == null || "us-east-1".equals(region)) {
      // for 'us-east-1', location constraint is not required.  for more info
      // http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region
      data = "".getBytes("UTF-8");
    } else {
      CreateBucketConfiguration config = new CreateBucketConfiguration();
      config.setLocationConstraint(region);
      data = config.toString().getBytes("UTF-8");

      byte[] md5sum = getMd5Digest(data, data.length);
      if (md5sum != null) {
        headerMap.put("Content-MD5", BaseEncoding.base64().encode(md5sum));
      }
    }

    if (acl == null) {
      headerMap.put("x-amz-acl", Acl.PRIVATE.toString());
    } else {
      headerMap.put("x-amz-acl", acl.toString());
    }

    executePut(bucketName, null, headerMap, null, "us-east-1", data, data.length);
  }


  /**
   * Remove a bucket with a given name.
   * <p>
   * NOTE: -
   * All objects (including all object versions and delete markers) in the bucket
   * must be deleted prior, this API will not recursively delete objects
   * </p>
   *
   * @param bucketName Bucket name
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void removeBucket(String bucketName)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    executeDelete(bucketName, null, null);
  }


  /**
   * Get the bucket's ACL.
   *
   * @param bucketName Bucket name
   *
   * @return Acl type
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public Acl getBucketAcl(String bucketName)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    Map<String,String> queryParamMap = new HashMap<String,String>();
    queryParamMap.put("acl", "");

    HttpResponse response = executeGet(bucketName, null, null, queryParamMap);

    AccessControlPolicy result = new AccessControlPolicy();
    result.parseXml(response.body().charStream());

    Acl acl = Acl.PRIVATE;
    List<Grant> grants = result.grants();
    switch (grants.size()) {
      case 1:
        for (Grant grant : grants) {
          if (grant.grantee().uri() == null && "FULL_CONTROL".equals(grant.permission())) {
            acl = Acl.PRIVATE;
            break;
          }
        }
        break;
      case 2:
        for (Grant grant : grants) {
          if ("http://acs.amazonaws.com/groups/global/AuthenticatedUsers".equals(grant.grantee().uri())
              && "READ".equals(grant.permission())) {
            acl = Acl.AUTHENTICATED_READ;
            break;
          } else if ("http://acs.amazonaws.com/groups/global/AllUsers".equals(grant.grantee().uri())
                     && "READ".equals(grant.permission())) {
            acl = Acl.PUBLIC_READ;
            break;
          }
        }
        break;
      case 3:
        for (Grant grant : grants) {
          if ("http://acs.amazonaws.com/groups/global/AllUsers".equals(grant.grantee().uri())
              && "WRITE".equals(grant.permission())) {
            acl = Acl.PUBLIC_READ_WRITE;
            break;
          }
        }
        break;
      default:
        throw new InternalException("Invalid control flow.  Please report this issue at "
                                      + "https://github.com/minio/minio-java/issues");
    }
    return acl;
  }


  /**
   * Set the bucket's ACL.
   *
   * @param bucketName Bucket name
   * @param acl        Canned ACL
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws InvalidAclNameException     upon invalid ACL is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void setBucketAcl(String bucketName, Acl acl)
    throws InvalidAclNameException, InvalidBucketNameException, NoResponseException, IOException,
           XmlPullParserException, ErrorResponseException, InternalException {
    if (acl == null) {
      throw new InvalidAclNameException();
    }

    Map<String,String> queryParamMap = new HashMap<String,String>();
    queryParamMap.put("acl", "");

    Map<String,String> headerMap = new HashMap<String,String>();
    headerMap.put("x-amz-acl", acl.toString());

    executePut(bucketName, null, headerMap, queryParamMap, "".getBytes("UTF-8"), 0);
  }


  private String putObject(String bucketName, String objectName, String contentType, byte[] data, int length,
                           byte[] md5sum, String uploadId, int partNumber)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    Map<String,String> headerMap = new HashMap<String,String>();
    headerMap.put("Content-MD5", BaseEncoding.base64().encode(md5sum));

    Map<String,String> queryParamMap = null;
    if (partNumber > 0 && uploadId != null && !"".equals(uploadId.trim())) {
      queryParamMap = new HashMap<String,String>();
      queryParamMap.put("partNumber", Integer.toString(partNumber));
      queryParamMap.put("uploadId", uploadId);
    }

    HttpResponse response = executePut(bucketName, objectName, headerMap, queryParamMap, data, length);
    return response.header().getEtag();
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
   * @param bucketName  Bucket name
   * @param objectName  Object name to create in the bucket
   * @param fileName    File name to upload
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void putObject(String bucketName, String objectName, String fileName)
    throws MinioException, InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InvalidArgumentException, NoSuchAlgorithmException, InternalException {
    if (fileName == null || "".equals(fileName.trim())) {
      throw new InvalidArgumentException("empty file name is not allowed");
    }

    Path filePath = Paths.get(fileName);
    if (!Files.isRegularFile(filePath)) {
      throw new InvalidArgumentException("'" + fileName + "': not a regular file");
    }

    String contentType = Files.probeContentType(filePath);
    long size = Files.size(filePath);

    InputStream is = Files.newInputStream(filePath);
    try {
      putObject(bucketName, objectName, contentType, size, is);
    } finally {
      is.close();
    }
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
   * @param bucketName  Bucket name
   * @param objectName  Object name to create in the bucket
   * @param contentType Content type to set this object to
   * @param size        Size of all the data that will be uploaded.
   * @param body        Data to upload
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void putObject(String bucketName, String objectName, String contentType, long size, InputStream body)
    throws InvalidArgumentException, MinioException, InsufficientDataException, InputSizeMismatchException,
           InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, NoSuchAlgorithmException, InternalException {
    if (contentType == null || "".equals(contentType.trim())) {
      contentType = "application/octet-stream";
    }

    if (size <= MIN_MULTIPART_SIZE) {
      byte[] buf = new byte[(int) size];
      readStream(body, buf, (int) size);
      putObject(bucketName, objectName, contentType, buf, (int) size, getMd5Digest(buf, (int) size), null, 0);
      return;
    }

    int partSize = calculatePartSize(size);
    int partCount = (int) (size / (long) partSize);
    if (size % partSize != 0) {
      partCount++;
    }
    long lastPartSize = size - (long) (partSize * (partCount - 1));
    String uploadId = null;

    // get incomplete multipart upload of the same object if any
    Iterator<Result<Upload>> multipartUploads = listIncompleteUploads(bucketName, objectName);
    while (multipartUploads.hasNext()) {
      Upload upload = multipartUploads.next().getResult();
      if (upload.getObjectName().equals(objectName)) {
        // TODO: its possible to have multiple mutlipart upload session for the same object
        // TODO: if found we would need to error out
        uploadId = upload.getUploadId();
        break;
      }
    }

    Part part = null;
    // TODO: as partCount is known always, it better to use array than LinkedList
    List<Part> totalParts = new LinkedList<Part>();
    Iterator<Part> existingParts = null;
    if (uploadId != null) {
      existingParts = listObjectParts(bucketName, objectName, uploadId);
      if (existingParts.hasNext()) {
        part = existingParts.next();
      }
    } else {
      uploadId = initMultipartUpload(bucketName, objectName);
    }

    byte[] buf = new byte[partSize];
    int bytesRead = 0;
    int expectedReadSize = partSize;
    for (int partNumber = 1; partNumber <= partCount; partNumber++) {
      if (part != null && partNumber == part.getPartNumber() && part.getSize() == partSize) {
        // this part is already uploaded
        // TODO: validate the integrity of the part by md5sum etc
        // TODO: to make it simpler, we check the size time being
        totalParts.add(part);
        skipStream(body, partSize);
        part = null;

        if (existingParts.hasNext()) {
          part = existingParts.next();
        }

        continue;
      }

      if (partNumber == partCount) {
        expectedReadSize = (int) lastPartSize;
      }

      readStream(body, buf, expectedReadSize);
      String etag = putObject(bucketName, objectName, contentType, buf, expectedReadSize,
                              getMd5Digest(buf, expectedReadSize), uploadId, partNumber);
      totalParts.add(new Part(partNumber, etag));
    }

    completeMultipart(bucketName, objectName, uploadId, totalParts);
  }


  /**
   * listIncompleteUploads is a wrapper around listIncompleteUploads(bucketName, null, true)
   *
   * @param bucketName Bucket name
   *
   * @return an iterator of Upload.
   * @see #listIncompleteUploads(String, String, boolean)
   */
  public Iterator<Result<Upload>> listIncompleteUploads(String bucketName) throws XmlPullParserException {
    return listIncompleteUploads(bucketName, null, true);
  }


  /**
   * listIncompleteUploads is a wrapper around listIncompleteUploads(bucketName, prefix, true)
   *
   * @param bucketName Bucket name
   * @param prefix filters the list of uploads to include only those that start with prefix
   *
   * @return an iterator of Upload.
   * @see #listIncompleteUploads(String, String, boolean)
   */
  public Iterator<Result<Upload>> listIncompleteUploads(String bucketName, String prefix)
    throws XmlPullParserException {
    return listIncompleteUploads(bucketName, prefix, true);
  }


  /**
   * @param bucketName  Bucket name
   * @param prefix      Prefix string.  List objects whose name starts with `prefix`
   * @param recursive when false, emulates a directory structure where each listing returned is either a full object
   *                  or part of the object's key up to the first '/'. All uploads with the same prefix up to the first
   *                  '/' will be merged into one entry.
   *
   * @return an iterator of Upload.
   */
  public Iterator<Result<Upload>> listIncompleteUploads(final String bucketName, final String prefix,
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
            uploadResult = listIncompleteUploads(bucketName, keyMarker,
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


  private ListMultipartUploadsResult listIncompleteUploads(String bucketName, String keyMarker, String uploadIdMarker,
                                                           String prefix, String delimiter, int maxUploads)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    if (maxUploads < 0 || maxUploads > 1000) {
      maxUploads = 1000;
    }

    Map<String,String> queryParamMap = new HashMap<String,String>();
    queryParamMap.put("uploads", "");
    queryParamMap.put("max-uploads", Integer.toString(maxUploads));
    queryParamMap.put("prefix", prefix);
    queryParamMap.put("key-marker", keyMarker);
    queryParamMap.put("upload-id-marker", uploadIdMarker);
    queryParamMap.put("delimiter", delimiter);

    HttpResponse response = executeGet(bucketName, null, null, queryParamMap);

    ListMultipartUploadsResult result = new ListMultipartUploadsResult();
    result.parseXml(response.body().charStream());
    return result;
  }


  private String initMultipartUpload(String bucketName, String objectName)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    Map<String,String> queryParamMap = new HashMap<String,String>();
    queryParamMap.put("uploads", "");

    HttpResponse response = executePost(bucketName, objectName, queryParamMap, "".getBytes("UTF-8"));

    InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
    result.parseXml(response.body().charStream());
    return result.getUploadId();
  }


  private void completeMultipart(String bucketName, String objectName, String uploadId, List<Part> parts)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    Map<String,String> queryParamMap = new HashMap<String,String>();
    queryParamMap.put("uploadId", uploadId);

    CompleteMultipartUpload completeManifest = new CompleteMultipartUpload();
    completeManifest.setParts(parts);

    executePost(bucketName, objectName, queryParamMap, completeManifest.toString().getBytes("UTF-8"));
  }


  private Iterator<Part> listObjectParts(final String bucketName, final String objectName,
                                         final String uploadId) throws XmlPullParserException {
    return new MinioIterator<Part>() {
      public int marker;
      private boolean isComplete = false;

      @Override
      protected List<Part> populate()
        throws InvalidArgumentException, InvalidBucketNameException, NoResponseException, IOException,
        XmlPullParserException, ErrorResponseException, InternalException {
        if (!isComplete) {
          ListPartsResult result;
          result = listObjectParts(bucketName, objectName, uploadId, marker);
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


  private ListPartsResult listObjectParts(String bucketName, String objectName, String uploadId, int partNumberMarker)
    throws InvalidArgumentException, InvalidBucketNameException, NoResponseException, IOException,
           XmlPullParserException, ErrorResponseException, InternalException {
    if (partNumberMarker <= 0) {
      throw new InvalidArgumentException("part number marker should be greater than 0");
    }

    Map<String,String> queryParamMap = new HashMap<String,String>();
    queryParamMap.put("uploadId", uploadId);
    queryParamMap.put("part-number-marker", Integer.toString(partNumberMarker));

    HttpResponse response = executeGet(bucketName, objectName, null, queryParamMap);

    ListPartsResult result = new ListPartsResult();
    result.parseXml(response.body().charStream());
    return result;
  }


  private void abortMultipartUpload(String bucketName, String objectName, String uploadId)
    throws InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    Map<String,String> queryParamMap = new HashMap<String,String>();
    queryParamMap.put("uploadId", uploadId);
    executeDelete(bucketName, objectName, queryParamMap);
  }


  /**
   * Remove active incomplete multipart upload of an object.
   *
   * @param bucketName Bucket name
   * @param objectName Object name in the bucket
   *
   * @throws InvalidBucketNameException  upon invalid bucket name is given
   * @throws NoResponseException         upon no response from server
   * @throws IOException                 upon connection error
   * @throws XmlPullParserException      upon parsing response xml
   * @throws ErrorResponseException      upon unsuccessful execution
   * @throws InternalException           upon internal library error
   */
  public void removeIncompleteUpload(String bucketName, String objectName)
    throws MinioException, InvalidBucketNameException, NoResponseException, IOException, XmlPullParserException,
           ErrorResponseException, InternalException {
    Iterator<Result<Upload>> uploads = listIncompleteUploads(bucketName, objectName);
    while (uploads.hasNext()) {
      Upload upload = uploads.next().getResult();
      if (objectName.equals(upload.getObjectName())) {
        abortMultipartUpload(bucketName, objectName, upload.getUploadId());
        return;
      }
    }
  }


  private int calculatePartSize(long size) throws InvalidArgumentException {
    if (size > MAX_OBJECT_SIZE) {
      throw new InvalidArgumentException("size " + size + " is greater than allowed size 5TiB");
    }

    // 9999 is used instead of 10000 to cater for the last part being too small
    int partSize = (int) (size / (MAX_MULTIPART_COUNT - 1));
    if (partSize > MIN_MULTIPART_SIZE) {
      if (partSize > MAX_MULTIPART_SIZE) {
        return MAX_MULTIPART_SIZE;
      }
      return partSize;
    }
    return MIN_MULTIPART_SIZE;
  }


  private static byte[] getMd5Digest(byte[] data, int length) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("MD5");
    digest.update(data, 0, length);
    return digest.digest();
  }


  private void readStream(InputStream is, byte[] buf, int bufSize) throws IOException, InsufficientDataException {
    int bytesRead = 0;
    int totalBytesRead = 0;

    while ((bytesRead = is.read(buf, totalBytesRead, (bufSize - totalBytesRead))) >= 0) {
      totalBytesRead += bytesRead;
      if (totalBytesRead == bufSize) {
        return;
      }
    }

    throw new InsufficientDataException("Insufficient data.  bytes read " + totalBytesRead + " expected " + bufSize);
  }


  private void skipStream(InputStream is, long n) throws IOException, InsufficientDataException {
    long bytesSkipped = 0;
    long totalBytesSkipped = 0;

    while ((bytesSkipped = is.skip(n - totalBytesSkipped)) >= 0) {
      totalBytesSkipped += bytesSkipped;
      if (totalBytesSkipped == n) {
        return;
      }
    }

    throw new InsufficientDataException("Insufficient data.  bytes skipped " + totalBytesSkipped + " expected " + n);
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


  public URL getUrl() {
    return this.baseUrl.url();
  }
}
