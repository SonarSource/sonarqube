-- plsql:SingleLineCommentsSyntaxCheck
/* single line comment */

-- plsql:UpperCaseReservedWordsCheck
create TABLE ut_suite (
   id INTEGER, -- comment
   -- plsql:CharVarchar
   name char(1)
);
