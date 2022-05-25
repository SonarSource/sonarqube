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
package org.sonar.server.measure.live;

import java.util.Optional;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;

import static org.sonar.server.security.SecurityReviewRating.computePercent;
import static org.sonar.server.security.SecurityReviewRating.computeRating;

public class HotspotMeasureUpdater {
  private final DbClient dbClient;

  public HotspotMeasureUpdater(DbClient dbClient) {
    this.dbClient = dbClient;
  }
  public void apply(DbSession dbSession, MeasureMatrix matrix, ComponentIndex components, boolean useLeakFormulas, long beginningOfLeak) {
    HotspotsCounter hotspotsCounter = new HotspotsCounter(dbClient.issueDao().selectBranchHotspotsCount(dbSession, components.getBranch().uuid(), beginningOfLeak));
    ComponentDto branch = components.getBranch();

    long reviewed = hotspotsCounter.countHotspotsByStatus(Issue.STATUS_REVIEWED, false);
    long toReview = hotspotsCounter.countHotspotsByStatus(Issue.STATUS_TO_REVIEW, false);
    matrix.setValue(branch, CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY, reviewed);
    matrix.setValue(branch, CoreMetrics.SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY, toReview);
    Optional<Double> percent = computePercent(toReview, reviewed);
    percent.ifPresent(p -> matrix.setValue(branch, CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY, p));
    matrix.setValue(branch, CoreMetrics.SECURITY_REVIEW_RATING_KEY, computeRating(percent.orElse(null)));

    if (useLeakFormulas) {
      reviewed = hotspotsCounter.countHotspotsByStatus(Issue.STATUS_REVIEWED, true);
      toReview = hotspotsCounter.countHotspotsByStatus(Issue.STATUS_TO_REVIEW, true);
      matrix.setLeakValue(branch, CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY, reviewed);
      matrix.setLeakValue(branch, CoreMetrics.NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY, toReview);
      percent = computePercent(toReview, reviewed);
      percent.ifPresent(p -> matrix.setLeakValue(branch, CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY, p));
      matrix.setLeakValue(branch, CoreMetrics.NEW_SECURITY_REVIEW_RATING_KEY, computeRating(percent.orElse(null)));
    }
  }
}
