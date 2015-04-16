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
import java.util.Date;

import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

/**
 * SONAR-5218
 * Update all issues having action plan linked on removed action plan.
 * <p/>
 * Used in the Active Record Migration 531.
 *
 * @since 4.4
 */
public class IssueActionPlanKeyMigrationStep extends BaseDataChange {

  private final System2 system2;

  public IssueActionPlanKeyMigrationStep(Database database, System2 system2) {
    super(database);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    final Date now = new Date(system2.now());
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT i.id FROM issues i " +
      "LEFT OUTER JOIN action_plans ap ON ap.kee=i.action_plan_key " +
      "WHERE i.action_plan_key IS NOT NULL " +
      "AND ap.kee is null");
    massUpdate.update("UPDATE issues SET action_plan_key=NULL,updated_at=? WHERE id=?");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Long id = row.getNullableLong(1);

        update.setDate(1, now);
        update.setLong(2, id);
        return true;
      }
    });
  }
}
