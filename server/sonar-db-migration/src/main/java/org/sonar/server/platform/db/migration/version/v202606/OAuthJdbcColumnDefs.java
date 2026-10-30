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
package org.sonar.server.platform.db.migration.version.v202606;

import org.sonar.server.platform.db.migration.def.ColumnDef;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

final class OAuthJdbcColumnDefs {
  private OAuthJdbcColumnDefs() {
  }

  static ColumnDef text(String name, int length, boolean nullable) {
    return newVarcharColumnDefBuilder().setColumnName(name).setLimit(length).setIsNullable(nullable).build();
  }

  static ColumnDef clob(String name) {
    return newClobColumnDefBuilder().setColumnName(name).setIsNullable(true).build();
  }

  static ColumnDef epochMillis(String name, boolean nullable) {
    return newBigIntegerColumnDefBuilder().setColumnName(name).setIsNullable(nullable).build();
  }
}

