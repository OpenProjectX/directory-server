# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

FROM eclipse-temurin:21-jre

ARG APACHEDS_VERSION=${project.version}

LABEL org.opencontainers.image.title="Apache Directory Server" \
      org.opencontainers.image.version="${APACHEDS_VERSION}" \
      org.opencontainers.image.description="ApacheDS LDAP directory server"

RUN useradd --system --uid 10001 --home-dir /var/lib/apacheds --create-home apacheds

COPY apacheds-service.jar /opt/apacheds/lib/apacheds-service.jar
COPY log4j.properties /opt/apacheds/conf/log4j.properties
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

RUN chmod 755 /usr/local/bin/docker-entrypoint.sh \
    && mkdir -p /opt/apacheds/conf /opt/apacheds/lib /var/lib/apacheds/default \
    && chown -R apacheds:apacheds /opt/apacheds /var/lib/apacheds

USER apacheds

ENV APACHEDS_INSTANCE_DIR=/var/lib/apacheds/default \
    APACHEDS_CONTROLS= \
    APACHEDS_EXTENDED_OPERATIONS=

VOLUME ["/var/lib/apacheds"]

EXPOSE 10389 10636

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
