# For maintainers only
Minio Java SDK uses [gradle](https://gradle.org/) build system.

## Responsibilities
Go through [Maintainer Responsibility Guide](https://gist.github.com/abperiasamy/f4d9b31d3186bbd26522).

### Setup your minio-java Github Repository
Fork [minio-java](https://github.com/minio/minio-java/fork) source repository to your own personal repository.
```bash
$ git clone https://github.com/$USER_ID/minio-java
$ cd minio-java
```

### Build and verify
Run `runFunctionalTest` gradle task to build and verify the SDK.
```bash
$ ./gradlew runFunctionalTest
```

### Publishing new artifacts
#### Setup your gradle properties
Create a new gradle properties file

```bash
$ cat gradle.properties > ${HOME}/.gradle/gradle.properties <<EOF
signing.keyId=76A57749
signing.password=**REDACTED**
signing.secretKeyRingFile=/home/harsha/.gnupg/secring.gpg
ossrhUsername=minio
ossrhPassword=**REDACTED**
release=true
EOF
```

#### Import minio private key
```bash
$ gpg --import minio.asc
```

#### Upload archives to maven for publishing
```bash
$ ./gradlew uploadArchives
```

#### Cleanup
```bash
$ rm -v ${HOME}/.gradle/gradle.properties
```
