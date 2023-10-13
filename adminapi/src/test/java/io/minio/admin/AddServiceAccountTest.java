package io.minio.admin;

import io.minio.admin.messages.ServiceAccountCredentials;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Test;

public class AddServiceAccountTest {
  @Test
  public void canObtainServiceAccount()
      throws InvalidCipherTextException, NoSuchAlgorithmException, IOException,
          InvalidKeyException {
    MinioAdminClient adminClient =
        MinioAdminClient.builder()
            .endpoint("https://play.min.io")
            .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
            .build();
    ServiceAccountCredentials credentials = adminClient.addServiceAccount("Q3AM3UQ867SPQQA43P2F");
    System.out.println(credentials.accessKey());
    System.out.println(credentials.secretKey());
  }
}
