      *   Micro Focus BMS Screen Painter (ver MFBM 2.0.11)
      *   MapSet Name   MNTSET2
      *   Date Created  04/16/2001
      *   Time Created  14:38:47

      *  Input Data For Map MNTMAP1
         01 MNTMAP1I.
            03 FILLER                         PIC X(12).
            03 TRANID1L                       PIC S9(4) COMP.
            03 TRANID1F                       PIC X.
            03 FILLER REDEFINES TRANID1F.
               05 TRANID1A                       PIC X.
            03 FILLER                         PIC X(2).
            03 TRANID1I                       PIC X(4).
            03 CUSTNO1L                       PIC S9(4) COMP.
            03 CUSTNO1F                       PIC X.
            03 FILLER REDEFINES CUSTNO1F.
               05 CUSTNO1A                       PIC X.
            03 FILLER                         PIC X(2).
            03 CUSTNO1I                       PIC X(6).
            03 ACTIONL                        PIC S9(4) COMP.
            03 ACTIONF                        PIC X.
            03 FILLER REDEFINES ACTIONF.
               05 ACTIONA                        PIC X.
            03 FILLER                         PIC X(2).
            03 ACTIONI                        PIC X(1).
            03 MSG1L                          PIC S9(4) COMP.
            03 MSG1F                          PIC X.
            03 FILLER REDEFINES MSG1F.
               05 MSG1A                          PIC X.
            03 FILLER                         PIC X(2).
            03 MSG1I                          PIC X(79).
            03 DUMMY1L                        PIC S9(4) COMP.
            03 DUMMY1F                        PIC X.
            03 FILLER REDEFINES DUMMY1F.
               05 DUMMY1A                        PIC X.
            03 FILLER                         PIC X(2).
            03 DUMMY1I                        PIC X(1).

      *  Output Data For Map MNTMAP1
         01 MNTMAP1O REDEFINES MNTMAP1I.
            03 FILLER                         PIC X(12).
            03 FILLER                         PIC X(3).
            03 TRANID1C                       PIC X.
            03 TRANID1H                       PIC X.
            03 TRANID1O                       PIC X(4).
            03 FILLER                         PIC X(3).
            03 CUSTNO1C                       PIC X.
            03 CUSTNO1H                       PIC X.
            03 CUSTNO1O                       PIC X(6).
            03 FILLER                         PIC X(3).
            03 ACTIONC                        PIC X.
            03 ACTIONH                        PIC X.
            03 ACTIONO                        PIC X(1).
            03 FILLER                         PIC X(3).
            03 MSG1C                          PIC X.
            03 MSG1H                          PIC X.
            03 MSG1O                          PIC X(79).
            03 FILLER                         PIC X(3).
            03 DUMMY1C                        PIC X.
            03 DUMMY1H                        PIC X.
            03 DUMMY1O                        PIC X(1).

      *  Input Data For Map MNTMAP2
         01 MNTMAP2I.
            03 FILLER                         PIC X(12).
            03 TRANID2L                       PIC S9(4) COMP.
            03 TRANID2F                       PIC X.
            03 FILLER REDEFINES TRANID2F.
               05 TRANID2A                       PIC X.
            03 FILLER                         PIC X(2).
            03 TRANID2I                       PIC X(4).
            03 INSTR2L                        PIC S9(4) COMP.
            03 INSTR2F                        PIC X.
            03 FILLER REDEFINES INSTR2F.
               05 INSTR2A                        PIC X.
            03 FILLER                         PIC X(2).
            03 INSTR2I                        PIC X(79).
            03 CUSTNO2L                       PIC S9(4) COMP.
            03 CUSTNO2F                       PIC X.
            03 FILLER REDEFINES CUSTNO2F.
               05 CUSTNO2A                       PIC X.
            03 FILLER                         PIC X(2).
            03 CUSTNO2I                       PIC X(6).
            03 LNAMEL                         PIC S9(4) COMP.
            03 LNAMEF                         PIC X.
            03 FILLER REDEFINES LNAMEF.
               05 LNAMEA                         PIC X.
            03 FILLER                         PIC X(2).
            03 LNAMEI                         PIC X(30).
            03 FNAMEL                         PIC S9(4) COMP.
            03 FNAMEF                         PIC X.
            03 FILLER REDEFINES FNAMEF.
               05 FNAMEA                         PIC X.
            03 FILLER                         PIC X(2).
            03 FNAMEI                         PIC X(20).
            03 ADDRL                          PIC S9(4) COMP.
            03 ADDRF                          PIC X.
            03 FILLER REDEFINES ADDRF.
               05 ADDRA                          PIC X.
            03 FILLER                         PIC X(2).
            03 ADDRI                          PIC X(30).
            03 CITYL                          PIC S9(4) COMP.
            03 CITYF                          PIC X.
            03 FILLER REDEFINES CITYF.
               05 CITYA                          PIC X.
            03 FILLER                         PIC X(2).
            03 CITYI                          PIC X(20).
            03 STATEL                         PIC S9(4) COMP.
            03 STATEF                         PIC X.
            03 FILLER REDEFINES STATEF.
               05 STATEA                         PIC X.
            03 FILLER                         PIC X(2).
            03 STATEI                         PIC X(2).
            03 ZIPCODEL                       PIC S9(4) COMP.
            03 ZIPCODEF                       PIC X.
            03 FILLER REDEFINES ZIPCODEF.
               05 ZIPCODEA                       PIC X.
            03 FILLER                         PIC X(2).
            03 ZIPCODEI                       PIC X(10).
            03 MSG2L                          PIC S9(4) COMP.
            03 MSG2F                          PIC X.
            03 FILLER REDEFINES MSG2F.
               05 MSG2A                          PIC X.
            03 FILLER                         PIC X(2).
            03 MSG2I                          PIC X(79).
            03 DUMMY2L                        PIC S9(4) COMP.
            03 DUMMY2F                        PIC X.
            03 FILLER REDEFINES DUMMY2F.
               05 DUMMY2A                        PIC X.
            03 FILLER                         PIC X(2).
            03 DUMMY2I                        PIC X(1).

      *  Output Data For Map MNTMAP2
         01 MNTMAP2O REDEFINES MNTMAP2I.
            03 FILLER                         PIC X(12).
            03 FILLER                         PIC X(3).
            03 TRANID2C                       PIC X.
            03 TRANID2H                       PIC X.
            03 TRANID2O                       PIC X(4).
            03 FILLER                         PIC X(3).
            03 INSTR2C                        PIC X.
            03 INSTR2H                        PIC X.
            03 INSTR2O                        PIC X(79).
            03 FILLER                         PIC X(3).
            03 CUSTNO2C                       PIC X.
            03 CUSTNO2H                       PIC X.
            03 CUSTNO2O                       PIC X(6).
            03 FILLER                         PIC X(3).
            03 LNAMEC                         PIC X.
            03 LNAMEH                         PIC X.
            03 LNAMEO                         PIC X(30).
            03 FILLER                         PIC X(3).
            03 FNAMEC                         PIC X.
            03 FNAMEH                         PIC X.
            03 FNAMEO                         PIC X(20).
            03 FILLER                         PIC X(3).
            03 ADDRC                          PIC X.
            03 ADDRH                          PIC X.
            03 ADDRO                          PIC X(30).
            03 FILLER                         PIC X(3).
            03 CITYC                          PIC X.
            03 CITYH                          PIC X.
            03 CITYO                          PIC X(20).
            03 FILLER                         PIC X(3).
            03 STATEC                         PIC X.
            03 STATEH                         PIC X.
            03 STATEO                         PIC X(2).
            03 FILLER                         PIC X(3).
            03 ZIPCODEC                       PIC X.
            03 ZIPCODEH                       PIC X.
            03 ZIPCODEO                       PIC X(10).
            03 FILLER                         PIC X(3).
            03 MSG2C                          PIC X.
            03 MSG2H                          PIC X.
            03 MSG2O                          PIC X(79).
            03 FILLER                         PIC X(3).
            03 DUMMY2C                        PIC X.
            03 DUMMY2H                        PIC X.
            03 DUMMY2O                        PIC X(1).

