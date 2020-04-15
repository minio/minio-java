import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import io.minio.ServerSideEncryption;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;

public class PutGetObjectEncryptedFile {
  /** MinioClient.putObject() and MinioClient.getObject() to a file example for SSE_C. */
  public static void main(String[] args)
      throws NoSuchAlgorithmException, IOException, InvalidKeyException {
    try {
      /* play.min.io for test and development. */
      MinioClient minioClient =
          new MinioClient(
              "https://play.min.io",
              "Q3AM3UQ867SPQQA43P2F",
              "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

      // * Amazon S3: */
      // MinioClient minioClient = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID",
      //                                           "YOUR-SECRETACCESSKEY");

      String objectName = "my-objectname";
      String bucketName = "my-bucketname";
      String inputfile = "my-inputfile";
      String outputfile = "my-outputfile";
      long inputfileSize = 100000L;

      // Generate a new 256 bit AES key - This key must be remembered by the client.
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(256);

      // To test SSE-C
      ServerSideEncryption sse = ServerSideEncryption.withCustomerKey(keyGen.generateKey());
      PutObjectOptions options = new PutObjectOptions(inputfileSize, -1);
      options.setSse(sse);
      minioClient.putObject(bucketName, objectName, inputfile, options);
      System.out.println("my-objectname is encrypted and uploaded successfully.");

      minioClient.getObject(bucketName, objectName, sse, outputfile);
      System.out.println("Content of my-objectname saved to my-outputfile ");
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
