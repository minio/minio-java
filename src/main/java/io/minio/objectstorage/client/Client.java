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
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * <p>
 * This class implements a simple object storage client. This client consists
 * of a useful subset of S3 compatible functionality.
 * </p>
 *
 *   <h2>Service</h2>
 *   <ul>
 *      <li>Creating a bucket</li>
 *      <li>Listing buckets</li>
 *   </ul>
 *
 *   <h2>Bucket</h2>
 *   <ul>
 *      <li> Creating an object, including automatic upload resuming for large objects.</li>
 *      <li> Listing objects in a bucket</li>
 *      <li> Listing active multipart uploads</li>
 *      <li> Dropping all active multipart uploads</li>
 *      <li> Setting canned ACLs on buckets</li>
 *   </ul>
 *
 *   <h2>Object</h2>Object
 *   <ul>
 *      <li>Dropping an active multipart upload for a specific object and uploadId</li>
 *      <li>Read object metadata</li>
 *      <li>Reading an object</li>
 *      <li>Reading a range of bytes of an object</li>
 *      <li>Deleting an object</li>
 *   </ul>
 *
 *   Optionally, users can also provide access/secret keys or a precomputed
 *   signing key to the client. If keys are provided, all requests by the
 *   client will be signed using AWS Signature Version 4. @see #setKeys(String, String)
 *
 *   For an example of using this library, please see <a href="https://github.com/minio/objectstorage-java/blob/master/src/test/java/io/minio/objectstorage/example/S3Example.java">this example</a>.
 */
public class Client {
    /**
     * Canned acl: public-read-write
     *
     * Read: public
     * Write: public
     */
    public static final String ACL_PUBLIC_READ_WRITE = "public-read-write";
    /**
     * Canned acl: private
     *
     * Read: authorized users only
     * Write: authorized users only
     */
    public static final String ACL_PRIVATE = "private";
    /**
     * Canned acl: public-read
     *
     * Read: public
     * Write: authorized users only
     */
    public static final String ACL_PUBLIC_READ = "public-read";
    /**
     * Canned acl: authenticated-read
     *
     * Read: Only users with a valid account, all valid users authorized
     * Write: acl authorized users only
     */
    public static final String ACL_AUTHENTICATED_READ = "authenticated-read";
    /**
     * Canned acl: bucket-owner-read
     *
     * Read: Object owner and bucket owner
     * Write: Object owner only
     */
    public static final String ACL_BUCKET_OWNER_READ = "bucket-owner-read";
    /**
     * Canned acl: bucket-owner-read
     *
     * Read: Object owner and bucket owner
     * Write: Object owner and bucket owner
     */
    public static final String ACL_BUCKET_OWNER_FULL_CONTROL = "bucket-owner-full-control";

    private static final int PART_SIZE = 5 * 1024 * 1024;
    private final URL url;
    private HttpTransport transport = new NetHttpTransport();
    private String accessKey;
    private String secretKey;
    private Logger logger;
    private String userAgent;

    private Client(URL url) {
        this.url = url;
    }


    /**
     * Create a new client given a url
     *
     * @param url must be the full url to the object storage server, exluding both bucket or object paths.
     *            For example: http://play.minio.io
     *            Valid:
     *              * https://s3-us-west-2.amazonaws.com
     *              * http://play.minio.io
     *            Invalid:
     *              * https://s3-us-west-2.amazonaws.com/example/
     *              * https://s3-us-west-2.amazonaws.com/example/object
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
     * @see #getClient(URL url)
     * @return an object storage client backed by an S3 compatible server.
     * @throws MalformedURLException
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
     * @param bucket
     * @param key
     * @return Populated object metadata
     * @throws IOException
     * @see ObjectMetadata
     */
    public ObjectMetadata getObjectMetadata(String bucket, String key) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        HttpRequest request = getHttpRequest("HEAD", url);

        HttpResponse response = request.execute();
        try {
            HttpHeaders responseHeaders = response.getHeaders();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            Date lastModified = formatter.parse(responseHeaders.getLastModified());
            return new ObjectMetadata(bucket, key, lastModified, responseHeaders.getContentLength(), responseHeaders.getETag());
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            response.disconnect();
        }
        throw new IOException();
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
                request.setInterceptor(signer);
            }
        });
        HttpRequest request = requestFactory.buildRequest(method, url, null);
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
     * @param bucket
     * @param key
     * @return an InputStream containing the object. Close the InputStream when done.
     * @throws IOException
     */
    public InputStream getObject(String bucket, String key) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        HttpRequest request = getHttpRequest("GET", url);
        request = request.setRequestMethod("GET");

        HttpResponse response = request.execute();
        return response.getContent();
    }

    /**
     * Returns an InputStream containing a subset of the object. The InputStream must be
     * closed or the connection will remain open.
     *
     * @param bucket
     * @param key
     * @param offset Offset from the start of the object.
     * @param length Length of bytes to retrieve.
     * @return an InputStream containing the object. Close the InputStream when done.
     * @throws IOException
     */
    public InputStream getObject(String bucket, String key, long offset, long length) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        HttpRequest request = getHttpRequest("GET", url);
        request.getHeaders().setRange(offset + "-" + offset + length);

        HttpResponse response = request.execute();
        return response.getContent();
    }

    /**
     * List objects in a given bucket
     *
     * TODO: explain paramters and give examples
     * @param bucket
     * @param marker
     * @param prefix
     * @param delimiter
     * @param maxkeys
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    public ListBucketResult listObjectsInBucket(String bucket, String marker, String prefix, String delimiter, Integer maxkeys) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfBucket(bucket);
        if (maxkeys != null) {
            url.set("max-keys", maxkeys);
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

        HttpResponse response = request.execute();

        try {
            XmlPullParser parser = Xml.createParser();
            InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
            parser.setInput(reader);

            ListBucketResult result = new ListBucketResult();

            Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
            return result;
        } finally {
            response.disconnect();
        }
    }

    void setTransport(HttpTransport transport) {
        this.transport = transport;
    }

    /**
     * List buckets owned by the current user
     *
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    public ListAllMyBucketsResult listBuckets() throws IOException, XmlPullParserException {
        GenericUrl url = new GenericUrl(this.url);

        HttpRequest request = getHttpRequest("GET", url);
        request.setFollowRedirects(false);

        HttpResponse response = request.execute();
        try {
            XmlPullParser parser = Xml.createParser();
            InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
            parser.setInput(reader);

            ListAllMyBucketsResult result = new ListAllMyBucketsResult();

            Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
            return result;
        } finally {
            response.disconnect();
        }
    }


    /**
     * Test whether a bucket exists and the user has at least read access
     *
     * @param bucket
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
     * @param bucket
     * @param acl
     * @return
     * @throws IOException
     */
    public boolean makeBucket(String bucket, String acl) throws IOException {
        GenericUrl url = getGenericUrlOfBucket(bucket);

        HttpRequest request = getHttpRequest("PUT", url);
        if (acl != null) {
            request.getHeaders().set("x-amz-acl", acl);
        } else {
            request.getHeaders().set("x-amz-acl", ACL_PRIVATE);
        }

        try {
            HttpResponse execute = request.execute();
            try {
                return execute.getStatusCode() == 200;
            } finally {
                execute.disconnect();
            }
        } catch (HttpResponseException e) {
            return false;
        }
    }

    /**
     * Set the bucket's ACL.
     *
     * @param bucket
     * @param acl
     * @return
     * @throws IOException
     */
    public boolean setBucketACL(String bucket, String acl) throws IOException {
        GenericUrl url = getGenericUrlOfBucket(bucket);
        url.set("acl", "");

        HttpRequest request = getHttpRequest("PUT", url);
        if (acl == null) {
            return false;
        }
        request.getHeaders().set("x-amz-acl", acl);

        try {
            HttpResponse execute = request.execute();
            try {
                return execute.getStatusCode() == 200;
            } finally {
                execute.disconnect();
            }
        } catch (HttpResponseException e) {
            return false;
        }

    }

    /**
     * Create an object.
     *
     * If the object is larger than 5MB, the client will automatically use a multipart session.
     *
     * If the session fails, the user may attempt to reupload the object by attempting to create
     * the exact same object again. The client will examine all parts of any current upload session
     * and attempt to reuse the session automatically. If a mismatch is discovered, the upload will fail
     * before uploading any more data. Otherwise, it will resume uploading where the session left off.
     *
     * If the multipart session fails, the user is responsible for resuming or dropping the session.
     *
     * @param bucket Bucket to use
     * @param key Key of object
     * @param contentType Content type to set this object to
     * @param size Size of all the data that will be uploaded.
     * @param data Data to upload
     * @throws IOException
     * @throws XmlPullParserException
     * @see #listActiveMultipartUploads(String)
     * @see #abortMultipartUpload(String, String, String)
     */
    public void putObject(String bucket, String key, String contentType, long size, InputStream data) throws IOException, XmlPullParserException {
        boolean isMultipart = false;
        int partSize = 0;
        String uploadID = null;


        if (size > PART_SIZE) {
            // check if multipart exists
            ListMultipartUploadsResult multipartUploads = listActiveMultipartUploads(bucket, key);
            for (Upload upload : multipartUploads.getUploads()) {
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
            ListPartsResult objectParts = listObjectParts(bucket, key, uploadID);
            if (!objectParts.getParts().isEmpty()) {
                for (Part curPart : objectParts.getParts()) {
                    long curSize = curPart.getSize();
                    String curEtag = curPart.geteTag();
                    String curNormalizedEtag = curEtag.replaceAll("\"", "").toLowerCase().trim();
                    byte[] curData = readData((int) curSize, data);
                    String generatedEtag = DatatypeConverter.printHexBinary(calculateMd5sum(curData)).toLowerCase().trim();

                    System.out.println("Part # " + curPart.getPartNumber());
                    System.out.println("From upstream: " + curNormalizedEtag + " " + curSize);
                    System.out.println("From generated: " + generatedEtag + " " + curData.length);
                    if (!curNormalizedEtag.equals(generatedEtag) || curPart.getPartNumber() != part) {
                        throw new IOException("Partial upload does not match");
                    }
                    System.out.println("Adding: " + curEtag);
                    parts.add(curEtag);
                    objectLength += curSize;
                    part++;
                }
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
     * @param bucket
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    public ListMultipartUploadsResult listActiveMultipartUploads(String bucket) throws IOException, XmlPullParserException {
        return listActiveMultipartUploads(bucket, null);
    }

    /**
     * Lists all active multipart uploads in a bucket with a given key prefix
     *
     * @param bucket
     * @param prefix
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    private ListMultipartUploadsResult listActiveMultipartUploads(String bucket, String prefix) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfBucket(bucket);
        url.set("uploads", "");

        if (prefix != null) {
            url.set("prefix", prefix);
        }

        HttpRequest request = getHttpRequest("GET", url);
        request.setFollowRedirects(false);

        HttpResponse response = request.execute();
        try {
            XmlPullParser parser = Xml.createParser();
            InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
            parser.setInput(reader);

            ListMultipartUploadsResult result = new ListMultipartUploadsResult();

            Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
            return result;
        } finally {
            response.disconnect();
        }
    }

    /**
     * Abort all active multipart uploads in a given bucket.
     *
     * @param bucket
     * @throws IOException
     * @throws XmlPullParserException
     */
    public void abortAllMultipartUploads(String bucket) throws IOException, XmlPullParserException {
        ListMultipartUploadsResult uploads = listActiveMultipartUploads(bucket);
        for (Upload upload : uploads.getUploads()) {
            abortMultipartUpload(bucket, upload.getKey(), upload.getUploadID());
        }
    }

    /**
     * Set access keys for authenticated access
     *
     * @param accessKey
     * @param secretKey
     */
    public void setKeys(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * Set user agent of the client
     *
     * @param userAgent
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    private String newMultipartUpload(String bucket, String key) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploads", "");

        HttpRequest request = getHttpRequest("POST", url);

        HttpResponse response = request.execute();
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

    private void completeMultipart(String bucket, String key, String uploadID, List<String> etags) throws IOException {
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

        System.out.println(completeManifest.toString());

        HttpRequest request = getHttpRequest("POST", url, data);
        request.setContent(new ByteArrayContent("application/xml", data));

        HttpResponse response = request.execute();
        response.disconnect();
    }

    /**
     * List all parts in an active multipart upload.
     *
     * @param bucket
     * @param key
     * @param uploadID
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    public ListPartsResult listObjectParts(String bucket, String key, String uploadID) throws IOException, XmlPullParserException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploadId", uploadID);

        HttpRequest request = getHttpRequest("GET", url);

        HttpResponse response = request.execute();
        try {
            XmlPullParser parser = Xml.createParser();
            InputStreamReader reader = new InputStreamReader(response.getContent(), "UTF-8");
            parser.setInput(reader);

            ListPartsResult result = new ListPartsResult();

            Xml.parseElement(parser, result, new XmlNamespaceDictionary(), null);
            return result;
        } finally {
            response.disconnect();
        }
    }

    /**
     * Abort an active multipart upload
     *
     * @param bucket
     * @param key
     * @param uploadID
     * @throws IOException
     */
    public void abortMultipartUpload(String bucket, String key, String uploadID) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);
        url.set("uploadId", uploadID);

        HttpRequest request = getHttpRequest("DELETE", url);
        HttpResponse response = request.execute();
        response.disconnect();
    }

    private int computePartSize(long size) {
        int minimumPartSize = PART_SIZE; // 5MB
        int partSize = (int) (size / 9999);
        return Math.max(minimumPartSize, partSize);
    }

    private void putObject(String bucket, String key, String contentType, byte[] data) throws IOException {
        putObject(bucket, key, contentType, data, "", 0);
    }

    private String putObject(String bucket, String key, String contentType, byte[] data, String uploadId, int partID) throws IOException {
        GenericUrl url = getGenericUrlOfKey(bucket, key);

        if (partID > 0) {
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
        response.disconnect();
        if (response.getStatusCode() != 200) {
            throw new IOException("Unexpected result, try resending this part again");
        }
        return response.getHeaders().getETag();
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
    public void enableLogging() {
        if (logger == null) {
            logger = Logger.getLogger(HttpTransport.class.getName());
            logger.setLevel(Level.CONFIG);
            logger.addHandler(new Handler() {

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
        }
    }
}
