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
package org.sonar.server.platform.db.migration.version;

import java.sql.SQLException;
import org.sonar.server.platform.db.migration.step.DataChange;

public interface DefaultOrganizationUuidProvider {
  /**
   * Retrieves the uuid of the default organization from table INTERNAL_PROPERTIES.
   *
   * @throws IllegalStateException if uuid of the default organization can't be retrieved
   */
  String get(DataChange.Context context) throws SQLException;

  /**
   * Retrieves the uuid of the default organization from table INTERNAL_PROPERTIES and ensure the specified organization
   * exists in table ORGANIZATIONS.
   *
   * @throws IllegalStateException if uuid of the default organization can't be retrieved
   * @throws IllegalStateException if the default organization does not exist
   */
  String getAndCheck(DataChange.Context context) throws SQLException;
}
