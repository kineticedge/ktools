#!/bin/sh
set -e

cd "$(dirname "$0")"
gradle assemble

. ./.classpath.sh

#
#
#

export KAFKA_PRODUCER_BOOTSTRAP_SERVERS=localhost:9092


#
#
#

MAIN="io.kineticedge.tools.ToolCommand"

JAVA_OPTS=""
if [ -f ../jars/jolokia-agent.jar ]; then
  JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true -javaagent:../jars/jolokia-agent.jar=port=7072,host=localhost"
fi



exec java $JAVA_OPTS -cp "${CP}" "$MAIN" "$@"
