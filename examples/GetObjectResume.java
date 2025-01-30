import com.google.common.io.ByteStreams;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class GetObjectResume {
  public static void main(String[] args) throws Exception {
    MinioClient minioClient =
        MinioClient.builder()
            .endpoint("https://play.min.io")
            .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
            .build();

    String filename = "my-objectname";
    Path path = Paths.get(filename);
    long fileSize = 0;
    if (Files.exists(path)) fileSize = Files.size(path);

    StatObjectResponse stat =
        minioClient.statObject(
            StatObjectArgs.builder().bucket("my-bucketname").object("my-objectname").build());

    if (fileSize == stat.size()) {
      // Already fully downloaded.
      return;
    }

    if (fileSize > stat.size()) {
      throw new Exception("stored file size is greater than object size");
    }

    InputStream stream =
        minioClient.getObject(
            GetObjectArgs.builder()
                .bucket("my-bucketname")
                .object("my-objectname")
                .offset(fileSize)
                .build());

    try (OutputStream os =
        Files.newOutputStream(
            path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
      ByteStreams.copy(stream, os);
    } finally {
      stream.close();
    }
  }
}
