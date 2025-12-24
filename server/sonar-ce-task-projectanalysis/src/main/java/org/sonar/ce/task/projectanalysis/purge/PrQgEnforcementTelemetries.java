/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.purge;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.purge.PurgeConfiguration;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeMapper;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;
import org.sonar.telemetry.core.schema.InstallationMetric;
import org.sonar.telemetry.core.schema.Metric;

import static java.util.Collections.singletonList;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.ce.task.projectanalysis.purge.PrQgEnforcementTelemetries.QualityGateStatus.ERROR;
import static org.sonar.ce.task.projectanalysis.purge.PrQgEnforcementTelemetries.QualityGateStatus.OK;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

@ServerSide
@ComputeEngineSide
public class PrQgEnforcementTelemetries {
  private static final String FAILED_COUNT_METRIC = "installation_pr_qg_failed_count";
  private static final String PASSED_COUNT_METRIC = "installation_pr_qg_passed_count";
  private final PurgeDao purgeDao;
  private final MeasureDao measureDao;
  private final Configuration configuration;
  private Set<Metric> metrics = new HashSet<>();

  enum QualityGateStatus {
    OK, ERROR
  }

  public PrQgEnforcementTelemetries(PurgeDao purgeDao, MeasureDao measureDao, Configuration configuration) {
    this.measureDao = measureDao;
    this.purgeDao = purgeDao;
    this.configuration = configuration;
  }

  public Set<Metric> getMetrics() {
    return new HashSet<>(metrics);
  }

  public void calculateMetrics(DbSession session, PurgeConfiguration conf) {
    if (!configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false)) {
      return;
    }
    Map<String, Long> qgStatusesCount = getQualityGateStatuses(session, conf);
    if (qgStatusesCount.containsKey(ERROR.name())) {
      InstallationMetric failedMetric = new InstallationMetric(
        FAILED_COUNT_METRIC,
        qgStatusesCount.get(ERROR.name()),
        TelemetryDataType.INTEGER,
        Granularity.ADHOC
      );
      metrics.add(failedMetric);
    }
    if (qgStatusesCount.containsKey(OK.name())) {
      InstallationMetric passedMetric = new InstallationMetric(
        PASSED_COUNT_METRIC,
        qgStatusesCount.get(OK.name()),
        TelemetryDataType.INTEGER,
        Granularity.ADHOC
      );
      metrics.add(passedMetric);
    }
  }

  public void resetMetrics() {
    metrics = new HashSet<>();
  }

  private Map<String, Long> getQualityGateStatuses(DbSession session, PurgeConfiguration conf) {
    List<String> prsToPurge = purgeDao.getStaleBranchesToPurge(conf, session.getMapper(PurgeMapper.class),
        conf.rootUuid()).stream()
      .filter(branchDto -> BranchType.PULL_REQUEST.equals(branchDto.getBranchType()))
      .map(BranchDto::getUuid)
      .toList();
    return measureDao.selectByComponentUuidsAndMetricKeys(session, prsToPurge, singletonList(ALERT_STATUS_KEY))
      .stream()
      .map(qgMeasureDto -> qgMeasureDto.getString(ALERT_STATUS_KEY))
      .filter(Objects::nonNull)
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }
}
