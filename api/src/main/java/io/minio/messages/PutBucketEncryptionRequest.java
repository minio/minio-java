package io.minio.messages;

import org.xmlpull.v1.XmlPullParserException;

import com.google.api.client.util.Key;

import io.minio.ServerSideEncryption;
import io.minio.messages.Bucket;
import io.minio.messages.XmlEntity;

/**
 * Helper class to create Amazon AWS S3 request XML containing information for bucket encryption.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class PutBucketEncryptionRequest extends XmlEntity {
  @Key("ServerSideEncryption")
  private ServerSideEncryption serverSideEncryption;
  @Key("Bucket")
  private Bucket bucket;



  /**
   * Constructs new request for given server side encryption object and bucket.
   */
  public PutBucketEncryptionRequest(ServerSideEncryption serv, Bucket b) throws XmlPullParserException {
    super();
    super.name = "Put";
    super.namespaceDictionary.set("", "http://s3.amazonaws.com/doc/2006-03-01/");

    this.serverSideEncryption = serv;
    this.bucket = b;
  }
}
