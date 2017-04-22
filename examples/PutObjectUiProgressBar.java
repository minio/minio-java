/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 Minio, Inc.
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

import org.xmlpull.v1.XmlPullParserException;

import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.NoResponseException;


public class PutObjectUiProgressBar extends JFrame {

  private static final long serialVersionUID = 1L;
  private static final String defaultFileName = "/etc/issue";
  private JButton button;

  PutObjectUiProgressBar() {
    button = new JButton("Click here to upload !");
    ButtonActionListener bal = new ButtonActionListener();
    button.addActionListener(bal);

    this.getContentPane().add(button);
  }

  /**
   * go() implements a blocking UI frame.
   */
  public void go() {

    this.setLocationRelativeTo(null);
    this.setVisible(true);
    this.pack();
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  /**
   * uploadFile(fileName) uploads to configured object storage upon reading
   * a local file, while asynchronously updating the progress bar UI
   * as well. This function is involed when user clicks on the UI
   */
  private void uploadFile(String fileName) throws IOException, NoSuchAlgorithmException, InvalidKeyException,
      XmlPullParserException, InvalidEndpointException, InvalidPortException,
      InvalidBucketNameException, InsufficientDataException, NoResponseException,
      ErrorResponseException, InternalException, InvalidArgumentException {

    /* play.minio.io for test and development. */
    MinioClient minioClient = new MinioClient("https://play.minio.io:9000", "Q3AM3UQ867SPQQA43P2F",
                                              "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
    /* Amazon S3: */
    // MinioClient minioClient = new MinioClient("https://s3.amazonaws.com",
    // "YOUR-ACCESSKEYID",
    // "YOUR-SECRETACCESSKEY");

    File file = new File(fileName);
    BufferedInputStream bis;
    try {
      bis = new BufferedInputStream(new FileInputStream(file));
      ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(
          this,
          "Uploading... " + file.getAbsolutePath(),
          bis);

      pmis.getProgressMonitor().setMillisToPopup(10);
      minioClient.putObject("bank", "my-objectname", pmis, bis.available(), "application/octet-stream");
      pmis.close();
      System.out.println("my-objectname is uploaded successfully");
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Internal class extends button listener, adds methods to initiate upload operation.
   */
  private class ButtonActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {

      button.setEnabled(false);
      SwingWorker<?, ?> worker = new SwingWorker<Object, Object>() {

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

  /**
   * MinioClient.putObjectProgressBar() example.
   */
  public static void main(String[] args) {
    PutObjectUiProgressBar demo = new PutObjectUiProgressBar();
    demo.go();
  }
}
