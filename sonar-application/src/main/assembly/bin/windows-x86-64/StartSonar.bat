@rem SonarQube
@rem Copyright (C) 2009-2022 SonarSource SA
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

rem Path to the Java executable
set JAVA_EXE="java"


rem DO NOT EDIT THE FOLLOWING SECTIONS
call :check_if_sonar_is_running FAIL || goto:eof

%JAVA_EXE% -Xms8m -Xmx32m^
     -Djava.awt.headless=true^
     --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED^
     --add-opens=java.base/java.lang=ALL-UNNAMED^
     --add-opens=java.base/java.nio=ALL-UNNAMED^
     --add-opens=java.base/sun.nio.ch=ALL-UNNAMED^
     --add-opens=java.management/sun.management=ALL-UNNAMED^
     --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED^
     -cp "..\..\lib\sonar-application-@sqversion@.jar" "org.sonar.application.App"

goto:eof

:check_if_sonar_is_running
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
goto:eof

endlocal

