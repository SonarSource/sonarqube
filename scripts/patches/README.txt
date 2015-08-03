This directory contains patches to be used by the any script which use function call_patches from patches_utils.sh.

Patches are files with extension ".sh". Name of the patch is the name of the file without extension (so name of patch "debug.sh" is "debug").

Patches run in the same directory as the calling script provided patches_utils.sh as been sourced in the calling script.

Patches are passed on to start.sh script using command line argument "-p" by their name.
More than one can be specified using a colon.
Each patch is invoked with one argument:
 * SQ_HOME: the path to the home of the started SQ instance


************************************************************************************************************************
                         sample and common scripts are provided below
************************************************************************************************************************



******************************************      start of debug.sh      ******************************************
#!/bin/bash
###############################
# sets property sonar.web.javaAdditionalOpts in sonar.properties to activate debug
###############################

set -euo pipefail

source scripts/property_utils.sh

SQ_HOME=$1

echo "enabling debug in conf/sonar.properties, listening on port 5005"
set_property sonar.web.javaAdditionalOpts -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 $SQ_HOME/conf/sonar.properties
******************************************       end of debug.sh       ******************************************


******************************************      start of views.sh      ******************************************
#!/bin/bash
###############################
# copies the sonar-views plugin jar to the extension directory
###############################

set -euo pipefail

source scripts/property_utils.sh

SQ_HOME=$1

VIEWS_FILE=~/DEV/views/target/sonar-views-plugin-2.9-SNAPSHOT.jar
EXT_DIR=$SQ_HOME/extensions/plugins/
echo "copy $VIEWS_FILE to $EXT_DIR"
cp  $VIEWS_FILE $EXT_DIR
******************************************       end of views.sh       ******************************************
