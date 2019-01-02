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
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.platform.db.migration.def.Validations.validateColumnName;

/**
 * Used to define VARCHAR column
 */
@Immutable
public class VarcharColumnDef extends AbstractColumnDef {
  public static final int MAX_SIZE = 4_000;
  /**
   * @deprecated use {@link #UUID_SIZE} instead
   */
  @Deprecated
  public static final int UUID_VARCHAR_SIZE = 50;
  public static final int UUID_SIZE = 40;

  private final int columnSize;
  private final boolean ignoreOracleUnit;

  private VarcharColumnDef(Builder builder) {
    super(builder.columnName, builder.isNullable, builder.defaultValue);
    this.columnSize = builder.columnSize;
    this.ignoreOracleUnit = builder.ignoreOracleUnit;
  }

  public static Builder newVarcharColumnDefBuilder() {
    return new Builder();
  }

  public int getColumnSize() {
    return columnSize;
  }

  @Override
  public String generateSqlType(Dialect dialect) {
    switch (dialect.getId()) {
      case MsSql.ID:
        return format("NVARCHAR (%d)", columnSize);
      case Oracle.ID:
        return format("VARCHAR2 (%d%s)", columnSize, ignoreOracleUnit ? "" : " CHAR");
      default:
        return format("VARCHAR (%d)", columnSize);
    }
  }

  public static class Builder {
    @CheckForNull
    private Integer columnSize;
    @CheckForNull
    private String columnName;
    @CheckForNull
    private String defaultValue = null;
    private boolean isNullable = true;
    private boolean ignoreOracleUnit = false;

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

    public Builder setDefaultValue(@Nullable String s) {
      this.defaultValue = s;
      return this;
    }

    /**
     * In order to not depend on value of runtime variable NLS_LENGTH_SEMANTICS, unit of length
     * is enforced to CHAR so that we're sure that type can't be BYTE.
     * Unit is ignored for the columns created before SonarQube 6.3 (except for issues.message which
     * has been fixed in migration 1151 of SonarQube 5.6. See SONAR-7493).
     *
     * In most cases this method should not be called with parameter {@code true} after
     * version 6.3.
     *
     * @param b whether unit of length is hardcoded to CHAR.
     */
    public Builder setIgnoreOracleUnit(boolean b) {
      this.ignoreOracleUnit = b;
      return this;
    }

    public VarcharColumnDef build() {
      validateColumnName(columnName);
      requireNonNull(columnSize, "Limit cannot be null");
      return new VarcharColumnDef(this);
    }
  }

}
