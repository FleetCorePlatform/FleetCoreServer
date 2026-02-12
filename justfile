set shell := ["bash", "-c"]

registry := "registry.intrahomelab.xyz"
project := "fleetcore"
image := "fleetcoreserver"
tag := "latest"

default: clean format run

clean:
    rm -rf target

install: clean
    ./mvnw install -Dspotless.check.skip=true

install-native: clean
    ./mvnw install -Pnative

format:
    ./mvnw spotless:apply

lint:
    ./mvnw verify

test:
    ./mvnw test

run:
    ./mvnw quarkus:dev

# Usage: just build (jvm|native)
build mode="jvm":
    @echo "Building in {{mode}} mode..."
    @if [ "{{mode}}" == "native" ]; then \
        just install-native; \
        docker build -f src/main/docker/Dockerfile.native -t {{registry}}/{{project}}/{{image}}:{{tag}} .; \
    else \
        just install; \
        docker build -f src/main/docker/Dockerfile.jvm -t {{registry}}/{{project}}/{{image}}:{{tag}} .; \
    fi
    @echo "Pushing to {{registry}}..."
    docker push {{registry}}/{{project}}/{{image}}:{{tag}}
