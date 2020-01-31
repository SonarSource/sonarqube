/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v82;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class DeleteSecurityReviewRatingMeasures extends DataChange {

  private static final String SECURITY_REVIEW_RATING_METRIC_KEY = "security_review_rating";
  private static final String SELECT_COMPONENTS_STATEMENT = "select c.uuid from components c where c.scope in ('PRJ') and c.qualifier in ('VW', 'SVW', 'APP', 'TRK')";

  public DeleteSecurityReviewRatingMeasures(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Integer metricId = getSecurityReviewRatingMetricId(context);
    if (metricId != null) {
      deleteFromProjectMeasures(context, metricId);
      deleteFromLiveMeasures(context, metricId);
    }
  }

  @Nullable
  private static Integer getSecurityReviewRatingMetricId(Context context) throws SQLException {
    return context.prepareSelect("select id from metrics where name = ?")
      .setString(1, SECURITY_REVIEW_RATING_METRIC_KEY)
      .get(row -> row.getNullableInt(1));
  }

  private static void deleteFromLiveMeasures(Context context, Integer metricId) throws SQLException {
    MassUpdate deleteFromLiveMeasures = context.prepareMassUpdate();

    deleteFromLiveMeasures.select(SELECT_COMPONENTS_STATEMENT);
    deleteFromLiveMeasures.update("delete from live_measures where project_uuid = ? and metric_id = ?");

    deleteFromLiveMeasures.execute((row, update) -> {
      update.setString(1, row.getString(1));
      update.setInt(2, metricId);
      return true;
    });
  }

  private static void deleteFromProjectMeasures(Context context, Integer metricId) throws SQLException {
    MassUpdate deleteFromProjectMeasures = context.prepareMassUpdate();

    deleteFromProjectMeasures.select(SELECT_COMPONENTS_STATEMENT);
    deleteFromProjectMeasures.update("delete from project_measures where component_uuid = ? and metric_id = ?");

    deleteFromProjectMeasures.execute((row, update) -> {
      update.setString(1, row.getString(1));
      update.setInt(2, metricId);
      return true;
    });
  }
}
