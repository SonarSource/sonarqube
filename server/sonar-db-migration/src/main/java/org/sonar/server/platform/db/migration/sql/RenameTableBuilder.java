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

import java.util.Arrays;
import java.util.List;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static org.sonar.server.platform.db.migration.def.Validations.validateTableName;

/**
 * Limitation: only tables with auto-generated ID column can
 * be renamed as the Oracle implementation assumes that
 * the sequence and trigger related to ID column exist.
 */
public class RenameTableBuilder {

  private final Dialect dialect;
  private String name;
  private String newName;

  public RenameTableBuilder(Dialect dialect) {
    this.dialect = dialect;
  }

  public RenameTableBuilder setName(String s) {
    this.name = s;
    return this;
  }

  public RenameTableBuilder setNewName(String s) {
    this.newName = s;
    return this;
  }

  public List<String> build() {
    validateTableName(name);
    validateTableName(newName);
    checkArgument(!name.equals(newName), "Names must be different");
    return createSqlStatement();
  }

  private List<String> createSqlStatement() {
    switch (dialect.getId()) {
      case H2.ID:
      case MySql.ID:
      case PostgreSql.ID:
        return singletonList("ALTER TABLE " + name + " RENAME TO " + newName);
      case MsSql.ID:
        return singletonList("EXEC sp_rename '" + name + "', '" + newName + "'");
      case Oracle.ID:
        return Arrays.asList(
          "DROP TRIGGER " + name + "_idt",
          "RENAME " + name + " TO " + newName,
          "RENAME " + name + "_seq TO " + newName + "_seq",
          CreateTableBuilder.createOracleTriggerForTable(newName));
      default:
        throw new IllegalArgumentException("Unsupported dialect id " + dialect.getId());
    }
  }
}
