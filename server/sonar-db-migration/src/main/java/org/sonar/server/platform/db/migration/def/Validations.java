/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.db.migration.def;

import com.google.common.base.CharMatcher;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.CharMatcher.inRange;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

public class Validations {

  private static final int TABLE_NAME_MAX_SIZE = 25;
  private static final int CONSTRAINT_NAME_MAX_SIZE = 30;
  private static final int INDEX_NAME_MAX_SIZE = 30;

  private static final CharMatcher DIGIT_CHAR_MATCHER = inRange('0', '9');
  private static final CharMatcher LOWER_CASE_ASCII_LETTERS_CHAR_MATCHER = inRange('a', 'z');
  private static final CharMatcher UPPER_CASE_ASCII_LETTERS_CHAR_MATCHER = inRange('A', 'Z');
  private static final CharMatcher UNDERSCORE_CHAR_MATCHER = anyOf("_");

  // TODO: Refactor all existing identifiers that match SQL reserved keywords,
  // the list below is used as a workaround for validating existing non-complaint identifiers
  private static final String VALUE_COLUMN_NAME = "value";
  private static final String GROUPS_TABLE_NAME = "groups";
  private static final List<String> ALLOWED_IDENTIFIERS = List.of(VALUE_COLUMN_NAME, GROUPS_TABLE_NAME);

  // MS SQL keywords retrieved from: com.microsoft.sqlserver.jdbc.SQLServerDatabaseMetaData#createSqlKeyWords
  protected static final Set<String> MSSQL_KEYWORDS =  Set.of(
          "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUTHORIZATION",
          "BACKUP", "BEGIN", "BETWEEN", "BREAK", "BROWSE", "BULK", "BY",
          "CASCADE", "CASE", "CHECK", "CHECKPOINT", "CLOSE", "CLUSTERED", "COALESCE", "COLLATE", "COLUMN", "COMMIT", "COMPUTE", "CONSTRAINT", "CONTAINS",
          "CONTAINSTABLE", "CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER,CURSOR",
          "DATABASE", "DBCC", "DEALLOCATE", "DECLARE", "DEFAULT", "DELETE", "DENY", "DESC", "DISK", "DISTINCT", "DISTRIBUTED", "DOUBLE", "DROP", "DUMP",
          "ELSE", "END", "ERRLVL", "ESCAPE", "EXCEPT", "EXEC", "EXECUTE", "EXISTS", "EXIT", "EXTERNAL",
          "FETCH", "FILE", "FILLFACTOR", "FOR", "FOREIGN", "FREETEXT", "FREETEXTTABLE", "FROM", "FULL", "FUNCTION",
          "GOTO", "GRANT", "GROUP",
          "HAVING", "HOLDLOCK",
          "IDENTITY", "IDENTITY_INSERT", "IDENTITYCOL", "IF", "IN", "INDEX", "INNER", "INSERT", "INTERSECT", "INTO", "IS",
          "JOIN",
          "KEY", "KILL",
          "LEFT", "LIKE",
          "LINENO", "LOAD",
          "MERGE",
          "NATIONAL", "NOCHECK", "NONCLUSTERED", "NOT", "NULL", "NULLIF",
          "OF", "OFF", "OFFSETS", "ON", "OPEN", "OPENDATASOURCE", "OPENQUERY", "OPENROWSET", "OPENXML", "OPTION", "OR", "ORDER", "OUTER", "OVER",
          "PERCENT", "PIVOT", "PLAN", "PRECISION", "PRIMARY", "PRINT", "PROC", "PROCEDURE", "PUBLIC",
          "RAISERROR", "READ", "READTEXT", "RECONFIGURE", "REFERENCES", "REPLICATION", "RESTORE", "RESTRICT",
          "RETURN", "REVERT", "REVOKE", "RIGHT", "ROLLBACK", "ROWCOUNT", "ROWGUIDCOL", "RULE",
          "SAVE", "SCHEMA", "SECURITYAUDIT", "SELECT", "SEMANTICKEYPHRASETABLE", "SEMANTICSIMILARITYDETAILSTABLE",
          "SEMANTICSIMILARITYTABLE", "SESSION_USER", "SET", "SETUSER", "SHUTDOWN", "SOME", "STATISTICS", "SYSTEM_USER",
          "TABLE", "TABLESAMPLE", "TEXTSIZE", "THEN", "TO", "TOP", "TRAN", "TRANSACTION", "TRIGGER", "TRUNCATE", "TRY_CONVERT", "TSEQUAL",
          "UNION", "UNIQUE", "UNPIVOT", "UPDATE", "UPDATETEXT", "USE", "USER",
          "VALUES", "VARYING", "VIEW",
          "WAITFOR", "WHEN", "WHERE", "WHILE", "WITH", "WITHIN GROUP", "WRITETEXT");

  // H2 SQL keywords retrieved from: http://www.h2database.com/html/advanced.html
  protected static final Set<String> H2_KEYWORDS =  Set.of(
          "ALL", "AND", "ANY", "ARRAY", "AS", "ASYMMETRIC", "AUTHORIZATION",
          "BETWEEN", "BOTH",
          "CASE", "CAST", "CHECK", "CONSTRAINT", "CROSS", "CURRENT_CATALOG", "CURRENT_DATE", "CURRENT_PATH",
          "CURRENT_ROLE", "CURRENT_SCHEMA", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER",
          "DAY", "DEFAULT", "DISTINCT",
          "ELSE", "END", "EXISTS",
          "FALSE", "FETCH", "FILTER", "FOR", "FOREIGN", "FROM", "FULL",
          "GROUP", "GROUPS",
          "HAVING", "HOUR",
          "IF", "ILIKE", "IN", "INNER", "INTERSECT", "INTERVAL", "IS",
          "JOIN",
          "KEY",
          "LEADING", "LEFT", "LIKE", "LIMIT", "LOCALTIME", "LOCALTIMESTAMP",
          "MINUS", "MINUTE", "MONTH",
          "NATURAL", "NOT", "NULL",
          "OFFSET", "ON", "OR", "ORDER", "OVER",
          "PARTITION", "PRIMARY",
          "QUALIFY",
          "RANGE", "REGEXP", "RIGHT", "ROW", "ROWNUM", "ROWS",
          "SECOND", "SELECT", "SESSION_USER", "SET", "SOME", "SYMMETRIC", "SYSTEM_USER",
          "TABLE", "TO", "TOP", "TRAILING", "TRUE",
          "UESCAPE", "UNION", "UNIQUE", "UNKNOWN", "USER", "USING",
          "VALUE", "VALUES",
          "WHEN", "WHERE", "WINDOW", "WITH",
          "YEAR",
          "_ROWID_");

  // PostgreSQL keywords retrieved from: https://www.postgresql.org/docs/current/sql-keywords-appendix.html
  protected static final Set<String> POSTGRESQL_KEYWORDS =  Set.of(
          "ALL", "AND", "ANY", "ARRAY", "AS", "ASYMMETRIC",
          "BIGINT", "BINARY", "BIT", "BOOLEAN", "BOTH",
          "CASE", "CAST", "CHAR", "CHARACTER", "COALESCE", "COLLATE", "COLLATION", "COLUMN", "CONCURRENTLY", "CREATE",
          "CURRENT_CATALOG", "CURRENT_ROLE", "CURRENT_SCHEMA", "CURRENT_USER",
          "DEC", "DECIMAL", "DEFERRABLE", "DESC",
          "ELSE", "END", "EXCEPT",
          "FALSE", "FOR", "FREEZE", "FROM", "FULL",
          "GRANT", "GREATEST", "GROUPING",
          "ILIKE", "IN", "INITIALLY", "INTO", "IS", "ISNULL",
          "JOIN",
          "LATERAL", "LEADING", "LEFT", "LIKE", "LIMIT", "LOCALTIME", "LOCALTIMESTAMP",
          "NATIONAL", "NATURAL", "NCHAR", "NOT", "NOTNULL", "NULL",
          "OFFSET", "ON", "ONLY", "OR", "OUTER", "OVERLAPS",
          "PLACING",
          "REFERENCES", "RETURNING",
          "SETOF", "SIMILAR", "SMALLINT", "SOME", "SUBSTRING", "SYMMETRIC",
          "TABLESAMPLE", "THEN", "TIME", "TIMESTAMP", "TO", "TRAILING", "TREAT", "TRIM", "TRUE",
          "USER", "USING",
          "VARCHAR", "VARIADIC", "VERBOSE",
          "WHEN", "WINDOW", "WITH",
          "XMLCONCAT", "XMLELEMENT", "XMLEXISTS", "XMLFOREST", "XMLNAMESPACES", "XMLPARSE", "XMLPI", "XMLROOT", "XMLSERIALIZE", "XMLTABLE",
          "YEAR");

  protected static final  Set<String> ALL_KEYWORDS = Stream.of(MSSQL_KEYWORDS, H2_KEYWORDS, POSTGRESQL_KEYWORDS)
          .flatMap(Set::stream)
          .collect(Collectors.toSet());

  private Validations() {
    // Only static stuff here
  }

  /**
   * Ensure {@code columnName} is a valid name for a column.
   * @throws NullPointerException if {@code columnName} is {@code null}
   * @throws IllegalArgumentException if {@code columnName} is not valid
   * @return the same {@code columnName}
   * @see #checkDbIdentifier(String, String, int)
   */
  public static String validateColumnName(@Nullable String columnName) {
    String name = requireNonNull(columnName, "Column name cannot be null");
    checkDbIdentifierCharacters(columnName, "Column name");
    return name;
  }

  /**
   * Ensure {@code tableName} is a valid name for a table.
   * @throws NullPointerException if {@code tableName} is {@code null}
   * @throws IllegalArgumentException if {@code tableName} is not valid
   * @return the same {@code tableName}
   * @see #checkDbIdentifier(String, String, int)
   */
  public static String validateTableName(@Nullable String tableName) {
    checkDbIdentifier(tableName, "Table name", TABLE_NAME_MAX_SIZE);
    return tableName;
  }

  /**
   * Ensure {@code constraintName} is a valid name for a constraint.
   * @throws NullPointerException if {@code constraintName} is {@code null}
   * @throws IllegalArgumentException if {@code constraintName} is not valid
   * @return the same {@code constraintName}
   * @see #checkDbIdentifier(String, String, int)
   */
  public static String validateConstraintName(@Nullable String constraintName) {
    checkDbIdentifier(constraintName, "Constraint name", CONSTRAINT_NAME_MAX_SIZE);
    return constraintName;
  }

  /**
   * Ensure {@code indexName} is a valid name for an index.
   * @throws NullPointerException if {@code indexName} is {@code null}
   * @throws IllegalArgumentException if {@code indexName} is not valid
   * @return the same {@code indexName}
   * @see #checkDbIdentifier(String, String, int)
   */
  public static String validateIndexName(@Nullable String indexName) {
    checkDbIdentifier(indexName, "Index name", INDEX_NAME_MAX_SIZE);
    return indexName;
  }

  public static String validateIndexNameIgnoreCase(@Nullable String indexName) {
    checkDbIdentifier(indexName, "Index name", INDEX_NAME_MAX_SIZE, true);
    return indexName;
  }

  /**
   * Ensure {@code identifier} is a valid DB identifier.
   *
   * @throws NullPointerException if {@code identifier} is {@code null}
   * @throws IllegalArgumentException if {@code identifier} is empty
   * @throws IllegalArgumentException if {@code identifier} is longer than {@code maxSize}
   * @throws IllegalArgumentException if {@code identifier} is not lowercase
   * @throws IllegalArgumentException if {@code identifier} contains characters others than ASCII letters, ASCII numbers or {@code _}
   * @throws IllegalArgumentException if {@code identifier} starts with {@code _} or a number
   */
  static String checkDbIdentifier(@Nullable String identifier, String identifierDesc, int maxSize) {
    return checkDbIdentifier(identifier, identifierDesc, maxSize, false);
  }

  static String checkDbIdentifier(@Nullable String identifier, String identifierDesc, int maxSize, boolean ignoreCase) {
    String res = checkNotNull(identifier, "%s can't be null", identifierDesc);
    checkArgument(!res.isEmpty(), "%s, can't be empty", identifierDesc);
    checkArgument(
      identifier.length() <= maxSize,
      "%s length can't be more than %s", identifierDesc, maxSize);
    if (ignoreCase) {
      checkDbIdentifierCharactersIgnoreCase(identifier, identifierDesc);
    } else {
      checkDbIdentifierCharacters(identifier, identifierDesc);
    }
    return res;
  }

  private static boolean isSqlKeyword(String identifier) {
    return ALL_KEYWORDS.contains(identifier);
  }

  private static boolean isSqlKeywordIgnoreCase(String identifier) {
    return isSqlKeyword(identifier.toUpperCase(Locale.ENGLISH));
  }

  private static void checkDbIdentifierCharacters(String identifier, String identifierDesc) {
    checkArgument(identifier.length() > 0, "Identifier must not be empty");
    checkArgument(
      LOWER_CASE_ASCII_LETTERS_CHAR_MATCHER.or(DIGIT_CHAR_MATCHER).or(anyOf("_")).matchesAllOf(identifier),
      "%s must be lower case and contain only alphanumeric chars or '_', got '%s'", identifierDesc, identifier);
    checkArgument(
      DIGIT_CHAR_MATCHER.or(UNDERSCORE_CHAR_MATCHER).matchesNoneOf(identifier.subSequence(0, 1)),
      "%s must not start by a number or '_', got '%s'", identifierDesc, identifier);
    checkArgument(!isSqlKeyword(identifier.toUpperCase(Locale.ENGLISH)) || ALLOWED_IDENTIFIERS.contains(identifier),
      "%s must not be an SQL reserved keyword, got '%s'",
      identifierDesc,
      identifier);
  }

  private static void checkDbIdentifierCharactersIgnoreCase(String identifier, String identifierDesc) {
    checkArgument(identifier.length() > 0, "Identifier must not be empty");
    checkArgument(LOWER_CASE_ASCII_LETTERS_CHAR_MATCHER.or(DIGIT_CHAR_MATCHER).or(UPPER_CASE_ASCII_LETTERS_CHAR_MATCHER).or(anyOf("_")).matchesAllOf(identifier),
      "%s must contain only alphanumeric chars or '_', got '%s'", identifierDesc, identifier);
    checkArgument(
      DIGIT_CHAR_MATCHER.or(UNDERSCORE_CHAR_MATCHER).matchesNoneOf(identifier.subSequence(0, 1)),
      "%s must not start by a number or '_', got '%s'", identifierDesc, identifier);
    checkArgument(!isSqlKeywordIgnoreCase(identifier) || ALLOWED_IDENTIFIERS.contains(identifier.toLowerCase(Locale.ENGLISH)),
      "%s must not be an SQL reserved keyword, got '%s'",
      identifierDesc,
      identifier);
  }
}
