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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.Constants.MeasureValueType;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.component.ComponentTreeBuilders;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.issue.RuleCacheLoader;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.computation.measure.MetricCache;
import org.sonar.server.db.DbClient;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.metric.persistence.MetricDao;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class PersistMeasuresStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String METRIC_KEY = "metric-key";
  private static final RuleKey RULE_KEY = RuleKey.of("repo", "rule-key");

  DbSession session;

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  DbClient dbClient;
  RuleCache ruleCache;
  MetricCache metricCache;
  MeasureDao measureDao;
  DbComponentsRefCache dbComponentsRefCache;

  MetricDto metric;
  RuleDto rule;

  PersistMeasuresStep sut;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    dbComponentsRefCache = new DbComponentsRefCache();

    measureDao = new MeasureDao();
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), measureDao, new ComponentDao(), new MetricDao(), new RuleDao(System2.INSTANCE));
    session = dbClient.openSession(false);

    metric = new MetricDto().setKey(METRIC_KEY).setEnabled(true).setOptimizedBestValue(false).setHidden(false).setDeleteHistoricalData(false);
    dbClient.metricDao().insert(session, metric);
    rule = RuleTesting.newDto(RULE_KEY);
    dbClient.ruleDao().insert(session, rule);
    session.commit();

    ruleCache = new RuleCache(new RuleCacheLoader(dbClient));
    metricCache = new MetricCache(dbClient);
    session.commit();

    sut = new PersistMeasuresStep(dbClient, ruleCache, metricCache, dbComponentsRefCache);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void insert_measures_from_report() throws Exception {
    ComponentDto project = addComponent(1, "project-key");
    ComponentDto file = addComponent(2, "file-key");

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setAnalysisDate(new Date().getTime())
      .setRootComponentRef(1)
      .setProjectKey("project-key")
      .setSnapshotId(3)
      .build());

    reportReader.putComponent(defaultComponent()
      .addChildRef(2)
      .build());
    reportReader.putComponent(
      defaultComponent()
        .setRef(2)
        .build());

    reportReader.putMeasures(1, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.STRING)
        .setStringValue("measure-data")
        .setVariationValue1(1.1d)
        .setVariationValue2(2.2d)
        .setVariationValue3(3.3d)
        .setVariationValue4(4.4d)
        .setVariationValue5(5.5d)
        .setAlertStatus("WARN")
        .setAlertText("Open issues > 0")
        .setDescription("measure-description")
        .setSeverity(Constants.Severity.INFO)
        .setMetricKey(METRIC_KEY)
        .setRuleKey(RULE_KEY.toString())
        .setCharactericId(123456)
        .build()));

    reportReader.putMeasures(2, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.DOUBLE)
        .setDoubleValue(123.123d)
        .setVariationValue1(1.1d)
        .setVariationValue2(2.2d)
        .setVariationValue3(3.3d)
        .setVariationValue4(4.4d)
        .setVariationValue5(5.5d)
        .setAlertStatus("ERROR")
        .setAlertText("Blocker issues variation > 0")
        .setDescription("measure-description")
        .setSeverity(Constants.Severity.BLOCKER)
        .setMetricKey(METRIC_KEY)
        .setRuleKey(RULE_KEY.toString())
        .setCharactericId(123456)
        .build()));

    sut.execute(new ComputationContext(reportReader, PROJECT_KEY, new Settings(),
      dbClient, ComponentTreeBuilders.from(DumbComponent.DUMB_PROJECT), mock(LanguageRepository.class)));
    session.commit();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(2);

    List<Map<String, Object>> dtos = dbTester.select(
      "select snapshot_id as \"snapshotId\", project_id as \"componentId\", metric_id as \"metricId\", rule_id as \"ruleId\", value as \"value\", text_value as \"textValue\", " +
        "rule_priority as \"severity\" from project_measures");

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isNotNull();
    assertThat(dto.get("componentId")).isEqualTo(project.getId());
    assertThat(dto.get("metricId")).isEqualTo(metric.getId().longValue());
    assertThat(dto.get("ruleId")).isEqualTo(rule.getId().longValue());
    assertThat(dto.get("textValue")).isEqualTo("measure-data");
    assertThat(dto.get("severity")).isEqualTo(0L);

    dto = dtos.get(1);
    assertThat(dto.get("snapshotId")).isNotNull();
    assertThat(dto.get("componentId")).isEqualTo(file.getId());
    assertThat(dto.get("metricId")).isEqualTo(metric.getId().longValue());
    assertThat(dto.get("ruleId")).isEqualTo(rule.getId().longValue());
    assertThat(dto.get("value")).isEqualTo(123.123d);
    assertThat(dto.get("severity")).isEqualTo(4L);
  }

  @Test
  public void map_full_batch_measure() {
    BatchReport.Component component = defaultComponent().build();
    addComponent(component.getRef(), "component-key");

    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.DOUBLE)
      .setDoubleValue(123.123d)
      .setVariationValue1(1.1d)
      .setVariationValue2(2.2d)
      .setVariationValue3(3.3d)
      .setVariationValue4(4.4d)
      .setVariationValue5(5.5d)
      .setAlertStatus("WARN")
      .setAlertText("Open issues > 0")
      .setDescription("measure-description")
      .setSeverity(Constants.Severity.CRITICAL)
      .setMetricKey(METRIC_KEY)
      .setRuleKey(RULE_KEY.toString())
      .setCharactericId(123456)
      .setPersonId(5432)
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure).isEqualToComparingFieldByField(expectedFullMeasure());
  }

  @Test
  public void map_minimal_batch_measure() {
    BatchReport.Component component = defaultComponent().build();
    addComponent(component.getRef(), "component-key");

    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.INT)
      .setMetricKey(METRIC_KEY)
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure).isEqualToComparingFieldByField(expectedMinimalistMeasure());
  }

  @Test
  public void map_boolean_batch_measure() {
    BatchReport.Component component = defaultComponent().build();
    addComponent(component.getRef(), "component-key");

    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.BOOLEAN)
      .setBooleanValue(true)
      .setMetricKey(METRIC_KEY)
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isEqualTo(1.0);

    batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.BOOLEAN)
      .setBooleanValue(false)
      .setMetricKey(METRIC_KEY)
      .build();

    measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isEqualTo(0.0);

    batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.BOOLEAN)
      .setMetricKey(METRIC_KEY)
      .build();

    measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isNull();
  }

  @Test
  public void map_double_batch_measure() {
    BatchReport.Component component = defaultComponent().build();
    addComponent(component.getRef(), "component-key");

    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.DOUBLE)
      .setDoubleValue(3.2)
      .setMetricKey(METRIC_KEY)
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isEqualTo(3.2);

    batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.DOUBLE)
      .setMetricKey(METRIC_KEY)
      .build();

    measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isNull();
  }

  @Test
  public void map_int_batch_measure() {
    BatchReport.Component component = defaultComponent().build();
    addComponent(component.getRef(), "component-key");

    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.INT)
      .setIntValue(3)
      .setMetricKey(METRIC_KEY)
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isEqualTo(3.0);

    batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.INT)
      .setMetricKey(METRIC_KEY)
      .build();

    measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isNull();
  }

  @Test
  public void map_long_batch_measure() {
    BatchReport.Component component = defaultComponent().build();
    addComponent(component.getRef(), "component-key");

    BatchReport.Measure batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.LONG)
      .setLongValue(3L)
      .setMetricKey(METRIC_KEY)
      .build();

    MeasureDto measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isEqualTo(3.0);

    batchMeasure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.LONG)
      .setMetricKey(METRIC_KEY)
      .build();

    measure = sut.toMeasureDto(batchMeasure, component);

    assertThat(measure.getValue()).isNull();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_when_no_metric_key() {
    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setValueType(MeasureValueType.STRING)
      .setStringValue("string-value")
      .build();
    BatchReport.Component component = defaultComponent()
      .build();
    addComponent(component.getRef(), "component-key");
    sut.toMeasureDto(measure, component);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_when_no_value() {
    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setMetricKey("repo:metric-key")
      .build();
    BatchReport.Component component = defaultComponent()
      .build();
    addComponent(component.getRef(), "component-key");
    sut.toMeasureDto(measure, component);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_when_forbid_metric() {
    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setMetricKey("duplications_data")
      .build();
    BatchReport.Component component = defaultComponent()
      .build();
    addComponent(component.getRef(), "component-key");
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
      .setAlertStatus("WARN")
      .setAlertText("Open issues > 0")
      .setDescription("measure-description")
      .setSeverity(Severity.CRITICAL)
      .setMetricId(metric.getId())
      .setRuleId(rule.getId());
  }

  private MeasureDto expectedMinimalistMeasure() {
    return new MeasureDto()
      .setComponentId(2L)
      .setSnapshotId(3L)
      .setMetricId(metric.getId());
  }

  private BatchReport.Component.Builder defaultComponent() {
    return BatchReport.Component.newBuilder()
      .setRef(1)
      .setSnapshotId(3);
  }

  private ComponentDto addComponent(int ref, String key) {
    ComponentDto componentDto = new ComponentDto().setKey(key).setUuid(Uuids.create());
    dbClient.componentDao().insert(session, componentDto);
    session.commit();
    dbComponentsRefCache.addComponent(ref, new DbComponentsRefCache.DbComponent(componentDto.getId(), key, componentDto.uuid()));
    return componentDto;
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }
}
