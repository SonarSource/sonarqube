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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Long.parseLong;

public class ReadGlobalSonarQualityGateSettingToDefaultOrg extends DataChange {

  private static final Logger LOGGER = Loggers.get(ReadGlobalSonarQualityGateSettingToDefaultOrg.class);
  private final System2 system2;

  public ReadGlobalSonarQualityGateSettingToDefaultOrg(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String defaultOrgUuid = getDefaultOrgUuid(context);
    String defaultQualityGate = getDefaultQualityGate(context);
    if (defaultQualityGate == null) {
      LOGGER.info("No default quality gate set");
      return;
    }

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid from quality_gates where id=?")
      .setLong(1, parseLong(defaultQualityGate));
    massUpdate.update("update organizations set default_quality_gate_uuid = ?, updated_at = ? where uuid=?");
    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(1));
      update.setLong(2, system2.now());
      update.setString(3, defaultOrgUuid);
      return true;
    });
  }

  private static String getDefaultOrgUuid(Context context) throws SQLException {
    String defaultOrgUuid = context.prepareSelect("select text_value from internal_properties where kee = 'organization.default'")
      .get(row -> row.getString(1));
    checkState(defaultOrgUuid != null, "Default organization uuid is missing");
    return defaultOrgUuid;
  }

  @CheckForNull
  private static String getDefaultQualityGate(Context context) throws SQLException {
    return context.prepareSelect("select text_value from properties where prop_key=? and resource_id is null")
      .setString(1, "sonar.qualitygate")
      .get(row -> row.getNullableString(1));
  }
}
