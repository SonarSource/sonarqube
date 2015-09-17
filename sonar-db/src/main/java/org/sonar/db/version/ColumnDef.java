/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.db.version;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.Oracle;

public class ColumnDef {

  private String name;
  private Type type;
  private boolean isNullable;
  private Integer limit;

  public enum Type {
    STRING, BIG_INTEGER
  }

  public ColumnDef setNullable(boolean isNullable) {
    this.isNullable = isNullable;
    return this;
  }

  public ColumnDef setLimit(@Nullable Integer limit) {
    this.limit = limit;
    return this;
  }

  public ColumnDef setName(String name) {
    Preconditions.checkArgument(CharMatcher.JAVA_LOWER_CASE.or(CharMatcher.anyOf("_")).matchesAllOf(name), "Column name should only contains lowercase and _ characters");
    this.name = name;
    return this;
  }

  public ColumnDef setType(Type type) {
    this.type = type;
    return this;
  }

  public boolean isNullable() {
    return isNullable;
  }

  @CheckForNull
  public Integer getLimit() {
    return limit;
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  public String getSqlType(Dialect dialect) {
    switch (type) {
      case STRING:
        return "VARCHAR";
      case BIG_INTEGER:
        return dialect.getId().equals(Oracle.ID) ? "NUMBER (38)" : "BIGINT";
      default:
        throw new IllegalArgumentException("Unsupported type : " + type);
    }
  }

}
