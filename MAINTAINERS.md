# For maintainers only

### Setup your minio-java Github Repository

Fork [minio-java upstream](https://github.com/minio/minio-java/fork) source repository to your own personal repository.
```bash
$ git clone https://github.com/$USER_ID/minio-java
$ cd minio-java
```

Minio Java Library uses gradle for its dependency management https://gradle.org/

### Publishing new artifacts

#### Setup your gradle properties

Create a new gradle properties file

```bash
$ cat >> ${HOME}/.gradle/gradle.properties << EOF
signing.keyId=76A57749
signing.password=**REDACTED**
signing.secretKeyRingFile=/home/harsha/.gnupg/secring.gpg
ossrhUsername=minio
ossrhPassword=**REDACTED**
EOF
```

#### Import minio private key

```bash
$ gpg --import minio.asc
```

#### Modify build.gradle with new version 

```bash
$ cat build.gradle
...
...
group = 'io.minio'
archivesBaseName = 'minio'
version = '0.3.0'
...
...
```

#### Upload archives to maven for publishing

```bash
$ ./gradlew uploadArchives
```