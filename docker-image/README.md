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

# ApacheDS Docker Image

This module builds an ApacheDS server image from the `apacheds-service` uber
jar. It uses the Fabric8 `docker-maven-plugin` behind the `docker` Maven
profile.

Build a local image:

```sh
./mvnw -pl docker-image -am -Pdocker package -DskipTests \
  -Dapacheds.docker.image=apachedirectory/apacheds:local
```

Build only the Docker archive without contacting the Docker daemon:

```sh
./mvnw -pl docker-image -am -Pdocker package -DskipTests \
  -Ddocker.buildArchiveOnly=true \
  -Dapacheds.docker.image=apachedirectory/apacheds:local
```

Run the image:

```sh
docker run --rm \
  -p 10389:10389 \
  -p 10636:10636 \
  apachedirectory/apacheds:local
```

The image exposes LDAP on `10389` and LDAPS on `10636`. Instance data is under
`/var/lib/apacheds/default`, with `/var/lib/apacheds` declared as a volume.

The default admin bind DN is `uid=admin,ou=system` with password `secret`.

## Startup LDIF Import

ApacheDS can load LDIF entries during startup through its built-in test entry
loader. The Docker image exposes this with `APACHEDS_LDIF_DIR`, which defaults
to `/var/lib/apacheds/default/ldif`.

Mount one or more `.ldif` files into that directory:

```sh
docker run --rm \
  -p 10389:10389 \
  -v "$PWD/ldif:/var/lib/apacheds/default/ldif:ro" \
  apachedirectory/apacheds:local
```

At startup the service reads the directory through the
`apacheds.ldif.dir` system property and imports the entries before the LDAP
listener starts.

The Fabric8 plugin output directory is intentionally set to
`target/docker-build`, while the Docker context is `target/docker`. Keeping
those paths separate avoids the Docker build archive including itself.
