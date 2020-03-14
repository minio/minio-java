/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2017 MinIO, Inc.
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

import java.io.IOException;
import java.io.InputStream;

class ContentInputStream extends InputStream {
  private static final char[] CONTENT =
      new StringBuilder()
          .append("2v0hrugITxqVXRL3h1SDThnMj30h91Bx6HFyMmB92u1b2mX1XmYjAJVtLNnsN0YX")
          .append("SoW13DO1YyC33m8QZLa5LmrSe1jgZ8O2O3ey8qclyWx6f3XO02KpGNp6VGjBxA2t")
          .append("fR2bejNIkTFeyJIpPVoNz8QJMukQcqwlHxGGbB54XckrAZQ6byc8JGxQhktDcct0")
          .append("VCX8lU68lcRLlVKu7eyiMFPT9yxR9MB2wKCTq1J4Y5vzsqxT7W25ClR4LCSfcFIy")
          .append("2AbbJ3Z0jUrHrjhKS2jZmmqi1lwvVSEevKnPH5LMSQInvNzrfCW3iVCMzIzjoatT")
          .append("wayZHqwMufNTGovVkGBLaslRC2jMIoe9izwQj0uTfciE5D9iM8gaTOyqn9RVBJat")
          .append("Va12KKAOixm8C3QbTOKtAqvyDqrP1JILyXAsXKLNd856l7tjwqM1zg6ueaY8auzB")
          .append("grNxDMekuO2MvccYA6gRMjXhZF3fo8pr0wpKTee6BDx02I8WfdPacyOWuDCFCOu7")
          .append("RiyLa12sggCXE3p1XKircLds0NTw2ciAN3ZTFV0adckGmHtOUdSh7qQQ7g27TQY9")
          .append("Q2z0b6mPjuamBHrxZ7HAtxvBidfTrvBQzkfJL9zgvVVrnA5ePgRDr42UZdE5LM7w")
          .append("8oPLDS9H2SQMAvbHVfLJ7YZf62LvkNfOdRutIBrFVdwwCmKrRocPFyQcaOMSTiKg")
          .append("wOADs8RTg3pIuGHUzYgMVx5AKRikMDX3Xk3LYAHGcUk7zROgXUx1NqEeJoBB4HYR")
          .append("dUlKOJzpPOMjlogZ1aOSIdFocG8X9DS3hsMsqRRbkzSLTUKGAhvLBozR71zMMzum")
          .append("c7CGmXxK1Yen7X1T5Hdhjk4rOWuIS7e4KmpFInq0IqEGXjoObAyG74odLlR7e7Nd")
          .append("2LUkHnXl1zOriUH6sQZfu6XS7ioKWinqLgkxje01cYqd1ukUJwm7AJLvhCTc7PCX")
          .append("kmDnYPvlpEuc3QATCLWtMhz8GCFcZ4IOQA7hsu3ekuX7YjGBsnyNwEWgj1mAIbtg")
          .append("bSAVwVYtv8E9XF49uVc1TyQNAgYnJkreVNB2Opj3oBZ0Wuq0VqDbpw9XqvjRISnR")
          .append("1PWeyEbG57K6bkijyuQ2d5zo3ZxOlaHwgoUr6YJWSeTqiaFxw6240ofrhhLIOBCM")
          .append("s4LaZXKVASrWzyqQXnKSFqG9gNgPVgfxuCPo2aRbzrVuFmkYFE4FqkQkV41qUxuX")
          .append("WzFfIOMpNNJ3v6b3omKQ1aWlpu0fzdYpG4Jt6jYsilon388iZNPFcLjkMlYMESfd")
          .append("nTuSqKGs8JawEgzzVaZ9C8GiFSjgN11QAWIaQ1lAbMwScx3jwNvvVryr8HDz7uAx")
          .append("wmfZchpVkaqdwMiHbMRiv6g3ETbzprySUby59nhin0Nw9svLr715CjHiIP7nuUBd")
          .append("i8BfzN5BXZiuNux3e3ZgcP8MRLtR8RQriRUDufp0gsuGP5Gw5pHHWt6qBYqWAp86")
          .append("sp3Vov9qt5WaVxugVYHp3bqTYVcs8aQQX2mj5wyDE5wpjhwq22MOlRAc5XjJwWV0")
          .append("jgfdrgNyv71ebaWM42QuJWcIapf514lLruSNWX0wUgwLK7Ja0sUky5wXEa0ywqJY")
          .append("gQuZrWr8AWrRVeqxBQOlfrvRBG2k7PK8j6OzgudbTozV9i1G4DS5b7ooj1UEYrEu")
          .append("Sjt6KEMSW1HenSaTZL7sveHHEkrmYdJMm4MuE2iQyEcnWec3XHdVSi33qFwcUoCP")
          .append("QuYfbksMbK4VJUKqZZqefvYm5pswGyzJ4XbXSi821Gq6fYSBcJS1qFzXKG2rSz0Y")
          .append("ejiDoYpjjLl1nAd6VAqpsoTrNWUEoECFykq9wwr5kBwbc8rVqpE8kO1M4lkkuv3r")
          .append("0A6XhKSwtWGGZFtRIoSmQRYbIVACMkqhM4efP7RIIMHUjSPNoLAp3x6pmthodOMg")
          .append("WE6px8M47EX9Bq419WqcBe4YdYylU7ulxZRGizsCeGWEmWxdp5szYiUR1jAo5MDn")
          .append("f585YFIfVZ1mEQPPJg9B7RCSjPG0V7oLafQgR6lafLVaJEtJLDP5ulWbpUsnJdOc")
          .append("KVzHhGkPlLzMZSuq9gp3dzMRrpSxPfrX6geBBVTiYEkJVBuDbLrfyDPz2qvbLJXf")
          .append("iPorAsqCPoeIfEExIM20AgZVFasuBY9c52afccTFAlFDOGCBH1RHDZbGlXleiuyD")
          .append("7m4nuegSkTXeg25AOyD9SAp2IarS3oDZLO66pEcBQmqMZaEpU5rwU5yoRwf1aJ9E")
          .append("vgdSCAPJKstwkAyFLgfBKEo2ux4oenNYSj6lTvZKep3aUjWvazlEcfbekWIVXPv5")
          .append("7Iqls5NOAf5p9svJarVambGrljr7zuQAKlYC7V3MgIgexodp4Vd8OwaaP8VsLB4M")
          .append("x0oiljJk3cTVqrN1aDn1kMO4R317zeFTbrkf0xpgRKxRsfXXzpqZdyrUgxN8WtXH")
          .append("KxjBCS7KPRa1yg07Xl5UdF6rylcmyGWBSobYOFcaiP5bRhwxSvH8SnFLN7kEMkkG")
          .append("QhHfKF4FTOIBap9BVCS8kAQilRCnE3cKRmIVJM4fwmq4MVAfY5UmqfA0Bv7L9f2s")
          .append("ORxD6EEb6fVGOpjCjAkxwbd6xOyzpNxZRrHvwOpUOtCT0FcgM0KMp7APJcFAdFLA")
          .append("hLT8u89YLnyYv5nIIPty1OA02vkSH9Tw2cubu2WuWw7aeFppohmorejwk8lg15je")
          .append("05btrLIP1xGhshajQpN3VUpCZbwmztkiGD9nWYQCS26JSkfES4UA5xC9jpHgBqLU")
          .append("nhUoW74RnsuMSuPT3TXZxR4QqzHSN8fElpGGto6tlxolm4sFopfacbBJii46ygB2")
          .append("GGUBkWCUOOEKzbRW2wcsq54Nd7wOOyqGoayq4YLtJKpr4VghJ4NiVDQ1BzSqqB47")
          .append("mWGDkbGxrLJPeVzT35qUpsQvMhjqfhSujmP9BOhZjCrSWWVOQ2l6tAA66uwkSsbU")
          .append("NdQoKcIDQeV6tAtx1QUMinDf57v7jaxgx2ujhdGQahZcyosqv3KQjw5LWFVh4MCv")
          .append("y2vJhs7sMIwbjNAtrcqFRYIm0qbS4uzcghPdySBwIW8l2hbezLfaXRr4oAwD3CgY")
          .append("Lngs7ala2mrxDFDpTISy1wqKwBL7h4gAuEKXdW2JJW485JDyvtrxBqUjXI8LP1hu")
          .append("YwTfNpmnt6HRe8gExwcPZEwFe0oVAKJgJ6kgNrw8XMdfDiFfOczw1o4NUJRBx1mK")
          .append("07RymjlMjb0tBZWLgY8rJHQMoarwAp7vtpHN7lP49w2EmRwzSsru42448WipHj6q")
          .append("lgeged7odyaCVhROMyTJGZnbRaoxvhYzCr94KS00uXwe1Yzky5xdlBrsFbIKQgCx")
          .append("PW58MAAgngveltuQpo0cbC7IY932NsQUiaSAhb26vTSjouUBlVfxbVaa3ADnDSwe")
          .append("xqY4nocZVKLllLtxqHKPMNAl9pHUqNlUQVFhCDuA8puUAVgbS2rl3loXXaJjNkLT")
          .append("58cR7vuEb6BT4fJN2Y1tVimWNKhsvhnX0F6OkSJbTgJC3bG6YLisqMWKYTV9L4Xe")
          .append("kISKzhXAAfPT2JfJGhzxBhCz5gqUq0qnO6kKWU0YlHpwGkzR3aTXFbzfO0QVWB5A")
          .append("zVyG3vVIOkrRNYwGnNzW3zu9y3ItaHAspFjNoPb8G8gb7399VTjAYyNye1i0hQ2N")
          .append("wDVp3ZB8MBXlEwJxbYWm8L4wygeIQ1ZmZjTUflsZ2AUZRlWw5PSGGrTaTrOtbBGS")
          .append("HhtWiT9jXORggi38YjgjJROnBg4nECRlaSurPczcUtKiFMzNBPFEIfZLYRSxC57K")
          .append("jAVZljpdzKdGgvb7hylcyRj9BwFIkWB4yl6o5XooX3YuBoC0vCWpCWZ4yJpP3xPT")
          .append("e7h2yDUVUR4jGWPDnnLtozROEqzpe0T9HOCxymT40mRzpKvLGs0eV34LYkg58QRn")
          .append("1mePS2jNpF7ESEQcY0BM5JdN0yp2MGK7U4S4bnSztUMw2GAu0PjSUJRmAI7V9FXz")
          .append("111WBxQzeLkKCKdXhCuUFqlz6UU4ujTPJSAniUiQG6pfX6dE46j1BT0AcukxJlpA")
          .append("9Qj9iI9Bm01KlJS5Hua0RXbidHsepgAuBTADHXJEKBlZapZ0wjKEHdxWLCvS72wf")
          .append("a8Ndcpn7vvdkMfvOcvcVsP5wpybV629zkVEXwmB0dwgbWAfQjOuLQKhK6fc1DfDy")
          .append("RvU0dh0lL7azS1FPq747QdAnJMbKAoux1oVTDyrsBM6sVMq99t3Ddm1bkjjNeb9T")
          .append("QAiuWkqFwhnuvOTLGGRf9vTuA3sbJvrs21Uv8n4MBannOnoyOQJyr9cus93k7kgy")
          .append("ptFoqSJ87Z9kUQXS6roSuP5kee6xhqmQO6aRWpGQudKTrY8LiTK5B5BLve9dYkWY")
          .append("oZvFSGBbnv1HRPjSAY1ionGdKPRqr2sdGP7mhSdhZ3uSTUZcOIayh06hb0zmzd7E")
          .append("qaHRxkavmgND2pwKb09g93CWwdSQwPUFbxCmcClNs31msX60OXSRjLD86ZmTSJIj")
          .append("4gdcAHO3iQVisPNeDNmcvks3TP98cf4B5ul22gWAPc3D1dPOeQTDNw2hSJoUV4ug")
          .append("R6y2XzhC2Ulhv7zF0ijrbIX7VgZ1nopQFpw7iTy4GgqMXvGZUDK84kAqSfwBPnKd")
          .append("IaxK0k8zdV5sgf7WKcT88N3EmAS1SqHPLd2brE2ERXAWuaZIYlWXspaCw3EmuQz8")
          .append("R15Tskls7OICqIBhxsuURUcgZlQm49zW4moiwEbpQ1EA9XDW4efpLgAmStP3y9no")
          .append("1YkwwcvdabFn63lJAE5KhrBkFR0GmF0sRcs1pcUNivzyF27zEyDmYBParycmwd3W")
          .append("sTaxPQqaIx9czTUd1z8a7XbSGlmW9050To2n76dJil9YbLmfwwCygVoOcWBWO7Gh")
          .append("ggIustGjjKz2ITro9FWx8vD464xJBJaJTvEDCuGNtKCXqFNNHI2wTzX7JjYYxd6M")
          .append("d4VMY9WfqzoyNveSkJlJ3RUa4caia5LNoLtgDIHQitJUrTsvb31Y0eOI8GNTPBZa")
          .append("zoyVHtXP4IJGSWdmrn3bW2ujG2EMyT7Pp5mthnAIYsIYZeQbmDX3wK287X9onCGn")
          .append("fHHJaFCbxoyBkljICWaqRNS9K5tGv5pqDGeDYPBTUxzURYCRvT67jNRgJNQ99uLt")
          .append("dFViwP96ioZtain949ZlfeETF5BW1qK4mMQ1wPZ12Tl3qozG5DREV3ZSYaZQ9XfO")
          .append("TCCZ45mtHdiQNrp9ylXhghI1CzowRsFHXZqPyItC8G4zTzxAvIPjwb1g2legLGyD")
          .append("xvjbGB9s9qdVNwopN8SatMLWD4jdSnjxRtP1TNPr2UvIyG6AOfSuXcChCpWubM61")
          .append("HDzpnt0zl7n57zA39zQcuU6PXP1exdRasvSKjgFzSZupZT612m8mFkV7pOkDjCNK")
          .append("eGcA5v13NqDSjXkLyOMm2QyMBEUpsQXU8I9JWG2wB1kHTmkKOIVYcLPUYMqKBXfg")
          .append("KZw4m7Ua9lQmkaqvHyurHnZqtt5xCilDH3qBMYZWm1VPd6ivvbpUZqczmKp9I4Cb")
          .append("2IbILC4MZT7NTjMOC83oI00pu2rmtamgNNSE2VJZsP9flL7gqPPeUR2DqkwkEPVs")
          .append("H63R724borK2Z8rHvjq2DdzUGHDbmKksQqJv36BhRSKfB0d59TxB4TuUcDSglgcu")
          .append("WVBlxJHDaVTV2JmYoincp2qE4URZG62bbptR1LZQmUNrL0kmrp3zMNNDyaAcCwZG")
          .append("dA8lP791yM4jipCo9tBn1cTaOxkcPz8mwnVA5YdsFCGhHIpluuM28ob8Eale8GCF")
          .append("b6IktrSZyrkaT2GU1WHYJD77BQIsU1zhUlpkIsWDiCp5FCcG9uacyqt1LaVR2oRo")
          .append("4fuKLPUXitDyQKF6OHbcePEPV7gj6jYSx9Curft25pIKVQvKcKHT7TxO7Wcj2BwG")
          .append("y8h1E6JFjH7Bhmn8TRH0hFqH8ZgZbAXOHCFaKsOb4PauhymKGKYlUQImMFzTLmPl")
          .append("2LdrVm9jnqFQvI45WkC0QzFM7aK62k5ROoAwInS5p4ib3SjXjDTtpNDFH8XAJXNV")
          .append("Nblvf8lY2nlxHTJ2kLnhcA7aFppPaqqweXSvfqgMAbq4pTLzJcCSWyY6tpyP53Sb")
          .append("YznOS9mU1d0uLrMePsauw5sNTcyJYgvzIsuumfjRthvrdcIlGahWs5MWfHwnBXv5")
          .append("eYHpOgRyda4Va72NCfLhO16p7wtfOLLv85l4j0JNnG50sIBYiVQ2XDiTXPtmD8RV")
          .append("K2Yw8kbnwUOgqDTZr5hoTxQbCXIe6SdK8IqUxTP7GdBY5FmLSceXhC2A15CURdy8")
          .append("cDB50ZPeVy6SlsSzck1NU56riTghogOvTEJWnD6QdElDXmbEjmsfpZkhzo4rm1jz")
          .append("uviuxw1ByFiX8fe6gKWxyEzCrT0dE0CrPd0pMsA2oijO1q7bR0TTsfUuaLc1noRu")
          .append("m7mYd7I3308d2FvvV3BLzyd5ZWYah5uIRMYDtc9BL93HYvVGPUVgHWwKQehI5QGl")
          .append("8mHBiltS4u7N1f2uxTLFc35DEBvkHzxgGITc7uJrHiCZfQOeIef9y3Srjq1m0et8")
          .append("ytbCXvppAU0qe1QWWb2u1EEYRRUFrkdJYzK16M0FXFfBKTa55vTP4BItQBJbINZG")
          .append("fcIuesbK3gmGRmpNpenQN17k3GS6UfyklSzl5LdSggbNSoXjBhUrh88loXjTUQwE")
          .append("ZeJhLkvNaxgYe3GUlIIu0oSiMeGvYHXQX0R5g19oAdoWUTSO2VJBJHmsC1vZsN6l")
          .append("gdI5xtO8cPquj2H5hICee1zNUfQljWNGn4yLxYIsLW0jMOeBMyWRLFugIELJ3twr")
          .append("rXYdtdYG4nS6bs3RL8EbL9prONn4bnUa8gUSWcmFvnuFvaS6eqtwka5Cz4thNUgp")
          .append("6Au4NChVkb59lzbOWV7cEzBfbQe4uJn5tE9v0oemJpsjNprgqVQC0e1M836hT2VA")
          .append("hXFuWcmhN4b0OMvpVT5DIeGeuMLr8ECWvO3e0LJ9ouQLU0XZdCjgtan1PDOTybJP")
          .append("26hfZrQWnBROk3yEN8hGVzDDT6yxsDb4pppC1x8cqK41g10ZficX6vqg60CbmJRu")
          .append("aefFB20jPpocZQX9ZnBcnLPHSkkYSg5PGT0f8zVimeEUQlKbuMnQyMIGNZkgIEuj")
          .append("TL25VdN5yTQPvDh0zllBagibMOfK1jZHTMJguNCPd8sgzzD05eiU4QuCM5iFKrJp")
          .append("QhwxRZPfzDiVL0Byr3iFNatZdtmEezV6h2UsfApLAcoKghs7i2IUP6yKKw7mJ4nx")
          .append("CC5wE4twA9xUEANrWmhgUF2fkbK9H9lxzm9PAn6z6T2PG4jucUMQrd9XtJCaeWTD")
          .append("XFfQxag9V2SGCgcvqYgbhaWAat9XfOUWcNedEa8tLEz9QB8ELQUFvZAQ0gxTQEL9")
          .append("k0IwVrZCFNlpVTka4rsGymUc3mZHOHVzBHnJPr48ItElhXgTAnm5TpnqAA28lQZQ")
          .append("cNPCzfFx6DdmJRTwFnUhVzG9UVvb1GuPWgX4Fv5C21vTVD5Zb8DQA5n20a8PdHms")
          .append("6xtF1LPVOrOGkRmF7myum5shNKKwNv5VtKyLl2idrCGSikShwIotB69OHjTSaY0y")
          .append("Ypzxst41uLotmHr0Onpk1Rs2TD9Q0lwgoGYdjsYHWxe7B2htWj4z79aRkereiRaX")
          .append("gnqEoevLWaUd9twjhxeVScVPHPD0Yq8xl1N7AmV080qR2cbaP1f6V2hMpfnVKdQc")
          .append("6Ycwpgqar9Jx7hMcZKKWA4aCCiPkjRfLSdDmPyBU8HDc3Sx3lIbfYmaa1Iq42np6")
          .append("bQ9Y26Cyd30scSeG6vLZM0OWaRqp7cgDYx6IGSMpgDlnQ28fs1r4kHTZKaHTAxbr")
          .append("fA78dpE8OifjzaCNFrqKdsTGYpeNEsn0u8oTJE2b9cqlIGf1udxeadYWxtrLmOjT")
          .append("FrwPG3WiT7Gxf7VJtD51XEMgewSfqDKuUlqefag3ECQE74eJwcXzhFwiiT6z1q3l")
          .append("3Wz8n0JDkW3bLS7E5Dq1YyHDOE2VTSzd0kENcw64oVZY2Ta1mJ9dSk0pizjeiRSU")
          .append("qvgubd4Hz93flD5SNzjtnBdimieB7UD7hj8LPvUzAPFhOqruqAEWMTtpF4wYPbH1")
          .toString()
          .toCharArray();
  private long size = 0;
  private long available = 0;
  private boolean isClosed = false;
  private int pos = 0;

  public ContentInputStream(long size) {
    this.size = size;
    this.available = size;
  }

  public int available() throws IOException {
    return (int) available;
  }

  public void close() throws IOException {
    isClosed = true;
  }

  public int read() throws IOException {
    if (isClosed) {
      throw new IOException("closed stream");
    }

    if (available <= 0) {
      // Indicate EOF
      return -1;
    }

    if (pos == CONTENT.length) {
      pos = 0;
    }

    char c = CONTENT[pos];
    pos++;
    available--;

    return (int) c;
  }

  public long skip(long n) throws IOException {
    if (isClosed) {
      throw new IOException("closed stream");
    }

    if (available <= 0) {
      throw new IOException("no more data");
    }

    if (size - available < n) {
      n = size - available;
      available = 0;
    } else {
      available -= n;
    }

    return n;
  }
}
