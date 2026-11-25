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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.AnalysisPropertyValuePerProject;
import org.sonar.db.component.ProjectLastAnalysisDateDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.sonar.core.config.CorePropertyDefinitions.SONAR_ANALYSIS_DETECTEDCI;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

/**
 * Analysed projects split by whether the scanner detected a CI system on the last analysis of the main branch
 * ({@code sonar.analysis.detectedci}, the same convention as the onboarding dashboard and the legacy telemetry).
 * {@code ci_detected} counts the projects with a detected CI system; {@code ci_undetected} counts the analysed
 * projects with no CI system detected by the scanner, which includes local scans and unrecognised CI systems.
 * A project counts as analysed with the exact definition used by {@code analysed_projects} in
 * {@link TelemetryOnboardingCountsProvider}, so {@code ci_detected + ci_undetected} always equals
 * {@code analysed_projects}. Projects never analysed are reported by {@code onboarding_last_analysis_bucket}.
 */
@ServerSide
public class TelemetryOnboardingProjectsByAnalysisModeProvider extends AbstractTelemetryDataProvider<Integer> {

  public static final String METRIC_KEY = "onboarding_projects_by_analysis_mode";

  public static final String KEY_CI_DETECTED = "ci_detected";
  public static final String KEY_CI_UNDETECTED = "ci_undetected";

  private static final String CI_UNDETECTED = "undetected";

  private final DbClient dbClient;

  public TelemetryOnboardingProjectsByAnalysisModeProvider(DbClient dbClient) {
    super(METRIC_KEY, Dimension.INSTALLATION, Granularity.DAILY, TelemetryDataType.INTEGER);
    this.dbClient = dbClient;
  }

  @Override
  public Map<String, Integer> getValues() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<String> projectUuids = dbClient.projectDao().selectProjects(dbSession).stream()
        .map(ProjectDto::getUuid)
        .collect(Collectors.toSet());
      Set<String> analysedProjectUuids = executeLargeInputs(projectUuids,
        partition -> dbClient.snapshotDao().selectLastAnalysisDateByProjectUuids(dbSession, partition)).stream()
        .map(ProjectLastAnalysisDateDto::getProjectUuid)
        .collect(Collectors.toSet());
      Set<String> ciProjectUuids = dbClient.analysisPropertiesDao()
        .selectAnalysisPropertyValueInLastAnalysisPerProject(dbSession, SONAR_ANALYSIS_DETECTEDCI).stream()
        .filter(property -> isCiDetected(property.getPropertyValue()))
        .map(AnalysisPropertyValuePerProject::getProjectUuid)
        .collect(Collectors.toSet());

      int ci = (int) analysedProjectUuids.stream().filter(ciProjectUuids::contains).count();
      Map<String, Integer> counts = new HashMap<>();
      counts.put(KEY_CI_DETECTED, ci);
      counts.put(KEY_CI_UNDETECTED, analysedProjectUuids.size() - ci);
      return counts;
    }
  }

  private static boolean isCiDetected(@Nullable String detectedCi) {
    return detectedCi != null && !detectedCi.isBlank() && !CI_UNDETECTED.equalsIgnoreCase(detectedCi);
  }
}
