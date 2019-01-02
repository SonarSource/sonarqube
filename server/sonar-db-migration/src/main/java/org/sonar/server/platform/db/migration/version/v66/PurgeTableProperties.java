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
package org.sonar.server.platform.db.migration.version.v66;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class PurgeTableProperties extends DataChange {

  public PurgeTableProperties(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    deleteByKey(context, "views.analysisDelayingInMinutes");
    deleteByKey(context, "views.status");
    deleteByKeyPrefix(context, "sonar.views.license");
    deleteByKeyPrefix(context, "views.license");
    deleteByKeyPrefix(context, "masterproject.");

    deleteByKeyPrefix(context, "sonar.sqale.");
    deleteByKeyPrefix(context, "sqale.license");
    deleteByKeyPrefix(context, "devcockpit.");
    deleteByKeyPrefix(context, "sonar.devcockpit.");

    deleteByKey(context, "sonar.core.version");
    deleteByKey(context, "sonar.issuesdensity.weight");
    deleteByKeyPrefix(context, "sonar.reports.");
    deleteByKeyPrefix(context, "sonar.report.license");
    deleteByKeyPrefix(context, "sonar.natural.");
    deleteByKeyPrefix(context, "sonarsource.natural.");
    deleteByKeyPrefix(context, "sonarsource.identity.");
    deleteByKeyPrefix(context, "sonar.build-stability.");
  }

  private static void deleteByKey(Context context, String key) throws SQLException {
    context.prepareUpsert("delete from properties where prop_key = ?")
      .setString(1, key)
      .execute()
      .commit();
  }

  private static void deleteByKeyPrefix(Context context, String key) throws SQLException {
    context.prepareUpsert("delete from properties where prop_key like ?")
      .setString(1, key + "%")
      .execute()
      .commit();
  }
}
