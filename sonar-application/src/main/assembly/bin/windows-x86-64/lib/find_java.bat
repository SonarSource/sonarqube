@echo off
call:%~1
goto exit

rem Sets the JAVA_EXE var to be used by the calling script.
rem By default, the java.exe in the PATH is selected. This can be overwritten by the environmental variable SONAR_JAVA_PATH.
rem Returns an error code if Java executable is not found in the PATH and the environmental variable SONAR_JAVA_PATH is not properly set.
:set_java_exe
    rem use java.exe from PATH, by default
    where "java.exe" >nul 2>nul
    if %errorlevel% equ 0 (
        set JAVA_EXE="java.exe"
    )

    rem if the environmental variable SONAR_JAVA_PATH is set, override the default java.exe
    if not "%SONAR_JAVA_PATH%"=="" (
        if exist "%SONAR_JAVA_PATH%" (
            set JAVA_EXE="%SONAR_JAVA_PATH%"
        ) else (
            echo ERROR: "%SONAR_JAVA_PATH%" not found. Please make sure that the environmental variable SONAR_JAVA_PATH points to the Java executable.
            exit /b 1
        )
    )

    if [%JAVA_EXE%]==[] (
        echo ERROR: java.exe not found. Please make sure that the environmental variable SONAR_JAVA_PATH points to the Java executable.
        exit /b 1
    )
    exit /b 0

:exit
exit /b