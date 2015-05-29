/*
 * Minimal Object Storage Library, (C) 2015 Minio, Inc.
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

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.xml.Xml;
import com.google.api.client.xml.XmlNamespaceDictionary;
import io.minio.client.acl.Acl;
import io.minio.client.errors.*;
import io.minio.client.messages.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * <p>
 * This class implements a simple object storage client. This client consists
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
 * Optionally, users can also provide access/secret keys or a precomputed
 * signing key to the client. If keys are provided, all requests by the
 * client will be signed using AWS Signature Version 4. See {@link #setKeys(String, String)} and {@link #setSigningKey(byte[])}
 * </p>
 * For an example of using this library, please see <a href="https://github.com/minio/minio-java/blob/master/src/test/java/io/minio/client/example/Example.java">this example</a>.
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class Client {
    // default multipart upload size is 5MB
    private static final int PART_SIZE = 5 * 1024 * 1024;
    // default transport is an HTTP client.
    private static final HttpTransport defaultTransport = new NetHttpTransport();
    // the current client instance's base URL.
    private final URL url;
    // logger which is set only on enableLogger. Atomic reference is used to prevent multiple loggers from being instantiated
    private final AtomicReference<Logger> logger = new AtomicReference<Logger>();
    // current transporter which can be used to mock
    private HttpTransport transport = defaultTransport;
    // access key to sign all requests with
    private String accessKey;
    // Secret key to sign all requests with
    private String secretKey;
    // user agent to tag all requests with
    private String userAgent = "minio-java/0.0.1" + " (" + System.getProperty("os.name") + ", " + System.getProperty("os.arch") + ") ";
    // signing key to sign all requests with. Is not used if access and secret key are set
    private byte[] signingKey;

    // Don't allow users to instantiate clients themselves, since it is bad form to throw exceptions in constructors.
    // Use Client.getClient instead
    private Client(URL url) {
        this.url = url;
    }


    /**
     * Create a new client given a url
     *
     * @param url must be the full url to the object storage server, excluding both bucket or object paths.
     *            For example: http://play.minio.io
     *            Valid:
     *            * https://s3-us-west-2.amazonaws.com
     *            * http://play.minio.io
     *            Invalid:
     *            * https://s3-us-west-2.amazonaws.com/example/
     *            * https://s3-us-west-2.amazonaws.com/example/object
     * @return an object storage client backed by an S3 compatible server.
     * @throws MalformedURLException
     * @see #getClient(String)
     */
    public static Client getClient(URL url) throws MalformedURLException {
        // URL should not be null
        if (url == null) {
            throw new NullPointerException();
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
        return new Client(url);
    }

    /**
     * @return an object storage client backed by an S3 compatible server.
     * @throws MalformedURLException
     * @see #getClient(URL url)
     */
    public static Client getClient(String url) throws MalformedURLException {
        if (url == null) {
            throw new NullPointerException();
        }
        return getClient(new URL(url));
    }

    /**
     * Returns the URL this client uses
     *
     * @return the URL backed by this.
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Returns metadata about the object
     *
     * @param bucket object's bucket
     * @param key    object's key
     * @return Populated object metadata
     * @throws IOException
     * @see ObjectStat
     */
    public ObjectStat statObject(String bucket, String key) throws IOException, ClientException {
        if (bucket == null) {
            throw new InvalidBucketNameException();
        }
        if (key == null) {
            throw new InvalidKeyNameException();
        }
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        HttpRequest request = getHttpRequest("HEAD", url);
        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    // all info we need is in the headers
                    HttpHeaders responseHeaders = response.getHeaders();
                    SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                    Date lastModified = formatter.parse(responseHeaders.getLastModified());
                    return new ObjectStat(bucket, key, lastModified, responseHeaders.getContentLength(), responseHeaders.getETag());
                } else {
                    parseError(response);
                }
            } catch (ParseException e) {
                // if a parse exception occurred, this indicates an error in the client.
                InternalClientException internalClientException = new InternalClientException();
                internalClientException.initCause(e);
                throw internalClientException;
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    private void parseError(HttpResponse response) throws IOException, ClientException {
        // if response is null, throw an IOException and finish here
        if (response == null) {
            throw new IOException("No response was returned");
        }

        // if response.getContent is null, throw an IOException with status code in string
        if (response.getContent() == null) {
            throw new IOException("Unsuccessful response from server without error: " + response.getStatusCode());
        }

        // Populate an XmlError, will throw an ClientException if unparseable. We should just pass it up.
        XmlError xmlError = new XmlError();
        parseXml(response, xmlError);

        // Return the correct exception based upon the error code.
        // Minor note, flipped .equals() protects against null pointer exceptions
        String code = xmlError.getCode();
        ClientException e;
        if ("NoSuchBucket".equals(code)) e = new BucketNotFoundException();
        else if ("NoSuchKey".equals(code)) e = new ObjectNotFoundException();
        else if ("InvalidBucketName".equals(code)) e = new InvalidKeyNameException();
        else if ("InvalidObjectName".equals(code)) e = new InvalidKeyNameException();
        else if ("AccessDenied".equals(code)) e = new AccessDeniedException();
        else if ("BucketAlreadyExists".equals(code)) e = new BucketExistsException();
        else if ("ObjectAlreadyExists".equals(code)) e = new ObjectExistsException();
        else if ("InternalError".equals(code)) e = new InternalServerException();
        else if ("KeyTooLong".equals(code)) e = new InvalidKeyNameException();
        else if ("TooManyBuckets".equals(code)) e = new MaxBucketsReachedException();
        else if ("PermanentRedirect".equals(code)) e = new RedirectionException();
        else e = new InternalClientException();
        e.setXmlError(xmlError);
        throw e;
    }

    private void parseXml(HttpResponse response, Object objectToPopulate) throws IOException, InternalClientException {
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
            InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
            parser.setInput(reader);
            // create a dictionary and populate based on object type
            XmlNamespaceDictionary dictionary = new XmlNamespaceDictionary();
            if (objectToPopulate instanceof XmlError) {
                // Errors have no namespace, so we set a default empty alias and namespace
                dictionary.set("", "");
            } else {
                // Setting an empty alias causes a failure when the namespace exists, so we don't set it when
                // we are not using XmlError. Set the real namespace instead
                dictionary.set("s3", "http://s3.amazonaws.com/doc/2006-03-01/");
            }
            // parse and return
            Xml.parseElement(parser, objectToPopulate, dictionary, null);
        } catch (XmlPullParserException e) {
            InternalClientException internalClientException = new InternalClientException();
            internalClientException.initCause(e);
            throw internalClientException;
        }
    }

    private HttpRequest getHttpRequest(String method, GenericUrl url) throws IOException, InternalClientException {
        return getHttpRequest(method, url, null);
    }

    private HttpRequest getHttpRequest(String method, GenericUrl url, final byte[] data) throws IOException, InternalClientException {
        if (method == null || method.trim().equals("")) {
            throw new InternalClientException("Method should be populated");
        }
        if (url == null) {
            throw new InternalClientException("URL should be populated");
        }
        // create a new request factory that will sign the code on execute()
        HttpRequestFactory requestFactory = this.transport.createRequestFactory(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                // wire up secrets for code signing
                RequestSigner signer = new RequestSigner(data);
                signer.setAccessKeys(accessKey, secretKey);
                signer.setSigningKey(signingKey);
                request.setInterceptor(signer);
            }
        });
        HttpRequest request = requestFactory.buildRequest(method, url, null);
        // Workaround for where user agent for google is appended after signing interceptor is called.
        request.setSuppressUserAgentSuffix(true);
        // Disable throwing exceptions on execute()
        request.setThrowExceptionOnExecuteError(false);
        // set our own user agent
        request.getHeaders().setUserAgent(this.userAgent);
        return request;
    }

    private GenericUrl getGenericUrlOfKey(String bucket, String key) throws InvalidBucketNameException, InvalidKeyNameException {
        if (bucket == null) {
            throw new InvalidBucketNameException();
        }
        if (key == null) {
            throw new InvalidKeyNameException();
        }
        GenericUrl url = new GenericUrl(this.url);

        List<String> pathParts = new LinkedList<String>();
        // pathparts adds slashes between each part
        // e.g. foo, bar => foo/bar
        // we add a "" in the beginning to force it to add a / at the beginning or the url will not be differentiated from the port
        // e.g. "", bucket, key => /bucket/key
        pathParts.add("");
        pathParts.add(bucket);
        pathParts.add(key);

        // add the path to the url and return
        url.setPathParts(pathParts);
        return url;
    }

    private GenericUrl getGenericUrlOfBucket(String bucket) throws InvalidBucketNameException {
        if (bucket == null) {
            throw new InvalidBucketNameException();
        }
        GenericUrl url = new GenericUrl(this.url);

        // pathparts adds slashes between each part
        // e.g. foo, bar => foo/bar
        // we add a "" in the beginning to force it to add a / at the beginning or the url will not be differentiated from the port
        // e.g. "", bucket => /bucket
        List<String> pathParts = new LinkedList<String>();
        pathParts.add("");
        pathParts.add(bucket);

        url.setPathParts(pathParts);
        return url;
    }

    /**
     * Returns an InputStream containing the object. The InputStream must be closed when
     * complete or the connection will remain open.
     *
     * @param bucket object's bucket
     * @param key    object's key
     * @return an InputStream containing the object. Close the InputStream when done.
     * @throws IOException
     */
    public InputStream getObject(String bucket, String key) throws IOException, ClientException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        HttpRequest request = getHttpRequest("GET", url);
        HttpResponse response = request.execute();
        // we close the response only on failure or the user will be unable to retrieve the object
        // it is the user's responsibility to close the input stream
        if (response != null) {
            if (!response.isSuccessStatusCode()) {
                try {
                    parseError(response);
                } finally {
                    response.disconnect();
                }
            }
            return response.getContent();
        }
        // TODO create a better exception
        throw new IOException();
    }

    /**
     * Delete an object.
     *
     * @param bucket object's bucket
     * @param key    object's key
     * @throws IOException if the connection fails
     */
    public void deleteObject(String bucket, String key) throws IOException, ClientException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        HttpRequest request = getHttpRequest("DELETE", url);
        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    return;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }


    /**
     * Returns an InputStream containing a subset of the object. The InputStream must be
     * closed or the connection will remain open.
     *
     * @param bucket object's bucket
     * @param key    object's key
     * @param offsetStart Offset from the start of the object.
     * @param length Length of bytes to retrieve.
     * @return an InputStream containing the object. Close the InputStream when done.
     * @throws IOException if the connection does not succeed
     */
    public InputStream getObject(String bucket, String key, long offsetStart, long length) throws IOException, ClientException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        HttpRequest request = getHttpRequest("GET", url);
        long offsetEnd = offsetStart+length;
        request.getHeaders().setRange(offsetStart + "-" + offsetEnd);

        // we close the response only on failure or the user will be unable to retrieve the object
        // it is the user's responsibility to close the input stream
        HttpResponse response = request.execute();
        if (response != null) {
            if (response.isSuccessStatusCode()) {
                return response.getContent();
            }
            try {
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    /**
     *
     * @param bucket
     * @param prefix
     * @return
     */
    public ExceptionIterator<Item> listObjectsInBucket(final String bucket, final String prefix) {
        // list all objects recursively
        return listObjectsInBucket(bucket, prefix, true);
    }

    /**
     *
     * @param bucket
     * @param prefix
     * @param recursive
     * @return
     */
    public ExceptionIterator<Item> listObjectsInBucket(final String bucket, final String prefix, final boolean recursive) {
        return new ExceptionIterator<Item>() {
            private String marker = null;
            private boolean isComplete = false;

            @Override
            protected List<Item> populate() throws ClientException, IOException {
                if (!isComplete) {
                    try {
                        String delimiter = null;
                        // set delimiter  to '/' if not recursive to emulate directories
                        if(!recursive) {
                            delimiter = "/";
                        }
                        ListBucketResult listBucketResult = listObjectsInBucket(bucket, marker, prefix, delimiter, 1000);
                        if (listBucketResult.isTruncated()) {
                            marker = listBucketResult.getNextMarker();
                        } else {
                            isComplete = true;
                        }
                        return listBucketResult.getContents();
                    } catch (XmlPullParserException e) {
                        InternalClientException xmlParsingError = new InternalClientException();
                        xmlParsingError.initCause(e);
                        throw xmlParsingError;
                    }
                }
                return new LinkedList<Item>();
            }
        };
    }

    /**
     *
     * @param bucket
     * @return
     */
    public ExceptionIterator<Item> listObjectsInBucket(final String bucket) {
        return listObjectsInBucket(bucket, null);
    }

    /**
     *
     * @param bucket
     * @param marker
     * @param prefix
     * @param delimiter
     * @param maxKeys
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     * @throws ClientException
     */
    private ListBucketResult listObjectsInBucket(String bucket, String marker, String prefix, String delimiter, int maxKeys) throws IOException, XmlPullParserException, ClientException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        // max keys limits the number of keys returned, max limit is 1000
        if (maxKeys > 0 && maxKeys <= 1000) {
            url.set("max-keys", maxKeys);
        } else {
            url.set("max-keys", 1000);
        }

        // marker is similar to a book mark, returns objects in alphabetical order starting from the marker
        if (marker != null) {
            url.set("marker", marker);
        }

        // prefix filters results, result must contain the given prefix
        if (prefix != null) {
            url.set("prefix", prefix);
        }

        // delimiter will limit results to unique entries with keys truncated at the first instance of the delimiter
        // useful for emulating file system directories
        if (delimiter != null) {
            url.set("delimiter", delimiter);
        }

        HttpRequest request = getHttpRequest("GET", url);

        HttpResponse response = request.execute();

        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    ListBucketResult result = new ListBucketResult();
                    parseXml(response, result);
                    return result;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    /**
     * Set test transports for mocking the http request and response
     * @param transport
     */
    void setTransport(HttpTransport transport) {
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
     * @throws IOException            if the connection fails
     * @throws XmlPullParserException
     */
    public ListAllMyBucketsResult listBuckets() throws IOException, XmlPullParserException, ClientException {
        GenericUrl url = new GenericUrl(this.url);

        HttpRequest request = getHttpRequest("GET", url);
        request.setFollowRedirects(false);

        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    ListAllMyBucketsResult result = new ListAllMyBucketsResult();
                    parseXml(response, result);
                    return result;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }


    /**
     * Test whether a bucket exists and the user has at least read access
     *
     * @param bucket bucket to test
     * @return true if the bucket exists and the user has at least read access
     * @throws IOException
     */
    public boolean bucketExists(String bucket) throws IOException, ClientException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequest request = getHttpRequest("HEAD", url);
        HttpResponse response = request.execute();
        return response != null && response.getStatusCode() == 200;
    }

    /**
     *
     * @param bucket
     * @throws IOException
     * @throws ClientException
     */
    public void makeBucket(String bucket) throws IOException, ClientException {
        this.makeBucket(bucket, Acl.PRIVATE);
    }

    /**
     * Create a bucket with a given name and ACL
     *
     * @param bucket bucket to create
     * @param acl    canned acl
     * @throws IOException
     */
    public void makeBucket(String bucket, Acl acl) throws IOException, ClientException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequest request = getHttpRequest("PUT", url);
        if (acl == null) {
            acl = Acl.PRIVATE;
        }
        request.getHeaders().set("x-amz-acl", acl.toString());

        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    return;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
    }

    /**
     * Delete a bucket with a given name
     *
     * @param bucket bucket to create
     * @throws IOException
     * @throws ClientException
     */
    public void deleteBucket(String bucket) throws IOException, ClientException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequest request = getHttpRequest("DELETE", url);
        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    return;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
	throw new IOException();
    }

    /**
     * Set the bucket's ACL.
     *
     * @param bucket bucket to set ACL on
     * @param acl    canned acl
     * @throws IOException
     */
    public void setBucketACL(String bucket, Acl acl) throws IOException, ClientException {
        if (acl == null) {
            throw new InvalidAclNameException();
        }

        GenericUrl url = getGenericUrlOfBucket(bucket);
        HttpRequest request = getHttpRequest("PUT", url);
        request.getHeaders().set("x-amz-acl", acl.toString());

        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    return;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    /**
     * Create an object.
     * <p>
     * If the object is larger than 5MB, the client will automatically use a multipart session.
     * </p>
     * If the session fails, the user may attempt to re-upload the object by attempting to create
     * the exact same object again. The client will examine all parts of any current upload session
     * and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail
     * before uploading any more data. Otherwise, it will resume uploading where the session left off.
     * <p>
     * If the multipart session fails, the user is responsible for resuming or dropping the session.
     *
     * @param bucket      Bucket to use
     * @param key         Key of object
     * @param contentType Content type to set this object to
     * @param size        Size of all the data that will be uploaded.
     * @param data        Data to upload
     * @throws IOException            on failure
     * @throws XmlPullParserException on unexpected xml // TODO don't fail like this, wrap as our own error
     * @see #listActiveMultipartUploads(String)
     * @see #abortMultipartUpload(String, String, String)
     * </p>
     */
    public void putObject(String bucket, String key, String contentType, long size, InputStream data) throws IOException, XmlPullParserException, ClientException {
        boolean isMultipart = false;
        int partSize = 0;
        String uploadID = null;


        if (size > PART_SIZE) {
            // check if multipart exists
            ExceptionIterator<Upload> multipartUploads = listActiveMultipartUploads(bucket, key);
            while (multipartUploads.hasNext()) {
                Upload upload = multipartUploads.next();
                if (upload.getKey().equals(key)) {
                    uploadID = upload.getUploadID();
                }
            }

            isMultipart = true;
            partSize = computePartSize(size);
            if (uploadID == null) {
                uploadID = newMultipartUpload(bucket, key);
            }
        }


        if (!isMultipart) {
            byte[] dataArray = readData((int) size, data);
            putObject(bucket, key, contentType, dataArray);
        } else {
            long objectLength = 0;
            List<String> parts = new LinkedList<String>();
            int part = 1;
            ExceptionIterator<Part> objectParts = listObjectParts(bucket, key, uploadID);
            while (objectParts.hasNext()) {
                Part curPart = objectParts.next();
                long curSize = curPart.getSize();
                String curETag = curPart.geteTag();
                String curNormalizedETag = curETag.replaceAll("\"", "").toLowerCase().trim();
                byte[] curData = readData((int) curSize, data);
                String generatedEtag = DatatypeConverter.printHexBinary(calculateMd5sum(curData)).toLowerCase().trim();
                if (!curNormalizedETag.equals(generatedEtag) || curPart.getPartNumber() != part) {
                    throw new IOException("Partial upload does not match");
                }
                parts.add(curETag);
                objectLength += curSize;
                part++;
            }
            while (true) {
                byte[] dataArray = readData(partSize, data);
                if (dataArray.length == 0) {
                    break;
                }
                parts.add(putObject(bucket, key, contentType, dataArray, uploadID, part));
                part++;
                objectLength += dataArray.length;
            }
            if (objectLength != size) {
                throw new IOException("Data size mismatched");
            }
            completeMultipart(bucket, key, uploadID, parts);
        }
    }

    /**
     * Lists all active multipart uploads in a bucket
     *
     * @param bucket bucket to list active multipart uploads
     * @return list of active multipart uploads
     */
    public ExceptionIterator<Upload> listActiveMultipartUploads(String bucket) {
        return listActiveMultipartUploads(bucket, null);
    }

    /**
     *
     * @param bucket
     * @param prefix
     * @return
     */
    public ExceptionIterator<Upload> listActiveMultipartUploads(final String bucket, final String prefix) {
        return new ExceptionIterator<Upload>() {
            private boolean isComplete = false;
            private String keyMarker = null;
            private String uploadIdMarker;

            @Override
            protected List<Upload> populate() throws ClientException, IOException {
                if (!isComplete) {
                    ListMultipartUploadsResult result;
                    try {
                        result = listActiveMultipartUploads(bucket, keyMarker, uploadIdMarker, prefix, null, 1000);
                        if (result.isTruncated()) {
                            keyMarker = result.getNextKeyMarker();
                            uploadIdMarker = result.getNextUploadIDMarker();
                        } else {
                            isComplete = true;
                        }
                        return result.getUploads();
                    } catch (XmlPullParserException e) {
                        InternalClientException xmlError = new InternalClientException();
                        xmlError.initCause(e);
                        throw xmlError;
                    }
                }
                return new LinkedList<Upload>();
            }
        };
    }

    /**
     *
     * @param bucket
     * @param keyMarker
     * @param uploadIDMarker
     * @param prefix
     * @param delimiter
     * @param maxUploads
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     * @throws ClientException
     */
    private ListMultipartUploadsResult listActiveMultipartUploads(String bucket, String keyMarker, String uploadIDMarker, String prefix, String delimiter, int maxUploads) throws IOException, XmlPullParserException, ClientException {
        GenericUrl url = getGenericUrlOfBucket(bucket);
        url.set("uploads", "");

        // max uploads limits the number of uploads returned, max limit is 1000
        if (maxUploads > 0 && maxUploads <= 1000) {
            url.set("max-uploads", maxUploads);
        } else {
            url.set("max-uploads", 1000);
        }
        if (prefix != null) {
            url.set("prefix", prefix);
        }
        if (keyMarker != null) {
            url.set("key-marker", keyMarker);
        }
        if (uploadIDMarker != null) {
            url.set("upload-id-marker", uploadIDMarker);
        }
        if (delimiter != null) {
            url.set("delimiter", delimiter);
        }

        HttpRequest request = getHttpRequest("GET", url);
        request.setFollowRedirects(false);

        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    ListMultipartUploadsResult result = new ListMultipartUploadsResult();
                    parseXml(response, result);
                    return result;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    /**
     * Drop all active multipart uploads in a given bucket.
     *
     * @param bucket to drop all active multipart uploads in
     * @throws IOException on connection failure
     * @throws ClientException
     */
    public void dropAllMultipartUploads(String bucket) throws IOException, ClientException {
        ExceptionIterator<Upload> uploads = listActiveMultipartUploads(bucket);
        while (uploads.hasNext()) {
            Upload upload = uploads.next();
            abortMultipartUpload(bucket, upload.getKey(), upload.getUploadID());
        }
    }

    /**
     * Set access keys for authenticated access
     *
     * @param accessKey access key to sign requests
     * @param secretKey secret key to sign requests
     */
    public void setKeys(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * Add additional user agent string of the app - http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
     *
     * @param name     name of your application
     * @param version  version of your application
     * @param comments optional list of comments
     */
    public void addUserAgent(String name, String version, String... comments) {
        if (name != null && version != null) {
            String newUserAgent = name.trim() + "/" + version.trim() + " (";
            StringBuilder sb = new StringBuilder();
            for (String comment : comments) {
                if (comment != null) {
                    sb.append(comment.trim()).append(", ");
                }
            }
            this.userAgent = this.userAgent + newUserAgent + sb.toString() + ") ";
        }
    }

    /**
     *
     * @param bucket
     * @param key
     * @return
     * @throws IOException
     * @throws ClientException
     */
    private String newMultipartUpload(String bucket, String key) throws IOException, ClientException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploads", "");

        HttpRequest request = getHttpRequest("POST", url);

        HttpResponse response = request.execute();
        if (response != null) {
            try {
                InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
                parseXml(response, result);
                return result.getUploadId();
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    /**
     *
     * @param bucket
     * @param key
     * @param uploadID
     * @param etags
     * @throws IOException
     * @throws ClientException
     */
    private void completeMultipart(String bucket, String key, String uploadID, List<String> etags) throws IOException, ClientException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploadId", uploadID);

        List<Part> parts = new LinkedList<Part>();
        for (int i = 0; i < etags.size(); i++) {
            Part part = new Part();
            part.setPartNumber(i + 1);
            part.seteTag(etags.get(i));
            parts.add(part);
        }

        CompleteMultipartUpload completeManifest = new CompleteMultipartUpload();
        completeManifest.setParts(parts);

        byte[] data = completeManifest.toString().getBytes("UTF-8");

        HttpRequest request = getHttpRequest("POST", url, data);
        request.setContent(new ByteArrayContent("application/xml", data));

        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    return;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
    }

    /**
     *
     * @param bucket
     * @param key
     * @param uploadID
     * @return
     */
    public ExceptionIterator<Part> listObjectParts(final String bucket, final String key, final String uploadID) {
        return new ExceptionIterator<Part>() {
            public int marker;
            private boolean isComplete = false;

            @Override
            protected List<Part> populate() throws IOException, ClientException {
                if (!isComplete) {
                    ListPartsResult result;
                    try {
                        result = listObjectParts(bucket, key, uploadID, marker);
                        if (result.isTruncated()) {
                            marker = result.getNextPartNumberMarker();
                        } else {
                            isComplete = true;
                        }
                        return result.getParts();
                    } catch (XmlPullParserException e) {
                        InternalClientException internalClientException = new InternalClientException();
                        internalClientException.initCause(e);
                        throw internalClientException;
                    }
                }
                return new LinkedList<Part>();
            }
        };
    }

    /**
     *
     * @param bucket
     * @param key
     * @param uploadID
     * @param partNumberMarker
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     * @throws ClientException
     */
    private ListPartsResult listObjectParts(String bucket, String key, String uploadID, int partNumberMarker) throws IOException, XmlPullParserException, ClientException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploadId", uploadID);

        if (partNumberMarker > 0) {
            url.set("part-number-marker", partNumberMarker);
        }

        HttpRequest request = getHttpRequest("GET", url);

        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    ListPartsResult result = new ListPartsResult();
                    parseXml(response, result);
                    return result;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    /** Abort an active multipart upload
     *
     * @param bucket   of multipart upload to abort
     * @param key      of multipart upload to abort
     * @param uploadID of multipart upload to abort
     */
    private void abortMultipartUpload(String bucket, String key, String uploadID) throws IOException, ClientException {
	if (bucket == null) {
	    throw new InternalClientException("Bucket cannot be null");
	}
	if (key == null) {
	    throw new InternalClientException("Key cannot be null");
	}
	if (uploadID == null) {
	    throw new InternalClientException("UploadID cannot be null");
	}
	GenericUrl url = getGenericUrlOfKey(bucket, key);
	url.set("uploadId", uploadID);

	HttpRequest request = getHttpRequest("DELETE", url);
	HttpResponse response = request.execute();
	if (response != null) {
	    try {
		if (response.isSuccessStatusCode()) {
		    return;
		}
		parseError(response);
	    } finally {
		response.disconnect();
	    }
	}
	throw new IOException();
    }

    /**
     * Drop active multipart uploads, starting from key
     *
     * @param bucket   of multipart upload to drop
     * @param key      of multipart upload to drop
     * @throws IOException on connection failure
     * @throws ClientException
     */
    public void dropMultipartUploads(String bucket, String key) throws IOException, ClientException {
        ExceptionIterator<Upload> uploads = listActiveMultipartUploads(bucket, key);
        while (uploads.hasNext()) {
            Upload upload = uploads.next();
            abortMultipartUpload(bucket, upload.getKey(), upload.getUploadID());
        }
    }

    /**
     *
     * @param size of total object
     * @return multipart size
     */
    private int computePartSize(long size) {
        int minimumPartSize = PART_SIZE; // 5MB
        int partSize = (int) (size / 9999); // using 10000 may cause part size to become too small, and not fit the entire object in
        return Math.max(minimumPartSize, partSize);
    }

    /**
     *
     * @param bucket to put object
     * @param key
     * @param contentType
     * @param data
     * @throws IOException
     * @throws ClientException
     */
    private void putObject(String bucket, String key, String contentType, byte[] data) throws IOException, ClientException {
        putObject(bucket, key, contentType, data, "", 0);
    }

    /**
     *
     * @param bucket to put object to
     * @param key to put object to
     * @param contentType of data
     * @param data to upload
     * @param uploadId of multipart upload, set to null if not a multipart upload
     * @param partID of multipart upload, set to 0 if not a multipart upload.
     * @return string representing the returned etag
     * @throws IOException
     * @throws ClientException
     */
    private String putObject(String bucket, String key, String contentType, byte[] data, String uploadId, int partID) throws IOException, ClientException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        if (partID > 0 && uploadId != null && "".equals(uploadId.trim())) {
            url.set("partNumber", partID);
            url.set("uploadId", uploadId);
        }

        byte[] md5sum = calculateMd5sum(data);

        HttpRequest request = getHttpRequest("PUT", url, data);

        if (md5sum != null) {
            String base64md5sum = DatatypeConverter.printBase64Binary(md5sum);
            request.getHeaders().setContentMD5(base64md5sum);
        }

        ByteArrayContent content = new ByteArrayContent(contentType, data);
        request.setContent(content);
        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    return response.getHeaders().getETag();
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    /**
     *
     * @param data to calculate sum for
     * @return md5sum
     */
    private byte[] calculateMd5sum(byte[] data) {
        byte[] md5sum;
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            md5sum = md5Digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            // we should never see this, unless the underlying JVM is broken.
            // Throw a runtime exception if we run into this, the environment
            // is not sane
            System.err.println("MD5 message digest is not found, the current JVM is likely broken.");
            throw new RuntimeException(e);
        }
        return md5sum;
    }

    /**
     *
     * @param size of data to read
     * @param data to read from
     * @return byte array of read data
     * @throws IOException
     */
    private byte[] readData(int size, InputStream data) throws IOException {
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

        return fullData;
    }

    /**
     * Enable logging to a java logger for debug purposes. This will enable logging for all http requests.
     */
    @SuppressWarnings("unused")
    public void enableLogging() {
        if (this.logger.get() == null) {
            this.logger.set(Logger.getLogger(HttpTransport.class.getName()));
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

    /**
     * Set signing key to sign requests with
     *
     * @param signingKey to use in this connection
     */
    public void setSigningKey(byte[] signingKey) {
        if (signingKey != null) {
            this.signingKey = signingKey.clone();
        }
    }
}
