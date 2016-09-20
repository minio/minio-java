### Setup your minio-java Github Repository
Fork [minio-java upstream](https://github.com/minio/minio-java/fork) source repository to your own personal repository.
```sh
$ git clone https://github.com/$USER_ID/minio-java
$ cd minio-java
```

Minio Java Library uses gradle for its dependency management https://gradle.org/

### Gradle start up script for UN*X
```sh
$ ./gradlew build
Downloading https://services.gradle.org/distributions/gradle-2.5-bin.zip
...
...

BUILD SUCCESSFUL
...
$ ls build/libs/
[2015-11-27 19:44:07 PST] 6.6MiB minio-2.0.3-all.jar
[2015-11-27 19:43:59 PST] 182KiB minio-2.0.3-javadoc.jar
[2015-11-27 19:44:08 PST]  53KiB minio-2.0.3-sources.jar
[2015-11-27 19:43:47 PST]  64KiB minio-2.0.3.jar
$ cd minio-java/examples
$ cp ../build/libs/minio-2.0.3-all.jar .
[ edit ListBuckets.java ]
$ javac -cp 'minio-2.0.3-all.jar' ListBuckets.java
$ java -cp '.:minio-2.0.3-all.jar' ListBuckets
bucket1
bucket2
....
...
bucketN
```

### Gradle startup script for Windows

On windows command prompt

```bat
C:\minio-java\> gradlew.bat build
Downloading https://services.gradle.org/distributions/gradle-2.5-bin.zip
...
...

BUILD SUCCESSFUL

C:\minio-java\> dir build\libs\
[2015-11-27 19:44:07 PST] 6.6MiB minio-2.0.3-all.jar
[2015-11-27 19:43:59 PST] 182KiB minio-2.0.3-javadoc.jar
[2015-11-27 19:44:08 PST]  53KiB minio-2.0.3-sources.jar
[2015-11-27 19:43:47 PST]  64KiB minio-2.0.3.jar
C:\minio-java\examples> cd minio-java/examples
C:\minio-java\examples> mv ..\build\libs\minio-2.0.3-all.jar .
[ edit ListBuckets.java ]
C:\minio-java\examples> javac -cp 'minio-2.0.3-all.jar' ListBuckets.java
C:\minio-java\examples> java -cp '.:minio-2.0.3-all.jar' ListBuckets
bucket1
bucket2
....
...
bucketN
```

###  Developer Guidelines

``minio-java`` welcomes your contribution. To make the process as seamless as possible, we ask for the following:

* Go ahead and fork the project and make your changes. We encourage pull requests to discuss code changes.
    - Fork it
    - Create your feature branch (git checkout -b my-new-feature)
    - Commit your changes (git commit -am 'Add some feature')
    - Push to the branch (git push origin my-new-feature)
    - Create new Pull Request
