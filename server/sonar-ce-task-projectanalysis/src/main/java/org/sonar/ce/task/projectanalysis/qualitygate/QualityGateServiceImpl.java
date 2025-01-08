/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.qualitygate;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.project.Project;

public class QualityGateServiceImpl implements QualityGateService {
  private final DbClient dbClient;
  private final MetricRepository metricRepository;

  public QualityGateServiceImpl(DbClient dbClient, MetricRepository metricRepository) {
    this.dbClient = dbClient;
    this.metricRepository = metricRepository;
  }

  @Override
  public QualityGate findEffectiveQualityGate(Project project) {
    return findQualityGate(project).orElseGet(this::findDefaultQualityGate);
  }

  private QualityGate findDefaultQualityGate() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto qualityGateDto = dbClient.qualityGateDao().selectDefault(dbSession);
      if (qualityGateDto == null) {
        throw new IllegalStateException("The default Quality gate is missing");
      }
      return toQualityGate(dbSession, qualityGateDto);
    }
  }

  private Optional<QualityGate> findQualityGate(Project project) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return Optional.ofNullable(dbClient.qualityGateDao().selectByProjectUuid(dbSession, project.getUuid()))
        .map(qg -> toQualityGate(dbSession, qg));
    }
  }

  private QualityGate toQualityGate(DbSession dbSession, QualityGateDto qualityGateDto) {
    Collection<QualityGateConditionDto> dtos = dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGateDto.getUuid());

    Collection<Condition> conditions = dtos.stream()
      .map(input -> metricRepository.getOptionalByUuid(input.getMetricUuid())
        .map(metric -> new Condition(metric, input.getOperator(), input.getErrorThreshold()))
        .orElse(null))
      .filter(Objects::nonNull)
      .toList();

    return new QualityGate(qualityGateDto.getUuid(), qualityGateDto.getName(), conditions);
  }
}
