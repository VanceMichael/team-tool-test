#!/bin/bash

set -e

IMAGE_NAME="standup-backend-test"

echo "Building test Docker image..."
docker build -f Dockerfile.test -t $IMAGE_NAME .

echo "Running tests..."
docker run --rm $IMAGE_NAME
