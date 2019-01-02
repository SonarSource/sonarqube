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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static com.google.common.base.Preconditions.checkState;

public class SetRulesProfilesIsBuiltInToTrueForDefaultOrganization extends DataChange {
  private static final String PROP_DEFAULT_ORGANIZATION_UUID = "organization.default";
  private static final String PROP_ORGANIZATION_ENABLED = "organization.enabled";

  public SetRulesProfilesIsBuiltInToTrueForDefaultOrganization(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (!isOrganizationEnabled(context)) {
      return;
    }

    String defaultOrganizationUuid = getDefaultOrganizationUuid(context);
    checkState(defaultOrganizationUuid!=null, "Missing internal property: '%s'", PROP_DEFAULT_ORGANIZATION_UUID);

    MassUpdate massUpdate = context.prepareMassUpdate()
      .rowPluralName("rules profiles");
    massUpdate.select("select rp.kee " +
      "from rules_profiles rp " +
      "inner join org_qprofiles oqp on rp.kee = oqp.rules_profile_uuid " +
      "where oqp.organization_uuid = ? ")
      .setString(1, defaultOrganizationUuid);
    massUpdate.update("update rules_profiles " +
      "set is_built_in=? " +
      "where kee=?");
    massUpdate.execute((row, update) -> {
        String rulesProfilesUuid = row.getString(1);
        update.setBoolean(1, true);
        update.setString(2, rulesProfilesUuid);
        return true;
      });
  }

  private static String getDefaultOrganizationUuid(Context context) throws SQLException {
    return context.prepareSelect("select text_value from internal_properties where kee=?")
      .setString(1, PROP_DEFAULT_ORGANIZATION_UUID)
      .get(row -> row.getString(1));
  }

  private static boolean isOrganizationEnabled(Context context) throws SQLException {
    String value = context.prepareSelect("select text_value from internal_properties where kee=?")
      .setString(1, PROP_ORGANIZATION_ENABLED)
      .get(row -> row.getNullableString(1));

    return "true".equals(value);
  }
}
