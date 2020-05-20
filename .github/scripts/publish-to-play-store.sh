#!/bin/bash

# Make Bash strict
set -o nounset
set -o errexit
set -o pipefail

VERSION_NAME="$1"

# We have to pass some secrets to Gradle and we'll pass as many as we can as environment variables,
# but some must be passed as file paths. To avoid leaking those files, we'll use process
# substitution.
./gradlew publish \
  -PversionName="${VERSION_NAME}" \
  -PsigningKeystorePath=<(echo "${ANDROID_KEYSTORE}") \
  -PgcpServiceAccountCredentials=<(echo "${GCP_SERVICE_ACCOUNT}")
