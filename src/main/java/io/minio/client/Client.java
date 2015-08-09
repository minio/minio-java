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

package io.minio.client;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.Headers;

import com.google.api.client.xml.Xml;
import com.google.api.client.xml.XmlNamespaceDictionary;

import io.minio.client.acl.Acl;
import io.minio.client.errors.*;
import io.minio.client.messages.*;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
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
 * <li> Dropping all active multipart uploads</li>
 * <li> Setting canned ACLs on buckets</li>
 * </ul>
 * <h2>Object</h2>
 * <ul>
 * <li>Dropping an active multipart upload for a specific object and uploadId</li>
 * <li>Read object metadata</li>
 * <li>Reading an object</li>
 * <li>Reading a range of bytes of an object</li>
 * <li>Deleting an object</li>
 * </ul>
 * <p>
 * Optionally, users can also provide access/secret keys. If keys are provided, all requests by the
 * client will be signed using AWS Signature Version 4. See {@link #getClient(URL, String, String)}
 * </p>
 * For examples on using this library, please see <a href="https://github.com/minio/minio-java/tree/master/src/test/java/io/minio/examples"></a>.
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class Client {
    // default multipart upload size is 5MB
    private static final int PART_SIZE = 5 * 1024 * 1024;
    // default transport is an HTTP client.
    private static final OkHttpClient defaultTransport = new OkHttpClient();
    // the current client instance's base URL.
    private final HttpUrl url;

    // logger which is set only on enableLogger. Atomic reference is used to prevent multiple loggers from being instantiated
    private final AtomicReference<Logger> logger = new AtomicReference<Logger>();

    // current transporter which can be used to mock
    private OkHttpClient transport = defaultTransport;
    // access key to sign all requests with
    private String accessKey;
    // Secret key to sign all requests with
    private String secretKey;
    // user agent to tag all requests with
    private String userAgent = "minio-java/" + MinioProperties.INSTANCE.getVersion() + " (" + System.getProperty("os.name") + "; " + System.getProperty("os.arch") + ")";
    // user agent can be set only once with in a class
    private boolean userAgentSet = false;

    // Don't allow users to instantiate clients themselves, since it is bad form to throw exceptions in constructors.
    // Use Client.getClient instead
    private Client(URL url, String accessKey, String secretKey) {
        this.url = HttpUrl.get(url);
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * Create a new client given a url
     *
     * @param url must be the full url to the cloud storage server, excluding both bucket or object paths.
     *            For example: http://play.minio.io
     *            Valid:
     *            * https://s3-us-west-2.amazonaws.com
     *            * http://play.minio.io
     *            Invalid:
     *            * https://s3-us-west-2.amazonaws.com/example/
     *            * https://s3-us-west-2.amazonaws.com/example/object
     * @param accessKey access key id for authenticating API requests
     * @param secretKey secret key id for authenticating API requests
     *
     * @return an cloud storage client backed by an S3 compatible server.
     *
     * @throws MalformedURLException malformed url
     * @throws ClientException invalid argument
     * @see #getClient(URL url, String accessKey, String secretKey)
     */
    public static Client getClient(URL url, String accessKey, String secretKey) throws MalformedURLException, ClientException {
        // URL should not be null
        if (url == null) {
            throw new InvalidArgumentException();
        }

        // check if url is http or https
        if (!("http".equals(url.getProtocol()) || "https".equals(url.getProtocol()))) {
            throw new MalformedURLException("Scheme should be http or https");
        }

        // Set trailing / in path
        if (url.getPath().length() == 0) {
            String path = url.toString() + "/";
            url = new URL(path);
        }

        // Only a trailing path should be present in the path
        if (url.getPath().length() > 0 && !"/".equals(url.getPath())) {
            throw new MalformedURLException("Path should be empty: '" + url.getPath() + "'");
        }

        // return a new http client
        return new Client(url, accessKey, secretKey);
    }

    /**
     * Create a new client given a url
     *
     * @param url must be the full url to the cloud storage server, excluding both bucket or object paths.
     *            For example: http://play.minio.io
     *            Valid:
     *            * https://s3-us-west-2.amazonaws.com
     *            * http://play.minio.io
     *            Invalid:
     *            * https://s3-us-west-2.amazonaws.com/example/
     *            * https://s3-us-west-2.amazonaws.com/example/object
     *
     * @return an cloud storage client backed by an S3 compatible server.
     *
     * @throws MalformedURLException malformed url
     * @throws ClientException invalid argument
     * @see #getClient(URL url)
     */
    public static Client getClient(URL url) throws MalformedURLException, ClientException {
        return getClient(url, null, null);
    }

    /**
     * @param url must be the full url to the cloud storage server, excluding both bucket or object paths.
     * @param accessKey access key id for authenticating API requests
     * @param secretKey secret key id for authenticating API requests
     *
     * @return an cloud storage client backed by an S3 compatible server.
     *
     * @throws MalformedURLException malformed url
     * @throws ClientException invalid argument
     * @see #getClient(String url, String accesskey, String secretKey)
     */
    public static Client getClient(String url, String accessKey, String secretKey) throws MalformedURLException, ClientException {
        if (url == null) {
            throw new InvalidArgumentException();
        }
        return getClient(new URL(url), accessKey, secretKey);
    }

    /**
     * @param url must be the full url to the cloud storage server, excluding both bucket or object paths.
     *
     * @return an cloud storage client backed by an S3 compatible server.
     *
     * @throws MalformedURLException malformed url
     * @throws ClientException invalid argument
     * @see #getClient(String url)
     */
    public static Client getClient(String url) throws MalformedURLException, ClientException {
        if (url == null) {
            throw new InvalidArgumentException();
        }
        return getClient(new URL(url), null, null);
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
        if (!this.userAgentSet && name != null && version != null) {
            String newUserAgent = name.trim() + "/" + version.trim() + " (";
            StringBuilder sb = new StringBuilder();
            for (String comment : comments) {
                if (comment != null) {
                    sb.append(comment.trim()).append("; ");
                }
            }
            this.userAgent = this.userAgent + newUserAgent + sb.toString() + ") ";
            this.userAgentSet = true;
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
     * @return Populated object metadata
     *
     * @throws IOException     upon connection failure
     * @throws ClientException upon failure from server
     * @see ObjectStat
     */
    public ObjectStat statObject(String bucket, String key) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfKey(bucket, key);
        Request request = getRequest("HEAD", url);
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
                InternalClientException internalClientException = new InternalClientException();
                internalClientException.initCause(e);
                throw internalClientException;
            } finally {
                response.body().close();
            }
        }
        throw new IOException();
    }

    private void parseError(Response response) throws IOException, ClientException {
        // if response is null, throw an IOException and finish here
        if (response == null) {
            throw new IOException("No response was returned");
        }

        if (response.isRedirect()) {
            throw new HTTPRedirectException();
        }

        int statusCode = response.code();
        if (statusCode == 404 || statusCode == 403 || statusCode == 501 || statusCode == 405) {
            ClientException e;
            ErrorResponse errorResponse = new ErrorResponse();
            String hostId = String.valueOf(response.headers().get("x-amz-id-2"));

            if ("null".equals(hostId)) {
                hostId = null;
            }
            String requestId = String.valueOf(response.headers().get("x-amz-request-id"));
            if ("null".equals(requestId)) {
                requestId = null;
            }
            errorResponse.setxAmzID2(hostId);
            errorResponse.setRequestID(requestId);

            String resource = response.request().url().getPath();
            if (resource == null) {
                resource = "/";
            }

            int pathLength = resource.split("/").length;
            errorResponse.setResource(resource);

            if (statusCode == 404) {
                if (pathLength > 2) {
                    errorResponse.setCode("NoSuchKey");
                    e = new ObjectNotFoundException();
                } else if (pathLength == 2) {
                    errorResponse.setCode("NoSuchBucket");
                    e = new BucketNotFoundException();
                } else {
                    e = new InternalClientException("404 without body resulted in path with less than two components");
                }
            } else if (statusCode == 501 || statusCode == 405) {
                errorResponse.setCode("MethodNotAllowed");
                e = new MethodNotAllowedException();
            } else {
                errorResponse.setCode("AccessDenied");
                e = new AccessDeniedException();
            }
            e.setErrorResponse(errorResponse);
            throw e;
        }

        // Populate an ErrorResponse, will throw an ClientException if not parsable. We should just pass it up.
        ErrorResponse errorResponse = new ErrorResponse();
        parseXml(response, errorResponse);

        // Return the correct exception based upon the error code.
        // Minor note, flipped .equals() protects against null pointer exceptions
        String code = errorResponse.getCode();
        ClientException e;
        if ("NoSuchBucket".equals(code)) e = new BucketNotFoundException();
        else if ("NoSuchKey".equals(code)) e = new ObjectNotFoundException();
        else if ("InvalidBucketName".equals(code)) e = new InvalidKeyNameException();
        else if ("InvalidObjectName".equals(code)) e = new InvalidKeyNameException();
        else if ("AccessDenied".equals(code)) e = new AccessDeniedException();
        else if ("BucketAlreadyExists".equals(code)) e = new BucketExistsException();
        else if ("InternalError".equals(code)) e = new InternalServerException();
        else if ("KeyTooLong".equals(code)) e = new InvalidKeyNameException();
        else if ("TooManyBuckets".equals(code)) e = new MaxBucketsReachedException();
        else if ("PermanentRedirect".equals(code)) e = new HTTPRedirectException();
        else if ("TemporaryRedirect".equals(code)) e = new HTTPRedirectException();
        else if ("MethodNotAllowed".equals(code)) e = new ObjectExistsException();
        else if ("BucketAlreadyOwnedByYou".equals(code)) e = new BucketExistsException();
        else e = new InternalClientException(errorResponse.toString());
        e.setErrorResponse(errorResponse);
        throw e;
    }

    private void parseXml(Response response, Object objectToPopulate) throws IOException, InternalClientException {
        // if response is null, throw an IOException
        if (response == null) {
            throw new IOException("No response was returned");
        }
        // if objectToPopulate is null, this indicates a flaw in the library. Throw a relevant exception.
        if (objectToPopulate == null) {
            throw new InternalClientException("Object to populate should not be null");
        }
        try {
            // set up a parser
            XmlPullParser parser = Xml.createParser();
            // write up the response body to the parser
            parser.setInput(response.body().charStream());
            // create a dictionary and populate based on object type
            XmlNamespaceDictionary dictionary = new XmlNamespaceDictionary();
            if (objectToPopulate instanceof ErrorResponse) {
                // Errors have no namespace, so we set a default empty alias and namespace
                dictionary.set("", "");
            } //else {
            // parse and return
            Xml.parseElement(parser, objectToPopulate, dictionary, null);
        } catch (XmlPullParserException e) {
            InternalClientException internalClientException = new InternalClientException();
            internalClientException.initCause(e);
            throw internalClientException;
        }
    }

    private Request getRequest(String method, HttpUrl url) throws IOException, InternalClientException {
        return getRequest(method, url, null);
    }

    private Request getRequest(String method, HttpUrl url, final byte[] data) throws IOException, InternalClientException {
        DateTimeFormatter dateFormatyyyyMMddThhmmssZ = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'").withZoneUTC();
        if (method == null || "".equals(method.trim())) {
            throw new InternalClientException("Method should be populated");
        }
        if (url == null) {
            throw new InternalClientException("URL should be populated");
        }

        DateTime date = new DateTime();
        String dateString = date.toString(dateFormatyyyyMMddThhmmssZ);

        this.transport.setFollowRedirects(false);
        this.transport.interceptors().add(new RequestSigner(data, accessKey, secretKey, date));

        RequestBody requestBody = null;
        if (data != null) {
            requestBody = RequestBody.create(null, data);
        }
        Request request = new Request.Builder()
            .url(url)
            .method(method, requestBody)
            .header("User-Agent", this.userAgent)
            .header("x-amz-date", dateString)
            .build();

        return request;
    }

    private HttpUrl getHttpUrlOfKey(String bucket, String key) throws InvalidBucketNameException, InvalidKeyNameException {
        if (bucket == null || "".equals(bucket.trim())) {
            throw new InvalidBucketNameException();
        }
        if (key == null || "".equals(bucket.trim())) {
            throw new InvalidKeyNameException();
        }

        HttpUrl url = this.url.newBuilder()
            .addPathSegment(bucket)
            .addPathSegment(key)
            .build();

        return url;
    }

    private HttpUrl getHttpUrlOfBucket(String bucket) throws InvalidBucketNameException {
        if (bucket == null || "".equals(bucket.trim())) {
            throw new InvalidBucketNameException();
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
    public InputStream getObject(String bucket, String key) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfKey(bucket, key);

        Request request = getRequest("GET", url);
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

    /**
     * Remove an object from a bucket
     *
     * @param bucket object's bucket
     * @param key    object's key
     *
     * @throws IOException     upon connection error
     * @throws ClientException upon failure from server
     */
    public void removeObject(String bucket, String key) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfKey(bucket, key);

        Request request = getRequest("DELETE", url);
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
     * Returns an InputStream containing a subset of the object. The InputStream must be
     * closed or the connection will remain open.
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
    public InputStream getPartialObject(String bucket, String key, long offsetStart) throws IOException, ClientException {
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
    public InputStream getPartialObject(String bucket, String key, long offsetStart, long length) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfKey(bucket, key);

        if (offsetStart < 0 || length <= 0) {
            throw new InvalidRangeException();
        }

        Request request = getRequest("GET", url);
        long offsetEnd = offsetStart + length - 1;

        Request rangeRequest = request.newBuilder()
            .header("Range", "bytes=" + offsetStart + "-" + offsetEnd)
            .build();

        // we close the response only on failure or the user will be unable to retrieve the object
        // it is the user's responsibility to close the input stream
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

    /**
     * listObjects is a wrapper around listObjects(bucket, prefix, true)
     *
     * @param bucket to list objects of
     * @param prefix filters the list of objects to include only those that start with prefix
     *
     * @return an iterator of Items.
     *
     * @see #listObjects(String, String, boolean)
     */
    public Iterator<Result<Item>> listObjects(final String bucket, final String prefix) {
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
    public Iterator<Result<Item>> listObjects(final String bucket, final String prefix, final boolean recursive) {
        return new MinioIterator<Result<Item>>() {
            private String marker = null;
            private boolean isComplete = false;

            @Override
            protected List<Result<Item>> populate() {
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
                    } catch (ClientException e) {
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

    /**
     * listObjects is a wrapper around listObjects(bucket, null, true)
     *
     * @param bucket is the bucket to list objects from
     *
     * @return an iterator of Items.
     *
     * @see #listObjects(String, String, boolean)
     */
    public Iterator<Result<Item>> listObjects(final String bucket) {
        return listObjects(bucket, null);
    }

    private ListBucketResult listObjects(String bucket, String marker, String prefix, String delimiter, int maxKeys) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfBucket(bucket);

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

        Request request = getRequest("GET", url);
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
     * Set test transports for mocking the http request and response
     */
    void setTransport(OkHttpClient transport) {
        this.transport = transport;
    }

    /**
     * Sets the test transport back to the default http transport
     */
    @SuppressWarnings("unused")
    void resetTransport() {
        this.transport = defaultTransport;
    }

    /**
     * List buckets owned by the current user.
     *
     * @return a list of buckets owned by the current user
     *
     * @throws IOException     upon connection failure
     * @throws ClientException upon failure from server
     */
    public Iterator<Bucket> listBuckets() throws IOException, ClientException {
        Request request = getRequest("GET", this.url);

        Response response = this.transport.newCall(request).execute();
        if (response != null) {
            try {
                if (response.isSuccessful()) {
                    ListAllMyBucketsResult retrievedBuckets = new ListAllMyBucketsResult();
                    parseXml(response, retrievedBuckets);
                    return retrievedBuckets.getBuckets().iterator();
                }
                try {
                    parseError(response);
                } catch (HTTPRedirectException ex) {
                    AccessDeniedException fe = new AccessDeniedException();
                    fe.initCause(ex);
                    throw fe;
                }
            } finally {
                response.body().close();
            }
        }
        throw new IOException();
    }


    /**
     * Test whether a bucket exists and the user has at least read access
     *
     * @param bucket bucket to test for existence and access
     *
     * @return true if the bucket exists and the user has at least read access
     *
     * @throws IOException     upon connection error
     * @throws ClientException upon failure from server
     */
    public boolean bucketExists(String bucket) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfBucket(bucket);

        Request request = getRequest("HEAD", url);
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
     * @param bucket bucket to create
     *
     * @throws IOException     upon connection error
     * @throws ClientException upon failure from server
     */
    public void makeBucket(String bucket) throws IOException, ClientException {
        this.makeBucket(bucket, Acl.PRIVATE);
    }

    /**
     * Create a bucket with a given name and ACL
     *
     * @param bucket bucket to create
     * @param acl    canned acl
     *
     * @throws IOException     upon connection error
     * @throws ClientException upon failure from server
     */
    public void makeBucket(String bucket, Acl acl) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfBucket(bucket);
        Request request = null;

        CreateBucketConfiguration config = new CreateBucketConfiguration();
        String region = Regions.INSTANCE.getRegion(url.uri().getHost());

        // ``us-east-1`` is not a valid location constraint according to amazon, so we skip it
        // Valid constraints are
        // [ us-west-1 | us-west-2 | EU or eu-west-1 | eu-central-1 | ap-southeast-1 | ap-northeast-1 | ap-southeast-2 | sa-east-1 ]
        if (!("milkyway".equals(region) || "us-east-1".equals(region))) {
            config.setLocationConstraint(region);
            byte[] data = config.toString().getBytes("UTF-8");
            byte[] md5sum = calculateMd5sum(data);
            String base64md5sum = "";
            if (md5sum != null) {
                base64md5sum = DatatypeConverter.printBase64Binary(md5sum);
            }
            request = getRequest("PUT", url, data);
            request = request.newBuilder()
                .header("Content-MD5", base64md5sum)
                .build();
        } else {
            // okhttp requires PUT objects to have non-nil body, so we send a dummy not "null"
            byte[] dummy = "".getBytes("UTF-8");
            request = getRequest("PUT", url, dummy) ;
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
     * Remove a bucket with a given name
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
    public void removeBucket(String bucket) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfBucket(bucket);

        Request request = getRequest("DELETE", url);
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
    public Acl getBucketACL(String bucket) throws IOException, ClientException {
        AccessControlPolicy policy = this.getAccessPolicy(bucket);
        if (policy == null) {
            throw new InvalidArgumentException();
        }
        Acl acl = Acl.PRIVATE;
        List<Grant> accessControlList = policy.getAccessControlList();
        switch (accessControlList.size()) {
            case 1:
                for (Grant grant : accessControlList) {
                    if (grant.getGrantee().getURI() == null && "FULL_CONTROL".equals(grant.getPermission())) {
                        acl = Acl.PRIVATE;
                        break;
                    }
                }
                break;
            case 2:
                for (Grant grant : accessControlList) {
                    if ("http://acs.amazonaws.com/groups/global/AuthenticatedUsers".equals(grant.getGrantee().getURI()) &&
                            "READ".equals(grant.getPermission())) {
                        acl = Acl.AUTHENTICATED_READ;
                        break;
                    }
                    if ("http://acs.amazonaws.com/groups/global/AllUsers".equals(grant.getGrantee().getURI()) &&
                            "READ".equals(grant.getPermission())) {
                        acl = Acl.PUBLIC_READ;
                        break;
                    }
                }
                break;
            case 3:
                for (Grant grant : accessControlList) {
                    if ("http://acs.amazonaws.com/groups/global/AllUsers".equals(grant.getGrantee().getURI()) &&
                            "WRITE".equals(grant.getPermission())) {
                        acl = Acl.PUBLIC_READ_WRITE;
                        break;
                    }
                }
                break;
        }
        return acl;
    }

    private AccessControlPolicy getAccessPolicy(String bucket) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfBucket(bucket);
        url = url.newBuilder()
            .addQueryParameter("acl", "")
            .build();

        Request request = getRequest("GET", url);
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
    public void setBucketACL(String bucket, Acl acl) throws IOException, ClientException {
        if (acl == null) {
            throw new InvalidAclNameException();
        }

        HttpUrl url = getHttpUrlOfBucket(bucket);
        // make sure to set this, otherwise it would convert this call into a regular makeBucket operation
        url = url.newBuilder()
            .addQueryParameter("acl", "")
            .build();

        // FUTURE: dummy access control policy for now
        AccessControlPolicy accessControlPolicy = new AccessControlPolicy();

        byte[] data = accessControlPolicy.toString().getBytes("UTF-8");
        byte[] md5sum = calculateMd5sum(data);
        String base64md5sum = "";
        if (md5sum != null) {
            base64md5sum = DatatypeConverter.printBase64Binary(md5sum);
        }

        Request request = getRequest("PUT", url, data);
        request = request.newBuilder()
            .header("x-amz-acl", acl.toString())
            .header("Content-MD5", base64md5sum)
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
     * If the multipart session fails, the user is responsible for resuming or dropping the session.
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
     * @see #listAllIncompleteUploads(String)
     * @see #abortMultipartUpload(String, String, String)
     */
    public void putObject(String bucket, String key, String contentType, long size, InputStream body) throws IOException, ClientException {
        boolean isMultipart = false;
        int partSize = 0;
        String uploadID = null;

        if (contentType == null || "".equals(contentType.trim())) {
            contentType = "application/octet-stream";
        }

        if (size > PART_SIZE) {
            // check if multipart exists
            Iterator<Result<Upload>> multipartUploads = listAllIncompleteUploads(bucket, key);
            while (multipartUploads.hasNext()) {
                Upload upload = multipartUploads.next().getResult();
                if (upload.getKey().equals(key)) {
                    uploadID = upload.getUploadID();
                }
            }

            isMultipart = true;
            partSize = calculatePartSize(size);
            if (uploadID == null) {
                uploadID = newMultipartUpload(bucket, key);
            }
        }

        if (!isMultipart) {
            Data data = readData((int) size, body);
            if (data.getData().length != size || destructiveHasMore(body)) {
                throw new InputSizeMismatchException();
            }
            try {
                putObject(bucket, key, contentType, data.getData(), data.getMD5());
            } catch (MethodNotAllowedException ex) {
                ObjectExistsException objectExistsException = new ObjectExistsException();
                objectExistsException.setErrorResponse(ex.getErrorResponse());
                objectExistsException.initCause(ex);
                throw objectExistsException;
            }
        } else {
            long objectLength = 0;
            long totalSeen = 0;
            List<Part> parts = new LinkedList<Part>();
            int partNumber = 1;
            Iterator<Part> existingParts = listObjectParts(bucket, key, uploadID);
            while (true) {
                Data data = readData(partSize, body);
                totalSeen += data.getData().length;
                if (totalSeen > size) {
                    throw new InputSizeMismatchException();
                }
                if (existingParts.hasNext()) {
                    Part existingPart = existingParts.next();
                    if (existingPart.getPartNumber() == partNumber && existingPart.geteTag().toLowerCase().equals(DatatypeConverter.printHexBinary(data.getMD5()).toLowerCase())) {
                        partNumber++;
                        continue;
                    }
                }
                if (data.getData().length == 0) {
                    break;
                }
                String etag = putObject(bucket, key, contentType, data.getData(), data.getMD5(), uploadID, partNumber);
                Part part = new Part();
                part.setPartNumber(partNumber);
                part.seteTag(etag);
                parts.add(part);
                objectLength += data.getData().length;
                partNumber++;
            }
            if (objectLength != size) {
                throw new IOException("Data size mismatched");
            }
            if (totalSeen != size) {
                throw new InputSizeMismatchException();
            }
            try {
                completeMultipart(bucket, key, uploadID, parts);
            } catch (MethodNotAllowedException ex) {
                ObjectExistsException objectExistsException = new ObjectExistsException();
                objectExistsException.setErrorResponse(ex.getErrorResponse());
                objectExistsException.initCause(ex);
                throw objectExistsException;
            }
        }
    }

    private Iterator<Result<Upload>> listAllIncompleteUploads(String bucket) {
        return listAllIncompleteUploads(bucket, null);
    }

    private Iterator<Result<Upload>> listAllIncompleteUploads(final String bucket, final String prefix) {
        return new MinioIterator<Result<Upload>>() {
            private boolean isComplete = false;
            private String keyMarker = null;
            private String uploadIdMarker;

            @Override
            protected List<Result<Upload>> populate() {
                List<Result<Upload>> ret = new LinkedList<Result<Upload>>();
                if (!isComplete) {
                    ListMultipartUploadsResult uploadResult;
                    try {
                        uploadResult = listAllIncompleteUploads(bucket, keyMarker, uploadIdMarker, prefix, null, 1000);
                        if (uploadResult.isTruncated()) {
                            keyMarker = uploadResult.getNextKeyMarker();
                            uploadIdMarker = uploadResult.getNextUploadIDMarker();
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
                    } catch (ClientException e) {
                        ret.add(new Result<Upload>(null, e));
                        isComplete = true;
                    }
                }
                return ret;
            }
        };
    }

    private ListMultipartUploadsResult listAllIncompleteUploads(String bucket, String keyMarker, String uploadIDMarker, String prefix, String delimiter, int maxUploads) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfBucket(bucket);
        // max uploads limits the number of uploads returned, max limit is 1000
        if (maxUploads >= 1000 || maxUploads < 0) {
            maxUploads = 1000;
        }

        url = url.newBuilder()
            .addQueryParameter("uploads", "")
            .addQueryParameter("max-uploads", Integer.toString(maxUploads))
            .addQueryParameter("prefix", prefix)
            .addQueryParameter("key-marker", keyMarker)
            .addQueryParameter("upload-id-marker", uploadIDMarker)
            .addQueryParameter("delimiter", delimiter)
            .build();

        Request request = getRequest("GET", url);
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

    /**
     * Drop all active multipart uploads in a given bucket.
     *
     * @param bucket to drop all active multipart uploads in
     *
     * @throws IOException     upon connection failure
     * @throws ClientException upon failure from server
     */
    public void dropAllIncompleteUploads(String bucket) throws IOException, ClientException {
        Iterator<Result<Upload>> uploads = listAllIncompleteUploads(bucket);
        while (uploads.hasNext()) {
            Upload upload = uploads.next().getResult();
            abortMultipartUpload(bucket, upload.getKey(), upload.getUploadID());
        }
    }

    private String newMultipartUpload(String bucket, String key) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfKey(bucket, key);
        url = url.newBuilder()
            .addQueryParameter("uploads", "")
            .build();

        Request request = getRequest("POST", url);
        Response response = this.transport.newCall(request).execute();
        if (response != null) {
            try {
                InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
                parseXml(response, result);
                return result.getUploadId();
            } finally {
                response.body().close();
            }
        }
        throw new IOException();
    }

    private void completeMultipart(String bucket, String key, String uploadID, List<Part> parts) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfKey(bucket, key);
        url = url.newBuilder()
            .addQueryParameter("uploadId", uploadID)
            .build();

        CompleteMultipartUpload completeManifest = new CompleteMultipartUpload();
        completeManifest.setParts(parts);

        byte[] data = completeManifest.toString().getBytes("UTF-8");
        Request request = getRequest("POST", url, data);

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

    private Iterator<Part> listObjectParts(final String bucket, final String key, final String uploadID) {
        return new MinioIterator<Part>() {
            public int marker;
            private boolean isComplete = false;

            @Override
            protected List<Part> populate() throws IOException, ClientException {
                if (!isComplete) {
                    ListPartsResult result;
                    result = listObjectParts(bucket, key, uploadID, marker);
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

    private ListPartsResult listObjectParts(String bucket, String key, String uploadID, int partNumberMarker) throws IOException, ClientException {
        if (partNumberMarker <= 0) {
            throw new InvalidArgumentException();
        }

        HttpUrl url = getHttpUrlOfKey(bucket, key);
        url = url.newBuilder()
            .addQueryParameter("uploadId", uploadID)
            .addQueryParameter("part-number-marker", Integer.toString(partNumberMarker))
            .build();

        Request request = getRequest("GET", url);
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

    private void abortMultipartUpload(String bucket, String key, String uploadID) throws IOException, ClientException {
        if (bucket == null) {
            throw new InvalidBucketNameException();
        }
        if (key == null) {
            throw new InvalidKeyNameException();
        }
        if (uploadID == null) {
            throw new InternalClientException("UploadID cannot be null");
        }
        HttpUrl url = getHttpUrlOfKey(bucket, key);
        url = url.newBuilder()
            .addQueryParameter("uploadId", uploadID)
            .build();

        Request request = getRequest("DELETE", url);
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
     * Drop active multipart uploads, starting from key
     *
     * @param bucket of multipart upload to drop
     * @param key    of multipart upload to drop
     *
     * @throws IOException     upon connection failure
     * @throws ClientException upon failure from server
     */
    public void dropIncompleteUpload(String bucket, String key) throws IOException, ClientException {
        Iterator<Result<Upload>> uploads = listAllIncompleteUploads(bucket, key);
        while (uploads.hasNext()) {
            Upload upload = uploads.next().getResult();
            abortMultipartUpload(bucket, upload.getKey(), upload.getUploadID());
        }
    }

    private int calculatePartSize(long size) {
        int minimumPartSize = PART_SIZE; // 5MB
        int partSize = (int) (size / 9999); // using 10000 may cause part size to become too small, and not fit the entire object in
        return Math.max(minimumPartSize, partSize);
    }

    private void putObject(String bucket, String key, String contentType, byte[] data, byte[] md5sum) throws IOException, ClientException {
        putObject(bucket, key, contentType, data, md5sum, "", 0);
    }

    private String putObject(String bucket, String key, String contentType, byte[] data, byte[] md5sum, String uploadID, int partID) throws IOException, ClientException {
        HttpUrl url = getHttpUrlOfKey(bucket, key);

        if (partID > 0 && uploadID != null && !"".equals(uploadID.trim())) {
            url = url.newBuilder()
                .addQueryParameter("partNumber", Integer.toString(partID))
                .addQueryParameter("uploadId", uploadID)
                .build();
        }

        String base64md5sum = "";
        if (md5sum != null) {
            base64md5sum = DatatypeConverter.printBase64Binary(md5sum);
        }

        Request request = getRequest("PUT", url, data);
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
     * Disable logging http requests
     */
    @SuppressWarnings("unused")
    public void disableLogging() {
        if (this.logger.get() != null) {
            this.logger.get().setLevel(Level.OFF);
        }
    }
}
