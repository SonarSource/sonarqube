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
package org.sonar.server.platform.db.migration.sql;

import java.util.List;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

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
    return switch (dialect.getId()) {
      case Oracle.ID -> forOracle(tableName);
      case H2.ID, PostgreSql.ID -> singletonList("drop table if exists " + tableName);
      case MsSql.ID ->
        // "if exists" is supported only since MSSQL 2016.
        singletonList("drop table " + tableName);
      default -> throw new IllegalStateException("Unsupported DB: " + dialect.getId());
    };
  }

  private static List<String> forOracle(String tableName) {
    return asList(
      dropIfExistsOnOracle("DROP SEQUENCE " + tableName + "_seq", -2289),
      dropIfExistsOnOracle("DROP TRIGGER " + tableName + "_idt", -4080),
      dropIfExistsOnOracle("DROP TABLE " + tableName, -942));
  }

  private static String dropIfExistsOnOracle(String command, int codeIfNotExists) {
    return "BEGIN\n" +
      "EXECUTE IMMEDIATE '" + command + "';\n" +
      "EXCEPTION\n" +
      "WHEN OTHERS THEN\n" +
      "  IF SQLCODE != " + codeIfNotExists + " THEN\n" +
      "  RAISE;\n" +
      "  END IF;\n" +
      "END;";
  }
}
