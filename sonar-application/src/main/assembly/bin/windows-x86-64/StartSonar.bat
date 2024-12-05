@rem SonarQube
@rem Copyright (C) 2009-2024 SonarSource SA
@rem mailto:info AT sonarsource DOT com
@rem
@rem This program is free software; you can redistribute it and/or
@rem modify it under the terms of the GNU Lesser General Public
@rem License as published by the Free Software Foundation; either
@rem version 3 of the License, or (at your option) any later version.
@rem
@rem This program is distributed in the hope that it will be useful,
@rem but WITHOUT ANY WARRANTY; without even the implied warranty of
@rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
@rem Lesser General Public License for more details.
@rem
@rem You should have received a copy of the GNU Lesser General Public License
@rem along with this program; if not, write to the Free Software Foundation,
@rem Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

@echo off
setlocal

rem DO NOT EDIT THE FOLLOWING SECTIONS

set REALPATH=%~dp0

set JAVA_EXE=
call "%REALPATH%lib\find_java.bat" set_java_exe FAIL || goto:eof

call :check_if_sonar_is_running FAIL || goto:eof

echo Starting SonarQube...
%JAVA_EXE% -Xms8m -Xmx32m^
     -Djava.awt.headless=true^
     --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED^
     --add-opens=java.base/java.lang=ALL-UNNAMED^
     --add-opens=java.base/java.nio=ALL-UNNAMED^
     --add-opens=java.base/sun.nio.ch=ALL-UNNAMED^
     --add-opens=java.management/sun.management=ALL-UNNAMED^
     --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED^
     -cp "%REALPATH%..\..\lib\sonar-application-@sqversion@.jar" "org.sonar.application.App"

goto:eof

:check_if_sonar_is_running
    set "SQ_SERVICE="
    for /f  %%i in ('%REALPATH%SonarService.bat status ^>nul 2^>nul') do set "SQ_SERVICE=%%i"
    if [%SQ_SERVICE%]==[Started] (
        echo ERROR: SonarQube is already running as a service.
        exit /b 1
    )

	findstr /m "sonar.cluster.enabled=true" "%REALPATH%\..\..\conf\sonar.properties" >Nul
	if %errorlevel%==0 (
		rem For Data Center Edition we skip the detection of another SonarQube's java.exe process running on the same machine
		exit /b 0
	)
    set "SQ_PROCESS="
    where jps >nul 2>nul
    if %errorlevel% equ 0 (
        rem give priority to jps command if present
        for /f "tokens=1" %%i in ('jps -l ^| findstr "org.sonar.application.App"') do set "SQ_PROCESS=%%i"
    ) else (
        rem fallback to wmic command
        for /f "tokens=2" %%i in ('wmic process where "name='java.exe' and commandline like '%%org.sonar.application.App%%'" get name^, processid 2^>nul ^| findstr "java"') do set "SQ_PROCESS=%%i"
    )

    if not [%SQ_PROCESS%]==[] (
        echo ERROR: Another instance of the SonarQube application is already running with PID %SQ_PROCESS%
        exit /b 1
    )
    exit /b 0

endlocal

