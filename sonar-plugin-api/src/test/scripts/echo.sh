#!/bin/sh

WORKING_DIR=`pwd`
echo $WORKING_DIR > echo.log
echo "Parameter: $1" >> echo.log
echo "Environment variable: $ENVVAR" >> echo.log
