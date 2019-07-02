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

import javax.annotation.concurrent.Immutable;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.sonar.server.platform.db.migration.def.Validations.validateColumnName;

/**
 * Used to define TIMESTAMP columns.
 *
 * @deprecated implemented for compatibility with old tables, but {@link BigIntegerColumnDef}
 * must be used for storing datetimes as bigints (no problems regarding timezone).
 */
@Immutable
@Deprecated
public class TimestampColumnDef extends AbstractColumnDef {

  private TimestampColumnDef(Builder builder) {
    super(builder.columnName, builder.isNullable, null);
  }

  public static Builder newTimestampColumnDefBuilder() {
    return new Builder();
  }

  @Override
  public String generateSqlType(Dialect dialect) {
    switch (dialect.getId()) {
      case MsSql.ID:
        return "DATETIME";
      case Oracle.ID:
        return "TIMESTAMP (6)";
      case H2.ID:
      case PostgreSql.ID:
        return "TIMESTAMP";
      default:
        throw new IllegalArgumentException("Unsupported dialect id " + dialect.getId());
    }
  }

  public static class Builder {
    private String columnName;
    private boolean isNullable = true;

    public Builder setColumnName(String columnName) {
      this.columnName = validateColumnName(columnName);
      return this;
    }

    public Builder setIsNullable(boolean b) {
      this.isNullable = b;
      return this;
    }

    public TimestampColumnDef build() {
      validateColumnName(columnName);
      return new TimestampColumnDef(this);
    }
  }

}
