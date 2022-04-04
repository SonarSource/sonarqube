/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.metrics.projectmeasures;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class DeleteSecurityReviewRatingProjectMeasures extends DataChange {

  private static final String SECURITY_REVIEW_RATING_METRIC_KEY = "security_review_rating";
  private static final String SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY = "security_review_rating_effort";

  public DeleteSecurityReviewRatingProjectMeasures(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String reviewRatingUuid = getMetricUuid(context, SECURITY_REVIEW_RATING_METRIC_KEY);
    String reviewRatingEffortUuid = getMetricUuid(context, SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY);
    if (reviewRatingUuid != null) {
      deleteFromProjectMeasures(context, reviewRatingUuid, reviewRatingEffortUuid);
    }
  }

  @Nullable
  private static String getMetricUuid(Context context, String metricName) throws SQLException {
    return context.prepareSelect("select uuid from metrics where name = ?")
      .setString(1, metricName)
      .get(row -> row.getNullableString(1));
  }

  private static void deleteFromProjectMeasures(Context context, String reviewRatingUuid, @Nullable String reviewRatingEffortUuid) throws SQLException {
    deleteFromProjectMeasures(context, reviewRatingUuid);

    if (reviewRatingEffortUuid != null) {
      deleteFromProjectMeasures(context, reviewRatingEffortUuid);
    }
  }

  private static void deleteFromProjectMeasures(Context context, @Nullable String metricUuid) throws SQLException {
    if (metricUuid == null) {
      return;
    }
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid from project_measures where metric_uuid = ?")
      .setString(1, metricUuid);
    massUpdate.update("delete from project_measures where uuid = ?");
    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(1));
      return true;
    });
  }
}
