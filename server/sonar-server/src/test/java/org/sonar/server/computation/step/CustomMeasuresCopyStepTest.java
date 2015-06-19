/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.sonar.core.measure.custom.db.CustomMeasureDto;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.component.MutableTreeRootHolderRule;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.db.DbClient;
import org.sonar.server.measure.custom.persistence.CustomMeasureDao;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.step.CustomMeasuresCopyStep.dtoToMeasure;

@Category(DbTests.class)
public class CustomMeasuresCopyStepTest {

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public MutableTreeRootHolderRule treeRootHolder = new MutableTreeRootHolderRule();

  MetricRepository metricRepository = mock(MetricRepository.class);
  MeasureRepository measureRepository = mock(MeasureRepository.class);

  CustomMeasuresCopyStep sut;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new CustomMeasureDao());
    sut = new CustomMeasuresCopyStep(treeRootHolder, dbClient, metricRepository, measureRepository);
  }

  @Test
  public void copy_custom_measures() throws Exception {
    dbTester.prepareDbUnit(getClass(), "custom-measures.xml");

    // custom metrics
    MetricImpl floatMetric = new MetricImpl(10, "float_metric", "Float Metric", Metric.MetricType.FLOAT);
    when(metricRepository.getById(floatMetric.getId())).thenReturn(floatMetric);
    MetricImpl stringMetric = new MetricImpl(11, "string_metric", "String Metric", Metric.MetricType.STRING);
    when(metricRepository.getById(stringMetric.getId())).thenReturn(stringMetric);

    // components. File1 and project have custom measures, but not file2
    Component file1 = DumbComponent.builder(Component.Type.FILE, 1).setUuid("FILE1").build();
    Component file2 = DumbComponent.builder(Component.Type.FILE, 2).setUuid("FILE2").build();
    Component project = DumbComponent.builder(Component.Type.PROJECT, 3).setUuid("PROJECT1").addChildren(file1, file2).build();
    treeRootHolder.setRoot(project);

    sut.execute();

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    verify(measureRepository).add(eq(file1), eq(floatMetric), measureCaptor.capture());
    Measure fileMeasure = measureCaptor.getValue();
    assertThat(fileMeasure.getDoubleValue()).isEqualTo(3.14, Offset.offset(0.001));

    verify(measureRepository).add(eq(project), eq(stringMetric), measureCaptor.capture());
    Measure projectMeasure = measureCaptor.getValue();
    assertThat(projectMeasure.getStringValue()).isEqualTo("good");

    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void test_float_value_type() throws Exception {
    CustomMeasureDto dto = new CustomMeasureDto();
    dto.setValue(10.0);
    assertThat(dtoToMeasure(dto, new MetricImpl(1, "m", "M", Metric.MetricType.FLOAT)).getDoubleValue()).isEqualTo(10.0);
  }

  @Test
  public void test_int_value_type() throws Exception {
    CustomMeasureDto dto = new CustomMeasureDto();
    dto.setValue(10.0);
    assertThat(dtoToMeasure(dto, new MetricImpl(1, "m", "M", Metric.MetricType.INT)).getIntValue()).isEqualTo(10);
  }

  @Test
  public void test_long_value_type() throws Exception {
    CustomMeasureDto dto = new CustomMeasureDto();
    dto.setValue(10.0);
    assertThat(dtoToMeasure(dto, new MetricImpl(1, "m", "M", Metric.MetricType.WORK_DUR)).getLongValue()).isEqualTo(10);
  }

  @Test
  public void test_percent_value_type() throws Exception {
    CustomMeasureDto dto = new CustomMeasureDto();
    dto.setValue(10.0);
    assertThat(dtoToMeasure(dto, new MetricImpl(1, "m", "M", Metric.MetricType.PERCENT)).getDoubleValue()).isEqualTo(10);
  }

  @Test
  public void test_string_value_type() throws Exception {
    CustomMeasureDto dto = new CustomMeasureDto();
    dto.setTextValue("foo");
    assertThat(dtoToMeasure(dto, new MetricImpl(1, "m", "M", Metric.MetricType.STRING)).getStringValue()).isEqualTo("foo");
  }

  @Test
  public void test_boolean_value_type() throws Exception {
    MetricImpl booleanMetric = new MetricImpl(1, "m", "M", Metric.MetricType.BOOL);
    CustomMeasureDto dto = new CustomMeasureDto();
    assertThat(dtoToMeasure(dto.setValue(1.0), booleanMetric).getBooleanValue()).isTrue();
    assertThat(dtoToMeasure(dto.setValue(0.0), booleanMetric).getBooleanValue()).isFalse();
  }

}
