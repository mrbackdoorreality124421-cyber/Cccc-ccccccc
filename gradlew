#!/usr/bin/env bash

# Gradle Wrapper startup script for Unix

PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_BASE_NAME=`basename "$0"`
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# If wrapper jar is not present, use installed gradle if available
if [ ! -f "$CLASSPATH" ]; then
    if command -v gradle >/dev/null 2>&1; then
        exec gradle "$@"
    else
        echo "Error: Neither gradle-wrapper.jar nor system gradle found." >&2
        exit 1
    fi
fi

# Locate Java binary
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" -Dorg.gradle.appname="$APP_BASE_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
