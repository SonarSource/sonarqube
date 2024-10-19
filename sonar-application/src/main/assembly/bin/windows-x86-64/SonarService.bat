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
rem check if Java is found
set JAVA_EXE=
call "%REALPATH%lib\find_java.bat" set_java_exe FAIL || goto:eof
rem replace JAVA_EXE with the Java path in configuration file
powershell -Command "(Get-Content '%REALPATH%lib\SonarServiceWrapperTemplate.xml') -replace 'JAVA_EXE', '%JAVA_EXE%' | Out-File -encoding ASCII '%REALPATH%lib\SonarServiceWrapper.xml'"

rem call the SonarServiceWrapper.exe passing all the parameters
"%REALPATH%lib\SonarServiceWrapper.exe" %*

endlocal
