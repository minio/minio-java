import io.minio.MinioClient;
import io.minio.admin.MinioAdminClient;

public class FunctionalTest {
  public static void runS3Tests(TestArgs args) throws Exception {
    if (!args.MINT_ENV) System.out.println(">>> Running S3 tests:");
    new TestMinioClient(
            args,
            args.IS_QUICK_TEST,
            MinioClient.builder()
                .endpoint(args.endpoint)
                .credentials(args.accessKey, args.secretKey)
                .build())
        .runTests();

    if (args.automated) {
      if (!args.MINT_ENV) {
        System.out.println();
        System.out.println(">>> Running S3 tests on TLS endpoint:");
      }
      MinioClient client =
          MinioClient.builder()
              .endpoint(args.endpointTLS)
              .credentials(args.accessKey, args.secretKey)
              .build();
      client.ignoreCertCheck();
      new TestMinioClient(args, args.IS_QUICK_TEST, client).runTests();
    }

    if (!args.MINT_ENV) {
      System.out.println();
      System.out.println(">>> Running quick tests specific region:");
      new TestMinioClient(
              args,
              true,
              MinioClient.builder()
                  .endpoint(args.endpoint)
                  .credentials(args.accessKey, args.secretKey)
                  .region(args.region)
                  .build())
          .runTests();
    }
  }

  public static void runMinioAdminTests(TestArgs args) throws Exception {
    if (!args.MINT_ENV) {
      System.out.println();
      System.out.println(">>> Running MinIO admin API tests:");
      new TestMinioAdminClient(
              args,
              MinioAdminClient.builder()
                  .endpoint(args.endpoint)
                  .credentials(args.accessKey, args.secretKey)
                  .build())
          .runAdminTests();
    }
  }

  public static void runTests(TestArgs args) throws Exception {
    runS3Tests(args);
    runMinioAdminTests(args);
  }

  public static void main(String[] args) throws Exception {
    String endpoint = null;
    String accessKey = null;
    String secretKey = null;
    String region = null;
    if (args.length == 4) {
      endpoint = args[0];
      accessKey = args[1];
      secretKey = args[2];
      region = args[3];
    }
    TestArgs testArgs = new TestArgs(endpoint, accessKey, secretKey, region);

    Process minioProcess = null;
    Process minioProcessTLS = null;
    if (args.length != 4) {
      if (!TestArgs.downloadMinioServer()) {
        System.out.println("usage: FunctionalTest <ENDPOINT> <ACCESSKEY> <SECRETKEY> <REGION>");
        System.exit(-1);
      }

      minioProcess = TestArgs.runMinioServer(false);
      try {
        int exitValue = minioProcess.exitValue();
        System.out.println("minio server process exited with " + exitValue);
        System.out.println("usage: FunctionalTest <ENDPOINT> <ACCESSKEY> <SECRETKEY> <REGION>");
        System.exit(-1);
      } catch (IllegalThreadStateException e) {
        TestArgs.ignore();
      }

      minioProcessTLS = TestArgs.runMinioServer(true);
      try {
        int exitValue = minioProcessTLS.exitValue();
        System.out.println("minio server process exited with " + exitValue);
        System.out.println("usage: FunctionalTest <ENDPOINT> <ACCESSKEY> <SECRETKEY> <REGION>");
        System.exit(-1);
      } catch (IllegalThreadStateException e) {
        TestArgs.ignore();
      }
    }

    int exitValue = 0;
    try {
      runTests(testArgs);
    } catch (Exception e) {
      if (!testArgs.MINT_ENV) e.printStackTrace();
      exitValue = -1;
    } finally {
      if (minioProcess != null) minioProcess.destroy();
      if (minioProcessTLS != null) minioProcessTLS.destroy();
    }

    System.exit(exitValue);
  }
}
