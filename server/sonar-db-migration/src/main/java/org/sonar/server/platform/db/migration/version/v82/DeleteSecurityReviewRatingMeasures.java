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
  private static final String SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY = "security_review_rating_effort";
  private static final String SELECT_COMPONENTS_STATEMENT = "select c.uuid from components c where c.scope in ('PRJ') and c.qualifier in ('VW', 'SVW', 'APP', 'TRK')";

  public DeleteSecurityReviewRatingMeasures(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Integer reviewRatingId = getMetricId(context, SECURITY_REVIEW_RATING_METRIC_KEY);
    Integer reviewRatingEffortId = getMetricId(context, SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY);
    if (reviewRatingId != null) {
      deleteFromProjectMeasures(context, reviewRatingId, reviewRatingEffortId);
      deleteFromLiveMeasures(context, reviewRatingId, reviewRatingEffortId);
    }
  }

  @Nullable
  private static Integer getMetricId(Context context, String metricName) throws SQLException {
    return context.prepareSelect("select id from metrics where name = ?")
      .setString(1, metricName)
      .get(row -> row.getNullableInt(1));
  }

  private static void deleteFromLiveMeasures(Context context, Integer reviewRatingId, @Nullable Integer reviewRatingEffortId) throws SQLException {
    MassUpdate deleteFromLiveMeasures = context.prepareMassUpdate();

    deleteFromLiveMeasures.select(SELECT_COMPONENTS_STATEMENT);
    if (reviewRatingEffortId != null) {
      deleteFromLiveMeasures.update("delete from live_measures where project_uuid = ? and metric_id in (?, ?)");
    } else {
      deleteFromLiveMeasures.update("delete from live_measures where project_uuid = ? and metric_id = ?");
    }

    deleteFromLiveMeasures.execute((row, update) -> {
      String projectUuid = row.getString(1);
      update.setString(1, projectUuid)
        .setInt(2, reviewRatingId);
      if (reviewRatingEffortId != null) {
        update.setInt(3, reviewRatingEffortId);
      }
      return true;
    });
  }

  private static void deleteFromProjectMeasures(Context context, Integer reviewRatingId, @Nullable Integer reviewRatingEffortId) throws SQLException {
    MassUpdate deleteFromProjectMeasures = context.prepareMassUpdate();

    deleteFromProjectMeasures.select(SELECT_COMPONENTS_STATEMENT);
    if (reviewRatingEffortId != null) {
      deleteFromProjectMeasures.update("delete from project_measures where component_uuid = ? and metric_id in (?, ?)");
    } else {
      deleteFromProjectMeasures.update("delete from project_measures where component_uuid = ? and metric_id = ?");
    }

    deleteFromProjectMeasures.execute((row, update) -> {
      String componentUuid = row.getString(1);
      update.setString(1, componentUuid)
        .setInt(2, reviewRatingId);
      if (reviewRatingEffortId != null) {
        update.setInt(3, reviewRatingEffortId);
      }
      return true;
    });
  }
}
