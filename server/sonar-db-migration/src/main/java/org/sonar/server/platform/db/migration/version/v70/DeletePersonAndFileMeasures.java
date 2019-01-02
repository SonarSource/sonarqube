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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import org.sonar.api.config.Configuration;
import org.sonar.db.Database;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class DeletePersonAndFileMeasures extends DataChange {

  private final Configuration configuration;

  public DeletePersonAndFileMeasures(Database db, Configuration configuration) {
    super(db);
    this.configuration = configuration;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (configuration.getBoolean("sonar.sonarcloud.enabled").orElse(false)) {
      // clean-up will be done in background so that interruption of service
      // is reduced during upgrade
      return;
    }
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid from snapshots");
    massUpdate.rowPluralName("snapshots");
    massUpdate.update(getDeleteSql()).setBatchSize(1);

    massUpdate.execute((row, update) -> {
      String analysisUuid = row.getString(1);
      update.setString(1, analysisUuid);
      if (getDialect().getId().equals(Oracle.ID)) {
        update.setString(2, analysisUuid);
      }
      return true;
    });
  }

  private String getDeleteSql() {
    switch (getDialect().getId()) {
      case MySql.ID:
      case MsSql.ID:
        return "delete pm from project_measures pm " +
          "inner join projects c on c.uuid = pm.component_uuid " +
          "where pm.analysis_uuid = ? " +
          "and (c.qualifier in ('UTS', 'FIL') or pm.person_id is not null)";
      case H2.ID:
        return "delete from project_measures " +
          "where id in ( " +
          "  select pm.id from project_measures pm " +
          "  inner join projects c on c.uuid = pm.component_uuid " +
          "  where pm.analysis_uuid = ? " +
          "  and (c.qualifier in ('UTS', 'FIL') or pm.person_id is not null) " +
          ")";
      case PostgreSql.ID:
        return "delete from project_measures pm " +
          "using projects c " +
          "where pm.analysis_uuid = ? " +
          "and c.uuid = pm.component_uuid " +
          "and (c.qualifier in ('UTS', 'FIL') or pm.person_id is not null) ";
      case Oracle.ID:
        return "delete from project_measures pm where exists (" +
          "  select 1 from project_measures pm2 " +
          "  inner join projects c on c.uuid = pm2.component_uuid " +
          "  where pm2.analysis_uuid = ? " +
          "  and (c.qualifier in ('UTS', 'FIL') or pm.person_id is not null) " +
          "  and pm.id = pm2.id" +
          ") and pm.analysis_uuid = ?";
      default:
        throw new IllegalStateException("Unsupported DB dialect: " + getDialect());
    }
  }
}
