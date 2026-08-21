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
import java.util.HashSet;
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
import org.sonar.db.project.ProjectDto;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

/**
 * How projects are spread across DevOps platforms: one count per ALM type, plus a {@code not_bound}
 * bucket for projects with no ALM binding at all. Every known ALM id is always reported, even at zero,
 * so a customer with no GitLab projects reports {@code gitlab: 0} rather than omitting the key entirely.
 *
 * <p>{@code not_bound_scanned} narrows {@code not_bound} to the ones that have actually been analysed —
 * the "traditional onboarding" pattern called out both in the original telemetry gap-mapping session and
 * the SQS LTA Onboarding Telemetry Gap Sheet's "additional situations to track": a repo scanned via CI
 * before ever being bound to a DevOps platform, which CS treats as a churn/binding-hygiene risk signal.
 */
@ServerSide
public class TelemetryOnboardingBoundProjectsByAlmProvider extends AbstractTelemetryDataProvider<Integer> {

  public static final String METRIC_KEY = "onboarding_bound_projects_by_alm";
  public static final String KEY_NOT_BOUND = "not_bound";
  public static final String KEY_NOT_BOUND_SCANNED = "not_bound_scanned";

  private final DbClient dbClient;

  public TelemetryOnboardingBoundProjectsByAlmProvider(DbClient dbClient) {
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

      Set<String> allProjectUuids = dbClient.projectDao().selectProjects(dbSession).stream()
        .map(ProjectDto::getUuid)
        .collect(Collectors.toSet());

      List<ProjectAlmKeyAndProject> bindings = dbClient.projectAlmSettingDao().selectAlmTypeAndUrlByProject(dbSession);
      bindings.forEach(binding -> counts.merge(binding.getAlmId(), 1, Integer::sum));

      Set<String> unboundProjectUuids = new HashSet<>(allProjectUuids);
      bindings.forEach(binding -> unboundProjectUuids.remove(binding.getProjectUuid()));
      counts.put(KEY_NOT_BOUND, unboundProjectUuids.size());

      List<ProjectLastAnalysisDateDto> unboundAnalyses = executeLargeInputs(unboundProjectUuids,
        partition -> dbClient.snapshotDao().selectLastAnalysisDateByProjectUuids(dbSession, partition));
      counts.put(KEY_NOT_BOUND_SCANNED, unboundAnalyses.size());

      return counts;
    }
  }
}
