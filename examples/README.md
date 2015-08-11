# Minio Java Examples for Minio Cloud Storage Java Library [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Minio/minio?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Collection of simple Java applications illustrating usage of the Minio Cloud Storage Java library.

## How to run these examples?

### Clone the project

```bash
$ git clone https://github.com/minio/minio-java
```

### Download minio full JAR

```bash
$ cd minio-java; wget http://repo1.maven.org/maven2/io/minio/minio/0.2.4/minio-0.2.4-all.jar;
```
### Edit examples

Edit any examples and fill in access key parameters.

```bash
$ javac -cp 'minio-0.2.4-all.jar' ListBuckets.java
```

### Run the compiled example

```bash
$ java -cp '.:minio-0.2.4-all.jar' ListBuckets
bucket1
bucket2
....
...
bucketN

```

## License

These sample application examples are distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

