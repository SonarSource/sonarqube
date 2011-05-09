@ECHO OFF

:LOOP
  @ping 1.1.1.1 -n 2 -w 60000 > nul
GOTO LOOP
