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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.junit.Test;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualityGateServiceImplTest {
  private static final long SOME_ID = 123;
  private static final String SOME_NAME = "some name";
  private static final QualityGateDto QUALITY_GATE_DTO = new QualityGateDto().setId(SOME_ID).setName(SOME_NAME);
  private static final long METRIC_ID_1 = 951;
  private static final long METRIC_ID_2 = 753;
  private static final Metric METRIC_1 = mock(Metric.class);
  private static final Metric METRIC_2 = mock(Metric.class);
  private static final QualityGateConditionDto CONDITION_1 = new QualityGateConditionDto().setId(321).setMetricId(METRIC_ID_1).setOperator("EQ").setPeriod(1).setWarningThreshold("warnin_th").setErrorThreshold("error_th");
  private static final QualityGateConditionDto CONDITION_2 = new QualityGateConditionDto().setId(456).setMetricId(METRIC_ID_2).setOperator("NE");

  private QualityGateDao qualityGateDao = mock(QualityGateDao.class);
  private QualityGateConditionDao qualityGateConditionDao = mock(QualityGateConditionDao.class);
  private MetricRepository metricRepository = mock(MetricRepository.class);
  private QualityGateServiceImpl underTest = new QualityGateServiceImpl(qualityGateDao, qualityGateConditionDao, metricRepository);

  @Test
  public void findById_returns_absent_when_QualityGateDto_does_not_exist() {
    assertThat(underTest.findById(SOME_ID)).isAbsent();
  }

  @Test
  public void findById_returns_QualityGate_with_empty_set_of_conditions_when_there_is_none_in_DB() {
    when(qualityGateDao.selectById(SOME_ID)).thenReturn(QUALITY_GATE_DTO);
    when(qualityGateConditionDao.selectForQualityGate(SOME_ID)).thenReturn(Collections.<QualityGateConditionDto>emptyList());

    Optional<QualityGate> res = underTest.findById(SOME_ID);

    assertThat(res).isPresent();
    assertThat(res.get().getId()).isEqualTo(SOME_ID);
    assertThat(res.get().getName()).isEqualTo(SOME_NAME);
    assertThat(res.get().getConditions()).isEmpty();
  }

  @Test
  public void findById_returns_conditions_when_there_is_some_in_DB() {
    when(qualityGateDao.selectById(SOME_ID)).thenReturn(QUALITY_GATE_DTO);
    when(qualityGateConditionDao.selectForQualityGate(SOME_ID)).thenReturn(ImmutableList.of(CONDITION_1, CONDITION_2));
    // metrics are always supposed to be there
    when(metricRepository.getById(METRIC_ID_1)).thenReturn(METRIC_1);
    when(metricRepository.getById(METRIC_ID_2)).thenReturn(METRIC_2);

    Optional<QualityGate> res = underTest.findById(SOME_ID);

    assertThat(res).isPresent();
    assertThat(res.get().getId()).isEqualTo(SOME_ID);
    assertThat(res.get().getName()).isEqualTo(SOME_NAME);
    assertThat(res.get().getConditions()).containsOnly(
        new Condition(METRIC_1, CONDITION_1.getOperator(), CONDITION_1.getErrorThreshold(), CONDITION_1.getWarningThreshold(), CONDITION_1.getPeriod()),
        new Condition(METRIC_2, CONDITION_2.getOperator(), CONDITION_2.getErrorThreshold(), CONDITION_2.getWarningThreshold(), CONDITION_2.getPeriod())
        );
  }
}
