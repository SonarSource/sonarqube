/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.db.migrations.v43;

import java.sql.SQLException;
import java.util.Date;

import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

/**
 * Used in the Active Record Migration 513.
 * WARNING - this migration is not re-entrant.
 *
 * @since 4.3
 */
public class ConvertIssueDebtToMinutesMigrationStep extends BaseDataChange {

  private final WorkDurationConvertor workDurationConvertor;
  private final System2 system2;

  public ConvertIssueDebtToMinutesMigrationStep(Database database, PropertiesDao propertiesDao, System2 system2) {
    super(database);
    this.workDurationConvertor = new WorkDurationConvertor(propertiesDao);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    workDurationConvertor.init();
    final Date now = new Date(system2.now());
    MassUpdate massUpdate = context.prepareMassUpdate();

    // See https://jira.codehaus.org/browse/SONAR-5394
    // The SQL request should not set the filter on technical_debt is not null. There's no index
    // on this column, so filtering is done programmatically.
    massUpdate.select("select id, technical_debt from issues");

    massUpdate.update("update issues set technical_debt=?, updated_at=? where id=?");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Long debt = row.getNullableLong(2);
        if (debt != null) {
          Long id = row.getNullableLong(1);
          update.setLong(1, workDurationConvertor.createFromLong(debt));
          update.setDate(2, now);
          update.setLong(3, id);
          return true;
        }
        return false;
      }
    });
  }

}
