/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;

import static com.google.common.collect.FluentIterable.from;

public class QualityGateServiceImpl implements QualityGateService {
  private final QualityGateDao qualityGateDao;
  private final QualityGateConditionDao conditionDao;
  private final Function<QualityGateConditionDto, Condition> conditionDtoToBean;

  public QualityGateServiceImpl(QualityGateDao qualityGateDao, QualityGateConditionDao conditionDao, final MetricRepository metricRepository) {
    this.qualityGateDao = qualityGateDao;
    this.conditionDao = conditionDao;
    this.conditionDtoToBean = (QualityGateConditionDto input) -> {
      Metric metric = metricRepository.getById(input.getMetricId());
      return new Condition(metric, input.getOperator(), input.getErrorThreshold(), input.getWarningThreshold(), input.getPeriod());
    };
  }

  @Override
  public Optional<QualityGate> findById(long id) {
    QualityGateDto qualityGateDto = qualityGateDao.selectById(id);
    if (qualityGateDto == null) {
      return Optional.absent();
    }
    return Optional.of(toQualityGate(qualityGateDto));
  }

  private QualityGate toQualityGate(QualityGateDto qualityGateDto) {
    Iterable<Condition> conditions = from(conditionDao.selectForQualityGate(qualityGateDto.getId())).transform(conditionDtoToBean);

    return new QualityGate(qualityGateDto.getId(), qualityGateDto.getName(), conditions);
  }

}
