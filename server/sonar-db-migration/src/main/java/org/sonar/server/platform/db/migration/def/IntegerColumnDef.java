/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.sonar.server.platform.db.migration.def.Validations.validateColumnName;

@Immutable
public class IntegerColumnDef extends AbstractColumnDef {

  private IntegerColumnDef(Builder builder) {
    super(builder.columnName, builder.isNullable, builder.defaultValue);
  }

  public static Builder newIntegerColumnDefBuilder() {
    return new Builder();
  }

  @Override
  public String generateSqlType(Dialect dialect) {
    switch (dialect.getId()) {
      case PostgreSql.ID:
      case H2.ID:
        return "INTEGER";
      case MsSql.ID:
        return "INT";
      case Oracle.ID:
        return "NUMBER(38,0)";
      default:
        throw new IllegalArgumentException("Unsupported dialect id " + dialect.getId());
    }
  }

  public static class Builder {
    @CheckForNull
    private String columnName;
    private boolean isNullable = true;
    @CheckForNull
    private Integer defaultValue = null;

    public Builder setColumnName(String columnName) {
      this.columnName = validateColumnName(columnName);
      return this;
    }

    public Builder setIsNullable(boolean isNullable) {
      this.isNullable = isNullable;
      return this;
    }

    public Builder setDefaultValue(@Nullable Integer i) {
      this.defaultValue = i;
      return this;
    }

    public IntegerColumnDef build() {
      validateColumnName(columnName);
      return new IntegerColumnDef(this);
    }
  }

}
