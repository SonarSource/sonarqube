@echo off
echo Arguments Passed In: %*

setlocal enabledelayedexpansion
set "POSITIONAL_ARGS="

:loop
if "%~1"=="" goto endloop
if "%~1"=="--zip-filename" (
  set "FILENAME=%~2"
  shift
  shift
) else (
  set "POSITIONAL_ARGS=!POSITIONAL_ARGS! %~1"
  shift
)
goto loop
:endloop

echo TIDELIFT_SKIP_UPDATE_CHECK=%TIDELIFT_SKIP_UPDATE_CHECK%
echo TIDELIFT_RECURSIVE_MANIFEST_SEARCH=%TIDELIFT_RECURSIVE_MANIFEST_SEARCH%
echo ZIP FILE LOCATION = %FILENAME%
echo. > %FILENAME%
