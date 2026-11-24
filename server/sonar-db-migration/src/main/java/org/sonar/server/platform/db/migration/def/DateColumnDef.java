/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
 * Defines a SQL {@code DATE} column for values that represent calendar dates (e.g., {@code YYYY-MM-DD} and similar formats). The SQL {@code DATE} roughly corresponds to Java's
 * {@link java.time.LocalDate} type, and thus MyBatis' built-in {@link java.time.LocalDate} handler, with {@code jdbcType=DATE}, can be used on columns defined via this class.
 *
 * <p>Note that Oracle's {@code DATE} includes a time component; when translating from {@link java.time.LocalDate}, midnight is used for the time component on Oracle systems.
 */
@Immutable
public final class DateColumnDef extends AbstractColumnDef {
  private DateColumnDef(Builder builder) {
    super(builder.columnName, builder.isNullable, null);
  }

  public static Builder newDateColumnDefBuilder() {
    return new Builder();
  }

  @Override
  public String generateSqlType(Dialect dialect) {
    return switch (dialect.getId()) {
      case H2.ID, PostgreSql.ID, Oracle.ID, MsSql.ID -> "DATE";
      default -> throw new IllegalArgumentException("Unsupported dialect id " + dialect.getId());
    };
  }

  public static class Builder {
    private String columnName;
    private boolean isNullable = true;

    public Builder setColumnName(String columnName) {
      this.columnName = validateColumnName(columnName);
      return this;
    }

    public Builder setIsNullable(boolean isNullable) {
      this.isNullable = isNullable;
      return this;
    }

    public DateColumnDef build() {
      validateColumnName(columnName);
      return new DateColumnDef(this);
    }
  }
}
