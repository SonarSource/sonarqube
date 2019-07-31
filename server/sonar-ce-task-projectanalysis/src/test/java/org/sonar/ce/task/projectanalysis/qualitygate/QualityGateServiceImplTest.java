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

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.analysis.Organization;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.db.DbClient;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
  private static final QualityGateConditionDto CONDITION_1 = new QualityGateConditionDto().setId(321).setMetricId(METRIC_ID_1).setOperator("LT")
    .setErrorThreshold("error_th");
  private static final QualityGateConditionDto CONDITION_2 = new QualityGateConditionDto().setId(456).setMetricId(METRIC_ID_2).setOperator("GT").setErrorThreshold("error_th");

  private QualityGateDao qualityGateDao = mock(QualityGateDao.class);
  private QualityGateConditionDao qualityGateConditionDao = mock(QualityGateConditionDao.class);
  private MetricRepository metricRepository = mock(MetricRepository.class);
  private DbClient dbClient = mock(DbClient.class);
  private QualityGateServiceImpl underTest = new QualityGateServiceImpl(dbClient, metricRepository);

  @Before
  public void setUp() throws Exception {
    when(dbClient.qualityGateDao()).thenReturn(qualityGateDao);
    when(dbClient.gateConditionDao()).thenReturn(qualityGateConditionDao);

    when(METRIC_1.getKey()).thenReturn("metric");
    when(METRIC_2.getKey()).thenReturn("new_metric");
  }

  @Test
  public void findById_returns_absent_when_QualityGateDto_does_not_exist() {
    assertThat(underTest.findById(SOME_ID)).isNotPresent();
  }

  @Test
  public void findById_returns_QualityGate_with_empty_set_of_conditions_when_there_is_none_in_DB() {
    when(qualityGateDao.selectById(any(), eq(SOME_ID))).thenReturn(QUALITY_GATE_DTO);
    when(qualityGateConditionDao.selectForQualityGate(any(), eq(SOME_ID))).thenReturn(Collections.emptyList());

    Optional<QualityGate> res = underTest.findById(SOME_ID);

    assertThat(res).isPresent();
    assertThat(res.get().getId()).isEqualTo(SOME_ID);
    assertThat(res.get().getName()).isEqualTo(SOME_NAME);
    assertThat(res.get().getConditions()).isEmpty();
  }

  @Test
  public void findById_returns_conditions_when_there_is_some_in_DB() {
    when(qualityGateDao.selectById(any(), eq(SOME_ID))).thenReturn(QUALITY_GATE_DTO);
    when(qualityGateConditionDao.selectForQualityGate(any(), eq(SOME_ID))).thenReturn(ImmutableList.of(CONDITION_1, CONDITION_2));
    // metrics are always supposed to be there
    when(metricRepository.getOptionalById(METRIC_ID_1)).thenReturn(Optional.of(METRIC_1));
    when(metricRepository.getOptionalById(METRIC_ID_2)).thenReturn(Optional.of(METRIC_2));

    Optional<QualityGate> res = underTest.findById(SOME_ID);

    assertThat(res).isPresent();
    assertThat(res.get().getId()).isEqualTo(SOME_ID);
    assertThat(res.get().getName()).isEqualTo(SOME_NAME);
    assertThat(res.get().getConditions()).containsOnly(
      new Condition(METRIC_1, CONDITION_1.getOperator(), CONDITION_1.getErrorThreshold()),
      new Condition(METRIC_2, CONDITION_2.getOperator(), CONDITION_2.getErrorThreshold()));
  }

  @Test
  public void findById_ignores_conditions_on_missing_metrics() {
    when(qualityGateDao.selectById(any(), eq(SOME_ID))).thenReturn(QUALITY_GATE_DTO);
    when(qualityGateConditionDao.selectForQualityGate(any(), eq(SOME_ID))).thenReturn(ImmutableList.of(CONDITION_1, CONDITION_2));
    // metrics are always supposed to be there
    when(metricRepository.getOptionalById(METRIC_ID_1)).thenReturn(Optional.empty());
    when(metricRepository.getOptionalById(METRIC_ID_2)).thenReturn(Optional.of(METRIC_2));

    Optional<QualityGate> res = underTest.findById(SOME_ID);

    assertThat(res).isPresent();
    assertThat(res.get().getId()).isEqualTo(SOME_ID);
    assertThat(res.get().getName()).isEqualTo(SOME_NAME);
    assertThat(res.get().getConditions()).containsOnly(
      new Condition(METRIC_2, CONDITION_2.getOperator(), CONDITION_2.getErrorThreshold()));
  }

  @Test(expected = IllegalStateException.class)
  public void findDefaultQualityGate_by_organization_not_found() {
    when(qualityGateDao.selectByOrganizationAndUuid(any(), any(), any())).thenReturn(null);

    underTest.findDefaultQualityGate(mock(Organization.class));
  }

  @Test
  public void findDefaultQualityGate_by_organization_found() {
    QGateWithOrgDto qGateWithOrgDto = new QGateWithOrgDto();
    qGateWithOrgDto.setId(QUALITY_GATE_DTO.getId());
    qGateWithOrgDto.setName(QUALITY_GATE_DTO.getName());
    when(qualityGateDao.selectByOrganizationAndUuid(any(), any(), any())).thenReturn(qGateWithOrgDto);
    when(qualityGateConditionDao.selectForQualityGate(any(), eq(SOME_ID))).thenReturn(ImmutableList.of(CONDITION_1, CONDITION_2));
    when(metricRepository.getOptionalById(METRIC_ID_1)).thenReturn(Optional.empty());
    when(metricRepository.getOptionalById(METRIC_ID_2)).thenReturn(Optional.of(METRIC_2));

    QualityGate result = underTest.findDefaultQualityGate(mock(Organization.class));

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(QUALITY_GATE_DTO.getId());
    assertThat(result.getName()).isNotBlank();
    assertThat(result.getName()).isEqualTo(QUALITY_GATE_DTO.getName());
  }

  @Test
  public void findQualityGate_by_project_not_found() {
    when(qualityGateDao.selectByProjectUuid(any(), any())).thenReturn(null);
    Optional<QualityGate> result = underTest.findQualityGate(mock(Project.class));
    assertThat(result).isEmpty();
  }

  @Test
  public void findQualityGate_by_project_found() {
    QGateWithOrgDto qGateWithOrgDto = new QGateWithOrgDto();
    qGateWithOrgDto.setId(QUALITY_GATE_DTO.getId());
    qGateWithOrgDto.setName(QUALITY_GATE_DTO.getName());
    when(qualityGateDao.selectByProjectUuid(any(), any())).thenReturn(qGateWithOrgDto);
    when(qualityGateConditionDao.selectForQualityGate(any(), eq(SOME_ID))).thenReturn(ImmutableList.of(CONDITION_1, CONDITION_2));
    when(metricRepository.getOptionalById(METRIC_ID_1)).thenReturn(Optional.empty());
    when(metricRepository.getOptionalById(METRIC_ID_2)).thenReturn(Optional.of(METRIC_2));

    Optional<QualityGate> result = underTest.findQualityGate(mock(Project.class));

    assertThat(result).isNotNull();
    assertThat(result).isNotEmpty();

    QualityGate resultData = result.get();
    assertThat(resultData.getId()).isEqualTo(QUALITY_GATE_DTO.getId());
    assertThat(resultData.getName()).isNotBlank();
    assertThat(resultData.getName()).isEqualTo(QUALITY_GATE_DTO.getName());
  }
}
