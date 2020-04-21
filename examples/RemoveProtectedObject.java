import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteObject;
import io.minio.messages.Retention;
import io.minio.messages.RetentionMode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import org.xmlpull.v1.XmlPullParserException;

public class RemoveProtectedObject {
  /** MinioClient.removeObject(bucketName,deletObject,true) example. */
  public static void main(String[] args)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException, XmlPullParserException {
    try {
      /* play.min.io for test and development. */
      MinioClient minioClient =
          new MinioClient(
              "https://play.min.io",
              "Q3AM3UQ867SPQQA43P2F",
              "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");

      /* Amazon S3: */
      // MinioClient minioClient = new MinioClient("https://s3.amazonaws.com", "YOUR-ACCESSKEYID",
      //                                           "YOUR-SECRETACCESSKEY");

      String bucketName = "my-bucketname";
      String objectName = "my-objectname";

      // minioClient.traceOn(System.out);

      // Create bucket if it doesn't exist.
      boolean found = minioClient.bucketExists(bucketName);
      if (found) {
        System.out.println("my-bucketname already exists");
      } else {
        // Create bucket 'my-bucketname' with object lock functionality enabled
        minioClient.makeBucket(bucketName, null, true);
        System.out.println(
            "my-bucketname is created successfully with object lock functionality enabled.");
      }

      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < 1; i++) {
        builder.append(
            "Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
        builder.append("(29 letters)\n");
      }

      // Create a InputStream for object upload.
      ByteArrayInputStream bais = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
      // Create object 'my-objectname' in 'my-bucketname' with content from the input stream.
      minioClient.putObject(
          bucketName, objectName, bais, new PutObjectOptions(bais.available(), -1));
      bais.close();
      System.out.println("my-objectname is uploaded successfully");

      // Declaring config with Retention mode as GOVERNANCE and
      // retain until 1 days to current date.
      ZonedDateTime retentionUntil = ZonedDateTime.now().plusDays(1);
      Retention config = new Retention(RetentionMode.GOVERNANCE, retentionUntil);

      // Set object lock configuration
      minioClient.setObjectRetention(bucketName, objectName, null, config, true);
      Retention retention = minioClient.getObjectRetention(bucketName, objectName, null);

      System.out.println("Mode: " + retention.mode());
      System.out.println("Retainuntil Date: " + retention.retainUntilDate());
      // Remove object 'my-objectname' in 'my-bucketname'.
      minioClient.removeObject(bucketName, new DeleteObject(objectName, null), true);
      System.out.println(
          "successfully removed my-bucketname/my-objectname "
              + " even when my-objectname had a Governance-type Object Lock in place.");
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
