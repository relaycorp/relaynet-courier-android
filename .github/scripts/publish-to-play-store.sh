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
  -DenableGpp=true \
  -DversionName="${VERSION_NAME}" \
  -DsigningKeystorePath=<(echo "${ANDROID_KEYSTORE}") \
  -DgcpServiceAccountCredentials=<(echo "${GCP_SERVICE_ACCOUNT}")
