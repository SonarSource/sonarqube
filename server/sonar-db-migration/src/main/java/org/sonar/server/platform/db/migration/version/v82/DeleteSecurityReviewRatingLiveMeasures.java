/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

public class DeleteSecurityReviewRatingLiveMeasures extends DataChange {

  private static final String SECURITY_REVIEW_RATING_METRIC_KEY = "security_review_rating";
  private static final String SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY = "security_review_rating_effort";

  public DeleteSecurityReviewRatingLiveMeasures(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Integer reviewRatingId = getMetricId(context, SECURITY_REVIEW_RATING_METRIC_KEY);
    Integer reviewRatingEffortId = getMetricId(context, SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY);
    deleteMetricFromLiveMeasures(context, reviewRatingId);
    deleteMetricFromLiveMeasures(context, reviewRatingEffortId);
  }

  @Nullable
  private static Integer getMetricId(Context context, String metricName) throws SQLException {
    return context.prepareSelect("select id from metrics where name = ?")
      .setString(1, metricName)
      .get(row -> row.getNullableInt(1));
  }

  private static void deleteMetricFromLiveMeasures(Context context, @Nullable Integer metricId) throws SQLException {
    if (metricId == null) {
      return;
    }
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select lm.uuid from live_measures lm inner join components c on lm.component_uuid = c.uuid and lm.metric_id = ?")
      .setInt(1, metricId);
    massUpdate.update("delete from live_measures where uuid = ?");

    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(1));
      return true;
    });
  }
}
