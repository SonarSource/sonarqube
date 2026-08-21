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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ProjectLastAnalysisDateDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

/**
 * Onboarding funnel counts: how many projects exist, how many of those have been analysed at least once,
 * how many were imported via a DevOps platform rather than created some other way, and how many DevOps
 * platform integrations are configured. Mirrors the shape the onboarding dashboard already computes for
 * itself (OnboardingCounts in the onboarding-unified-app module), computed independently here since that
 * module's ports are Spring beans in the web-only context while telemetry providers are resolved from the
 * Pico container.
 *
 * <p>{@code alm_imported_projects} exists specifically to resolve the open "what does 'imported' mean"
 * question from the SQS LTA Onboarding Telemetry Gap Sheet (ALM_IMPORT_* creation methods only, vs.
 * {@code total_projects} which counts every project regardless of how it was created) — Data can pick
 * whichever denominator CS settles on without needing a second round of telemetry work.
 */
@ServerSide
public class TelemetryOnboardingCountsProvider extends AbstractTelemetryDataProvider<Integer> {

  public static final String METRIC_KEY = "onboarding_counts";

  public static final String KEY_TOTAL_PROJECTS = "total_projects";
  public static final String KEY_ANALYSED_PROJECTS = "analysed_projects";
  public static final String KEY_ALM_IMPORTED_PROJECTS = "alm_imported_projects";
  public static final String KEY_CONFIGURED_ALM = "configured_alm";

  private static final Set<CreationMethod> ALM_IMPORT_METHODS = EnumSet.of(
    CreationMethod.ALM_IMPORT_API, CreationMethod.ALM_IMPORT_BROWSER,
    CreationMethod.ALM_IMPORT_MONOREPO_API, CreationMethod.ALM_IMPORT_MONOREPO_BROWSER);

  private final DbClient dbClient;

  public TelemetryOnboardingCountsProvider(DbClient dbClient) {
    super(METRIC_KEY, Dimension.INSTALLATION, Granularity.DAILY, TelemetryDataType.INTEGER);
    this.dbClient = dbClient;
  }

  @Override
  public Map<String, Integer> getValues() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<ProjectDto> projects = dbClient.projectDao().selectProjects(dbSession);
      Set<String> projectUuids = projects.stream().map(ProjectDto::getUuid).collect(Collectors.toSet());
      List<ProjectLastAnalysisDateDto> analyses = executeLargeInputs(projectUuids,
        partition -> dbClient.snapshotDao().selectLastAnalysisDateByProjectUuids(dbSession, partition));

      Map<String, Integer> counts = new HashMap<>();
      counts.put(KEY_TOTAL_PROJECTS, projects.size());
      counts.put(KEY_ANALYSED_PROJECTS, analyses.size());
      counts.put(KEY_ALM_IMPORTED_PROJECTS, (int) projects.stream()
        .filter(project -> ALM_IMPORT_METHODS.contains(project.getCreationMethod()))
        .count());
      counts.put(KEY_CONFIGURED_ALM, dbClient.almSettingDao().selectAll(dbSession).size());
      return counts;
    }
  }
}
