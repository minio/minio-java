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

package io.minio.objectstorage.client;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.xml.Xml;
import com.google.api.client.xml.XmlNamespaceDictionary;
import io.minio.objectstorage.client.errors.*;
import io.minio.objectstorage.client.messages.*;
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
import java.util.*;
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
 * <p/>
 * <h2>Service</h2>
 * <ul>
 * <li>Creating a bucket</li>
 * <li>Listing buckets</li>
 * </ul>
 * <p/>
 * <h2>Bucket</h2>
 * <ul>
 * <li> Creating an object, including automatic upload resuming for large objects.</li>
 * <li> Listing objects in a bucket</li>
 * <li> Listing active multipart uploads</li>
 * <li> Dropping all active multipart uploads</li>
 * <li> Setting canned ACLs on buckets</li>
 * </ul>
 * <p/>
 * <h2>Object</h2>Object
 * <ul>
 * <li>Dropping an active multipart upload for a specific object and uploadId</li>
 * <li>Read object metadata</li>
 * <li>Reading an object</li>
 * <li>Reading a range of bytes of an object</li>
 * <li>Deleting an object</li>
 * </ul>
 * <p/>
 * Optionally, users can also provide access/secret keys or a precomputed
 * signing key to the client. If keys are provided, all requests by the
 * client will be signed using AWS Signature Version 4. @see #setKeys(String, String)
 * <p/>
 * For an example of using this library, please see <a href="https://github.com/minio/objectstorage-java/blob/master/src/test/java/io/minio/objectstorage/example/S3Example.java">this example</a>.
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class Client {
    /**
     * Canned acl: public-read-write
     * <p/>
     * Read: public
     * Write: public
     */
    @SuppressWarnings("unused")
    public static final String ACL_PUBLIC_READ_WRITE = "public-read-write";
    /**
     * Canned acl: private
     * <p/>
     * Read: authorized users only
     * Write: authorized users only
     */
    @SuppressWarnings("unused")
    public static final String ACL_PRIVATE = "private";
    /**
     * Canned acl: public-read
     * <p/>
     * Read: public
     * Write: authorized users only
     */
    @SuppressWarnings("unused")
    public static final String ACL_PUBLIC_READ = "public-read";
    /**
     * Canned acl: authenticated-read
     * <p/>
     * Read: Only users with a valid account, all valid users authorized
     * Write: acl authorized users only
     */
    @SuppressWarnings("unused")
    public static final String ACL_AUTHENTICATED_READ = "authenticated-read";
    /**
     * Canned acl: bucket-owner-read
     * <p/>
     * Read: Object owner and bucket owner
     * Write: Object owner only
     */
    @SuppressWarnings("unused")
    public static final String ACL_BUCKET_OWNER_READ = "bucket-owner-read";
    /**
     * Canned acl: bucket-owner-read
     * <p/>
     * Read: Object owner and bucket owner
     * Write: Object owner and bucket owner
     */
    @SuppressWarnings("unused")
    public static final String ACL_BUCKET_OWNER_FULL_CONTROL = "bucket-owner-full-control";

    private static final int PART_SIZE = 5 * 1024 * 1024;
    private final URL url;
    private HttpTransport transport = new NetHttpTransport();
    private String accessKey;
    private String secretKey;
    private final AtomicReference<Logger> logger = new AtomicReference<Logger>();
    private String userAgent;
    private byte[] signingKey;

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
     */
    public static Client getClient(URL url) throws MalformedURLException {
        if (url == null) {
            throw new NullPointerException();
        }
        // Set trailing / in path
        if (url.getPath().length() == 0) {
            String path = url.toString() + "/";
            url = new URL(path);
        }

        // Only a trailing path should be present in the path
        if (url.getPath().length() > 0 && !url.getPath().equals("/")) {
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
     * @see ObjectMetadata
     */
    public ObjectMetadata getObjectMetadata(String bucket, String key) throws IOException, ObjectStorageException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        HttpRequest request = getHttpRequest("HEAD", url);
        request.setThrowExceptionOnExecuteError(false);
        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    HttpHeaders responseHeaders = response.getHeaders();
                    SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                    Date lastModified = formatter.parse(responseHeaders.getLastModified());
                    return new ObjectMetadata(bucket, key, lastModified, responseHeaders.getContentLength(), responseHeaders.getETag());
                } else {
                    parseError(response);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    private void parseError(HttpResponse response) throws IOException, ObjectStorageException {
        if (response.getContent() == null) {
            throw new IOException("Unsuccessful response from server without error: " + response.getStatusCode());
        }
        XmlPullParser parser;
        XmlError xmlError = new XmlError();
        try {
            parser = Xml.createParser();
            InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
            parser.setInput(reader);
            Xml.parseElement(parser, xmlError, new XmlNamespaceDictionary(), null);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            InvalidStateException invalidStateException = new InvalidStateException();
            invalidStateException.initCause(e);
            throw invalidStateException;
        }

        String code = xmlError.getCode();
        ObjectStorageException e;
        if (code.equals("NoSuchBucket")) e = new BucketNotFoundException();
        else if (code.equals("NoSuchKey")) e = new ObjectNotFoundException();
        else if (code.equals("InvalidBucketName")) e = new InvalidObjectNameException();
        else if (code.equals("InvalidObjectName")) e = new InvalidObjectNameException();
        else if (code.equals("AccessDenied")) e = new AccessDeniedException();
        else if (code.equals("BucketAlreadyExists")) e = new BucketExistsException();
        else if (code.equals("ObjectAlreadyExists")) e = new ObjectExistsException();
        else if (code.equals("InternalError")) e = new InternalServerException();
        else if (code.equals("KeyTooLong")) e = new InvalidObjectNameException();
        else if (code.equals("TooManyBuckets")) e = new MaxBucketsReachedException();
        else e = new InvalidStateException();
        e.setXmlError(xmlError);
        throw e;
    }

    private HttpRequest getHttpRequest(String method, GenericUrl url) throws IOException {
        return getHttpRequest(method, url, null);
    }

    private HttpRequest getHttpRequest(String method, GenericUrl url, final byte[] data) throws IOException {
        HttpRequestFactory requestFactory = this.transport.createRequestFactory(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                RequestSigner signer = new RequestSigner(data);
                signer.setAccessKeys(accessKey, secretKey);
                signer.setSigningKey(signingKey);
                request.setInterceptor(signer);
            }
        });
        HttpRequest request = requestFactory.buildRequest(method, url, null);
        request.setSuppressUserAgentSuffix(true);
        if (userAgent != null) {
            request.getHeaders().setUserAgent(userAgent);
        }
        return request;
    }

    private GenericUrl getGenericUrlOfKey(String bucket, String key) {
        GenericUrl url = new GenericUrl(this.url);

        List<String> pathParts = new LinkedList<String>();
        pathParts.add("");
        pathParts.add(bucket);
        pathParts.add(key);

        url.setPathParts(pathParts);
        return url;
    }

    private GenericUrl getGenericUrlOfBucket(String bucket) {
        GenericUrl url = new GenericUrl(this.url);

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
    public InputStream getObject(String bucket, String key) throws IOException, ObjectStorageException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        HttpRequest request = getHttpRequest("GET", url);
        request.setThrowExceptionOnExecuteError(false);
        HttpResponse response = request.execute();
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
    public void deleteObject(String bucket, String key) throws IOException, ObjectStorageException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        HttpRequest request = getHttpRequest("DELETE", url);
        request.setThrowExceptionOnExecuteError(false);
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
     * @param offset Offset from the start of the object.
     * @param length Length of bytes to retrieve.
     * @return an InputStream containing the object. Close the InputStream when done.
     * @throws IOException if the connection does not succeed
     */
    public InputStream getObject(String bucket, String key, long offset, long length) throws IOException, ObjectStorageException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        HttpRequest request = getHttpRequest("GET", url);
        request.setThrowExceptionOnExecuteError(false);
        request.getHeaders().setRange(offset + "-" + offset + length);

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

    public Iterator<Item> listObjectsInBucket(final String bucket, final String prefix) {
        return new ListObjectsIterator<Item>() {
            private String marker = null;
            private boolean isComplete = false;
            @Override
            protected List<Item> populate() {
                if(!isComplete) {
                    try {
                        ListBucketResult listBucketResult = listObjectsInBucket(bucket, marker, prefix, null, 1000);
                        if (listBucketResult.isTruncated()) {
                            marker = listBucketResult.getNextMarker();
                        } else {
                            isComplete = true;
                        }
                        return listBucketResult.getContents();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    } catch (ObjectStorageException e) {
                        e.printStackTrace();
                    }
                }
                return new LinkedList<Item>();
            }
        };
    }

    public Iterator<Item> listObjectsInBucket(final String bucket) {
        return listObjectsInBucket(bucket, null);
    }

    private ListBucketResult listObjectsInBucket(String bucket, String marker, String prefix, String delimiter, Integer maxKeys) throws IOException, XmlPullParserException, ObjectStorageException {
        GenericUrl url = getGenericUrlOfBucket(bucket);
        if (maxKeys != null) {
            url.set("max-keys", maxKeys);
        }
        if (marker != null) {
            url.set("marker", marker);
        }
        if (prefix != null) {
            url.set("prefix", prefix);
        }
        if (delimiter != null) {
            url.set("delimiter", delimiter);
        }

        HttpRequest request = getHttpRequest("GET", url);
        request.setThrowExceptionOnExecuteError(false);

        HttpResponse response = request.execute();

        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    XmlPullParser parser = Xml.createParser();
                    InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
                    parser.setInput(reader);

                    ListBucketResult result = new ListBucketResult();

                    Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
                    return result;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    void setTransport(HttpTransport transport) {
        this.transport = transport;
    }

    /**
     * List buckets owned by the current user.
     *
     * @return a list of buckets owned by the current user
     * @throws IOException            if the connection fails
     * @throws XmlPullParserException
     */
    public ListAllMyBucketsResult listBuckets() throws IOException, XmlPullParserException, ObjectStorageException {
        GenericUrl url = new GenericUrl(this.url);

        HttpRequest request = getHttpRequest("GET", url);
        request.setFollowRedirects(false);

        HttpResponse response = request.execute();
        if (response != null) {
            try {
                if (response.isSuccessStatusCode()) {
                    XmlPullParser parser = Xml.createParser();
                    InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
                    parser.setInput(reader);

                    ListAllMyBucketsResult result = new ListAllMyBucketsResult();

                    Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
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
    public boolean testBucketAccess(String bucket) throws IOException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequest request = getHttpRequest("HEAD", url);

        try {
            HttpResponse response = request.execute();
            try {
                return response.getStatusCode() == 200;
            } finally {
                response.disconnect();
            }
        } catch (HttpResponseException e) {
            return false;
        }
    }

    /**
     * Create a bucket with a given name and ACL
     *
     * @param bucket bucket to create
     * @param acl    canned acl
     * @return true if succeeds // TODO return void, throw exception on failure
     * @throws IOException
     */
    public void makeBucket(String bucket, String acl) throws IOException, ObjectStorageException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequest request = getHttpRequest("PUT", url);
        request.setThrowExceptionOnExecuteError(false);
        if (acl != null) {
            request.getHeaders().set("x-amz-acl", acl);
        } else {
            request.getHeaders().set("x-amz-acl", ACL_PRIVATE);
        }

        HttpResponse response = request.execute();
        if(response != null) {
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
     * Set the bucket's ACL.
     *
     * @param bucket bucket to set ACL on
     * @param acl    canned acl
     * @return // TODO throw exception on failure
     * @throws IOException
     */
    public void setBucketACL(String bucket, String acl) throws IOException, ObjectStorageException {
        if (acl == null) {
            throw new NullPointerException();
        }

        GenericUrl url = getGenericUrlOfBucket(bucket);
        url.set("acl", "");

        HttpRequest request = getHttpRequest("PUT", url);
        request.setThrowExceptionOnExecuteError(false);
        request.getHeaders().set("x-amz-acl", acl);

        HttpResponse response = request.execute();
        if(response != null) {
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
     * <p/>
     * If the object is larger than 5MB, the client will automatically use a multipart session.
     * <p/>
     * If the session fails, the user may attempt to re-upload the object by attempting to create
     * the exact same object again. The client will examine all parts of any current upload session
     * and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail
     * before uploading any more data. Otherwise, it will resume uploading where the session left off.
     * <p/>
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
     */
    public void putObject(String bucket, String key, String contentType, long size, InputStream data) throws IOException, XmlPullParserException, ObjectStorageException {
        boolean isMultipart = false;
        int partSize = 0;
        String uploadID = null;


        if (size > PART_SIZE) {
            // check if multipart exists
            Iterator<Upload> multipartUploads = listActiveMultipartUploads(bucket, key);
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
            Iterator<Part> objectParts = listObjectParts(bucket, key, uploadID);
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
     * @throws IOException
     * @throws XmlPullParserException
     */
    public Iterator<Upload> listActiveMultipartUploads(String bucket) throws IOException, XmlPullParserException, ObjectStorageException {
        return listActiveMultipartUploads(bucket, null);
    }
    public Iterator<Upload> listActiveMultipartUploads(final String bucket, final String prefix) throws IOException, XmlPullParserException, ObjectStorageException {
        return new ListObjectsIterator<Upload>() {
            private boolean isComplete = false;
            private String keyMarker = null;
            private String uploadIdMarker;
            @Override
            protected List<Upload> populate() {
                if(!isComplete) {
                    ListMultipartUploadsResult result = null;
                    try {
                        result = listActiveMultipartUploads(bucket, keyMarker, uploadIdMarker, prefix, null, 1000);
                        if(result.isTruncated()) {
                            keyMarker = result.getNextKeyMarker();
                            uploadIdMarker = result.getNextUploadIDMarker();
                        } else {
                            isComplete = true;
                        }
                        return result.getUploads();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    } catch (ObjectStorageException e) {
                        e.printStackTrace();
                    }
                }
                return new LinkedList<Upload>();
            }
        };
    }

    private ListMultipartUploadsResult listActiveMultipartUploads(String bucket, String keyMarker, String uploadIdMarker, String prefix, String delimiter, int maxKeys) throws IOException, XmlPullParserException, ObjectStorageException {
        GenericUrl url = getGenericUrlOfBucket(bucket);
        url.set("uploads", "");

        if (prefix != null) {
            url.set("prefix", prefix);
        }
        if (prefix != null) {
            url.set("marker", prefix);
        }
        if (prefix != null) {
            url.set("uploadIdMarker", prefix);
        }
        if (prefix != null) {
            url.set("delimiter", prefix);
        }
        if(maxKeys > 0 && maxKeys < 1000) {
            url.set("max-keys", maxKeys);
        }

        HttpRequest request = getHttpRequest("GET", url);
        request.setFollowRedirects(false);
        request.setThrowExceptionOnExecuteError(false);

        HttpResponse response = request.execute();
        if(response != null) {
            try {
                if(response.isSuccessStatusCode()) {
                    XmlPullParser parser = Xml.createParser();
                    InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
                    parser.setInput(reader);

                    ListMultipartUploadsResult result = new ListMultipartUploadsResult();

                    Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
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
     * Abort all active multipart uploads in a given bucket.
     *
     * @param bucket to dorp all active multipart uploads in
     * @throws IOException            on connection failure
     * @throws XmlPullParserException // TODO return better error
     */
    public void abortAllMultipartUploads(String bucket) throws IOException, XmlPullParserException, ObjectStorageException {
        Iterator<Upload> uploads = listActiveMultipartUploads(bucket);
        while (uploads.hasNext()){
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
     * Set user agent of the client
     *
     * @param userAgent Sets the user agent of the request.
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    private String newMultipartUpload(String bucket, String key) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploads", "");

        HttpRequest request = getHttpRequest("POST", url);
        request.setThrowExceptionOnExecuteError(false);

        HttpResponse response = request.execute();
        if(response != null) {
            try {
                XmlPullParser parser = Xml.createParser();
                InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
                parser.setInput(reader);

                InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();

                Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
                return result.getUploadId();
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    private void completeMultipart(String bucket, String key, String uploadID, List<String> etags) throws IOException, ObjectStorageException {
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
        request.setThrowExceptionOnExecuteError(false);
        request.setContent(new ByteArrayContent("application/xml", data));

        HttpResponse response = request.execute();
        if(response != null) {
            try {
                if(response.isSuccessStatusCode()) {
                    return;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
    }

    public Iterator<Part> listObjectParts(final String bucket, final String key, final String uploadID) {
        return new ListObjectsIterator<Part>() {
            public int marker;
            private boolean isComplete = false;
            @Override
            protected List<Part> populate() {
                while(!isComplete) {
                    ListPartsResult result = null;
                    try {
                        result = listObjectParts(bucket, key, uploadID, marker);
                        if(result.isTruncated()) {
                            marker = result.getNextPartNumberMarker();
                        } else {
                            isComplete = true;
                        }
                        return result.getParts();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    } catch (ObjectStorageException e) {
                        e.printStackTrace();
                    }
                }
                return new LinkedList<Part>();
            }
        };
    }

    /**
     * List all parts in an active multipart upload.
     *
     * @param bucket   of object
     * @param key      of object
     * @param uploadID of object
     * @param partMarker
     * @return a list of parts in a given multipart upload
     * @throws IOException            on connection failure
     * @throws XmlPullParserException // TODO better error
     */
    private ListPartsResult listObjectParts(String bucket, String key, String uploadID, int partMarker) throws IOException, XmlPullParserException, ObjectStorageException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploadId", uploadID);

        HttpRequest request = getHttpRequest("GET", url);
        request.setThrowExceptionOnExecuteError(false);

        HttpResponse response = request.execute();
        if(response != null) {
            try {
                if(response.isSuccessStatusCode()) {
                    XmlPullParser parser = Xml.createParser();
                    InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
                    parser.setInput(reader);

                    ListPartsResult result = new ListPartsResult();

                    Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
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
     * Abort an active multipart upload
     *
     * @param bucket   of multipart upload to abort
     * @param key      of multipart upload to abort
     * @param uploadID of multipart upload to abort
     * @throws IOException on connection failure
     *                     // TODO handle failure, given bucket/object/uploadid might not exist
     */
    public void abortMultipartUpload(String bucket, String key, String uploadID) throws IOException, ObjectStorageException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploadId", uploadID);

        HttpRequest request = getHttpRequest("DELETE", url);
        request.setThrowExceptionOnExecuteError(false);
        HttpResponse response = request.execute();
        if(response != null) {
            try {
                if(response.isSuccessStatusCode()) {
                    return;
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    private int computePartSize(long size) {
        int minimumPartSize = PART_SIZE; // 5MB
        int partSize = (int) (size / 9999); // using 10000 may cause part size to become too small, and not fit the entire object in
        return Math.max(minimumPartSize, partSize);
    }

    private void putObject(String bucket, String key, String contentType, byte[] data) throws IOException, ObjectStorageException {
        putObject(bucket, key, contentType, data, "", 0);
    }

    private String putObject(String bucket, String key, String contentType, byte[] data, String uploadId, int partID) throws IOException, ObjectStorageException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        if (partID > 0) {
            url.set("partNumber", partID);
            url.set("uploadId", uploadId);
        }

        byte[] md5sum = calculateMd5sum(data);

        HttpRequest request = getHttpRequest("PUT", url, data);
        request.setThrowExceptionOnExecuteError(false);

        if (md5sum != null) {
            String base64md5sum = DatatypeConverter.printBase64Binary(md5sum);
            request.getHeaders().setContentMD5(base64md5sum);
        }

        ByteArrayContent content = new ByteArrayContent(contentType, data);
        request.setContent(content);
        HttpResponse response = request.execute();
        if(response != null) {
            try {
                if(response.isSuccessStatusCode()) {
                    return response.getHeaders().getETag();
                }
                parseError(response);
            } finally {
                response.disconnect();
            }
        }
        throw new IOException();
    }

    private byte[] calculateMd5sum(byte[] data) {
        byte[] md5sum = null;
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            md5sum = md5Digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5sum;
    }

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
     * Enable logging to a java logger for debug purposes.
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
        if(this.logger.get() != null) {
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
