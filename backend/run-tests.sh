#!/bin/bash
set -e

cd "$(dirname "$0")"

docker build -t standup-test .
docker run --rm -v "$(pwd)/test-reports:/app/target/surefire-reports" standup-test bash -c 'cat /app/target/surefire-reports/*.txt 2>/dev/null || true'
echo "Test reports are available in $(pwd)/test-reports"
