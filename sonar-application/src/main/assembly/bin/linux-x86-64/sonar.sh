#! /bin/sh

APP_NAME="SonarQube"

# Location of the pid file.
PIDDIR="${PIDDIR-.}"

# By default, java from the PATH is used, except if SONAR_JAVA_PATH env variable is set
findjava() {
  if [ -z "${SONAR_JAVA_PATH}" ]; then
    if ! command -v java 2>&1; then
      echo "Java not found. Please make sure that the environmental variable SONAR_JAVA_PATH points to a Java executable"
      exit 1
    fi
    JAVA_CMD=java
  else
    if ! [ -x "${SONAR_JAVA_PATH}" ] || ! [ -f "${SONAR_JAVA_PATH}" ]; then
      echo "File '${SONAR_JAVA_PATH}' is not executable. Please make sure that the environmental variable SONAR_JAVA_PATH points to a Java executable"
      exit 1
    fi
    JAVA_CMD="${SONAR_JAVA_PATH}"
  fi
}

findjava

# Get the fully qualified path to the script
case $0 in
    /*)
        SCRIPT="$0"
        ;;
    *)
        PWD=`pwd`
        SCRIPT="$PWD/$0"
        ;;
esac

# Resolve the true real path without any sym links.
CHANGED=true
while [ "X$CHANGED" != "X" ]
do
    # Change spaces to ":" so the tokens can be parsed.
    SAFESCRIPT=`echo $SCRIPT | sed -e 's; ;:;g'`
    # Get the real path to this script, resolving any symbolic links
    TOKENS=`echo $SAFESCRIPT | sed -e 's;/; ;g'`
    REALPATH=
    for C in $TOKENS; do
        # Change any ":" in the token back to a space.
        C=`echo $C | sed -e 's;:; ;g'`
        REALPATH="$REALPATH/$C"
        # If REALPATH is a sym link, resolve it.  Loop for nested links.
        while [ -h "$REALPATH" ] ; do
            LS="`ls -ld "$REALPATH"`"
            LINK="`expr "$LS" : '.*-> \(.*\)$'`"
            if expr "$LINK" : '/.*' > /dev/null; then
                # LINK is absolute.
                REALPATH="$LINK"
            else
                # LINK is relative.
                REALPATH="`dirname "$REALPATH"`""/$LINK"
            fi
        done
    done

    if [ "$REALPATH" = "$SCRIPT" ]
    then
        CHANGED=""
    else
        SCRIPT="$REALPATH"
    fi
done

# Change the current directory to the location of the script
cd "`dirname "$REALPATH"`"

LIB_DIR="../../lib"

HAZELCAST_ADDITIONAL="--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.nio=ALL-UNNAMED \
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
--add-opens=java.management/sun.management=ALL-UNNAMED \
--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED"

# Sonar app launching process memory setting
XMS="-Xms8m"
XMX="-Xmx32m"

COMMAND_LINE="$JAVA_CMD $XMS $XMX $HAZELCAST_ADDITIONAL -jar $LIB_DIR/sonar-application-@sqversion@.jar"

# Location of the pid file.
PIDFILE="$PIDDIR/$APP_NAME.pid"

# Resolve the location of the 'ps' command
PSEXE="/usr/bin/ps"
if [ ! -x "$PSEXE" ]
then
    PSEXE="/bin/ps"
    if [ ! -x "$PSEXE" ]
    then
        echo "Unable to locate 'ps'."
        echo "Please report this message along with the location of the command on your system."
        exit 1
    fi
fi

getpid() {
    if [ -f "$PIDFILE" ]
    then
        if [ -r "$PIDFILE" ]
        then
            pid=`cat "$PIDFILE"`
            if [ "X$pid" != "X" ]
            then
                # It is possible that 'a' process with the pid exists but that it is not the
                #  correct process.  This can happen in a number of cases, but the most
                #  common is during system startup after an unclean shutdown.
                # The ps statement below looks for the specific wrapper command running as
                #  the pid.  If it is not found then the pid file is considered to be stale.
                pidtest=`$PSEXE -p $pid -o args | grep "sonar-application-@sqversion@.jar" | tail -1`
                if [ "X$pidtest" = "X" ]
                then
                    # This is a stale pid file.
                    rm -f "$PIDFILE"
                    echo "Removed stale pid file: $PIDFILE"
                    pid=""
                fi
            fi
        else
            echo "Cannot read $PIDFILE."
            exit 1
        fi
    fi
}

testpid() {
    pid=`$PSEXE -p $pid | grep $pid | grep -v grep | awk '{print $1}' | tail -1`
    if [ "X$pid" = "X" ]
    then
        # Process is gone so remove the pid file.
        rm -f "$PIDFILE"
        pid=""
    fi
}

console() {
    echo "Running $APP_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        echo $$ > $PIDFILE
        exec $COMMAND_LINE -Dsonar.log.console=true
    else
        echo "$APP_NAME is already running."
        exit 1
    fi
}

start() {
    echo "Starting $APP_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        exec nohup $COMMAND_LINE >../../logs/nohup.log 2>&1 &
        echo $! > $PIDFILE
    else
        echo "$APP_NAME is already running."
        exit 1
    fi
    getpid
    if [ "X$pid" != "X" ]
    then
        echo "Started $APP_NAME."
    else
        echo "Failed to start $APP_NAME."
    fi
}

waitforstop() {
    savepid=$pid
    CNT=0
    TOTCNT=0
    while [ "X$pid" != "X" ]
    do
        # Show a waiting message every 5 seconds.
        if [ "$CNT" -lt "5" ]
        then
            CNT=`expr $CNT + 1`
        else
            echo "Waiting for $APP_NAME to exit..."
            CNT=0
        fi
        TOTCNT=`expr $TOTCNT + 1`

        sleep 1

        testpid
    done

    pid=$savepid
    testpid
    if [ "X$pid" != "X" ]
    then
        echo "Failed to stop $APP_NAME."
        exit 1
    else
        echo "Stopped $APP_NAME."
    fi
}

stopit() {
    echo "Gracefully stopping $APP_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_NAME was not running."
    else
        kill $pid
        if [ $? -ne 0 ]
        then
            # An explanation for the failure should have been given
            echo "Unable to stop $APP_NAME."
            exit 1
        fi

        waitforstop
    fi
}

forcestopit() {
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_NAME not running"
        exit 1
    fi

    testpid
    if [ "X$pid" != "X" ]
    then
      # start shutdowner from SQ installation directory
      cd "../.."

      echo "Force stopping $APP_NAME..."
      ${JAVA_CMD} -jar "lib/sonar-shutdowner-@sqversion@.jar"

      waitforstop
    fi
}

status() {
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_NAME is not running."
        exit 1
    else
        echo "$APP_NAME is running ($pid)."
        exit 0
    fi
}

dump() {
    echo "Dumping $APP_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_NAME was not running."

    else
        kill -3 $pid

        if [ $? -ne 0 ]
        then
            echo "Failed to dump $APP_NAME."
            exit 1
        else
            echo "Dumped $APP_NAME."
        fi
    fi
}

case "$1" in

    'console')
        console
        ;;

    'start')
        start
        ;;

    'stop')
        stopit
        ;;

    'force-stop')
        forcestopit
        ;;

    'restart')
        stopit
        start
        ;;

    'status')
        status
        ;;

    'dump')
        dump
        ;;

    *)
        echo "Usage: $0 { console | start | stop | force-stop | restart | status | dump }"
        exit 1
        ;;
esac

exit 0
