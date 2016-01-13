/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version;

import javax.annotation.CheckForNull;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.MsSql;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.version.ColumnDefValidation.validateColumnName;

/**
 * Used to define VARCHAR column
 */
public class VarcharColumnDef extends AbstractColumnDef {
  private final int columnSize;

  private VarcharColumnDef(Builder builder) {
    super(builder.columnName, builder.isNullable);
    this.columnSize = builder.columnSize;
  }

  public static Builder newVarcharColumnDefBuilder() {
    return new Builder();
  }

  public int getColumnSize() {
    return columnSize;
  }

  @Override
  public String generateSqlType(Dialect dialect) {
    if (MsSql.ID.equals(dialect.getId())) {
      return format("NVARCHAR (%d) COLLATE %s", columnSize, MsSql.COLLATION);
    }
    return format("VARCHAR (%d)", columnSize);
  }

  public static class Builder {
    @CheckForNull
    private Integer columnSize;

    @CheckForNull
    private String columnName;

    private boolean isNullable = true;

    public Builder setColumnName(String columnName) {
      this.columnName = validateColumnName(columnName);
      return this;
    }

    public Builder setLimit(int limit) {
      this.columnSize = limit;
      return this;
    }

    public Builder setIsNullable(boolean isNullable) {
      this.isNullable = isNullable;
      return this;
    }

    public VarcharColumnDef build() {
      validateColumnName(columnName);
      requireNonNull(columnSize, "Limit cannot be null");
      return new VarcharColumnDef(this);
    }
  }

}
