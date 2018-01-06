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
package org.sonar.server.platform.db.migration.version.v65;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static com.google.common.base.Preconditions.checkState;

public class UpdateOrgQProfilesToPointToBuiltInProfiles extends DataChange {
  private static final String PROP_DEFAULT_ORGANIZATION_UUID = "organization.default";
  private static final String PROP_ORGANIZATION_ENABLED = "organization.enabled";

  public UpdateOrgQProfilesToPointToBuiltInProfiles(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (!isOrganizationEnabled(context)) {
      return;
    }

    String defaultOrganizationUuid = getDefaultOrganizationUuid(context);
    BuiltInRulesProfiles builtInRulesProfiles = retrieveBuiltInRulesProfiles(context);

    MassUpdate massUpdate = context.prepareMassUpdate()
      .rowPluralName("org qprofiles");

    massUpdate.select("select oqp.uuid, rp.language, rp.name " +
      " from org_qprofiles oqp " +
      " inner join rules_profiles rp on rp.kee = oqp.rules_profile_uuid " +
      " where oqp.organization_uuid <> ? " +
      "   and rp.is_built_in = ? " +
      "   and oqp.user_updated_at is null")
      .setString(1, defaultOrganizationUuid)
      .setBoolean(2, false);

    massUpdate.update("update org_qprofiles " +
      "set rules_profile_uuid = ? " +
      "where uuid=?")
      .execute((row, update) -> {
        String orgQProfileUuid = row.getString(1);
        String language = row.getString(2);
        String name = row.getString(3);
        if (!builtInRulesProfiles.contains(name, language)) {
          return false;
        }

        update.setString(1, builtInRulesProfiles.get(name, language));
        update.setString(2, orgQProfileUuid);
        return true;
      });
  }

  private static String getDefaultOrganizationUuid(Context context) throws SQLException {
    String defaultOrganizationUuid = context.prepareSelect("select text_value from internal_properties where kee=?")
      .setString(1, PROP_DEFAULT_ORGANIZATION_UUID)
      .get(row -> row.getString(1));

    checkState(defaultOrganizationUuid != null, "Missing internal property: '%s'", PROP_DEFAULT_ORGANIZATION_UUID);

    return defaultOrganizationUuid;
  }

  private static boolean isOrganizationEnabled(Context context) throws SQLException {
    String value = context.prepareSelect("select text_value from internal_properties where kee=?")
      .setString(1, PROP_ORGANIZATION_ENABLED)
      .get(row -> row.getNullableString(1));

    return "true".equals(value);
  }

  private static BuiltInRulesProfiles retrieveBuiltInRulesProfiles(Context context) throws SQLException {
    BuiltInRulesProfiles result = new BuiltInRulesProfiles();

    context.prepareSelect("select name, language, kee" +
      " from rules_profiles " +
      " where is_built_in = ? ")
      .setBoolean(1, true)
      .list(row -> result.put(row.getString(1), row.getString(2), row.getString(3)));

    return result;
  }

  private static class BuiltInRulesProfiles {
    private Table<String, String, String> table = HashBasedTable.create();

    private String put(String name, String language, String rulesProfileUuid) {
      return table.put(name, language, rulesProfileUuid);
    }

    private boolean contains(String name, String language) {
      return table.contains(name, language);
    }

    private String get(String name, String language) {
      return table.get(name, language);
    }
  }

}
