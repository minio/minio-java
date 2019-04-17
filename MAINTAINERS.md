# For maintainers only
MinIO Java SDK uses [gradle](https://gradle.org/) build system.

## Responsibilities
Go through [Maintainer Responsibility Guide](https://gist.github.com/abperiasamy/f4d9b31d3186bbd26522).

## Setup your minio-java Github Repository
Clone [minio-java](https://github.com/minio/minio-java/) source repository locally.
```sh
$ git clone https://github.com/minio/minio-java
$ cd minio-java
```

### Build and verify
Run `runFunctionalTest` gradle task to build and verify the SDK.
```sh
$ ./gradlew runFunctionalTest
```

## Publishing new artifacts
#### Setup your gradle properties
Create a new gradle properties file
```sh
$ cat > ${HOME}/.gradle/gradle.properties <<EOF
signing.keyId=76A57749
signing.password=*******
signing.secretKeyRingFile=/media/${USER}/Minio2/trusted/secring.gpg
nexusUsername=********
nexusPassword=********
release=true
EOF
```

#### Upload to maven
Upload all artifacts belonging to `io.minio` artifact repository, additionally this step requires you to have access to MinIO's trusted private key.
```sh
$ ./gradlew uploadArchives
```

#### Release
Closes and releases `io.minio` artifacts repository in Nexus to maven.
```sh
$ ./gradlew closeAndReleaseRepository
```

### Tag
Tag and sign your release commit, additionally this step requires you to have access to MinIO's trusted private key.
```
$ export GNUPGHOME=/media/${USER}/Minio2/trusted
$ git tag -s 0.3.0
$ git push
$ git push --tags
```

### Announce
Announce new release by adding release notes at https://github.com/minio/minio-java/releases from `trusted@min.io` account. Release notes requires two sections `highlights` and `changelog`. Highlights is a bulleted list of salient features in this release and Changelog contains list of all commits since the last release.

To generate `changelog`
```sh
git log --no-color --pretty=format:'-%d %s (%cr) <%an>' <last_release_tag>..<latest_release_tag>
```
