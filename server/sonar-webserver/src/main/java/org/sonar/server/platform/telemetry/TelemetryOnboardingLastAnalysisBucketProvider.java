/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.telemetry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ProjectLastAnalysisDateDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

/**
 * Project activity distribution, bucketed by how long ago each project was last analysed (on any
 * branch — the cheapest bulk query available, and arguably a better activity signal than main-branch-only
 * since it also counts PR/feature-branch scans). Buckets, not raw per-project dates, so this stays one
 * small aggregate metric regardless of installation size.
 */
@ServerSide
public class TelemetryOnboardingLastAnalysisBucketProvider extends AbstractTelemetryDataProvider<Integer> {

  public static final String METRIC_KEY = "onboarding_last_analysis_bucket";

  private static final long DAY_MILLIS = TimeUnit.DAYS.toMillis(1);
  public static final String BUCKET_LE_7D = "le_7d";
  public static final String BUCKET_LE_30D = "le_30d";
  public static final String BUCKET_LE_180D = "le_180d";
  public static final String BUCKET_GT_180D = "gt_180d";
  public static final String BUCKET_NEVER = "never";

  private final DbClient dbClient;
  private final System2 system2;

  public TelemetryOnboardingLastAnalysisBucketProvider(DbClient dbClient, System2 system2) {
    super(METRIC_KEY, Dimension.INSTALLATION, Granularity.DAILY, TelemetryDataType.INTEGER);
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  @Override
  public Map<String, Integer> getValues() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<String> projectUuids = dbClient.projectDao().selectProjects(dbSession).stream()
        .map(ProjectDto::getUuid)
        .collect(Collectors.toSet());
      List<ProjectLastAnalysisDateDto> analyses = executeLargeInputs(projectUuids,
        partition -> dbClient.snapshotDao().selectLastAnalysisDateByProjectUuids(dbSession, partition));

      Map<String, Integer> buckets = new HashMap<>();
      buckets.put(BUCKET_LE_7D, 0);
      buckets.put(BUCKET_LE_30D, 0);
      buckets.put(BUCKET_LE_180D, 0);
      buckets.put(BUCKET_GT_180D, 0);
      buckets.put(BUCKET_NEVER, projectUuids.size() - analyses.size());

      long now = system2.now();
      for (ProjectLastAnalysisDateDto analysis : analyses) {
        buckets.merge(bucketFor(now - analysis.getDate()), 1, Integer::sum);
      }
      return buckets;
    }
  }

  private static String bucketFor(long ageMillis) {
    if (ageMillis <= 7 * DAY_MILLIS) {
      return BUCKET_LE_7D;
    }
    if (ageMillis <= 30 * DAY_MILLIS) {
      return BUCKET_LE_30D;
    }
    if (ageMillis <= 180 * DAY_MILLIS) {
      return BUCKET_LE_180D;
    }
    return BUCKET_GT_180D;
  }
}
