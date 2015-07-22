/* Formatted on 2002/03/31 23:53 (Formatter Plus v4.5.2) */
CREATE OR REPLACE PACKAGE BODY Utreport
IS
   
/************************************************************************
GNU General Public License for utPLSQL

Copyright (C) 2000-2003 
Steven Feuerstein and the utPLSQL Project
(steven@stevenfeuerstein.com)

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program (see license.txt); if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
************************************************************************
$Log: ut_report.pkb,v $
Revision 1.3  2005/05/11 21:33:36  chrisrimmer
Added testing of reporter infrastructure

Revision 1.2  2004/11/16 09:46:49  chrisrimmer
Changed to new version detection system.

Revision 1.1  2004/07/14 17:01:57  chrisrimmer
Added first version of pluggable reporter packages


************************************************************************/

   DEFAULT_REPORTER VARCHAR2(100) := 'Output';
   
   DYNAMIC_PLSQL_FAILURE NUMBER(10) := -6550;

   --This is the reporter we have been asked to use
   g_reporter VARCHAR2(100);
   
   --This is the reporter we are actually using
   --(this differs from the above in the event of error)
   g_actual VARCHAR2(100);

   FUNCTION parse_it(proc IN VARCHAR2, params IN NUMBER, force_reporter IN VARCHAR2)
      RETURN INTEGER
   IS
      dyn_handle INTEGER := NULL;
      query VARCHAR2(1000);         
   BEGIN
      dyn_handle := DBMS_SQL.OPEN_CURSOR;
      QUERY := 'BEGIN ut' || NVL(force_reporter, g_actual) || 'Reporter.' || proc ;
      IF params = 1 THEN
         QUERY := QUERY || '(:p)';
      END IF;
      QUERY := QUERY || '; END;'; 
      DBMS_SQL.PARSE(dyn_handle, QUERY, DBMS_SQL.NATIVE);   
      RETURN dyn_handle;
   EXCEPTION
      WHEN OTHERS THEN
        DBMS_SQL.CLOSE_CURSOR (dyn_handle);
        RAISE;
   END;

   PROCEDURE execute_it(dyn_handle IN OUT INTEGER)
   IS
      dyn_result INTEGER;   
   BEGIN
      dyn_result := DBMS_SQL.EXECUTE (dyn_handle);
      DBMS_SQL.CLOSE_CURSOR (dyn_handle);    
   END;
     
   --We use this to make dynamic calls to reporter packages
   PROCEDURE call(proc IN VARCHAR2, 
                  param IN VARCHAR2, 
                  params IN NUMBER := 1,  
                  force_reporter IN VARCHAR2 := NULL,
                  failover IN BOOLEAN := TRUE)
   IS
      dyn_handle INTEGER := NULL;
   BEGIN
      dyn_handle := parse_it(proc, params, force_reporter);
      IF params = 1 THEN
         DBMS_SQL.BIND_VARIABLE (dyn_handle, 'p', param);
      END IF;
      execute_it(dyn_handle);      
    EXCEPTION
      WHEN OTHERS THEN
        
        IF dyn_handle IS NOT NULL THEN
          DBMS_SQL.CLOSE_CURSOR (dyn_handle);
        END IF;
        
        IF g_actual <> DEFAULT_REPORTER THEN
        
          IF NOT failover OR SQLCODE <> DYNAMIC_PLSQL_FAILURE THEN
            g_actual := DEFAULT_REPORTER;
            pl(SQLERRM);
            pl('** REVERTING TO DEFAULT REPORTER **');
          END IF;
         
        ELSE
          RAISE;
        END IF;       
        
        call(proc, param, params, force_reporter => DEFAULT_REPORTER);        
   END;
      
   PROCEDURE call(proc IN VARCHAR2,
                  failover IN BOOLEAN := TRUE)
   IS
   BEGIN
     call(proc => proc, 
          param => '', 
          params => 0,
          failover => failover);
   END;
      
   PROCEDURE use(reporter IN VARCHAR2)
   IS
   BEGIN
     g_reporter := NVL(reporter, DEFAULT_REPORTER);
     g_actual := g_reporter;
   END;
   
   FUNCTION using RETURN VARCHAR2
   IS
   BEGIN
     RETURN g_reporter;
   END;

   PROCEDURE open
   IS
   BEGIN
      g_actual := g_reporter;
      call('open', failover => FALSE);
   END;

   PROCEDURE pl (str IN VARCHAR2)
   IS
   BEGIN
      call('pl', str);
   END;

   PROCEDURE pl (bool IN BOOLEAN)
   IS
   BEGIN
      pl (Utplsql.bool2vc (bool));
   END;

   PROCEDURE before_results(run_id IN utr_outcome.run_id%TYPE)
   IS
   BEGIN
      call('before_results', run_id);
   END;
   
   PROCEDURE show_failure(rec_result IN utr_outcome%ROWTYPE)
   IS
   BEGIN
      outcome := rec_result;
      call('show_failure');   
   END;
   
   PROCEDURE show_result(rec_result IN utr_outcome%ROWTYPE)
   IS
   BEGIN
      outcome := rec_result;
      call('show_result');
   END;
   
   PROCEDURE after_results(run_id IN utr_outcome.run_id%TYPE)
   IS
   BEGIN
      call('after_results', run_id);
   END;
   
   PROCEDURE before_errors(run_id IN utr_error.run_id%TYPE)
   IS
   BEGIN
      call('before_errors', run_id);
   END;
   
   PROCEDURE show_error(rec_error IN utr_error%ROWTYPE)
   IS
   BEGIN
      error := rec_error;
      call('show_error');
   END;
   
   PROCEDURE after_errors(run_id IN utr_error.run_id%TYPE)
   IS
   BEGIN
      call('after_errors', run_id);   
   END;   
   
   PROCEDURE close
   IS
   BEGIN
      call('close');
   END;

BEGIN

   g_reporter := NVL(utconfig.getreporter, DEFAULT_REPORTER);
   g_actual := g_reporter;
   
END;
/
