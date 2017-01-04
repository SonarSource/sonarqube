/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;

import static com.google.common.base.Preconditions.checkState;

/**
 * Component which can be injected in steps which provides access to the UUID of the default organization, read directly
 * from the BD.
 */
public class DefaultOrganizationUuidImpl implements DefaultOrganizationUuid {

  private static final String INTERNAL_PROPERTY_DEFAULT_ORGANIZATION = "organization.default";

  @Override
  public String get(DataChange.Context context) throws SQLException {
    Select select = context.prepareSelect("select text_value from internal_properties where kee=?");
    select.setString(1, INTERNAL_PROPERTY_DEFAULT_ORGANIZATION);
    String uuid = select.get(row -> row.getString(1));
    checkState(uuid != null, "Default organization uuid is missing");
    return uuid;
  }
}
