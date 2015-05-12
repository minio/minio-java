### Setup your objectstorage-java Github Repository
Fork [objectstorage-java upstream](https://github.com/minio/objectstorage-java/fork) source repository to your own personal repository.
```sh
$ git clone https://github.com/$USER_ID/objectstorage-java
$ cd objectstorage-java
```

Objectstorage Java library uses gradle for its dependency management https://gradle.org/

### Gradle start up script for UN*X
```sh
$ ./gradlew check
Downloading https://services.gradle.org/distributions/gradle-2.3-bin.zip
...
...
Unzipping /home/user/.gradle/wrapper/dists/gradle-2.3-bin/a48v6zq5mdp1uyn9rwlj56945/gradle-2.3-bin.zip to /home/user/.gradle/wrapper/dists/gradle-2.3-bin/a48v6zq5mdp1uyn9rwlj56945
Set executable permissions for: /home/user/.gradle/wrapper/dists/gradle-2.3-bin/a48v6zq5mdp1uyn9rwlj56945/gradle-2.3/bin/gradle
:help

Welcome to Gradle 2.3.

To run a build, run gradlew <task> ...

To see a list of available tasks, run gradlew tasks

To see a list of command-line options, run gradlew --help

To see more detail about a task, run gradlew help --task <task>

BUILD SUCCESSFUL

```

### Gradle startup script for Windows

On windows command prompt

```bat
C:\objectstorage-java\>gradlew.bat check
```

###  Developer Guidelines

``objectstorage-java`` welcomes your contribution. To make the process as seamless as possible, we ask for the following:

* Go ahead and fork the project and make your changes. We encourage pull requests to discuss code changes.
    - Fork it
    - Create your feature branch (git checkout -b my-new-feature)
    - Commit your changes (git commit -am 'Add some feature')
    - Push to the branch (git push origin my-new-feature)
    - Create new Pull Request
