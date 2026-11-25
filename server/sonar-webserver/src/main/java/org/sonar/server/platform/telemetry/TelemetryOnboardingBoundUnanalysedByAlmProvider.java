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
import java.util.stream.Collectors;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.ProjectAlmKeyAndProject;
import org.sonar.db.component.ProjectLastAnalysisDateDto;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

/**
 * Per DevOps platform, how many bound projects have never been analysed: the "imported but never scanned" funnel
 * drop-off. A project counts as analysed with the exact definition used by {@code analysed_projects} in
 * {@link TelemetryOnboardingCountsProvider} and by {@code not_bound_scanned} in
 * {@link TelemetryOnboardingBoundProjectsByAlmProvider} (a last analysis on any branch), so that
 * {@code onboarding_bound_projects_by_alm[alm] - onboarding_bound_unanalysed_by_alm[alm]} is the number of analysed
 * projects bound to that platform. Every known ALM id is always reported, even at zero.
 */
@ServerSide
public class TelemetryOnboardingBoundUnanalysedByAlmProvider extends AbstractTelemetryDataProvider<Integer> {

  public static final String METRIC_KEY = "onboarding_bound_unanalysed_by_alm";

  private final DbClient dbClient;

  public TelemetryOnboardingBoundUnanalysedByAlmProvider(DbClient dbClient) {
    super(METRIC_KEY, Dimension.INSTALLATION, Granularity.DAILY, TelemetryDataType.INTEGER);
    this.dbClient = dbClient;
  }

  @Override
  public Map<String, Integer> getValues() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, Integer> counts = new HashMap<>();
      for (ALM alm : ALM.values()) {
        counts.put(alm.getId(), 0);
      }

      List<ProjectAlmKeyAndProject> bindings = dbClient.projectAlmSettingDao().selectAlmTypeAndUrlByProject(dbSession);
      Set<String> boundProjectUuids = bindings.stream()
        .map(ProjectAlmKeyAndProject::getProjectUuid)
        .collect(Collectors.toSet());
      Set<String> analysedProjectUuids = executeLargeInputs(boundProjectUuids,
        partition -> dbClient.snapshotDao().selectLastAnalysisDateByProjectUuids(dbSession, partition)).stream()
        .map(ProjectLastAnalysisDateDto::getProjectUuid)
        .collect(Collectors.toSet());

      bindings.stream()
        .filter(binding -> !analysedProjectUuids.contains(binding.getProjectUuid()))
        .forEach(binding -> counts.merge(binding.getAlmId(), 1, Integer::sum));
      return counts;
    }
  }
}
