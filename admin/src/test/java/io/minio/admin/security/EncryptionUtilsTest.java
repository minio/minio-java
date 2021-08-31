/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2015-2021 MinIO, Inc.
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

package io.minio.admin.security;

import com.google.common.io.BaseEncoding;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Assert;
import org.junit.Test;

public class EncryptionUtilsTest {

  @Test
  public void canEncryptDecryptText()
      throws UnsupportedEncodingException, InvalidCipherTextException {
    byte[] data = "bar".getBytes("UTF-8");
    ByteBuffer encryptedData = EncryptionUtils.encrypt("foo", data);
    ByteBuffer decryptedData = EncryptionUtils.decrypt("foo", encryptedData.array());
    Assert.assertArrayEquals(data, decryptedData.array());
  }

  @Test
  public void canDecryptText() throws UnsupportedEncodingException, InvalidCipherTextException {
    String hexData =
        "0c01c44abba473bae01f777f01edbf988723a60385170577d7644f1fb132b3de00bf47ea28fc00e6ca222e42538c5a5091fa64de7ed4da81c5d0b69c";
    EncryptionUtils.decrypt("foo", BaseEncoding.base16().lowerCase().decode(hexData));
  }
}
