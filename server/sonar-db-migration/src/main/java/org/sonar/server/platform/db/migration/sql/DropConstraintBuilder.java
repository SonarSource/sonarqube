/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import static java.util.Collections.singletonList;
import static org.sonar.server.platform.db.migration.def.Validations.validateIndexName;
import static org.sonar.server.platform.db.migration.def.Validations.validateTableName;

/**
 * This builder have the main goal to drop constraint of a column.
 * <p>
 * It shouldn't be used to drop primary keys constraint, use {@link DropPrimaryKeySqlGenerator}
 */
public class DropConstraintBuilder {

  private final Dialect dialect;
  private String tableName;
  private String constraintName;

  public DropConstraintBuilder(Dialect dialect) {
    this.dialect = dialect;
  }

  public DropConstraintBuilder setTable(String s) {
    this.tableName = s;
    return this;
  }

  public DropConstraintBuilder setName(String s) {
    if (s.startsWith("pk_")) {
      throw new IllegalArgumentException("This builder should not be used with primary keys");
    }
    this.constraintName = s;
    return this;
  }

  public List<String> build() {
    validateTableName(tableName);
    validateIndexName(constraintName);
    return singletonList(createSqlStatement());
  }

  private String createSqlStatement() {
    return switch (dialect.getId()) {
      case MsSql.ID, Oracle.ID, PostgreSql.ID, H2.ID -> "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName;
      default -> throw new IllegalStateException("Unsupported dialect for drop of constraint: " + dialect);
    };
  }
}
