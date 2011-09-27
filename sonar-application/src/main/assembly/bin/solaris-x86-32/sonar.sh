#! /bin/sh

#
# Copyright (c) 1999, 2006 Tanuki Software Inc.
#
# Java Service Wrapper sh script.  Suitable for starting and stopping
#  wrapped Java applications on UNIX platforms.
#

#-----------------------------------------------------------------------------
# These settings can be modified to fit the needs of your application

# Default values for the Application variables, below. 
# 
# NOTE: The build for specific applications may override this during the resource-copying
# phase, to fill in a concrete name and avoid the use of the defaults specified here.
DEF_APP_NAME="sonar"
DEF_APP_LONG_NAME="sonar"

# Application
APP_NAME="${DEF_APP_NAME}"
APP_LONG_NAME="${DEF_APP_LONG_NAME}"

# Wrapper
WRAPPER_CMD="./wrapper"
WRAPPER_CONF="../../conf/wrapper.conf"

# Priority at which to run the wrapper.  See "man nice" for valid priorities.
#  nice is only used if a priority is specified.
PRIORITY=

# Location of the pid file.
PIDDIR="."

# If uncommented, causes the Wrapper to be shutdown using an anchor file.
#  When launched with the 'start' command, it will also ignore all INT and
#  TERM signals.
#IGNORE_SIGNALS=true

# If specified, the Wrapper will be run as the specified user.
# IMPORTANT - Make sure that the user has the required privileges to write
#  the PID file and wrapper.log files.  Failure to be able to write the log
#  file will cause the Wrapper to exit without any way to write out an error
#  message.
# NOTE - This will set the user which is used to run the Wrapper as well as
#  the JVM and is not useful in situations where a privileged resource or
#  port needs to be allocated prior to the user being changed.
#RUN_AS_USER=nexus

# The following two lines are used by the chkconfig command. Change as is
#  appropriate for your application.  They should remain commented.
# chkconfig: 2345 20 80
# description: Test Wrapper Sample Application

# Do not modify anything beyond this point
#-----------------------------------------------------------------------------

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
REALDIR=`pwd`

# If the PIDDIR is relative, set its value relative to the full REALPATH to avoid problems if
#  the working directory is later changed.
FIRST_CHAR=`echo $PIDDIR | cut -c1,1`
if [ "$FIRST_CHAR" != "/" ]
then
    PIDDIR=$REALDIR/$PIDDIR
fi
# Same test for WRAPPER_CMD
FIRST_CHAR=`echo $WRAPPER_CMD | cut -c1,1`
if [ "$FIRST_CHAR" != "/" ]
then
    WRAPPER_CMD=$REALDIR/$WRAPPER_CMD
fi
# Same test for WRAPPER_CONF
FIRST_CHAR=`echo $WRAPPER_CONF | cut -c1,1`
if [ "$FIRST_CHAR" != "/" ]
then
    WRAPPER_CONF=$REALDIR/$WRAPPER_CONF
fi

# Process ID
ANCHORFILE="$PIDDIR/$APP_NAME.anchor"
PIDFILE="$PIDDIR/$APP_NAME.pid"
LOCKDIR="/var/lock/subsys"
LOCKFILE="$LOCKDIR/$APP_NAME"
pid=""

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

# Resolve the os
DIST_OS=`uname -s | tr [:upper:] [:lower:] | tr -d [:blank:]`
case "$DIST_OS" in
    'sunos')
        DIST_OS="solaris"
        ;;
    'hp-ux' | 'hp-ux64')
        DIST_OS="hpux"
        ;;
    'darwin')
        DIST_OS="macosx"
        ;;
    'unix_sv')
        DIST_OS="unixware"
        ;;
esac

# Resolve the architecture
DIST_ARCH=`uname -p | tr [:upper:] [:lower:] | tr -d [:blank:]`
if [ "$DIST_ARCH" = "unknown" ]
then
    DIST_ARCH=`uname -m | tr [:upper:] [:lower:] | tr -d [:blank:]`
fi
case "$DIST_ARCH" in
    'amd64' | 'athlon' | 'ia32' | 'ia64' | 'i386' | 'i486' | 'i586' | 'i686' | 'x86_64')
        DIST_ARCH="x86"
        ;;
    'ip27')
        DIST_ARCH="mips"
        ;;
    'power' | 'powerpc' | 'power_pc' | 'ppc64')
        DIST_ARCH="ppc"
        ;;
    'pa_risc' | 'pa-risc')
        DIST_ARCH="parisc"
        ;;
    'sun4u' | 'sparcv9')
        DIST_ARCH="sparc"
        ;;
    '9000/800')
        DIST_ARCH="parisc"
        ;;
esac

outputFile() {
    if [ -f "$1" ]
    then
        echo "  $1 (Found but not executable.)";
    else
        echo "  $1"
    fi
}

# Decide on the wrapper binary to use.
# If a 32-bit wrapper binary exists then it will work on 32 or 64 bit
#  platforms, if the 64-bit binary exists then the distribution most
#  likely wants to use long names.  Otherwise, look for the default.
# For macosx, we also want to look for universal binaries.
WRAPPER_TEST_CMD="$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-32"
if [ -x "$WRAPPER_TEST_CMD" ]
then
    WRAPPER_CMD="$WRAPPER_TEST_CMD"
else
    if [ "$DIST_OS" = "macosx" ]
    then
        WRAPPER_TEST_CMD="$WRAPPER_CMD-$DIST_OS-universal-32"
        if [ -x "$WRAPPER_TEST_CMD" ]
        then
            WRAPPER_CMD="$WRAPPER_TEST_CMD"
        else
            WRAPPER_TEST_CMD="$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-64"
            if [ -x "$WRAPPER_TEST_CMD" ]
            then
                WRAPPER_CMD="$WRAPPER_TEST_CMD"
            else
                WRAPPER_TEST_CMD="$WRAPPER_CMD-$DIST_OS-universal-64"
                if [ -x "$WRAPPER_TEST_CMD" ]
                then
                    WRAPPER_CMD="$WRAPPER_TEST_CMD"
                else
                    if [ ! -x "$WRAPPER_CMD" ]
                    then
                        echo "Unable to locate any of the following binaries:"
                        outputFile "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-32"
                        outputFile "$WRAPPER_CMD-$DIST_OS-universal-32"
                        outputFile "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-64"
                        outputFile "$WRAPPER_CMD-$DIST_OS-universal-64"
                        outputFile "$WRAPPER_CMD"
                        exit 1
                    fi
                fi
            fi
        fi
    else
        WRAPPER_TEST_CMD="$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-64"
        if [ -x "$WRAPPER_TEST_CMD" ]
        then
            WRAPPER_CMD="$WRAPPER_TEST_CMD"
        else
            if [ ! -x "$WRAPPER_CMD" ]
            then
                echo "Unable to locate any of the following binaries:"
                outputFile "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-32"
                outputFile "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-64"
                outputFile "$WRAPPER_CMD"
                exit 1
            fi
        fi
    fi
fi

# Build the nice clause
if [ "X$PRIORITY" = "X" ]
then
    CMDNICE=""
else
    CMDNICE="nice -$PRIORITY"
fi

# Build the anchor file clause.
if [ "X$IGNORE_SIGNALS" = "X" ]
then
   ANCHORPROP=
   IGNOREPROP=
else
   ANCHORPROP=wrapper.anchorfile=\"$ANCHORFILE\"
   IGNOREPROP=wrapper.ignore_signals=TRUE
fi

# Build the lock file clause.  Only create a lock file if the lock directory exists on this platform.
LOCKPROP=
if [ -d $LOCKDIR ]
then
    if [ -w $LOCKDIR ]
    then
        LOCKPROP=wrapper.lockfile=\"$LOCKFILE\"
    fi
fi

checkUser() {
    # $1 touchLock flag
    # $2 command

    # Check the configured user.  If necessary rerun this script as the desired user.
    if [ "X$RUN_AS_USER" != "X" ]
    then
        # Resolve the location of the 'id' command
        IDEXE="/usr/xpg4/bin/id"
        if [ ! -x "$IDEXE" ]
        then
            IDEXE="/usr/bin/id"
            if [ ! -x "$IDEXE" ]
            then
                echo "Unable to locate 'id'."
                echo "Please report this message along with the location of the command on your system."
                exit 1
            fi
        fi
    
        if [ "`$IDEXE -u -n`" = "$RUN_AS_USER" ]
        then
            # Already running as the configured user.  Avoid password prompts by not calling su.
            RUN_AS_USER=""
        fi
    fi
    if [ "X$RUN_AS_USER" != "X" ]
    then
        # If LOCKPROP and $RUN_AS_USER are defined then the new user will most likely not be
        # able to create the lock file.  The Wrapper will be able to update this file once it
        # is created but will not be able to delete it on shutdown.  If $2 is defined then
        # the lock file should be created for the current command
        if [ "X$LOCKPROP" != "X" ]
        then
            if [ "X$1" != "X" ]
            then
                # Resolve the primary group 
                RUN_AS_GROUP=`groups $RUN_AS_USER | awk '{print $3}' | tail -1`
                if [ "X$RUN_AS_GROUP" = "X" ]
                then
                    RUN_AS_GROUP=$RUN_AS_USER
                fi
                touch $LOCKFILE
                chown $RUN_AS_USER:$RUN_AS_GROUP $LOCKFILE
            fi
        fi

        # Still want to change users, recurse.  This means that the user will only be
        #  prompted for a password once. Variables shifted by 1
        sudo -u $RUN_AS_USER "$REALPATH" "$2"
        RETVAL=$?

        # Now that we are the original user again, we may need to clean up the lock file.
        if [ "X$LOCKPROP" != "X" ]
        then
            getpid
            if [ "X$pid" = "X" ]
            then
                # Wrapper is not running so make sure the lock file is deleted.
                if [ -f "$LOCKFILE" ]
                then
                    rm "$LOCKFILE"
                fi
            fi
        fi

        exit $RETVAL
    fi
}

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

                dir=`echo ${WRAPPER_CMD}`
                #echo "searching for $dir in command args for PID verification..."

                #echo "args for PID: ${pid}:"
                #echo `$PSEXE -p $pid -o args`

                #echo `$PSEXE -p $pid -o args | grep "$dir"`
                pidtest=`$PSEXE -p $pid -o args | grep "$dir" | tail -1`
                if [ "X$pidtest" = "X" ]
                then
                #    echo "Finding parent of $pid: $PSEXE -p $pid | grep $pid | awk '{print $3}'"
                    parent_pid=`$PSEXE -p $pid -f | grep $pid | awk '{print $3}'`
                #    echo "Parent PID: $parent_pid"

                #    echo "args for PID: ${parent_pid}:"
                #    echo `$PSEXE -p $parent_pid -o args`

                    pidtest=`$PSEXE -p $parent_pid -o args | grep "$dir" | tail -1`
                fi

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
    echo "Running $APP_LONG_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        # The string passed to eval must handles spaces in paths correctly.
        COMMAND_LINE="$CMDNICE \"$WRAPPER_CMD\" \"$WRAPPER_CONF\" wrapper.syslog.ident=$APP_NAME wrapper.pidfile=\"$PIDFILE\" $ANCHORPROP $LOCKPROP"
        eval $COMMAND_LINE
    else
        echo "$APP_LONG_NAME is already running."
        exit 1
    fi
}
 
start() {
    echo "Starting $APP_LONG_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        # The string passed to eval must handles spaces in paths correctly.
        COMMAND_LINE="$CMDNICE \"$WRAPPER_CMD\" \"$WRAPPER_CONF\" wrapper.syslog.ident=$APP_NAME wrapper.pidfile=\"$PIDFILE\" wrapper.daemonize=TRUE $ANCHORPROP $IGNOREPROP $LOCKPROP"
        eval $COMMAND_LINE
    else
        echo "$APP_LONG_NAME is already running."
        exit 1
    fi
}
 
stopit() {
    echo "Stopping $APP_LONG_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME was not running."
    else
        if [ "X$IGNORE_SIGNALS" = "X" ]
        then
            # Running so try to stop it.
            kill $pid
            if [ $? -ne 0 ]
            then
                # An explanation for the failure should have been given
                echo "Unable to stop $APP_LONG_NAME."
                exit 1
            fi
        else
            rm -f "$ANCHORFILE"
            if [ -f "$ANCHORFILE" ]
            then
                # An explanation for the failure should have been given
                echo "Unable to stop $APP_LONG_NAME."
                exit 1
            fi
        fi

        # We can not predict how long it will take for the wrapper to
        #  actually stop as it depends on settings in wrapper.conf.
        #  Loop until it does.
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
                echo "Waiting for $APP_LONG_NAME to exit..."
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
            echo "Failed to stop $APP_LONG_NAME."
            exit 1
        else
            echo "Stopped $APP_LONG_NAME."
        fi
    fi
}

status() {
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME is not running."
        exit 1
    else
        echo "$APP_LONG_NAME is running ($pid)."
        exit 0
    fi
}

dump() {
    echo "Dumping $APP_LONG_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME was not running."

    else
        kill -3 $pid

        if [ $? -ne 0 ]
        then
            echo "Failed to dump $APP_LONG_NAME."
            exit 1
        else
            echo "Dumped $APP_LONG_NAME."
        fi
    fi
}

case "$1" in

    'console')
        checkUser touchlock $1
        console
        ;;

    'start')
        checkUser touchlock $1
        start
        ;;

    'stop')
        checkUser "" $1
        stopit
        ;;

    'restart')
        checkUser touchlock $1
        stopit
        start
        ;;

    'status')
        checkUser "" $1
        status
        ;;

    'dump')
        checkUser "" $1
        dump
        ;;

    *)
        echo "Usage: $0 { console | start | stop | restart | status | dump }"
        exit 1
        ;;
esac

exit 0
