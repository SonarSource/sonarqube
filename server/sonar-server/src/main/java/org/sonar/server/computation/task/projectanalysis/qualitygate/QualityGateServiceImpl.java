/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.qualitygate;

import com.google.common.base.Optional;
import java.util.Collection;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;

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
        return Optional.absent();
      }
      return Optional.of(toQualityGate(dbSession, qualityGateDto));
    }
  }

  private QualityGate toQualityGate(DbSession dbSession, QualityGateDto qualityGateDto) {
    Collection<QualityGateConditionDto> dtos = dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGateDto.getId());

    Iterable<Condition> conditions = dtos.stream()
      .map(input -> {
        Metric metric = metricRepository.getById(input.getMetricId());
        return new Condition(metric, input.getOperator(), input.getErrorThreshold(), input.getWarningThreshold(), input.getPeriod() != null);
      })
      .collect(MoreCollectors.toList(dtos.size()));

    return new QualityGate(qualityGateDto.getId(), qualityGateDto.getName(), conditions);
  }

}
