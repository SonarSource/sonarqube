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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.sonar.server.platform.db.migration.def.Validations.validateColumnName;

abstract class AbstractIntegerColumnDefBuilder<T extends AbstractIntegerColumnDefBuilder<T>> {
  @CheckForNull
  String columnName;
  boolean isNullable = true;
  @CheckForNull
  Integer defaultValue = null;

  public T setColumnName(String columnName) {
    this.columnName = validateColumnName(columnName);
    return castThis();
  }

  public T setIsNullable(boolean isNullable) {
    this.isNullable = isNullable;
    return castThis();
  }

  public T setDefaultValue(@Nullable Integer i) {
    this.defaultValue = i;
    return castThis();
  }

  @SuppressWarnings("unchecked")
  private T castThis() {
    return (T) this;
  }
}
