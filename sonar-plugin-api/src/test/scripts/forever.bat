@ECHO OFF

:LOOP
  @ping 127.0.0.1 -n 2 -w 1000 > nul
GOTO LOOP
