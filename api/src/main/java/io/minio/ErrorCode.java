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


/**
 * Amazon AWS S3 error codes.
 */
public enum ErrorCode {
  // custom error codes
  NO_SUCH_OBJECT("NoSuchKey", "Object does not exist"),
  RESOURCE_NOT_FOUND("ResourceNotFound", "Request resource not found"),
  RESOURCE_CONFLICT("ResourceConflict", "Request resource conflicts"),

  // S3 error codes
  ACCESS_DENIED("AccessDenied", "Access denied"),
  ACCOUNT_PROBLEM("AccountProblem", "Problem with provided account"),
  AMBIGUOUS_GRANT_BY_EMAIL_ADDRESS("AmbiguousGrantByEmailAddress",
                                   "The email address you provided is associated with more than one account"),
  AUTHORIZATION_HEADER_MALFORMED("AuthorizationHeaderMalformed", "The authorization header is malformed"),
  BAD_DIGEST("BadDigest", "Specified Content-MD5 does not match"),
  BUCKET_ALREADY_EXISTS("BucketAlreadyExists", "Bucket already exists"),
  BUCKET_ALREADY_OWNED_BY_YOU("BucketAlreadyOwnedByYou", "Bucket is already owned by you"),
  BUCKET_NOT_EMPTY("BucketNotEmpty", "Bucket is not empty"),
  CREDENTIALS_NOT_SUPPORTED("CredentialsNotSupported", "Request does not support credentials"),
  CROSS_LOCATION_LOGGING_PROHIBITED("CrossLocationLoggingProhibited", "Cross-location logging not allowed"),
  ENTITY_TOO_SMALL("EntityTooSmall", "Upload is smaller than the minimum allowed object size"),
  ENTITY_TOO_LARGE("EntityTooLarge", "Upload exceeds the maximum allowed object size"),
  EXPIRED_TOKEN("ExpiredToken", "The provided token has expired"),
  ILLEGAL_VERSIONING_CONFIGURATION_EXCEPTION("IllegalVersioningConfigurationException",
                                             "The versioning configuration specified in the request is invalid."),
  INCOMPLETE_BODY("IncompleteBody", "HTTP body size does not match with the Content-Length HTTP header"),
  INCORRECT_NUMBER_OF_FILES_IN_POST_REQUEST("IncorrectNumberOfFilesInPostRequest",
                                            "POST requires exactly one file upload per request"),
  INLINE_DATA_TOO_LARGE("InlineDataTooLarge", "Inline data exceeds the maximum allowed size"),
  INTERNAL_ERROR("InternalError", "Internal error. Please try again"),
  INVALID_ACCESS_KEY_ID("InvalidAccessKeyId", "access key does not exist"),
  INVALID_ADDRESSING_HEADER("InvalidAddressingHeader", "Invalid addressing header.  Specify the Anonymous role"),
  INVALID_ARGUMENT("InvalidArgument", "Invalid Argument"),
  INVALID_BUCKET_NAME("InvalidBucketName", "Bucket name is not valid"),
  INVALID_BUCKET_STATE("InvalidBucketState", "The request is not valid with the current state of the bucket"),
  INVALID_DIGEST("InvalidDigest", "Specified Content-MD5 is not valid"),
  INVALID_ENCRYPTION_ALGORITHM_ERROR("InvalidEncryptionAlgorithmError", "Invalid encryption algorithm error"),
  INVALID_LOCATION_CONSTRAINT("InvalidLocationConstraint", "The specified location constraint is not valid"),
  INVALID_OBJECT_STATE("InvalidObjectState", "The operation is not valid for the current state of the object"),
  INVALID_PART("InvalidPart", "One or more of the specified parts could not be found"),
  INVALID_PART_ORDER("InvalidPartOrder", "The list of parts was not in ascending order.  "
                       + "Parts list must specified in order by part number"),
  INVALID_PAYER("InvalidPayer", "All access to this object has been disabled"),
  INVALID_POLICY_DOCUMENT("InvalidPolicyDocument",
                          "The content of the form does not meet the conditions specified in the policy document"),
  INVALID_RANGE("InvalidRange", "The requested range cannot be satisfied"),
  INVALID_REQUEST("InvalidRequest", "SOAP requests must be made over an HTTPS connection"),
  INVALID_SECURITY("InvalidSecurity", "The provided security credentials are not valid"),
  INVALID_SOAP_REQUEST("InvalidSOAPRequest", "The SOAP request body is invalid"),
  INVALID_STORAGE_CLASS("InvalidStorageClass", "The storage class you specified is not valid"),
  INVALID_TARGET_BUCKET_FOR_LOGGING("InvalidTargetBucketForLogging",
                                    "The target bucket for logging does not exist, is not owned by you, or does not "
                                      + "have the appropriate grants for the log-delivery group."),
  INVALID_TOKEN("InvalidToken", "malformed or invalid token"),
  INVALID_URI("InvalidURI", "Couldn't parse the specified URI"),
  KEY_TOO_LONG("KeyTooLong", "Key is too long"),
  MALFORMED_ACL_ERROR("MalformedACLError",
                      "The XML provided was not well-formed or did not validate against published schema"),
  MALFORMED_POST_REQUEST("MalformedPOSTRequest", "The body of POST request is not well-formed multipart/form-data"),
  MALFORMED_XML("MalformedXML", "Malformed XML"),
  MAX_MESSAGE_LENGTH_EXCEEDED("MaxMessageLengthExceeded", "Request was too big"),
  MAX_POST_PRE_DATA_LENGTH_EXCEEDED_ERROR("MaxPostPreDataLengthExceededError",
                                          "POST request fields preceding the upload file were too large"),
  METADATA_TOO_LARGE("MetadataTooLarge", "Metadata headers exceed the maximum allowed metadata size"),
  METHOD_NOT_ALLOWED("MethodNotAllowed", "The specified method is not allowed against this resource"),
  MISSING_ATTACHMENT("MissingAttachment", "A SOAP attachment was expected, but none were found"),
  MISSING_CONTENT_LENGTH("MissingContentLength", "missing the Content-Length HTTP header"),
  MISSING_REQUEST_BODY_ERROR("MissingRequestBodyError", "Request body is empty"),
  MISSING_SECURITY_ELEMENT("MissingSecurityElement", "The SOAP 1.1 request is missing a security element"),
  MISSING_SECURITY_HEADER("MissingSecurityHeader", "Request is missing a required header"),
  NO_LOGGING_STATUS_FOR_KEY("NoLoggingStatusForKey",
                            "There is no such thing as a logging status subresource for a key"),
  NO_SUCH_BUCKET("NoSuchBucket", "Bucket does not exist"),
  NO_SUCH_KEY("NoSuchKey", "Object does not exist"),
  NO_SUCH_LIFECYCLE_CONFIGURATION("NoSuchLifecycleConfiguration", "The lifecycle configuration does not exist"),
  NO_SUCH_UPLOAD("NoSuchUpload", "Multipart upload does not exist"),
  NO_SUCH_VERSION("NoSuchVersion", "Specified version ID does not match an existing version"),
  NOT_IMPLEMENTED("NotImplemented", "A header you provided implies functionality that is not implemented."),
  NOT_SIGNED_UP("NotSignedUp", "Account is not signed up"),
  NO_SUCH_BUCKET_POLICY("NoSuchBucketPolicy", "Bucket does not have a bucket policy"),
  OPERATION_ABORTED("OperationAborted", "A conflicting conditional operation is currently in progress "
                      + "against this resource. Try again"),
  PERMANENT_REDIRECT("PermanentRedirect", "Access to the bucket permanently redirected to this endpoint"),
  PRECONDITION_FAILED("PreconditionFailed", "One of the preconditions specified did not hold"),
  REDIRECT("Redirect", "Temporary redirect"),
  RESTORE_ALREADY_IN_PROGRESS("RestoreAlreadyInProgress", "Object restore is already in progress"),
  REQUEST_IS_NOT_MULTI_PART_CONTENT("RequestIsNotMultiPartContent",
                                    "Bucket POST must be of the enclosure-type multipart/form-data"),
  REQUEST_TIMEOUT("RequestTimeout", "request timed out"),
  REQUEST_TIME_TOO_SKEWED("RequestTimeTooSkewed",
                          "The difference between the request time and the server's time is too large"),
  REQUEST_TORRENT_OF_BUCKET_ERROR("RequestTorrentOfBucketError",
                                  "Requesting the torrent file of a bucket is not permitted"),
  SIGNATURE_DOES_NOT_MATCH("SignatureDoesNotMatch", "The request signature does not match"),
  SERVICE_UNAVAILABLE("ServiceUnavailable", "Service unavailable.  Retry again"),
  SLOW_DOWN("SlowDown", "Reduce request rate"),
  TEMPORARY_REDIRECT("TemporaryRedirect", "Temporary redirect due to DNS updates in progress"),
  TOKEN_REFRESH_REQUIRED("TokenRefreshRequired", "The provided token must be refreshed"),
  TOO_MANY_BUCKETS("TooManyBuckets", "Bucket creation is not allowed due to maximum limit reached"),
  UNEXPECTED_CONTENT("UnexpectedContent", "Request does not support content"),
  UNRESOLVABLE_GRANT_BY_EMAIL_ADDRESS("UnresolvableGrantByEmailAddress", "The email address provided does not match"),
  USER_KEY_MUST_BE_SPECIFIED("UserKeyMustBeSpecified",
                             "The bucket POST must contain the specified field name or check the order of the fields"),
  X_AMZ_CONTENT_SHA256_MISMATCH("XAmzContentSHA256Mismatch",
                                "content SHA256 mismatch");

  private final String code;
  private final String message;


  private ErrorCode(String code, String message) {
    this.code = code;
    this.message = message;
  }


  public String code() {
    return this.code;
  }


  public String message() {
    return this.message;
  }

  /**
   * Returns ErrorCode of given code string.
   */
  public static ErrorCode fromString(String codeString) {
    if (codeString == null) {
      return null;
    }

    for (ErrorCode ec : ErrorCode.values()) {
      if (codeString.equals(ec.code)) {
        return ec;
      }
    }

    // Unknown error code string.  Its not a standard Amazon S3 error.
    return null;
  }
}
