#!/usr/bin/env bash
set -euo pipefail

##
# Rebuild Management API from local sources and restart only the `management_api`
# container in docker quick-setup (MongoDB).
#
# This script automates steps 1 to 5 from `LOCAL_DEV_MANAGEMENT_API.md`.
#
# Usage (from repo root):
#   bash docker/quick-setup/mongodb/local-management-api.sh
#
# Options:
#   --no-skip-validation   Enable Maven validations (default: skip validations)
#   --no-skip-tests        Run tests (default: skip tests)
#   --no-java-home         Do not auto-set JAVA_HOME on macOS (default: auto-set)
#   --no-cache             Build docker image with --no-cache
##

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

SKIP_VALIDATION="true"
SKIP_TESTS="true"
AUTO_JAVA_HOME="true"
DOCKER_NO_CACHE="false"

get_mtime_epoch() {
  local p="${1:?path}"
  if [[ ! -e "${p}" ]]; then
    echo ""
    return 0
  fi
  if stat -f %m "${p}" >/dev/null 2>&1; then
    stat -f %m "${p}" 2>/dev/null || true
  elif stat -c %Y "${p}" >/dev/null 2>&1; then
    stat -c %Y "${p}" 2>/dev/null || true
  else
    echo ""
  fi
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-skip-validation)
      SKIP_VALIDATION="false"
      shift
      ;;
    --no-skip-tests)
      SKIP_TESTS="false"
      shift
      ;;
    --no-java-home)
      AUTO_JAVA_HOME="false"
      shift
      ;;
    --no-cache)
      DOCKER_NO_CACHE="true"
      shift
      ;;
    -h|--help)
      sed -n '1,120p' "$0"
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      echo "Run with --help for usage." >&2
      exit 2
      ;;
  esac
done

if [[ "${AUTO_JAVA_HOME}" == "true" ]] && [[ "$(uname -s)" == "Darwin" ]] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
  # Force Java 21 for Maven builds (project requires Java 21; newer JDKs can break Lombok/javac integration).
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi

MVN_ARGS=()
if [[ "${SKIP_TESTS}" == "true" ]]; then
  # Skip running + compiling tests.
  # Some modules gate surefire with the property `skipTests`, so we set BOTH knobs.
  MVN_ARGS+=("-DskipTests=true" "-Dmaven.test.skip=true")
fi
if [[ "${SKIP_VALIDATION}" == "true" ]]; then
  MVN_ARGS+=("-Dskip.validation=true")
fi

DIST_PATH="${REPO_ROOT}/gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution"
DIST_MTIME_BEFORE_BUILD="$(get_mtime_epoch "${DIST_PATH}")"
echo "==> Distribution (pre-build) mtime: ${DIST_MTIME_BEFORE_BUILD:-missing}"

echo "==> Building Management API modules (repo: ${REPO_ROOT})"
cd "${REPO_ROOT}"
#
# Build the standalone distribution module explicitly (this is the folder we later COPY into the Docker image).
#
set +e
mvn "${MVN_ARGS[@]}" \
  -pl gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution \
  -am clean package
MVN_RC=$?
set -e
if [[ "${MVN_RC}" -ne 0 ]]; then
  echo "Maven build failed (exit=${MVN_RC}). The Docker image will NOT be rebuilt; fix compilation errors above and re-run." >&2
  exit "${MVN_RC}"
fi

DIST_MTIME_AFTER_BUILD="$(get_mtime_epoch "${DIST_PATH}")"
echo "==> Distribution (post-build) mtime: ${DIST_MTIME_AFTER_BUILD:-missing}"

echo "==> Computing local docker tag from Maven project version"
LOCAL_TAG="$(mvn -q \
  -Dexec.executable=echo \
  -Dexec.args='${project.version}' \
  org.codehaus.mojo:exec-maven-plugin:3.5.1:exec \
  -pl gravitee-apim-rest-api | tail -1)"
if [[ -z "${LOCAL_TAG}" ]]; then
  echo "Failed to compute LOCAL_TAG from Maven project version." >&2
  exit 1
fi
echo "    LOCAL_TAG=${LOCAL_TAG}"

echo "==> Building docker image graviteeio/apim-management-api:${LOCAL_TAG}"
IMAGE_WORKDIR="${REPO_ROOT}/gravitee-apim-rest-api/.working/image"
rm -rf "${IMAGE_WORKDIR}"
mkdir -p "${IMAGE_WORKDIR}"

cp "${REPO_ROOT}/gravitee-apim-rest-api/docker/Dockerfile" "${IMAGE_WORKDIR}/Dockerfile"
cp -R "${REPO_ROOT}/gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution" \
  "${IMAGE_WORKDIR}/distribution"

BUILD_TS="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
if [[ -n "${SOURCE_DATE_EPOCH:-}" ]]; then
  unset SOURCE_DATE_EPOCH
fi

if [[ "${DOCKER_NO_CACHE}" == "true" ]]; then
  docker build --no-cache --label "gravitee.local.buildTs=${BUILD_TS}" -t "graviteeio/apim-management-api:${LOCAL_TAG}" "${IMAGE_WORKDIR}"
else
  docker build --label "gravitee.local.buildTs=${BUILD_TS}" -t "graviteeio/apim-management-api:${LOCAL_TAG}" "${IMAGE_WORKDIR}"
fi

echo "==> Updating docker quick-setup override to point management_api to local image"
OVERRIDE_FILE="${SCRIPT_DIR}/docker-compose.override.yml"
cat > "${OVERRIDE_FILE}" <<EOF
#
# Copyright © 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
services:
  management_api:
    image: graviteeio/apim-management-api:${LOCAL_TAG}
EOF

if docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  DOCKER_COMPOSE=(docker-compose)
else
  echo "Neither 'docker compose' nor 'docker-compose' was found. Please install Docker Desktop." >&2
  exit 1
fi

echo "==> Restarting management_api container (and ensuring mongodb + elasticsearch are up)"
cd "${SCRIPT_DIR}"
"${DOCKER_COMPOSE[@]}" up -d mongodb elasticsearch
"${DOCKER_COMPOSE[@]}" up -d --no-deps --force-recreate management_api

echo "==> Done. Management API should be available at: http://localhost:8083"

