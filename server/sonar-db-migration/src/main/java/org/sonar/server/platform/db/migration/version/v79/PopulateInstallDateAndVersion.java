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
package org.sonar.server.platform.db.migration.version.v79;

import java.sql.SQLException;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;

@SupportsBlueGreen
public class PopulateInstallDateAndVersion extends DataChange {

  private static final Logger LOG = Loggers.get(PopulateInstallDateAndVersion.class);
  private static final String INSTALLATION_DATE = "installation.date";
  private static final String INSTALLATION_VERSION = "installation.version";
  private final SonarRuntime sonarRuntime;
  private final System2 system2;

  public PopulateInstallDateAndVersion(Database db, SonarRuntime sonarRuntime, System2 system2) {
    super(db);
    this.sonarRuntime = sonarRuntime;
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    removeProperties(context);
    Long createdAt = context.prepareSelect("select min(created_at) from users where created_at is not null")
      .get(row -> row.getLong(1));
    if (createdAt != null && createdAt != 0) {
      populateInstallationDate(context, createdAt);
      populateInstallationVersion(context, createdAt);
    }
  }

  private void populateInstallationDate(Context context, Long createdAt) throws SQLException {
    insertInternalProperty(context, INSTALLATION_DATE, String.valueOf(createdAt));
  }

  private void populateInstallationVersion(Context context, Long createdAt) throws SQLException {
    if (Math.abs(system2.now() - createdAt) / 60 / 60 / 1000 <= 24) {
      String apiVersion = sonarRuntime.getApiVersion().toString();
      insertInternalProperty(context, INSTALLATION_VERSION, apiVersion);
    } else {
      // if the difference between now and smallest account creation date is more than a day, we consider that this is a
      // start with an existing SQ, and not a fresh start. in this case, we do not populate the internalProperty,
      // as there is no way to know the original SQ installation version.
      LOG.warn("skipping " + INSTALLATION_VERSION + " because we cannot determine what is the installation version.");
    }
  }

  private void insertInternalProperty(Context context, String key, String value) throws SQLException {
    context.prepareUpsert("insert into internal_properties (kee, is_empty, text_value, clob_value, created_at) VALUES (?, ?, ?, ?, ?)")
      .setString(1, key)
      .setBoolean(2, false)
      .setString(3, value)
      .setString(4, null)
      .setLong(5, system2.now())
      .execute().commit().close();
  }

  private static void removeProperties(Context context) throws SQLException {
    context.prepareUpsert("delete from internal_properties where kee = ? or kee = ?")
      .setString(1, INSTALLATION_DATE)
      .setString(2, INSTALLATION_VERSION)
      .execute().commit().close();

  }
}
