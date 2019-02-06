/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.ce.task.projectanalysis.analysis.Organization;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static org.sonar.core.util.stream.MoreCollectors.toList;

public class QualityGateServiceImpl implements QualityGateService {

  private final DbClient dbClient;
  private final MetricRepository metricRepository;

  public QualityGateServiceImpl(DbClient dbClient, MetricRepository metricRepository) {
    this.dbClient = dbClient;
    this.metricRepository = metricRepository;
  }

  @Override
  public Optional<QualityGate> findById(long id) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto qualityGateDto = dbClient.qualityGateDao().selectById(dbSession, id);
      if (qualityGateDto == null) {
        return Optional.empty();
      }
      return Optional.of(toQualityGate(dbSession, qualityGateDto));
    }
  }

  @Override
  public QualityGate findDefaultQualityGate(Organization organization) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto qualityGateDto = dbClient.qualityGateDao().selectByOrganizationAndUuid(dbSession, organization.toDto(), organization.getDefaultQualityGateUuid());
      if (qualityGateDto == null) {
        throw new IllegalStateException("The default Quality gate is missing on organization " + organization.getKey());
      }
      return toQualityGate(dbSession, qualityGateDto);
    }
  }

  private QualityGate toQualityGate(DbSession dbSession, QualityGateDto qualityGateDto) {
    Collection<QualityGateConditionDto> dtos = dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGateDto.getId());

    Iterable<Condition> conditions = dtos.stream()
      .map(input -> metricRepository.getOptionalById(input.getMetricId())
        .map(metric -> new Condition(metric, input.getOperator(), input.getErrorThreshold()))
        .orElse(null))
      .filter(Objects::nonNull)
      .collect(toList(dtos.size()));

    return new QualityGate(qualityGateDto.getId(), qualityGateDto.getName(), conditions);
  }
}
