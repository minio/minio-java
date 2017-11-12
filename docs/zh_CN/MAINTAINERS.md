# 仅供维护者阅读
Minio Java SDK使用[gradle](https://gradle.org/)进行构建。

## 职责
请查阅[维护者职责说明](https://gist.github.com/abperiasamy/f4d9b31d3186bbd26522).

## 设置你的minio-java Github仓库。
Clone [minio-java](https://github.com/minio/minio-java/)到你本地。
```sh
$ git clone https://github.com/minio/minio-java
$ cd minio-java
```

### 构建并验证
运行 `runFunctionalTest` gradle任务进行构建并验证。
```sh
$ ./gradlew runFunctionalTest
```

## 发布新的artifacts
#### 设置你的gradle properties
创建一个新的gradle properties文件
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

#### 上传到maven服务器
上传所有属于 `io.minio` artifact仓库下的artifacts，执行这个操作需要你有Minio信任的私钥。
```sh
$ ./gradlew uploadArchives
```

#### 发布
关闭并发布Nexus里的`io.minio` artifacts仓库到maven。
```sh
$ ./gradlew closeAndReleaseRepository
```

### 打tag
给你的release commit打tag并进行签名操作，执行这个操作同样需要你有Minio信任的私钥。
```
$ export GNUPGHOME=/media/${USER}/Minio2/trusted
$ git tag -s 0.3.0
$ git push
$ git push --tags
```

### 发表声明
通过使用`trusted@minio.io`在https://github.com/minio/minio-java/releases添加release notes来声明一个新的release版本问世。Release notes需要有两部分内容：`highlights`和`changelog`。Highlights是本发行版本中突出显示的功能的一个列表，Changelog则列出了从上一个release之后所有的commit信息。

生成`changelog`
```sh
git log --no-color --pretty=format:'-%d %s (%cr) <%an>' <last_release_tag>..<latest_release_tag>
```
