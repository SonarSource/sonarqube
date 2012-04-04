#!/bin/sh

echo stdOut: first line
echo stdOut: second line
echo stdErr: first line 1>&2
echo stdErr: second line 1>&2
