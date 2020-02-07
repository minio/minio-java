package io.minio.messages;
import java.io.IOException;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParserException;

import com.google.api.client.util.Key;

import io.minio.ErrorCode;
import io.minio.messages.XmlEntity;

public class PutBucketEncryptionResponse extends XmlEntity {
	@Key("Code")
	protected String code;
	

	protected ErrorCode errorCode;

	public PutBucketEncryptionResponse() throws XmlPullParserException {
		super();
		super.name = "PutBucketEncryptionResponse";
	}

	/**
	 * Constructs a new ErrorResponse object by reading given reader stream.
	 */
	public PutBucketEncryptionResponse(Reader reader) throws IOException, XmlPullParserException {
		this();
		this.parseXml(reader);
	}

	/**
	 * Constructs a new ErrorResponse object with error code, bucket name, object
	 * name, resource, request ID and host ID.
	 */
	public PutBucketEncryptionResponse(ErrorCode errorCode, String bucketName, String objectName, String resource,
			String requestId, String hostId) throws XmlPullParserException {
		this();
		this.errorCode = errorCode;
		this.code = errorCode.code();
	}

	/**
	 * Returns error code.
	 */
	public ErrorCode errorCode() {
		if (this.errorCode == null) {
			this.errorCode = ErrorCode.fromString(this.code);
		}

		return this.errorCode;
	}

	/**
	 * Returns error code string.
	 */
	public String code() {
		return this.code;
	}


}
