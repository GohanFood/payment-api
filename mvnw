#!/bin/sh

# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# Maven Wrapper Start Up Batch script

# Required ENV vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir
#
# Optional ENV vars
# -----------------
#   M2_HOME - location of maven2's installed home dir
#   MAVEN_OPTS - parameters passed to the Java VM when running Maven
#     e.g. to debug Maven itself, use
#       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
#   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
# ----------------------------------------------------------------------------

if [ -z "$MAVEN_SKIP_RC" ] ; then

  if [ -f /usr/local/etc/mavenrc ] ; then
    . /usr/local/etc/mavenrc
  fi

  if [ -f /etc/mavenrc ] ; then
    . /etc/mavenrc
  fi

  if [ -f "$HOME/.mavenrc" ] ; then
    . "$HOME/.mavenrc"
  fi

fi

# OS specific support
cygwin=false
darwin=false
mingw=false
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  MINGW*) mingw=true;;
  Darwin*) darwin=true
    # Use /usr/libexec/java_home if available, otherwise fall back to /Library/Java/Home
    # See https://developer.apple.com/library/mac/qa/qa1170/_index.html
    if [ -z "$JAVA_HOME" ]; then
      if [ -x "/usr/libexec/java_home" ]; then
        JAVA_HOME="`/usr/libexec/java_home`"; export JAVA_HOME
      else
        JAVA_HOME="/Library/Java/Home"; export JAVA_HOME
      fi
    fi
    ;;
esac

if [ -z "$JAVA_HOME" ] ; then
  if [ -r /etc/gentoo-release ] ; then
    JAVA_HOME=`java-config --jre-home`
  fi
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# For Mingw, ensure paths are in UNIX format before anything is touched
if $mingw ; then
  [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME" ] &&
    JAVA_HOME="`(cd "$JAVA_HOME" || (echo "cannot cd into $JAVA_HOME."; exit 1); pwd)`"
fi

if [ -z "$JAVA_HOME" ]; then
  javaExecutable="`which javac`"
  if [ -n "$javaExecutable" ] && ! [ "`expr \"$javaExecutable\" : '\([^ ]*\)'`" = "no" ]; then
    # readlink(1) is not available as standard on Solaris 10.
    readLink=`which readlink`
    if [ ! `expr "$readLink" : '\([^ ]*\)'` = "no" ]; then
      if $darwin ; then
        javaHome="`dirname \"$javaExecutable\"`"
        javaExecutable="`cd \"$javaHome\" && pwd -P`/javac"
      else
        javaExecutable="`readlink -f \"$javaExecutable\"`"
      fi
      javaHome="`dirname \"$javaExecutable\"`"
      javaHome=`expr "$javaHome" : '\(.*\)/bin'`
      JAVA_HOME="$javaHome"
      export JAVA_HOME
    fi
  fi
fi

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD="`\\unset -f command 2>/dev/null; \\command -v java`"
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly." 1>&2
  echo "  We cannot execute $JAVACMD" 1>&2
  exit 1
fi

if [ -z "$BASE_DIR" ] ; then
  # resolve links - $0 may be a softlink
  PRG="$0"
  while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
      PRG="$link"
    else
      PRG="`dirname "$PRG"`/$link"
    fi
  done
  PRG="`dirname "$PRG"`"
  BASE_DIR="`cd \"$PRG\" && pwd -P`"
fi

MAVEN_PROJECTBASEDIR="$BASE_DIR"
export MAVEN_PROJECTBASEDIR

# Load MAVEN_WRAPPER_REPO_URL and MAVEN_WRAPPER_VERSION from properties
eval "$("$JAVACMD" -cp "$BASE_DIR/.mvn/wrapper/maven-wrapper.jar" MavenWrapperDownloader printConfig 2>/dev/null)"

# Fallback to reading from properties files if JVM helper doesn't work
if [ -z "$MAVEN_WRAPPER_REPO_URL" ]; then
  MAVEN_WRAPPER_REPO_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
fi

# Attempt to set APP_HOME
if [ -z "$MAVEN_HOME" ] || [ "$MAVEN_HOME" = "" ] ; then
  if [ -e "$BASE_DIR/.mvn/wrapper/maven-wrapper.jar" ]; then
    if [ -d "$HOME/.m2/wrapper/dists/apache-maven-3.9.9" ] ; then
      MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.9"
    fi
  fi
fi

WRAPPER_LAUNCHER="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"

if [ ! -f "$WRAPPER_LAUNCHER" ]; then
  # Download the wrapper jar if it doesn't exist
  if [ -n "$MAVEN_WRAPPER_REPO_URL" ]; then
    if command -v wget > /dev/null; then
      echo "Downloading Maven Wrapper..."
      wget -q -O "$WRAPPER_LAUNCHER" "$MAVEN_WRAPPER_REPO_URL" || {
        echo "Failed to download Maven Wrapper via wget. Please install wget or download manually from:"
        echo "$MAVEN_WRAPPER_REPO_URL"
        exit 1
      }
    elif command -v curl > /dev/null; then
      echo "Downloading Maven Wrapper..."
      curl -s -o "$WRAPPER_LAUNCHER" "$MAVEN_WRAPPER_REPO_URL" || {
        echo "Failed to download Maven Wrapper via curl. Please install curl or download manually from:"
        echo "$MAVEN_WRAPPER_REPO_URL"
        exit 1
      }
    else
      echo "ERROR: Neither wget nor curl found. Please install one of them or download the Maven Wrapper manually from:"
      echo "$MAVEN_WRAPPER_REPO_URL"
      exit 1
    fi
  fi
fi

# Escape application args
save () {
    for i in "$@"; do printf %s\\\\n "$i" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/' \\\\/" ; done
    echo " "
}
APP_ARGS=`save "$@"`

# Collect all arguments for the java command
eval set -- "$DEFAULT_JVM_OPTS" "$JAVA_OPTS" "$MAVEN_OPTS" "-classpath" "$WRAPPER_LAUNCHER" "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" org.apache.maven.wrapper.MavenWrapperMain "$APP_ARGS"

exec "$JAVACMD" "$@"
