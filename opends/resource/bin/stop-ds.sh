#!/bin/sh
#
# CDDL HEADER START
#
# The contents of this file are subject to the terms of the
# Common Development and Distribution License, Version 1.0 only
# (the "License").  You may not use this file except in compliance
# with the License.
#
# You can obtain a copy of the license at
# trunk/opends/resource/legal-notices/OpenDS.LICENSE
# or https://OpenDS.dev.java.net/OpenDS.LICENSE.
# See the License for the specific language governing permissions
# and limitations under the License.
#
# When distributing Covered Code, include this CDDL HEADER in each
# file and include the License file at
# trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
# add the following below this CDDL HEADER, with the fields enclosed
# by brackets "[]" replaced with your own identifying * information:
#      Portions Copyright [yyyy] [name of copyright owner]
#
# CDDL HEADER END
#
#
#      Portions Copyright 2006 Sun Microsystems, Inc.


# This script may be used to request that the Directory Server shut down.
# It operates in two different ways, depending on how it is invoked.  If it
# is invoked without any arguments and a local PID file is available, then it
# will stop the server by sending a TERM signal to the process, and this
# script will wait until the server has stopped before exiting.  If any
# arguments were provided or there is no local PID file, then it will attempt
# to stop the server using an LDAP request.


# See if JAVA_HOME is set.  If not, then see if there is a java executable in
# the path and try to figure it out.
if test -z "${JAVA_BIN}"
then
  if test -z "${JAVA_HOME}"
  then
    JAVA_BIN=`which java 2> /dev/null`
    if test ${?} -eq 0
    then
      export JAVA_BIN
    else
      echo "Please set JAVA_HOME to the root of a Java 5.0 installation."
      exit 1
    fi
  else
    JAVA_BIN=${JAVA_HOME}/bin/java
    export JAVA_BIN
  fi
fi


# Explicitly set the PATH, LD_LIBRARY_PATH, LD_PRELOAD, and other important
# system environment variables for security and compatibility reasons.
PATH=/bin:/usr/bin
LD_LIBRARY_PATH=
LD_LIBRARY_PATH_32=
LD_LIBRARY_PATH_64=
LD_PRELOAD=
LD_PRELOAD_32=
LD_PRELOAD_64=
export PATH LD_LIBRARY_PATH LD_LIBRARY_PATH_32 LD_LIBRARY_PATH_64 \
       LD_PRELOAD LD_PRELOAD_32 LD_PRELOAD_34


# Capture the current working directory so that we can change to it later.
# Then capture the location of this script and the Directory Server instance
# root so that we can use them to create appropriate paths.
WORKING_DIR=`pwd`

cd `dirname "${0}"`
SCRIPT_DIR=`pwd`

cd ..
INSTANCE_ROOT=`pwd`
export INSTANCE_ROOT

cd "${WORKING_DIR}"


# Configure the appropriate CLASSPATH.
CLASSPATH=${INSTANCE_ROOT}/classes
for JAR in ${INSTANCE_ROOT}/lib/*.jar
do
  CLASSPATH=${CLASSPATH}:${JAR}
done
export CLASSPATH


# See if any arguments were provided and if a local PID file exists.  If there
# were no arguments and there is a PID file, then try to stop the server with
# a kill command.
if test -z "${1}" 
then
  if test -f "${INSTANCE_ROOT}/logs/server.pid"
  then
    kill `cat "${INSTANCE_ROOT}/logs/server.pid"`
    EXIT_CODE=${?}
    if test "${EXIT_CODE}" -eq 0
    then
      "${JAVA_BIN}" -Xms8M -Xmx8M org.opends.server.tools.WaitForFileDelete \
           --targetFile "${INSTANCE_ROOT}/logs/server.pid" \
           --logFile "${INSTANCE_ROOT}/logs/errors"
      EXIT_CODE=${?}
    fi
    exit ${EXIT_CODE}
  fi
fi


# If we've gotten here, then we should try to stop the server over LDAP.
"${JAVA_BIN}" ${JAVA_ARGS} org.opends.server.tools.StopDS "${@}"
