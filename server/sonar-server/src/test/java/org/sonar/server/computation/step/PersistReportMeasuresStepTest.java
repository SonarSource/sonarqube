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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.Constants.MeasureValueType;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.measure.MetricCache;
import org.sonar.server.db.DbClient;
import org.sonar.server.measure.persistence.MeasureDao;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PersistReportMeasuresStepTest extends BaseStepTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  DbClient dbClient;
  RuleCache ruleCache;
  MetricCache metricCache;
  MeasureDao measureDao;

  PersistReportMeasuresStep sut;

  private BatchReport.Component component;

  @Before
  public void setUp() throws Exception {
    dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
    ruleCache = mock(RuleCache.class, Mockito.RETURNS_DEEP_STUBS);
    metricCache = mock(MetricCache.class, Mockito.RETURNS_DEEP_STUBS);
    when(metricCache.get("metric-key").getId()).thenReturn(654);
    measureDao = mock(MeasureDao.class);
    when(ruleCache.get(any(RuleKey.class)).getId()).thenReturn(987);

    sut = new PersistReportMeasuresStep(dbClient, ruleCache, metricCache);

    component = defaultComponent().build();
  }

  @Test
  public void insert_measures_from_report() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter report = new BatchReportWriter(dir);

    when(dbClient.measureDao()).thenReturn(measureDao);

    report.writeMetadata(BatchReport.Metadata.newBuilder()
      .setAnalysisDate(new Date().getTime())
      .setRootComponentRef(1)
      .setProjectKey("project-key")
      .setSnapshotId(3)
      .build());

    report.writeComponent(defaultComponent()
      .addChildRef(2)
      .build());
    report.writeComponent(
      defaultComponent()
        .setRef(2)
        .build());

    report.writeComponentMeasures(1, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(Constants.MeasureValueType.STRING)
        .setStringValue("measure-data")
        .setVariationValue1(1.1d)
        .setVariationValue2(2.2d)
        .setVariationValue3(3.3d)
        .setVariationValue4(4.4d)
        .setVariationValue5(5.5d)
        .setAlertStatus("measure-alert-status")
        .setAlertText("measure-alert-text")
        .setDescription("measure-description")
        .setSeverity(Constants.Severity.INFO)
        .setMetricKey("metric-key")
        .setRuleKey("repo:rule-key")
        .setCharactericId(123456)
        .build()));

    report.writeComponentMeasures(2, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(Constants.MeasureValueType.DOUBLE)
        .setDoubleValue(123.123d)
        .setVariationValue1(1.1d)
        .setVariationValue2(2.2d)
        .setVariationValue3(3.3d)
        .setVariationValue4(4.4d)
        .setVariationValue5(5.5d)
        .setAlertStatus("measure-alert-status")
        .setAlertText("measure-alert-text")
        .setDescription("measure-description")
        .setSeverity(Constants.Severity.BLOCKER)
        .setMetricKey("metric-key")
        .setRuleKey("repo:rule-key")
        .setCharactericId(123456)
        .build()));

    sut.execute(new ComputationContext(new BatchReportReader(dir), mock(ComponentDto.class)));

    ArgumentCaptor<MeasureDto> argument = ArgumentCaptor.forClass(MeasureDto.class);
    verify(measureDao, times(2)).insert(any(DbSession.class), argument.capture());
    assertThat(argument.getValue().getValue()).isEqualTo(123.123d, Offset.offset(0.0001d));
    assertThat(argument.getValue().getMetricId()).isEqualTo(654);
    assertThat(argument.getValue().getRuleId()).isEqualTo(987);
    assertThat(argument.getValue().getSeverity()).isEqualTo(Severity.BLOCKER);
  }

  private BatchReport.Component.Builder defaultComponent() {
    return BatchReport.Component.newBuilder()
      .setRef(1)
      .setId(2)
      .setSnapshotId(3);
  }

  @Test
  public void map_full_batch_measure() throws Exception {
    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(Constants.MeasureValueType.DOUBLE)
      .setDoubleValue(123.123d)
      .setVariationValue1(1.1d)
      .setVariationValue2(2.2d)
      .setVariationValue3(3.3d)
      .setVariationValue4(4.4d)
      .setVariationValue5(5.5d)
      .setAlertStatus("measure-alert-status")
      .setAlertText("measure-alert-text")
      .setDescription("measure-description")
      .setSeverity(Constants.Severity.CRITICAL)
      .setMetricKey("metric-key")
      .setRuleKey("repo:rule-key")
      .setCharactericId(123456)
      .setPersonId(5432)
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure).isEqualToComparingFieldByField(expectedFullMeasure());
  }

  @Test
  public void map_minimal_batch_measure() throws Exception {
    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(Constants.MeasureValueType.INT)
      .setMetricKey("metric-key")
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure).isEqualToComparingFieldByField(expectedMinimalistMeasure());
  }

  @Test
  public void map_boolean_batch_measure() throws Exception {
    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(Constants.MeasureValueType.BOOLEAN)
      .setBooleanValue(true)
      .setMetricKey("metric-key")
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isEqualTo(1.0);

    batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(Constants.MeasureValueType.BOOLEAN)
      .setBooleanValue(false)
      .setMetricKey("metric-key")
      .build();

    measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isEqualTo(0.0);

    batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(Constants.MeasureValueType.BOOLEAN)
      .setMetricKey("metric-key")
      .build();

    measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isNull();
  }

  @Test
  public void map_double_batch_measure() throws Exception {
    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(Constants.MeasureValueType.DOUBLE)
      .setDoubleValue(3.2)
      .setMetricKey("metric-key")
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isEqualTo(3.2);

    batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(Constants.MeasureValueType.DOUBLE)
      .setMetricKey("metric-key")
      .build();

    measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isNull();
  }

  @Test
  public void map_int_batch_measure() throws Exception {
    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(Constants.MeasureValueType.INT)
      .setIntValue(3)
      .setMetricKey("metric-key")
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isEqualTo(3.0);

    batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(Constants.MeasureValueType.INT)
      .setMetricKey("metric-key")
      .build();

    measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isNull();
  }

  @Test
  public void map_long_batch_measure() throws Exception {
    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(Constants.MeasureValueType.LONG)
      .setLongValue(3L)
      .setMetricKey("metric-key")
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isEqualTo(3.0);

    batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(Constants.MeasureValueType.LONG)
      .setMetricKey("metric-key")
      .build();

    measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isNull();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_when_no_metric_key() throws Exception {
    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.STRING)
      .setStringValue("string-value")
      .build();
    BatchReport.Component component = defaultComponent()
      .build();
    sut.toMeasureDto(measure, component);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_when_no_value() throws Exception {
    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setMetricKey("repo:metric-key")
      .build();
    BatchReport.Component component = defaultComponent()
      .build();
    sut.toMeasureDto(measure, component);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_when_forbid_metric() throws Exception {
    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setMetricKey("duplications_data")
      .build();
    BatchReport.Component component = defaultComponent()
      .build();
    sut.toMeasureDto(measure, component);
  }

  private MeasureDto expectedFullMeasure() {
    return new MeasureDto()
      .setComponentId(2L)
      .setSnapshotId(3L)
      .setCharacteristicId(123456)
      .setPersonId(5432)
      .setValue(123.123d)
      .setVariation(1, 1.1d)
      .setVariation(2, 2.2d)
      .setVariation(3, 3.3d)
      .setVariation(4, 4.4d)
      .setVariation(5, 5.5d)
      .setAlertStatus("measure-alert-status")
      .setAlertText("measure-alert-text")
      .setDescription("measure-description")
      .setSeverity(Severity.CRITICAL)
      .setMetricId(654)
      .setRuleId(987);
  }

  private MeasureDto expectedMinimalistMeasure() {
    return new MeasureDto()
      .setComponentId(2L)
      .setSnapshotId(3L)
      .setMetricId(654);
  }

  @Override
  protected ComputationStep step() throws IOException {
    return sut;
  }
}
