### Setup your minio-java Github Repository
Fork [minio-java upstream](https://github.com/minio/minio-java/fork) source repository to your own personal repository.
```sh
$ git clone https://github.com/$USER_ID/minio-java
$ cd minio-java
```

Minio Java Library uses gradle for its dependency management https://gradle.org/

### Gradle start up script for UN*X
```sh
$ ./gradlew check
Downloading https://services.gradle.org/distributions/gradle-2.5-bin.zip
...
...

BUILD SUCCESSFUL

```

### Gradle startup script for Windows

On windows command prompt

```bat
C:\minio-java\>gradlew.bat check
```

###  Developer Guidelines

``minio-java`` welcomes your contribution. To make the process as seamless as possible, we ask for the following:

* Go ahead and fork the project and make your changes. We encourage pull requests to discuss code changes.
    - Fork it
    - Create your feature branch (git checkout -b my-new-feature)
    - Commit your changes (git commit -am 'Add some feature')
    - Push to the branch (git push origin my-new-feature)
    - Create new Pull Request
