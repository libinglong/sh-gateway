#!/bin/bash
set -e
# shellcheck disable=SC2086
exec java ${JAVA_OPTS} -jar app.jar