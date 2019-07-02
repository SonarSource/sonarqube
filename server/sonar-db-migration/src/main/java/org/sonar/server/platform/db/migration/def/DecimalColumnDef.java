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
import javax.annotation.concurrent.Immutable;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.sonar.server.platform.db.migration.def.Validations.validateColumnName;

@Immutable
public class DecimalColumnDef extends AbstractColumnDef {

  public static final int DEFAULT_PRECISION = 38;
  public static final int DEFAULT_SCALE = 20;

  private final int precision;
  private final int scale;

  private DecimalColumnDef(Builder builder) {
    super(builder.columnName, builder.isNullable, null);
    this.precision = builder.precision;
    this.scale = builder.scale;
  }

  public static Builder newDecimalColumnDefBuilder() {
    return new Builder();
  }

  public int getPrecision() {
    return precision;
  }

  public int getScale() {
    return scale;
  }

  @Override
  public String generateSqlType(Dialect dialect) {
    switch (dialect.getId()) {
      case PostgreSql.ID:
      case Oracle.ID:
        return String.format("NUMERIC (%s,%s)", precision, scale);
      case MsSql.ID:
        return String.format("DECIMAL (%s,%s)", precision, scale);
      case H2.ID:
        return "DOUBLE";
      default:
        throw new UnsupportedOperationException(String.format("Unknown dialect '%s'", dialect.getId()));
    }
  }

  public static class Builder {
    @CheckForNull
    private String columnName;
    private int precision = DEFAULT_PRECISION;
    private int scale = DEFAULT_SCALE;
    private boolean isNullable = true;

    public Builder setColumnName(String columnName) {
      this.columnName = validateColumnName(columnName);
      return this;
    }

    public Builder setIsNullable(boolean isNullable) {
      this.isNullable = isNullable;
      return this;
    }

    public Builder setPrecision(int precision) {
      this.precision = precision;
      return this;
    }

    public Builder setScale(int scale) {
      this.scale = scale;
      return this;
    }

    public DecimalColumnDef build() {
      validateColumnName(columnName);
      return new DecimalColumnDef(this);
    }
  }

}
