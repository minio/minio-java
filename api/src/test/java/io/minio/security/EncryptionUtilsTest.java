package io.minio.security;

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
