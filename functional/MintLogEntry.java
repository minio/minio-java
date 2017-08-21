/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015, 2016, 2017 Minio, Inc.
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

import java.io.Writer;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MintLogEntry {

  @JsonProperty("name")
  private String name;

  @JsonProperty("function")
  private String function;

  @JsonProperty("args")
  private String args;

  @JsonProperty("duration")
  private String duration;

  @JsonProperty("status")
  private String status;

  @JsonProperty("comment")
  private String alert;

  @JsonProperty("message")
  private String message;

  @JsonProperty("error")
  private String error;

  @JsonIgnore
  private static final ObjectMapper objectMapper = 
      new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
      .setSerializationInclusion(Include.NON_NULL);

  /**
    * Constructor.
    **/
  public MintLogEntry(String function,
                      String args,
                      String alert) {
    this.name = "minio-java";
    this.function = function;
    this.duration = null;
    this.args = args;
    this.status = null;
    this.alert = alert;
    this.message = null;
    this.error = null;
  }

  /**
    * Add Exception field.
    **/
  @JsonIgnore
  public void addResult(String status,
                        long duration,
                        String message, 
                        Exception e) {
        
    if (e != null) {
      Writer result = new StringWriter();
      PrintWriter printWriter = new PrintWriter(result);
      e.printStackTrace(printWriter);
      error = result.toString().replaceAll("\n", " ").replaceAll("\t", "");
     }
    if (duration > 0) {
      this.duration = String.valueOf(duration/1000.0) + " s";
    }
    this.status = status;
    this.message = message;        
  }

  /**
    * Generates JSON/Standard log entry based on passed parameter.
    **/
  @JsonIgnore
  public String getLogEntry(boolean jsonOutput) throws JsonProcessingException {

    if (jsonOutput) {
      return objectMapper.writeValueAsString(this);
    }

    return name + ": " + function + " args(" + (args == null  ? "" : args) + ") Status: " 
      + status + (duration != null ? " (" +  duration + ")" : "") ;
  }

   /**
    * Return Alert.
    **/
  @JsonIgnore
  public String getAlert() {
    return alert;
  }

  /**
    * Return Error.
    **/
  @JsonIgnore
  public String getError() {
    return error;
  }

  /**
    * Return Message.
    **/
  @JsonIgnore
  public String getMessage() {
    return message;
  }
}
