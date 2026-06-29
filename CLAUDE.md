# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

MinIO Java SDK — an S3-compatible client library (`io.minio:minio`) plus a MinIO
admin client (`io.minio:minio-admin`). Multi-module Gradle build. The public API
must remain **Java 8 source/bytecode compatible** even though the build runs on JDK 25.

## Modules (`settings.gradle`)

- `api` — the S3 client. Published as artifact `minio`. The core of the project.
- `adminapi` — MinIO server admin client (`io.minio.admin`). Published as `minio-admin`. Depends on `:api`.
- `functional` — integration tests run against a live S3/MinIO server (`FunctionalTest`, `TestMinioAdminClient`, etc.). Not published; sources live in `functional/` (not `src/`).
- `examples` — runnable usage examples. Sources live in `examples/` (not `src/`).

## Commands

```sh
./gradlew build                  # compile, run unit tests, spotless check, spotbugs — run before every PR
./gradlew spotlessApply          # auto-format (google-java-format); fixes most lint failures
./gradlew :api:test              # unit tests for one module
./gradlew :api:test --tests 'io.minio.MinioClientTest'        # single test class
./gradlew :api:test --tests 'io.minio.MakeBucketArgsTest.method'  # single test method
./gradlew :api:localeTest        # re-runs tests under locale de-DE (part of `check`)
./gradlew runFunctionalTest      # integration tests vs play.min.io by default
./gradlew runFunctionalTest -Pendpoint=http://localhost:9000 -PaccessKey=... -PsecretKey=... -Pregion=us-east-1
```

Unit tests are JUnit 5 (Jupiter) in `*/src/test/java`. `functional/` tests hit a real server and are not part of `build`.

## Build / lint gates (enforced in CI, fail the build)

- **`--release 8` with `-Werror`** and `-Xlint:unchecked,deprecation`. Any new warning breaks the build. Do not use APIs newer than Java 8 in `api`/`adminapi` main code.
- **Spotless** (`googleJavaFormat`, import order `edu,com,io,java,javax,org`, no unused imports). Run `./gradlew spotlessApply` before committing.
- **SpotBugs** at MAX effort / lowest confidence threshold; suppressions go in `spotbugs-filter.xml`.
- **No `com.google.common.base.Objects`** — CI greps for it; use `java.util.Objects` instead. (Guava is otherwise available.)

## Architecture

**Client layering** (`api/src/main/java/io/minio/`):
- `BaseS3Client` (abstract) — HTTP plumbing, request signing, response handling.
- `MinioAsyncClient extends BaseS3Client` — the real implementation; every operation returns `CompletableFuture`.
- `MinioClient` — synchronous facade that **wraps a `MinioAsyncClient`** and blocks on its futures. It does not extend the async client. A new sync operation almost always means adding the async method first, then a blocking wrapper.
- Both are built via a `.builder()...build()` (endpoint, credentials, region, http client).

**Args pattern** — every public operation takes one immutable `XxxArgs` object built via `XxxArgs.builder()`. The class hierarchy (see `arg-class-structure.txt`) is:
`BaseArgs` → `BucketArgs` (adds bucket/region) → `ObjectArgs` (adds object) → either `ObjectWriteArgs` (uploads: sse, tags, retention, ...) or `ObjectVersionArgs` → `ObjectReadArgs` (ssec) → `ObjectConditionalReadArgs` (offset/length/etag/since). When adding an operation, subclass the right level rather than re-declaring fields.

**Other key packages:**
- `io.minio.messages` — S3 XML request/response models, (de)serialized with simple-xml-safe.
- `io.minio.credentials` — credential `Provider`s (static, env, IAM, assume-role, LDAP, web-identity, certificate, chained).
- `io.minio.errors` — exception hierarchy rooted at `MinioException`.
- `Http`, `Signer` — OkHttp transport and AWS SigV4 signing.

## Conventions

- Public-facing API changes should be reflected in `docs/API.md`.
- Releases are driven by JReleaser (`build.sh` shows the deploy invocation); the `version` lives in `build.gradle` (`-DEV` suffix unless `-Prelease`).
- Target branch for PRs is `master`.
