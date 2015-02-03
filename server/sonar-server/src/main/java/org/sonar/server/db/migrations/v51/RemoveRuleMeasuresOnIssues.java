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

package org.sonar.server.db.migrations.v51;

import com.google.common.base.Joiner;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.Upsert;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class RemoveRuleMeasuresOnIssues extends BaseDataChange {

  public RemoveRuleMeasuresOnIssues(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    List<Long> metricIds = context
      .prepareSelect("SELECT m.id FROM metrics m WHERE m.name IN ( " +
        "'blocker_violations', 'critical_violations', 'major_violations', 'minor_violations', 'info_violations', " +
        "'new_blocker_violations', 'new_critical_violations', 'new_major_violations', 'new_minor_violations', 'new_info_violations')")
      .list(Select.LONG_READER);
    if (!metricIds.isEmpty()) {
      String sql = "DELETE FROM project_measures pm WHERE pm.metric_id IN (";
      String[] parameters = new String[metricIds.size()];
      Arrays.fill(parameters, "?");
      sql += Joiner.on(",").join(parameters);
      sql += ")";
      Upsert delete = context.prepareUpsert(sql);
      for (int index = 0; index < metricIds.size(); index++) {
        delete.setLong(index + 1, metricIds.get(index));
      }
      delete.execute().commit();
    }
  }

}
