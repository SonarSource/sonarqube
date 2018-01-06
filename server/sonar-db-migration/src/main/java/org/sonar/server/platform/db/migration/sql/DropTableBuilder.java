/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.sql;

import java.util.List;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.Oracle;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.platform.db.migration.def.Validations.validateTableName;

public class DropTableBuilder {

  private final Dialect dialect;
  private final String tableName;

  public DropTableBuilder(Dialect dialect, String tableName) {
    this.dialect = requireNonNull(dialect, "dialect can't be null");
    this.tableName = validateTableName(tableName);
  }

  public List<String> build() {
    if (Oracle.ID.equals(dialect.getId())) {
      return dropOracleSequenceAndTriggerAndTableStatements();
    }
    return singletonList(dropTableStatement());
  }

  private String dropTableStatement() {
    return "DROP TABLE " + tableName;
  }

  private List<String> dropOracleSequenceAndTriggerAndTableStatements() {
    return asList(dropSequenceFor(tableName), dropTriggerFor(tableName), dropTableStatement());
  }

  private static String dropSequenceFor(String tableName) {
    return "BEGIN\n" +
      "  EXECUTE IMMEDIATE 'DROP SEQUENCE " + tableName + "_seq';\n" +
      "EXCEPTION\n" +
      "  WHEN OTHERS THEN\n" +
      "    IF SQLCODE != -2289 THEN\n" +
      "      RAISE;\n" +
      "    END IF;\n" +
      "END;";
  }

  private static String dropTriggerFor(String tableName) {
    return "BEGIN\n" +
      "  EXECUTE IMMEDIATE 'DROP TRIGGER " + tableName + "_idt';\n" +
      "EXCEPTION\n" +
      "  WHEN OTHERS THEN\n" +
      "    IF SQLCODE != -4080 THEN\n" +
      "      RAISE;\n" +
      "    END IF;\n" +
      "END;";

  }

}
