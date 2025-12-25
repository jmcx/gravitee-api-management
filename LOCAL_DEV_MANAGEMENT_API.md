### Run Management API locally after making source changes

This guide shows how to rebuild the Management API from local sources and run it in the docker quick-setup stack, replacing the nightly image with your local one.


### Prerequisites (once)

- Java 21 and Maven installed on macOS:

```bash
brew install maven
brew install --cask temurin@21
```

- Ensure Java 21 is active in your shell:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
java -version
mvn -v
```


### One-time: Start the quick-setup stack (MongoDB)

If you haven't already started the stack:

```bash
cd docker/quick-setup/mongodb
docker compose up -d
```

This brings up MongoDB, Elasticsearch, Gateway (:8082), Console UI (:8084), Portal UI (:4100), and Management API (:8083). We will override only the Management API with your local image.


### Iteration loop after code changes

Run these steps each time you change the Management API source code.

### Single action (runs steps 1 to 5)

From the repo root, run:

```bash
cd /Users/jonathan.michaux/git/gravitee-api-management
make -C docker local-management-api
```

Notes:
- This will rebuild the Management API from local sources, build a local docker image `graviteeio/apim-management-api:<LOCAL_TAG>`, update `docker/quick-setup/mongodb/docker-compose.override.yml`, and restart only the `management_api` container.
- If you want to bypass the `make` wrapper, you can run:

```bash
bash docker/quick-setup/mongodb/local-management-api.sh
```

1) Build the Management API modules from source (skip tests + local validations for speed):

```bash
cd /Users/jonathan.michaux/git/gravitee-api-management
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn -DskipTests -Dskip.validation=true -pl gravitee-apim-rest-api -am clean install
```

Notes:
- `-Dskip.validation=true` skips local checks like `license-maven-plugin` and `prettier-maven-plugin` (recommended for fast iteration; CI will still run them).
- If you prefer to keep validations ON, make sure you **do not have** a local `docker/quick-setup/mongodb/docker-compose.override.yml` in your working tree (it is gitignored, but still picked up by the license check):

```bash
rm -f docker/quick-setup/mongodb/docker-compose.override.yml
```

2) Compute the local Management API image tag (uses current project version):

```bash
LOCAL_TAG=$(mvn -q \
  -Dexec.executable=echo \
  -Dexec.args='${project.version}' \
  org.codehaus.mojo:exec-maven-plugin:3.5.1:exec \
  -pl gravitee-apim-rest-api | tail -1)
echo "Building image tag: ${LOCAL_TAG}"
```

3) Build the Docker image from the built distribution output:

```bash
rm -rf gravitee-apim-rest-api/.working/image
mkdir -p gravitee-apim-rest-api/.working/image
cp gravitee-apim-rest-api/docker/Dockerfile gravitee-apim-rest-api/.working/image/Dockerfile
cp -R gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution \
      gravitee-apim-rest-api/.working/image/distribution

# Build the local image (use --no-cache if you want a clean rebuild)
docker build -t graviteeio/apim-management-api:${LOCAL_TAG} gravitee-apim-rest-api/.working/image
```

4) Point the running stack to your local Management API image (only once, or update as needed):

```bash
cd docker/quick-setup/mongodb
cat > docker-compose.override.yml <<EOF
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
```

5) Restart only the Management API container to pick up the new image:

```bash
docker compose up -d --no-deps --force-recreate management_api
```

### Troubleshooting

- If Maven fails with:
  `Failed to execute goal com.mycila:license-maven-plugin:...:check ... Some files do not have the expected license header`
  (often mentioning `docker-compose.override.yml`), either:
  - Re-run the build with `-Dskip.validation=true` (recommended for local iteration), or
  - Delete/regenerate `docker/quick-setup/mongodb/docker-compose.override.yml` using step 4 so it includes the standard Gravitee header.


### Useful commands

- View Management API logs:

```bash
docker logs -f gio_apim_management_api
```

- Rebuild with a clean Docker layer cache:

```bash
docker build --no-cache -t graviteeio/apim-management-api:${LOCAL_TAG} gravitee-apim-rest-api/.working/image
```

- If you change the project version (thus the tag), update the override file or regenerate it with the new `${LOCAL_TAG}`, then:

```bash
docker compose up -d --no-deps management_api
```

- Stop the stack:

```bash
docker compose down
```


### Endpoints

- Console UI: http://localhost:8084
- Portal UI: http://localhost:4100
- Management API: http://localhost:8083


### Add a license

You can provide an Enterprise License either as a file or via an environment variable.

- File-based (recommended)

```bash
cd docker/quick-setup/mongodb
mkdir -p .license
cp /path/to/license.key .license/license.key
# restart to mount the license file
docker compose up -d --no-deps management_api gateway
```

- Environment variable (Base64)

```bash
export LICENSE_KEY="$(base64 < /path/to/license.key | tr -d '\n')"
cd docker/quick-setup/mongodb
docker compose up -d --no-deps management_api gateway
```

Notes:
- The quick-setup compose already mounts `.license` and reads `LICENSE_KEY` for both `management_api` and `gateway`.
- If you run into permission issues creating `.license`, run:

```bash
make -C docker prepare TARGET=mongodb
```


### Run Console UI and Portal Next UI in dev mode

Run the UIs locally while using your local Management API at http://localhost:8083.

Prereqs (once per shell):

```bash
# Node 20 + Yarn 4
corepack enable && corepack prepare yarn@4.1.1 --activate
nvm use 20 || volta install node@20

# (optional) stop docker UIs so ports are free (Portal in docker uses :4100; dev uses :4101)
cd docker/quick-setup/mongodb
docker compose stop management_ui portal_ui
```

- Console UI (http://localhost:8084)

```bash
cd /Users/jonathan.michaux/git/gravitee-api-management/gravitee-apim-console-webui
yarn install

# optional: build shared libs on change (run in separate terminals)
yarn watch:gravitee-markdown
yarn watch:gravitee-dashboard

# start dev server on port 8084
yarn serve --port 8084 --host 0.0.0.0
# Proxy is configured to target http://localhost:8083 (see proxy.conf.mjs)
```

- Portal Next UI (http://localhost:4101)

```bash
cd /Users/jonathan.michaux/git/gravitee-api-management/gravitee-apim-portal-webui-next
yarn install

# start dev server on port 4101
yarn serve
# Proxy is configured to target http://localhost:8083 (see src/proxy.conf.mjs)
```

Notes:
- Ensure the Management API is running on :8083 before starting the UIs.
- If a port is busy, you can change the port (e.g., `yarn serve --port 4101 --host 0.0.0.0`).  The defaults are 8084 (Console) and 4101 (Portal Next).

