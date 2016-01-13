/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v52;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

/**
 * Remove all measures linked to a rule related to following metrics :
 * <ul>
 *   <li>blocker_violations</li>
 *   <li>critical_violations</li>
 *   <li>major_violations</li>
 *   <li>minor_violations</li>
 *   <li>info_violations</li>
 *   <li>new_blocker_violations</li>
 *   <li>new_critical_violations</li>
 *   <li>new_major_violations</li>
 *   <li>new_minor_violations</li>
 *   <li>new_info_violations</li>
 *   </ul>
 */
public class RemoveRuleMeasuresOnIssues extends BaseDataChange {

  public RemoveRuleMeasuresOnIssues(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("measures");
    update.select("SELECT p.id FROM project_measures p " +
      "INNER JOIN metrics m ON m.id=p.metric_id AND m.name IN " +
      "('blocker_violations', 'critical_violations', 'major_violations', 'minor_violations', 'info_violations', " +
      "'new_blocker_violations', 'new_critical_violations', 'new_major_violations', 'new_minor_violations', 'new_info_violations') " +
      "WHERE p.rule_id IS NOT NULL");
    update.update("DELETE FROM project_measures WHERE id=?");
    update.execute(MigrationHandler.INSTANCE);
  }

  private enum MigrationHandler implements MassUpdate.Handler {
    INSTANCE;

    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      update.setLong(1, row.getLong(1));
      return true;
    }
  }

}
