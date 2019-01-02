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
package org.sonar.server.platform.db.migration.version.v73;

import java.sql.SQLException;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

@SupportsBlueGreen
public class PopulateSubscriptionOnOrganizations extends DataChange {

  private final System2 system2;
  private final Configuration configuration;

  public PopulateSubscriptionOnOrganizations(Database db, System2 system2, Configuration configuration) {
    super(db);
    this.system2 = system2;
    this.configuration = configuration;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (configuration.getBoolean("sonar.sonarcloud.enabled").orElse(false)) {
      executeOnSonarCloud(context);
    } else {
      executeOnSonarQube(context);
    }
  }

  private void executeOnSonarQube(Context context) throws SQLException {
    long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("organizations");
    massUpdate.select("SELECT o.uuid FROM organizations o WHERE o.subscription IS NULL ");
    massUpdate.update("UPDATE organizations SET subscription=?, updated_at=? WHERE uuid=?");
    massUpdate.execute((row, update) -> {
      String uuid = row.getString(1);
      update.setString(1, "SONARQUBE");
      update.setLong(2, now);
      update.setString(3, uuid);
      return true;
    });
  }

  private void executeOnSonarCloud(Context context) throws SQLException {
    long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("organizations");
    massUpdate.select("SELECT o.uuid, count(p.uuid) FROM organizations o " +
      "LEFT OUTER JOIN projects p on p.organization_uuid=o.uuid AND p.private=? " +
      "WHERE subscription IS NULL " +
      "GROUP BY o.uuid")
      .setBoolean(1, true);
    massUpdate.update("UPDATE organizations SET subscription=?, updated_at=? WHERE uuid=?");
    massUpdate.execute((row, update) -> {
      String uuid = row.getString(1);
      long privateProjectsCount = row.getLong(2);
      update.setString(1, privateProjectsCount > 0 ? "PAID" : "FREE");
      update.setLong(2, now);
      update.setString(3, uuid);
      return true;
    });
  }

}
