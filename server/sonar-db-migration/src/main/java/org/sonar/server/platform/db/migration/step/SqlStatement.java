/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.step;

import java.sql.SQLException;
import java.util.Date;
import javax.annotation.Nullable;

public interface SqlStatement<CHILD extends SqlStatement> extends AutoCloseable {
  CHILD setBoolean(int columnIndex, @Nullable Boolean value) throws SQLException;

  CHILD setBytes(int columnIndex, @Nullable byte[] value) throws SQLException;

  CHILD setDate(int columnIndex, @Nullable Date value) throws SQLException;

  CHILD setDouble(int columnIndex, @Nullable Double value) throws SQLException;

  CHILD setInt(int columnIndex, @Nullable Integer value) throws SQLException;

  CHILD setLong(int columnIndex, @Nullable Long value) throws SQLException;

  CHILD setString(int columnIndex, @Nullable String value) throws SQLException;

  @Override
  void close();
}
