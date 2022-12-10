/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 MinIO, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingWorker;

public class PutObjectUiProgressBar extends JFrame {

  private static final long serialVersionUID = 1L;
  private static final String defaultFileName = "/etc/issue";
  private JButton button = null;

  PutObjectUiProgressBar() {}

  /** go() implements a blocking UI frame. */
  public void go() {
    button = new JButton("Click here to upload!");
    button.addActionListener(new ButtonActionListener());
    this.getContentPane().add(button);

    this.setLocationRelativeTo(null);
    this.setVisible(true);
    this.pack();
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  /**
   * uploadFile(fileName) uploads to configured object storage upon reading a local file, while
   * asynchronously updating the progress bar UI as well. This function is involed when user clicks
   * on the UI
   */
  private void uploadFile(String fileName)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException, MinioException {
    /* play.min.io for test and development. */
    MinioClient minioClient =
        MinioClient.builder()
            .endpoint("https://play.min.io")
            .credentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
            .build();

    /* Amazon S3: */
    // MinioClient minioClient =
    //     MinioClient.builder()
    //         .endpoint("https://s3.amazonaws.com")
    //         .credentials("YOUR-ACCESSKEY", "YOUR-SECRETACCESSKEY")
    //         .build();

    File file = new File(fileName);
    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
      ProgressMonitorInputStream pmis =
          new ProgressMonitorInputStream(this, "Uploading... " + file.getAbsolutePath(), bis);

      pmis.getProgressMonitor().setMillisToPopup(10);
      minioClient.putObject(
          PutObjectArgs.builder().bucket("bank").object("my-objectname").stream(
                  pmis, pmis.available(), -1)
              .build());
      System.out.println("my-objectname is uploaded successfully");
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /** Internal class extends button listener, adds methods to initiate upload operation. */
  private class ButtonActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {

      button.setEnabled(false);
      SwingWorker<?, ?> worker =
          new SwingWorker<Object, Object>() {

            @Override
            protected Object doInBackground() throws Exception {
              uploadFile(defaultFileName);
              return null;
            }

            @Override
            protected void done() {
              button.setEnabled(true);
            }
          };

      worker.execute();
    }
  }

  /** MinioClient.putObjectProgressBar() example. */
  public static void main(String[] args) {
    PutObjectUiProgressBar demo = new PutObjectUiProgressBar();
    demo.go();
  }
}
