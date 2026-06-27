<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Contributing to Apache Directory Server

Apache Directory Server is built with Maven. Use the Maven wrapper in this
repository unless you specifically need to test a different Maven version.

## JDK

The project is compiled for Java 8 compatibility:

```xml
<maven.compiler.release>8</maven.compiler.release>
<maven.compiler.source>8</maven.compiler.source>
<maven.compiler.target>8</maven.compiler.target>
```

For local development, use JDK 21. The GitHub pull request build runs on
Temurin JDK 21, while Jenkins also exercises newer JDKs. Release/deploy
configuration still preserves Java 8 compatibility.

Before building, verify the active JDK:

```sh
java -version
./mvnw -version
```

## Local Build

This repository consumes shared Apache Directory build artifacts that are
published by other Apache Directory repositories:

* `org.apache.directory.project:project` is the parent POM from
  `apache/directory-project`.
* `org.apache.directory.api:*` artifacts are from `apache/directory-ldap-api`.

Maven must be able to resolve these artifacts before ApacheDS modules can
compile. For ordinary local development, prefer released versions available in
Maven Central, such as parent POM `51` and LDAP API `2.1.8`.

Compile the project:

```sh
./mvnw clean compile
```

Run the normal local build:

```sh
./mvnw clean install
```

Build without running tests:

```sh
./mvnw clean install -DskipTests
```

If the build asks for unavailable LDAP API snapshot artifacts such as
`org.apache.directory.api:*:2.1.8-SNAPSHOT`, override the dependency version
with the released LDAP API artifacts:

```sh
./mvnw clean install -DskipTests -Dorg.apache.directory.api.version=2.1.8
```

Skip both test compilation and test execution:

```sh
./mvnw clean install -Dmaven.test.skip=true
```

Use `-DskipTests` when you want the fastest regular build that still verifies
test sources compile. Use `-Dmaven.test.skip=true` only when you intentionally
want to skip all test work.

## Docker

The existing `installers` module has a `docker` profile that copies
`installers/src/test/docker` into the build output. That profile supports
installer-related Docker test assets; it is not the ApacheDS server image
build.

The `docker-image` module builds an ApacheDS image from the `apacheds-service`
uber jar using the Fabric8 `docker-maven-plugin`:

```sh
./mvnw -pl docker-image -am -Pdocker package -DskipTests
```

Set the image name when you want a local development tag:

```sh
./mvnw -pl docker-image -am -Pdocker package -DskipTests \
  -Dapacheds.docker.image=apachedirectory/apacheds:local
```

Use the `docker` profile instead of the short `docker:build` goal from the
root reactor. Maven does not resolve the Fabric8 plugin prefix unless
`io.fabric8` is configured as a plugin group in the local Maven settings.

The image exposes LDAP on port `10389` and LDAPS on port `10636`. Runtime data
is stored under `/var/lib/apacheds`.

Startup LDIF files can be mounted into `/var/lib/apacheds/default/ldif`, or a
different directory can be selected with `APACHEDS_LDIF_DIR`. The image passes
that path to the service with `-Dapacheds.ldif.dir`; see
`docker-image/README.md` for details.

The `testcontainers` module contains Docker-based integration tests for that
image and publishes a reusable test library. Downstream tests can depend on
`org.apache.directory.server:apacheds-testcontainers` and use
`org.apache.directory.server.testcontainers.ApacheDsContainer`.

The library also includes `withActiveDirectoryFixture()`, an LDAP-facing Active
Directory simulation fixture for common user, group, and computer account
queries. See `testcontainers/README.md` for its scope and limitations.

Build the image first, then run the module's smoke test with the same tag:

```sh
./mvnw -pl testcontainers -am test \
  -Dapacheds.docker.image=apachedirectory/apacheds:local
```

## Security

Before reporting security issues, read `SECURITY.md` and the linked
`THREAT_MODEL.md`.
