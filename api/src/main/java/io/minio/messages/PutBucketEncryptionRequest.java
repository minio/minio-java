package io.minio.messages;

import org.xmlpull.v1.XmlPullParserException;

import com.google.api.client.util.Key;

import io.minio.ServerSideEncryption;
import io.minio.messages.Bucket;
import io.minio.messages.XmlEntity;


@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class PutBucketEncryptionRequest extends XmlEntity {
  @Key("ServerSideEncryption")
  private ServerSideEncryption serverSideEncryption;
  @Key("Bucket")
  private Bucket bucket;



  /**
   * Constructs new delete request for given object list and quiet flag.
   */
  public PutBucketEncryptionRequest(ServerSideEncryption serv, Bucket b) throws XmlPullParserException {
    super();
    super.name = "Put";
    super.namespaceDictionary.set("", "http://s3.amazonaws.com/doc/2006-03-01/");

    this.serverSideEncryption = serv;
    this.bucket = b;
  }
}
