import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Retention;
import io.minio.messages.RetentionMode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class RemoveProtectedObjects {
  /** MinioClient.removeObject(bucketName,objectName,true) example. */
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
      boolean found = minioClient.bucketExists(bucketName);
      if (found) {
        System.out.println("my-bucketname already exists");
      } else {
        minioClient.makeBucket(bucketName, null, true);
      }

      ZonedDateTime retentionUntil = ZonedDateTime.now().plusDays(1);
      Retention expectedConfig = new Retention(RetentionMode.GOVERNANCE, retentionUntil);

      List<DeleteObject> deleteObjects = new LinkedList<DeleteObject>();

      for (int j = 1; j <= 3; j++) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1; i++) {
          builder.append(
              "Sphinx of black quartz, judge my vow: Used by Adobe InDesign to display font samples. ");
          builder.append("(29 letters)\n");
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
        minioClient.putObject(
            bucketName, objectName + j, bais, new PutObjectOptions(bais.available(), -1));
        minioClient.setObjectRetention(bucketName, objectName + j, null, expectedConfig, true);
        deleteObjects.add(new DeleteObject(objectName + j, null));
        bais.close();
      }

      for (Result<DeleteError> errorResult :
          minioClient.removeObjects(bucketName, deleteObjects, true)) {
        DeleteError error = errorResult.get();
        System.out.println(
            "Failed to remove '" + error.objectName() + "'. Error:" + error.message());
      }
    } catch (MinioException e) {
      System.out.println("Error occurred: " + e);
    }
  }
}
