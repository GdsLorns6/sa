#!/bin/sh

# Standard Gradle wrapper execution script
set -e

APP_BASE_NAME=`basename "$0"`
APP_HOME="`pwd -P`"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Find java
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading gradle-wrapper.jar..."
    mkdir -p "$APP_HOME/gradle/wrapper"
    curl -sLo "$WRAPPER_JAR" https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar || \
    wget -qO "$WRAPPER_JAR" https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar
fi

exec "$JAVACMD" $JAVA_OPTS $DEFAULT_JVM_OPTS -jar "$WRAPPER_JAR" "$@"
