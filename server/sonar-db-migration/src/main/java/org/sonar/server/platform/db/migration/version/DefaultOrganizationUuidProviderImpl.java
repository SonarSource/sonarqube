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
import org.sonar.server.platform.db.migration.step.Select;

import static com.google.common.base.Preconditions.checkState;

/**
 * Component which can be injected in steps which provides access to the UUID of the default organization, it reads
 * directly from the BD.
 */
public class DefaultOrganizationUuidProviderImpl implements DefaultOrganizationUuidProvider {

  private static final String INTERNAL_PROPERTY_DEFAULT_ORGANIZATION = "organization.default";

  @Override
  public String get(DataChange.Context context) throws SQLException {
    Select select = context.prepareSelect("select text_value from internal_properties where kee=?");
    select.setString(1, INTERNAL_PROPERTY_DEFAULT_ORGANIZATION);
    String uuid = select.get(row -> row.getString(1));
    checkState(uuid != null, "Default organization uuid is missing");
    return uuid;
  }

  @Override
  public String getAndCheck(DataChange.Context context) throws SQLException {
    String organizationUuid = get(context);
    Select select = context.prepareSelect("select uuid from organizations where uuid=?")
      .setString(1, organizationUuid);
    checkState(select.get(row -> row.getString(1)) != null,
      "Default organization with uuid '%s' does not exist in table ORGANIZATIONS",
      organizationUuid);
    return organizationUuid;
  }
}
