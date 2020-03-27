/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v83.util;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.sonar.server.platform.db.migration.def.Validations.validateTableName;
import static org.sonar.server.platform.db.migration.sql.CreateTableBuilder.PRIMARY_KEY_PREFIX;

public class AddPrimaryKeyBuilder {

  private final String tableName;
  private final String primaryKey;

  public AddPrimaryKeyBuilder(String tableName, String column) {
    this.tableName = validateTableName(tableName);
    this.primaryKey = column;
  }

  public String build() {
    checkState(primaryKey != null, "Primary key is missing");
    return format("ALTER TABLE %s ADD CONSTRAINT %s%s PRIMARY KEY (%s)", tableName, PRIMARY_KEY_PREFIX, tableName, primaryKey);
  }

}
