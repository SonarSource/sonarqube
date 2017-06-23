       IDENTIFICATION DIVISION.
      *
       PROGRAM-ID.  CUSTMNT2.
      *
       ENVIRONMENT DIVISION.
      *
       DATA DIVISION.
      *
       WORKING-STORAGE SECTION.
      *
       01  SWITCHES.
      *
           05  VALID-DATA-SW                 PIC X(01) VALUE 'Y'.
               88  VALID-DATA                          VALUE 'Y'.
      *
       01  FLAGS.
      *
           05  SEND-FLAG                     PIC X(01).
               88  SEND-ERASE                       VALUE '1'.
               88  SEND-ERASE-ALARM                 VALUE '2'.
               88  SEND-DATAONLY                    VALUE '3'.
               88  SEND-DATAONLY-ALARM              VALUE '4'.
      *
       01  WORK-FIELDS.
      *
           05  RESPONSE-CODE                 PIC S9(08) COMP.
      *
       01  USER-INSTRUCTIONS.
      *
           05  ADD-INSTRUCTION               PIC X(79) VALUE
               'Type information for new customer.  Then Press Enter.'.
           05  CHANGE-INSTRUCTION            PIC X(79) VALUE
               'Type changes.  Then press Enter.'.
           05  DELETE-INSTRUCTION            PIC X(79) VALUE
               'Press Enter to delete this customer or press F12 to canc
      -        'el.'.
      *
       01  COMMUNICATION-AREA.
      *
           05  CA-CONTEXT-FLAG               PIC X(01).
               88  PROCESS-KEY-MAP                  VALUE '1'.
               88  PROCESS-ADD-CUSTOMER             VALUE '2'.
               88  PROCESS-CHANGE-CUSTOMER          VALUE '3'.
               88  PROCESS-DELETE-CUSTOMER          VALUE '4'.
           05  CA-CUSTOMER-RECORD.
               10  CA-CUSTOMER-NUMBER        PIC X(06).
               10  FILLER                    PIC X(112).
      *
       COPY CUSTMAS.
      *
       COPY MNTSET2.
      *
       COPY DFHAID.
      *
       COPY ATTR.
      *
       COPY ERRPARM.
      *
       LINKAGE SECTION.
      *
       01  DFHCOMMAREA                       PIC X(119).
      *
       PROCEDURE DIVISION.
      *
       0000-PROCESS-CUSTOMER-MAINT.
      *
           IF EIBCALEN > ZERO
               MOVE DFHCOMMAREA TO COMMUNICATION-AREA
           END-IF.
      *
           EVALUATE TRUE
      *
               WHEN EIBCALEN = ZERO
                   MOVE LOW-VALUE TO MNTMAP1O
                   MOVE -1 TO CUSTNO1L
                   SET SEND-ERASE TO TRUE
                   PERFORM 1500-SEND-KEY-MAP
                   SET PROCESS-KEY-MAP TO TRUE
      *
               WHEN EIBAID = DFHPF3
                   EXEC CICS
                       XCTL PROGRAM('INVMENU')
                   END-EXEC
      *
               WHEN EIBAID = DFHPF12
                   IF PROCESS-KEY-MAP
                       EXEC CICS
                           XCTL PROGRAM('INVMENU')
                       END-EXEC
                   ELSE
                       MOVE LOW-VALUE TO MNTMAP1O
                       MOVE -1 TO CUSTNO1L
                       SET SEND-ERASE TO TRUE
                       PERFORM 1500-SEND-KEY-MAP
                       SET PROCESS-KEY-MAP TO TRUE
                   END-IF
      *
               WHEN EIBAID = DFHCLEAR
                   IF PROCESS-KEY-MAP
                       MOVE LOW-VALUE TO MNTMAP1O
                       MOVE -1 TO CUSTNO1L
                       SET SEND-ERASE TO TRUE
                       PERFORM 1500-SEND-KEY-MAP
                   ELSE
                       MOVE LOW-VALUE TO MNTMAP2O
                       MOVE CA-CUSTOMER-NUMBER TO CUSTNO2O
                       EVALUATE TRUE
                           WHEN PROCESS-ADD-CUSTOMER
                               MOVE ADD-INSTRUCTION    TO INSTR2O
                           WHEN PROCESS-CHANGE-CUSTOMER
                               MOVE CHANGE-INSTRUCTION TO INSTR2O
                           WHEN PROCESS-DELETE-CUSTOMER
                               MOVE DELETE-INSTRUCTION TO INSTR2O
                       END-EVALUATE
                       MOVE -1 TO LNAMEL
                       SET SEND-ERASE TO TRUE
                       PERFORM 1400-SEND-DATA-MAP
                   END-IF
      *
               WHEN EIBAID = DFHPA1 OR DFHPA2 OR DFHPA3
                   CONTINUE
      *
               WHEN EIBAID = DFHENTER
                   EVALUATE TRUE
                       WHEN PROCESS-KEY-MAP
                           PERFORM 1000-PROCESS-KEY-MAP
                       WHEN PROCESS-ADD-CUSTOMER
                           PERFORM 2000-PROCESS-ADD-CUSTOMER
                       WHEN PROCESS-CHANGE-CUSTOMER
                           PERFORM 3000-PROCESS-CHANGE-CUSTOMER
                       WHEN PROCESS-DELETE-CUSTOMER
                           PERFORM 4000-PROCESS-DELETE-CUSTOMER
                   END-EVALUATE
      *
               WHEN OTHER
                   IF PROCESS-KEY-MAP
                       MOVE LOW-VALUE TO MNTMAP1O
                       MOVE 'That key is unassigned.' TO MSG1O
                       MOVE -1 TO CUSTNO1L
                       SET SEND-DATAONLY-ALARM TO TRUE
                       PERFORM 1500-SEND-KEY-MAP
                   ELSE
                       MOVE LOW-VALUE TO MNTMAP2O
                       MOVE 'That key is unassigned.' TO MSG2O
                       MOVE -1 TO LNAMEL
                       SET SEND-DATAONLY-ALARM TO TRUE
                       PERFORM 1400-SEND-DATA-MAP
                   END-IF
      *
           END-EVALUATE.

           EXEC CICS
               RETURN TRANSID('MNT2')
                      COMMAREA(COMMUNICATION-AREA)
           END-EXEC.
      *
       1000-PROCESS-KEY-MAP.
      *
           PERFORM 1100-RECEIVE-KEY-MAP.
           PERFORM 1200-EDIT-KEY-DATA.
           IF VALID-DATA
               IF NOT PROCESS-DELETE-CUSTOMER
                   INSPECT CUSTOMER-MASTER-RECORD
                       REPLACING ALL SPACE BY '_'
               END-IF
               MOVE CUSTNO1I      TO CUSTNO2O
               MOVE CM-LAST-NAME  TO LNAMEO
               MOVE CM-FIRST-NAME TO FNAMEO
               MOVE CM-ADDRESS    TO ADDRO
               MOVE CM-CITY       TO CITYO
               MOVE CM-STATE      TO STATEO
               MOVE CM-ZIP-CODE   TO ZIPCODEO
               MOVE -1 TO LNAMEL
               SET SEND-ERASE TO TRUE
               PERFORM 1400-SEND-DATA-MAP
           ELSE
               MOVE LOW-VALUE TO CUSTNO1O
                                 ACTIONO
               SET SEND-DATAONLY-ALARM TO TRUE
               PERFORM 1500-SEND-KEY-MAP
           END-IF.
      *
       1100-RECEIVE-KEY-MAP.
      *
           EXEC CICS
               RECEIVE MAP('MNTMAP1')
                       MAPSET('MNTSET2')
                       INTO(MNTMAP1I)
           END-EXEC.
      *
           INSPECT MNTMAP1I
               REPLACING ALL '_' BY SPACE.
      *
       1200-EDIT-KEY-DATA.
      *
           MOVE ATTR-NO-HIGHLIGHT TO ACTIONH
                                     CUSTNO1H.
      *
           IF ACTIONI NOT = '1' AND '2' AND '3' AND '4' AND '5'
               MOVE ATTR-REVERSE TO ACTIONH
               MOVE -1 TO ACTIONL
               MOVE 'Action must be 1, 2, or 3.' TO MSG1O
               MOVE 'N' TO VALID-DATA-SW
           END-IF.
      *
           IF       CUSTNO1L = ZERO
                 OR CUSTNO1I = SPACE
               MOVE ATTR-REVERSE TO CUSTNO1H
               MOVE -1 TO CUSTNO1L
               MOVE 'You must enter a customer number.' TO MSG1O
               MOVE 'N' TO VALID-DATA-SW
           END-IF.
      *
           IF VALID-DATA
               MOVE LOW-VALUE TO MNTMAP2O
               EVALUATE ACTIONI
                   WHEN '1'
                       PERFORM 1300-READ-CUSTOMER-RECORD
                       IF RESPONSE-CODE = DFHRESP(NOTFND)
                           MOVE ADD-INSTRUCTION TO INSTR2O
                           SET PROCESS-ADD-CUSTOMER TO TRUE
                           MOVE SPACE TO CUSTOMER-MASTER-RECORD
                       ELSE
                           IF RESPONSE-CODE = DFHRESP(NORMAL)
                               MOVE 'That customer already exists.'
                                    TO MSG1O
                               MOVE 'N' TO VALID-DATA-SW
                           END-IF
                       END-IF
                   WHEN '2'
                       PERFORM 1300-READ-CUSTOMER-RECORD
                       IF RESPONSE-CODE = DFHRESP(NORMAL)
                           MOVE CUSTOMER-MASTER-RECORD TO
                               CA-CUSTOMER-RECORD
                           MOVE CHANGE-INSTRUCTION TO INSTR2O
                           SET PROCESS-CHANGE-CUSTOMER TO TRUE
                       ELSE
                           IF RESPONSE-CODE = DFHRESP(NOTFND)
                               MOVE 'That customer does not exist.' TO
                                   MSG1O
                               MOVE 'N' TO VALID-DATA-SW
                           END-IF
                       END-IF
                   WHEN '3'
                       PERFORM 1300-READ-CUSTOMER-RECORD
                       IF RESPONSE-CODE = DFHRESP(NORMAL)
                           MOVE CUSTOMER-MASTER-RECORD TO
                               CA-CUSTOMER-RECORD
                           MOVE DELETE-INSTRUCTION TO INSTR2O
                           SET PROCESS-DELETE-CUSTOMER TO TRUE
                           MOVE ATTR-PROT TO LNAMEA
                                             FNAMEA
                                             ADDRA
                                             CITYA
                                             STATEA
                                             ZIPCODEA
                       ELSE
                           IF RESPONSE-CODE = DFHRESP(NOTFND)
                               MOVE 'That customer does not exist.' TO
                                   MSG1O
                               MOVE 'N' TO VALID-DATA-SW
                           END-IF
                       END-IF
               END-EVALUATE.
      *
       1300-READ-CUSTOMER-RECORD.
      *
           EXEC CICS
               READ FILE('CUSTMAS')
                    INTO(CUSTOMER-MASTER-RECORD)
                    RIDFLD(CUSTNO1I)
                    RESP(RESPONSE-CODE)
           END-EXEC.
      *
           IF      RESPONSE-CODE NOT = DFHRESP(NORMAL)
               AND RESPONSE-CODE NOT = DFHRESP(NOTFND)
               PERFORM 9999-TERMINATE-PROGRAM
           END-IF.
      *
       1400-SEND-DATA-MAP.
      *
           MOVE 'MNT2' TO TRANID2O.
      *
           EVALUATE TRUE
               WHEN SEND-ERASE
                   EXEC CICS
                       SEND MAP('MNTMAP2')
                            MAPSET('MNTSET2')
                            FROM(MNTMAP2O)
                            ERASE
                            CURSOR
                   END-EXEC
               WHEN SEND-DATAONLY-ALARM
                   EXEC CICS
                       SEND MAP('MNTMAP2')
                            MAPSET('MNTSET2')
                            FROM(MNTMAP2O)
                            DATAONLY
                            ALARM
                            CURSOR
               END-EXEC
           END-EVALUATE.
      *
       1500-SEND-KEY-MAP.
      *
           MOVE 'MNT2' TO TRANID1O.
      *
           EVALUATE TRUE
               WHEN SEND-ERASE
                   EXEC CICS
                       SEND MAP('MNTMAP1')
                            MAPSET('MNTSET2')
                            FROM(MNTMAP1O)
                            ERASE
                            CURSOR
                   END-EXEC
               WHEN SEND-ERASE-ALARM
                   EXEC CICS
                       SEND MAP('MNTMAP1')
                            MAPSET('MNTSET2')
                            FROM(MNTMAP1O)
                            ERASE
                            ALARM
                            CURSOR
                   END-EXEC
               WHEN SEND-DATAONLY-ALARM
                   EXEC CICS
                       SEND MAP('MNTMAP1')
                            MAPSET('MNTSET2')
                            FROM(MNTMAP1O)
                            DATAONLY
                            ALARM
                            CURSOR
                   END-EXEC
           END-EVALUATE.
      *
       2000-PROCESS-ADD-CUSTOMER.
      *
           PERFORM 2100-RECEIVE-DATA-MAP.
           PERFORM 2200-EDIT-CUSTOMER-DATA.
           IF VALID-DATA
               PERFORM 2300-WRITE-CUSTOMER-RECORD
               IF RESPONSE-CODE = DFHRESP(NORMAL)
                   MOVE 'Customer record added.' TO MSG1O
                   SET SEND-ERASE TO TRUE
               ELSE
                   IF RESPONSE-CODE = DFHRESP(DUPREC)
                       MOVE 'Another user has added a record with that c
      -                    'ustomer number.' TO MSG1O
                       SET SEND-ERASE-ALARM TO TRUE
                   END-IF
               END-IF
               MOVE -1 TO CUSTNO1L
               PERFORM 1500-SEND-KEY-MAP
               SET PROCESS-KEY-MAP TO TRUE
           ELSE
               MOVE LOW-VALUE TO LNAMEO
                                 FNAMEO
                                 ADDRO
                                 CITYO
                                 STATEO
                                 ZIPCODEO
               SET SEND-DATAONLY-ALARM TO TRUE
               PERFORM 1400-SEND-DATA-MAP
           END-IF.
      *
       2100-RECEIVE-DATA-MAP.
      *
           EXEC CICS
               RECEIVE MAP('MNTMAP2')
                       MAPSET('MNTSET2')
                       INTO(MNTMAP2I)
           END-EXEC.
      *
           INSPECT MNTMAP2I
               REPLACING ALL '_' BY SPACE.
      *
       2200-EDIT-CUSTOMER-DATA.
      *
           MOVE ATTR-NO-HIGHLIGHT TO ZIPCODEH
                                     STATEH
                                     CITYH
                                     ADDRH
                                     FNAMEH
                                     LNAMEH.

           IF       ZIPCODEI = SPACE
                 OR ZIPCODEL = ZERO
               MOVE ATTR-REVERSE TO ZIPCODEH
               MOVE -1 TO ZIPCODEL
               MOVE 'You must enter a zip code.' TO MSG2O
               MOVE 'N' TO VALID-DATA-SW
           END-IF.

           IF       STATEI = SPACE
                 OR STATEL = ZERO
               MOVE ATTR-REVERSE TO STATEH
               MOVE -1 TO STATEL
               MOVE 'You must enter a state.' TO MSG2O
               MOVE 'N' TO VALID-DATA-SW
           END-IF.

           IF       CITYI = SPACE
                 OR CITYL = ZERO
               MOVE ATTR-REVERSE TO CITYH
               MOVE -1 TO CITYL
               MOVE 'You must enter a city.' TO MSG2O
               MOVE 'N' TO VALID-DATA-SW
           END-IF.

           IF       ADDRI = SPACE
                 OR ADDRL = ZERO
               MOVE ATTR-REVERSE TO ADDRH
               MOVE -1 TO ADDRL
               MOVE 'You must enter an address.' TO MSG2O
               MOVE 'N' TO VALID-DATA-SW
           END-IF.

           IF       FNAMEI = SPACE
                 OR FNAMEL = ZERO
               MOVE ATTR-REVERSE TO FNAMEH
               MOVE -1 TO FNAMEL
               MOVE 'You must enter a first name.' TO MSG2O
               MOVE 'N' TO VALID-DATA-SW
           END-IF.

           IF       LNAMEI = SPACE
                 OR LNAMEL = ZERO
               MOVE ATTR-REVERSE TO LNAMEH
               MOVE -1 TO LNAMEL
               MOVE 'You must enter a last name.' TO MSG2O
               MOVE 'N' TO VALID-DATA-SW
           END-IF.
      *
       2300-WRITE-CUSTOMER-RECORD.
      *
           MOVE CUSTNO2I TO CM-CUSTOMER-NUMBER.
           MOVE LNAMEI   TO CM-LAST-NAME.
           MOVE FNAMEI   TO CM-FIRST-NAME.
           MOVE ADDRI    TO CM-ADDRESS.
           MOVE CITYI    TO CM-CITY.
           MOVE STATEI   TO CM-STATE.
           MOVE ZIPCODEI TO CM-ZIP-CODE.
      *
           EXEC CICS
               WRITE FILE('CUSTMAS')
                     FROM(CUSTOMER-MASTER-RECORD)
                     RIDFLD(CM-CUSTOMER-NUMBER)
                     RESP(RESPONSE-CODE)
           END-EXEC.
      *
           IF      RESPONSE-CODE NOT = DFHRESP(NORMAL)
               AND RESPONSE-CODE NOT = DFHRESP(DUPREC)
               PERFORM 9999-TERMINATE-PROGRAM
           END-IF.
      *
       3000-PROCESS-CHANGE-CUSTOMER.
      *
           PERFORM 2100-RECEIVE-DATA-MAP.
           PERFORM 2200-EDIT-CUSTOMER-DATA.
           IF VALID-DATA
               MOVE CUSTNO2I TO CM-CUSTOMER-NUMBER
               PERFORM 3100-READ-CUSTOMER-FOR-UPDATE
               IF RESPONSE-CODE = DFHRESP(NORMAL)
                   IF CUSTOMER-MASTER-RECORD = CA-CUSTOMER-RECORD
      * Introduce extra nested if as an example of rule violation 
                       IF VALID-DATA
                           IF RESPONSE-CODE = DFHRESP(NORMAL)
                               PERFORM 3200-REWRITE-CUSTOMER-RECORD
                               MOVE 'Customer record updated.' TO MSG1O
                               SET SEND-ERASE TO TRUE
                           END-IF
                       END-IF
                   ELSE
                       MOVE 'Another user has updated the record.  Try a
      -                     'gain.' TO MSG1O
                       SET SEND-ERASE-ALARM TO TRUE
                   END-IF
               ELSE
                   IF RESPONSE-CODE = DFHRESP(NOTFND)
                       MOVE 'Another user has deleted the record.' TO
                           MSG1O
                       SET SEND-ERASE-ALARM TO TRUE
                   END-IF
               END-IF
               MOVE -1 TO CUSTNO1L
               PERFORM 1500-SEND-KEY-MAP
               SET PROCESS-KEY-MAP TO TRUE
           ELSE
               MOVE LOW-VALUE TO LNAMEO
                                 FNAMEO
                                 ADDRO
                                 CITYO
                                 STATEO
                                 ZIPCODEO
               SET SEND-DATAONLY-ALARM TO TRUE
               PERFORM 1400-SEND-DATA-MAP
           END-IF.
      *
       3100-READ-CUSTOMER-FOR-UPDATE.
      *
           EXEC CICS
               READ FILE('CUSTMAS')
                    INTO(CUSTOMER-MASTER-RECORD)
                    RIDFLD(CM-CUSTOMER-NUMBER)
                    UPDATE
                    RESP(RESPONSE-CODE)
           END-EXEC.
      *
           IF      RESPONSE-CODE NOT = DFHRESP(NORMAL)
               AND RESPONSE-CODE NOT = DFHRESP(NOTFND)
               PERFORM 9999-TERMINATE-PROGRAM
           END-IF.
      *
       3200-REWRITE-CUSTOMER-RECORD.
      *
           MOVE LNAMEI   TO CM-LAST-NAME.
           MOVE FNAMEI   TO CM-FIRST-NAME.
           MOVE ADDRI    TO CM-ADDRESS.
           MOVE CITYI    TO CM-CITY.
           MOVE STATEI   TO CM-STATE.
           MOVE ZIPCODEI TO CM-ZIP-CODE.
      *
           EXEC CICS
               REWRITE FILE('CUSTMAS')
                       FROM(CUSTOMER-MASTER-RECORD)
                       RESP(RESPONSE-CODE)
           END-EXEC.
      *
           IF RESPONSE-CODE NOT = DFHRESP(NORMAL)
               PERFORM 9999-TERMINATE-PROGRAM
           END-IF.
      *
       4000-PROCESS-DELETE-CUSTOMER.
      *
           MOVE CA-CUSTOMER-NUMBER TO CM-CUSTOMER-NUMBER.
           PERFORM 3100-READ-CUSTOMER-FOR-UPDATE.
           IF RESPONSE-CODE = DFHRESP(NORMAL)
               ALTER X TO PROCEED TO Y
               IF CUSTOMER-MASTER-RECORD = CA-CUSTOMER-RECORD
                   PERFORM 4100-DELETE-CUSTOMER-RECORD
                   MOVE 'Customer deleted.' TO MSG1O
                   SET SEND-ERASE TO TRUE
               ELSE
                   MOVE 'Another user has updated the record.  Try again
      -                 '.' TO MSG1O
                   SET SEND-ERASE-ALARM TO TRUE
               END-IF
           ELSE
               IF RESPONSE-CODE = DFHRESP(NOTFND)
                   MOVE 'Another user has deleted the record.' TO
                       MSG1O
                   SET SEND-ERASE-ALARM TO TRUE
               END-IF
           END-IF.
           MOVE -1 TO CUSTNO1L.
           PERFORM 1500-SEND-KEY-MAP.
           SET PROCESS-KEY-MAP TO TRUE.
      *
       4100-DELETE-CUSTOMER-RECORD.
      * TODO Some comment
           EXEC CICS
               DELETE FILE('CUSTMAS')
                      RESP(RESPONSE-CODE)
           END-EXEC.
      *
           IF  RESPONSE-CODE NOT = DFHRESP(NORMAL)
               PERFORM 9999-TERMINATE-PROGRAM
           END-IF.
      *
       9999-TERMINATE-PROGRAM.
      *
           MOVE EIBRESP  TO ERR-RESP.
           MOVE EIBRESP2 TO ERR-RESP2.
           MOVE EIBTRNID TO ERR-TRNID.
           MOVE EIBRSRCE TO ERR-RSRCE.
      *
           EXEC CICS
               XCTL PROGRAM('SYSERR')
                    COMMAREA(ERROR-PARAMETERS)
           END-EXEC.
