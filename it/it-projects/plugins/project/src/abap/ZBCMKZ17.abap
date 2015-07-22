REPORT ZBCMKZ17.
*----------------------------------------------------------------------*
* Description:   Report and Transaction Starter                        *
*                It shows an individual list of reports/Trans. to start*
*                                                                      *
* Authorization: S_PROGRAM, Reports starten                            *
*                                                                      *
* Class:         Utility                                               *
*                                                                      *
* Customizing:   Needs Customer Table: ZBCMKZ1                         *
*  Field:                Key Type        Length    Descr.              *
*  ZBCMKZ1-BNAME         X   CHAR C     12         User name           *
*  ZBCMKZ1-NAME          X   CHAR C      8         Report/Trans. code  *
*  ZBCMKZ1-NUMMER            INT1 X      1         Priority level      *
*                                                                      *
* R/3 Release:   3.0d                                                  *
*                                                                      *
* Programmer:    Bence Toth                                            *
* Date:          1997 April                                            *
*                                                                      *
*----------------------------------------------------------------------*
INCLUDE: <ICON>.
TABLES: ZBCMKZ1, TRDIR, TSTCT, TSTC.
DATA: BEGIN OF BTAB OCCURS 50,         "Hilfstabelle fuer Textpool
            CODE(82),
      END OF BTAB.
DATA: BEGIN OF T OCCURS 100,
     NUMMER LIKE ZBCMKZ1-NUMMER,
     NAME LIKE TRDIR-NAME,
            CODE(82),
END OF T.
DATA: FI(20).
DATA BEGIN OF BDCDATA OCCURS 100.
        INCLUDE STRUCTURE BDCDATA.
DATA END OF BDCDATA.

DATA BEGIN OF MESSTAB OCCURS 10.
        INCLUDE STRUCTURE BDCMSGCOLL.
DATA END OF MESSTAB.

DATA REPORT.
AUTHORITY-CHECK OBJECT 'S_PROGRAM'
         ID 'P_GROUP' FIELD '*'
         ID 'P_ACTION' FIELD '*'.
IF SY-SUBRC NE 0. EXIT. ENDIF.
WRITE: /2 'Er. Modus', 12 'Name', 22 'Text'.


DETAIL.
SKIP.
SELECT * FROM ZBCMKZ1 WHERE BNAME EQ SY-UNAME.
  CHECK ZBCMKZ1-NAME+5(1) EQ ' '.
  SELECT SINGLE * FROM TSTC WHERE TCODE EQ ZBCMKZ1-NAME.
  CHECK SY-SUBRC EQ 0.
  CLEAR TSTCT.
  SELECT SINGLE * FROM TSTCT WHERE SPRSL EQ SY-LANGU AND
                                   TCODE EQ ZBCMKZ1-NAME.
  T-CODE = TSTCT-TTEXT.
  MOVE-CORRESPONDING ZBCMKZ1 TO T.
  APPEND T.
  CLEAR T.
ENDSELECT.
SORT T BY NUMMER CODE.
REPORT = ' '.
PERFORM LIST USING REPORT.
SELECT * FROM ZBCMKZ1  WHERE BNAME EQ SY-UNAME.
  CHECK ZBCMKZ1-NAME+5(1) NE ' '.
  READ TEXTPOOL ZBCMKZ1-NAME INTO BTAB LANGUAGE SY-LANGU.
  CHECK SY-SUBRC EQ 0.
  LOOP AT BTAB.
    IF BTAB-CODE(1) EQ 'R'.
      EXIT.
    ENDIF.
  ENDLOOP.
  MOVE BTAB-CODE+9(70) TO T-CODE.
  MOVE-CORRESPONDING ZBCMKZ1 TO T.
  APPEND T.
  CLEAR T.
ENDSELECT.
SORT T BY NUMMER CODE.
REPORT = 'X'.
PERFORM LIST USING REPORT.

AT LINE-SELECTION.
  CHECK NOT ( T-NAME IS INITIAL ).
  GET CURSOR FIELD FI.
    IF T-NAME+5(1) EQ ' '.
      REPORT = ' '.
    ELSE.
      REPORT = 'X'.
  ENDIF.
  IF FI = 'ICON_EXECUTE_OBJECT'.
    PERFORM PERO USING T-NAME REPORT.
  ELSEIF REPORT EQ ' '.
* SELECT SINGLE * FROM TSTC WHERE TCODE EQ ZBCMKZ1-NAME.
* IF T+5(1) EQ ' '.
    CALL TRANSACTION T-NAME.
  ELSE.
    SUBMIT (T-NAME) VIA SELECTION-SCREEN AND RETURN.
  ENDIF.
  CLEAR T-NAME.
*---------------------------------------------------------------------*
*       FORM LIST                                                     *
*---------------------------------------------------------------------*
*       ........                                                      *
*---------------------------------------------------------------------*
FORM LIST USING REPORT.
  LOOP AT T.
    IF REPORT = ' '.
      WRITE: /5 ICON_EXECUTE_OBJECT AS ICON, T-NAME UNDER 'Name',
                 T-CODE UNDER 'Text'.
    ELSE.
      WRITE: / T-NAME UNDER 'Name', T-CODE UNDER 'Text'.
    ENDIF.
  HIDE T.
    AT END OF NUMMER.
      SKIP.
    ENDAT.
  ENDLOOP.
  SKIP.
  CLEAR T.
  REFRESH T.
ENDFORM.
*---------------------------------------------------------------------*
*       FORM PERO                                                     *
*---------------------------------------------------------------------*
*       ........                                                      *
*---------------------------------------------------------------------*
*  -->  T-NAME                                                        *
*---------------------------------------------------------------------*
FORM PERO USING T-NAME REPORT.
  CHECK REPORT EQ ' '.
  MOVE T-NAME TO T-NAME+2(4).
  MOVE '/o' TO T-NAME+0(2).
  BDCDATA-PROGRAM  = 'SAPMS01J'.
  BDCDATA-DYNPRO   = '0310'.
  BDCDATA-DYNBEGIN = 'X'.
  APPEND BDCDATA.
  CLEAR BDCDATA.
  BDCDATA-FNAM     = 'BDC_OKCODE'.
  BDCDATA-FVAL     = T-NAME.
  APPEND BDCDATA.
  CALL TRANSACTION 'SU50'  USING BDCDATA  MODE 'N'
                           MESSAGES INTO MESSTAB.
  CLEAR BDCDATA.
  REFRESH BDCDATA.
ENDFORM.
