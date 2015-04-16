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

package org.sonar.server.db.migrations.v44;

import java.sql.SQLException;

import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

/**
 * SONAR-5249
 * Merge measure data table into project_measure
 *
 * Used in the Active Record Migration 530.
 * @since 4.4
 */
public class MeasureDataMigrationStep extends BaseDataChange {

  public MeasureDataMigrationStep(Database database) {
    super(database);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.rowPluralName("measures");
    massUpdate.select("select md.id, md.measure_id FROM measure_data md " +
      "inner join project_measures m on m.id=md.measure_id and m.measure_data is null");
    massUpdate.update("update project_measures SET measure_data = (SELECT md.data FROM measure_data md WHERE md.id = ?) WHERE id=?");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Long id = row.getNullableLong(1);
        Long measureId = row.getNullableLong(2);

        update.setLong(1, id);
        update.setLong(2, measureId);
        return true;
      }
    });
  }
}
