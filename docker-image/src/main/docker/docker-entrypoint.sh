#!/bin/sh

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

set -eu

INSTANCE_DIR="${APACHEDS_INSTANCE_DIR:-/var/lib/apacheds/default}"
CONF_DIR="${INSTANCE_DIR}/conf"
LOG_DIR="${INSTANCE_DIR}/log"
LDIF_DIR="${APACHEDS_LDIF_DIR:-${INSTANCE_DIR}/ldif}"

mkdir -p "${CONF_DIR}" "${INSTANCE_DIR}/partitions" "${LOG_DIR}" "${LDIF_DIR}"

if [ ! -f "${CONF_DIR}/log4j.properties" ]; then
  cp /opt/apacheds/conf/log4j.properties "${CONF_DIR}/log4j.properties"
fi

exec java ${JAVA_OPTS:-} \
  -Dlog4j.configuration="file:${CONF_DIR}/log4j.properties" \
  -Dapacheds.log.dir="${LOG_DIR}" \
  -Dapacheds.ldif.dir="${LDIF_DIR}" \
  -Dapacheds.controls="${APACHEDS_CONTROLS:-}" \
  -Dapacheds.extendedOperations="${APACHEDS_EXTENDED_OPERATIONS:-}" \
  -jar /opt/apacheds/lib/apacheds-service.jar \
  "${INSTANCE_DIR}" "$@"
