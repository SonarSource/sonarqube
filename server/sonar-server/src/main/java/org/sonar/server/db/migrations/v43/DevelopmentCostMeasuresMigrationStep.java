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

import org.sonar.core.persistence.Database;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

import javax.annotation.CheckForNull;

import java.sql.SQLException;

/**
 * Used in the Active Record Migration 516
 *
 * @since 4.3
 */
public class DevelopmentCostMeasuresMigrationStep extends BaseDataChange {

  private final WorkDurationConvertor workDurationConvertor;

  public DevelopmentCostMeasuresMigrationStep(Database database, PropertiesDao propertiesDao) {
    super(database);
    this.workDurationConvertor = new WorkDurationConvertor(propertiesDao);
  }

  @Override
  public void execute(Context context) throws SQLException {
    workDurationConvertor.init();

    Long metricId = context.prepareSelect("select id from metrics where name='development_cost'").get(Select.LONG_READER);
    if (metricId != null) {
      MassUpdate massUpdate = context.prepareMassUpdate();
      massUpdate.select("select id, value from project_measures where metric_id=? and value is not null").setLong(1, metricId);
      massUpdate.update("update project_measures set value=NULL,text_value=? where id=?");
      massUpdate.execute(new MassUpdate.Handler() {
        @Override
        public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
          Long id = row.getLong(1);
          Double value = row.getDouble(2);

          update.setString(1, convertDebtForDays(value));
          update.setLong(2, id);
          return true;
        }
      });
    }
  }

  @CheckForNull
  private String convertDebtForDays(Double data) {
    return Long.toString(workDurationConvertor.createFromDays(data));
  }
}
