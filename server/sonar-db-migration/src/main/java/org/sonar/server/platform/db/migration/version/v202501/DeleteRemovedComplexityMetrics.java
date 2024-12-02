/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.stream.Collectors;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.sonar.server.platform.db.migration.version.v202501.DeleteRemovedComplexityMeasuresFromProjectMeasures.COMPLEXITY_METRICS_TO_DELETE;

public class DeleteRemovedComplexityMetrics extends DataChange {

  private static final String DELETE_QUERY = """
    delete
    from metrics
    where name in (%s)
    """.formatted(COMPLEXITY_METRICS_TO_DELETE.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));


  public DeleteRemovedComplexityMetrics(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    context.prepareUpsert(DELETE_QUERY).execute().commit();
  }
}
