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
package org.sonar.server.platform.db.migration.version.v81;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@SupportsBlueGreen
public class MigrateDefaultBranchesToKeepSetting extends DataChange {
  private static final Logger LOG = Loggers.get(MigrateDefaultBranchesToKeepSetting.class);
  private static final String DEPRECATED_KEY = "sonar.branch.longLivedBranches.regex";
  private static final String NEW_KEY = "sonar.dbcleaner.branchesToKeepWhenInactive";

  private final System2 system;

  public MigrateDefaultBranchesToKeepSetting(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Long now = system.now();
    try {
      Long numberOfNewProps = context.prepareSelect("select count(*) from properties props where props.prop_key = '" + NEW_KEY + "'")
        .get(row -> row.getLong(1));
      if (numberOfNewProps != null && numberOfNewProps > 0) {
        // no need for a migration
        return;
      }

      Boolean defaultPropertyOverridden = context.prepareSelect("select count(*) from properties props where props.prop_key = '" + DEPRECATED_KEY + "'")
        .get(row -> row.getLong(1) > 0);
      if (FALSE.equals(defaultPropertyOverridden)) {
        migrateDefaultDeprecatedSettings(context, now);
      } else {
        migrateOverriddenDeprecatedSettings(context, now);
      }
    } catch (Exception ex) {
      LOG.error("Failed to migrate to new '{}' setting.", NEW_KEY);
      throw ex;
    }
  }

  private static void migrateDefaultDeprecatedSettings(Context context, Long time) throws SQLException {
    Boolean anyProjectAlreadyExists = context.prepareSelect("select count(*) from projects").get(row -> row.getLong(1) > 0);
    String newSettingValue = "master,develop,trunk";

    // if old `sonar.branch.longLivedBranches.regex` setting was at default value but there were already projects analyzed we need to add the
    // old defaults of
    // that setting to the defaults of the new `sonar.dbcleaner.branchesToKeepWhenInactive` setting for backward compatibility
    if (TRUE.equals(anyProjectAlreadyExists)) {
      newSettingValue = "master,develop,trunk,branch-.*,release-.*";
    }

    context
      .prepareUpsert("insert into properties (prop_key, is_empty, text_value, created_at) values (?, ?, ?, ?)")
      .setString(1, NEW_KEY)
      .setBoolean(2, false)
      .setString(3, newSettingValue)
      .setLong(4, time)
      .execute()
      .commit();
  }

  private static void migrateOverriddenDeprecatedSettings(Context context, Long time) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select resource_id, text_value from properties props where props.prop_key = '" + DEPRECATED_KEY + "'");
    massUpdate.rowPluralName("properties");
    massUpdate.update("insert into properties (resource_id, prop_key, is_empty, text_value, created_at) values (?, ?, ?, ?, ?)");
    massUpdate.execute((row, update) -> {
      update.setLong(1, row.getNullableLong(1));
      update.setString(2, NEW_KEY);
      update.setBoolean(3, false);
      update.setString(4, row.getString(2));
      update.setLong(5, time);
      return true;
    });
  }
}
