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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.def.ColumnDef;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.platform.db.migration.def.Validations.validateColumnName;
import static org.sonar.server.platform.db.migration.def.Validations.validateTableName;

/**
 * This builder have the main goal to change the name column.
 *
 * For MySQL the column definition is mandatory, however the change of column
 * is not supported on this class.
 * In case of renaming and changing a column, this must be done in two separate steps,
 * first rename the column then update the types of this column with @see {@link AlterColumnsBuilder}
 */
public class RenameColumnsBuilder {

  private final Dialect dialect;
  private final String tableName;
  private final List<Renaming> renamings = new ArrayList<>();

  public RenameColumnsBuilder(Dialect dialect, String tableName) {
    this.dialect = dialect;
    this.tableName = tableName;
  }

  public RenameColumnsBuilder renameColumn(String oldColumnName, ColumnDef columnDef) {
    renamings.add(new Renaming(oldColumnName, columnDef));
    return this;
  }

  public List<String> build() {
    validateTableName(tableName);
    renamings.forEach(
      r -> {
        validateColumnName(r.getOldColumnName());
        validateColumnName(r.getNewColumnName());
        checkArgument(!r.getNewColumnName().equals(r.getOldColumnName()), "Column names must be different");
      });
    return createSqlStatement();
  }

  private List<String> createSqlStatement() {
    return renamings.stream().map(
      r -> {
        switch (dialect.getId()) {
          case H2.ID:
            return "ALTER TABLE " + tableName + " ALTER COLUMN " + r.getOldColumnName() + " RENAME TO " + r.getNewColumnName();
          case Oracle.ID:
          case PostgreSql.ID:
            return "ALTER TABLE " + tableName + " RENAME COLUMN " + r.getOldColumnName() + " TO " + r.getNewColumnName();
          case MySql.ID:
            return "ALTER TABLE " + tableName + " CHANGE " + r.getOldColumnName() + " " + r.getNewColumnName() + " " + r.generateSqlType(dialect);
          case MsSql.ID:
            return "EXEC sp_rename '" + tableName + "." + r.getOldColumnName() + "', '" + r.getNewColumnName() + "', 'COLUMN'";
          default:
            throw new IllegalArgumentException("Unsupported dialect id " + dialect.getId());
        }
      }).collect(Collectors.toList());
  }

  private static class Renaming implements ColumnDef {
    private final ColumnDef columnDef;
    private final String oldColumnName;

    private Renaming(String oldColumnName, ColumnDef columnDef) {
      this.columnDef = columnDef;
      this.oldColumnName = oldColumnName;
    }

    public String getOldColumnName() {
      return oldColumnName;
    }

    public String getNewColumnName() {
      return columnDef.getName();
    }

    @Override
    public boolean isNullable() {
      return columnDef.isNullable();
    }

    @Override
    public String getName() {
      return columnDef.getName();
    }

    @Override
    public String generateSqlType(Dialect dialect) {
      return columnDef.generateSqlType(dialect);
    }

    @CheckForNull
    @Override
    public Object getDefaultValue() {
      return columnDef.getDefaultValue();
    }
  }
}
