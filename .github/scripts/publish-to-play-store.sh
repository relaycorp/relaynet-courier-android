#!/bin/bash

# Make Bash strict
set -o nounset
set -o errexit
set -o pipefail

VERSION_NAME="$1"

# We need to store the GCP Service Account credentials in a temporary file...
# https://github.com/Triple-T/gradle-play-publisher/issues/546#issuecomment-630978695
./gradlew publish \
  -PversionName="${VERSION_NAME}" \
  -PgcpServiceAccountCredentials=<(echo "${GCP_SERVICE_ACCOUNT}")
