/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 MinIO, Inc.
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

package io.minio.messages;

import io.minio.Utils;
import java.util.LinkedList;
import java.util.List;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Object representation of response XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjects.html">DeleteObjects
 * API</a>.
 */
@Root(name = "DeleteResult", strict = false)
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class DeleteResult {
  @ElementList(name = "Deleted", inline = true, required = false)
  private List<DeletedObject> objectList;

  @ElementList(name = "Error", inline = true, required = false)
  private List<DeleteError> errorList;

  public DeleteResult() {}

  /** Constructs new delete result with an error. */
  public DeleteResult(DeleteError error) {
    this.errorList = new LinkedList<DeleteError>();
    this.errorList.add(error);
  }

  /** Returns deleted object list. */
  public List<DeletedObject> objectList() {
    return Utils.unmodifiableList(objectList);
  }

  /** Returns delete error list. */
  public List<DeleteError> errorList() {
    return Utils.unmodifiableList(errorList);
  }
}
