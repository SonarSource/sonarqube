@ECHO OFF

:LOOP
  @rem Next line may lead to freeze of build process on Windows 7 due to non-terminated ping-processes
  @rem ping 1.1.1.1 -n 2 -w 60000 > nul
GOTO LOOP
