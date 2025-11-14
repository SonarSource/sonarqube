/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202501;

import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class DeleteRemovedComplexityMeasuresFromProjectMeasures extends DataChange {

  public static final Set<String> COMPLEXITY_METRICS_TO_DELETE = Set.of(
    "file_complexity",
    "complexity_in_classes",
    "class_complexity",
    "complexity_in_functions",
    "function_complexity",
    "function_complexity_distribution",
    "file_complexity_distribution");

  private static final String SELECT_QUERY = """
    select pm.uuid from project_measures pm
      inner join metrics m on pm.metric_uuid = m.uuid
     where m.name in (%s)
    """.formatted(COMPLEXITY_METRICS_TO_DELETE.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));

  public DeleteRemovedComplexityMeasuresFromProjectMeasures(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update("delete from project_measures where uuid = ?");

    massUpdate.execute((row, update, index) -> {
      update.setString(1, row.getString(1));
      return true;
    });
  }
}
