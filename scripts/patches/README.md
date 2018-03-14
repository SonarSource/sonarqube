Patches
=======

This directory contains patches to be used by any script which uses function `call_patches` from `patches_utils.sh`.

Patches are files with extension `.sh`. Name of the patch is the name of the file without extension (so name of patch "debug.sh" is "debug").

Patches run in the same directory as the calling script provided `patches_utils.sh` has been sourced in the calling script.

Patches are passed on to `start.sh` script using command line argument `-p` by their name.
More than one can be specified using comma as separator.
Each patch is invoked with one argument:

* `SQ_HOME`: path to the home of the started SQ instance

To enable custom patches not part of this directory, set the environment variable `SONARQUBE_USER_PATCHES_HOME` to the directory of the scripts.
If a script with the same name exists in *this* directory and under your custom script directory,
the one in the custom script directory will be used.


Example scripts
---------------

### Debug

    #!/usr/bin/env bash
    #
    # sets property sonar.web.javaAdditionalOpts in sonar.properties to activate debug
    #

    set -euo pipefail

    source scripts/property_utils.sh

    SQ_HOME=$1

    port=5005
    echo "enabling debug in conf/sonar.properties, listening on port $port"
    set_property sonar.web.javaAdditionalOpts "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$port" "$SQ_HOME/conf/sonar.properties"

### Views

    #!/usr/bin/env bash
    #
    # copies the sonar-views plugin jar to the extension directory
    #

    set -euo pipefail

    source scripts/property_utils.sh

    SQ_HOME=$1

    VIEWS_FILE=~/DEV/views/target/sonar-views-plugin-2.9-SNAPSHOT.jar
    EXT_DIR=$SQ_HOME/extensions/plugins
    cp -v "$VIEWS_FILE" "$EXT_DIR"
