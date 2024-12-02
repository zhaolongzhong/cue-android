#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")/.."
pwd

echo "Running lint ..."
./gradlew spotlessCheck
