/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.db.DbClient;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualityGateServiceImplTest {
  private static final String SOME_UUID = "123";
  private static final String SOME_NAME = "some name";
  private static final QualityGateDto QUALITY_GATE_DTO = new QualityGateDto().setUuid(SOME_UUID).setName(SOME_NAME);
  private static final String METRIC_UUID_1 = "uuid1";
  private static final String METRIC_UUID_2 = "uuid2";
  private static final Metric METRIC_1 = mock(Metric.class);
  private static final Metric METRIC_2 = mock(Metric.class);
  private static final QualityGateConditionDto CONDITION_1 = new QualityGateConditionDto().setUuid("321").setMetricUuid(METRIC_UUID_1).setOperator("LT")
    .setErrorThreshold("error_th");
  private static final QualityGateConditionDto CONDITION_2 = new QualityGateConditionDto().setUuid("456").setMetricUuid(METRIC_UUID_2).setOperator("GT")
    .setErrorThreshold("error_th");

  private final QualityGateDao qualityGateDao = mock(QualityGateDao.class);
  private final QualityGateConditionDao qualityGateConditionDao = mock(QualityGateConditionDao.class);
  private final PropertiesDao propertiesDao = mock(PropertiesDao.class);
  private final MetricRepository metricRepository = mock(MetricRepository.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final QualityGateServiceImpl underTest = new QualityGateServiceImpl(dbClient, metricRepository);

  @Before
  public void setUp() {
    when(dbClient.qualityGateDao()).thenReturn(qualityGateDao);
    when(dbClient.gateConditionDao()).thenReturn(qualityGateConditionDao);
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);

    when(METRIC_1.getKey()).thenReturn("metric");
    when(METRIC_2.getKey()).thenReturn("new_metric");
  }

  @Test
  public void findById_returns_absent_when_QualityGateDto_does_not_exist() {
    assertThat(underTest.findByUuid(SOME_UUID)).isNotPresent();
  }

  @Test
  public void findById_returns_QualityGate_with_empty_set_of_conditions_when_there_is_none_in_DB() {
    when(qualityGateDao.selectByUuid(any(), eq(SOME_UUID))).thenReturn(QUALITY_GATE_DTO);
    when(qualityGateConditionDao.selectForQualityGate(any(), eq(SOME_UUID))).thenReturn(Collections.emptyList());

    Optional<QualityGate> res = underTest.findByUuid(SOME_UUID);

    assertThat(res).isPresent();
    assertThat(res.get().getUuid()).isEqualTo(SOME_UUID);
    assertThat(res.get().getName()).isEqualTo(SOME_NAME);
    assertThat(res.get().getConditions()).isEmpty();
  }

  @Test
  public void findById_returns_conditions_when_there_is_some_in_DB() {
    when(qualityGateDao.selectByUuid(any(), eq(SOME_UUID))).thenReturn(QUALITY_GATE_DTO);
    when(qualityGateConditionDao.selectForQualityGate(any(), eq(SOME_UUID))).thenReturn(ImmutableList.of(CONDITION_1, CONDITION_2));
    // metrics are always supposed to be there
    when(metricRepository.getOptionalByUuid(METRIC_UUID_1)).thenReturn(Optional.of(METRIC_1));
    when(metricRepository.getOptionalByUuid(METRIC_UUID_2)).thenReturn(Optional.of(METRIC_2));

    Optional<QualityGate> res = underTest.findByUuid(SOME_UUID);

    assertThat(res).isPresent();
    assertThat(res.get().getUuid()).isEqualTo(SOME_UUID);
    assertThat(res.get().getName()).isEqualTo(SOME_NAME);
    assertThat(res.get().getConditions()).containsOnly(
      new Condition(METRIC_1, CONDITION_1.getOperator(), CONDITION_1.getErrorThreshold()),
      new Condition(METRIC_2, CONDITION_2.getOperator(), CONDITION_2.getErrorThreshold()));
  }

  @Test
  public void findById_ignores_conditions_on_missing_metrics() {
    when(qualityGateDao.selectByUuid(any(), eq(SOME_UUID))).thenReturn(QUALITY_GATE_DTO);
    when(qualityGateConditionDao.selectForQualityGate(any(), eq(SOME_UUID))).thenReturn(ImmutableList.of(CONDITION_1, CONDITION_2));
    // metrics are always supposed to be there
    when(metricRepository.getOptionalByUuid(METRIC_UUID_1)).thenReturn(Optional.empty());
    when(metricRepository.getOptionalByUuid(METRIC_UUID_2)).thenReturn(Optional.of(METRIC_2));

    Optional<QualityGate> res = underTest.findByUuid(SOME_UUID);

    assertThat(res).isPresent();
    assertThat(res.get().getUuid()).isEqualTo(SOME_UUID);
    assertThat(res.get().getName()).isEqualTo(SOME_NAME);
    assertThat(res.get().getConditions()).containsOnly(
      new Condition(METRIC_2, CONDITION_2.getOperator(), CONDITION_2.getErrorThreshold()));
  }

  @Test
  public void findDefaultQualityGate_by_property_not_found() {
    when(propertiesDao.selectGlobalProperty(any(), any())).thenReturn(null);

    assertThatThrownBy(() -> underTest.findDefaultQualityGate())
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void findDefaultQualityGate_by_property_found() {
    QualityGateDto qualityGateDto = new QualityGateDto();
    qualityGateDto.setUuid(QUALITY_GATE_DTO.getUuid());
    qualityGateDto.setName(QUALITY_GATE_DTO.getName());
    when(propertiesDao.selectGlobalProperty(any(), any())).thenReturn(new PropertyDto().setValue(QUALITY_GATE_DTO.getUuid()));

    when(qualityGateDao.selectByUuid(any(), any())).thenReturn(qualityGateDto);
    when(qualityGateConditionDao.selectForQualityGate(any(), eq(SOME_UUID))).thenReturn(ImmutableList.of(CONDITION_1, CONDITION_2));
    when(metricRepository.getOptionalByUuid(METRIC_UUID_1)).thenReturn(Optional.empty());
    when(metricRepository.getOptionalByUuid(METRIC_UUID_2)).thenReturn(Optional.of(METRIC_2));

    QualityGate result = underTest.findDefaultQualityGate();

    assertThat(result).isNotNull();
    assertThat(result.getUuid()).isEqualTo(QUALITY_GATE_DTO.getUuid());
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
    QualityGateDto qualityGateDto = new QualityGateDto();
    qualityGateDto.setUuid(QUALITY_GATE_DTO.getUuid());
    qualityGateDto.setName(QUALITY_GATE_DTO.getName());
    when(qualityGateDao.selectByProjectUuid(any(), any())).thenReturn(qualityGateDto);
    when(qualityGateConditionDao.selectForQualityGate(any(), eq(SOME_UUID))).thenReturn(ImmutableList.of(CONDITION_1, CONDITION_2));
    when(metricRepository.getOptionalByUuid(METRIC_UUID_1)).thenReturn(Optional.empty());
    when(metricRepository.getOptionalByUuid(METRIC_UUID_2)).thenReturn(Optional.of(METRIC_2));

    Optional<QualityGate> result = underTest.findQualityGate(mock(Project.class));

    assertThat(result).isNotNull();
    assertThat(result).isNotEmpty();

    QualityGate resultData = result.get();
    assertThat(resultData.getUuid()).isEqualTo(QUALITY_GATE_DTO.getUuid());
    assertThat(resultData.getName()).isNotBlank();
    assertThat(resultData.getName()).isEqualTo(QUALITY_GATE_DTO.getName());
  }
}
