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
package org.sonar.db.measure;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;

public class LiveMeasureDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private LiveMeasureDao underTest = db.getDbClient().liveMeasureDao();
  private MetricDto metric;

  @Before
  public void setUp() throws Exception {
    metric = db.measures().insertMetric();
  }

  @Test
  public void selectByComponentUuidsAndMetricIds() {
    LiveMeasureDto measure1 = newLiveMeasure().setMetricId(metric.getId());
    LiveMeasureDto measure2 = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricIds(db.getSession(),
      asList(measure1.getComponentUuid(), measure2.getComponentUuid()), singletonList(metric.getId()));
    assertThat(selected)
      .extracting(LiveMeasureDto::getComponentUuid, LiveMeasureDto::getProjectUuid, LiveMeasureDto::getMetricId, LiveMeasureDto::getValue, LiveMeasureDto::getDataAsString)
      .containsExactlyInAnyOrder(
        tuple(measure1.getComponentUuid(), measure1.getProjectUuid(), measure1.getMetricId(), measure1.getValue(), measure1.getDataAsString()),
        tuple(measure2.getComponentUuid(), measure2.getProjectUuid(), measure2.getMetricId(), measure2.getValue(), measure2.getDataAsString()));

    assertThat(underTest.selectByComponentUuidsAndMetricIds(db.getSession(), emptyList(), singletonList(metric.getId()))).isEmpty();
    assertThat(underTest.selectByComponentUuidsAndMetricIds(db.getSession(), singletonList(measure1.getComponentUuid()), emptyList())).isEmpty();
  }

  @Test
  public void selectByComponentUuidsAndMetricIds_returns_empty_list_if_metric_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure);

    int otherMetricId = metric.getId() + 100;
    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricIds(db.getSession(), singletonList(measure.getComponentUuid()), singletonList(otherMetricId));

    assertThat(selected).isEmpty();
  }

  @Test
  public void selectByComponentUuidsAndMetricIds_returns_empty_list_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure();
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricIds(db.getSession(), singletonList("_missing_"), singletonList(measure.getMetricId()));

    assertThat(selected).isEmpty();
  }

  @Test
  public void selectByComponentUuidsAndMetricKeys() {
    LiveMeasureDto measure1 = newLiveMeasure().setMetricId(metric.getId());
    LiveMeasureDto measure2 = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), asList(measure1.getComponentUuid(), measure2.getComponentUuid()),
      singletonList(metric.getKey()));
    assertThat(selected)
      .extracting(LiveMeasureDto::getComponentUuid, LiveMeasureDto::getProjectUuid, LiveMeasureDto::getMetricId, LiveMeasureDto::getValue, LiveMeasureDto::getDataAsString)
      .containsExactlyInAnyOrder(
        tuple(measure1.getComponentUuid(), measure1.getProjectUuid(), measure1.getMetricId(), measure1.getValue(), measure1.getDataAsString()),
        tuple(measure2.getComponentUuid(), measure2.getProjectUuid(), measure2.getMetricId(), measure2.getValue(), measure2.getDataAsString()));

    assertThat(underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), emptyList(), singletonList(metric.getKey()))).isEmpty();
    assertThat(underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), singletonList(measure1.getComponentUuid()), emptyList())).isEmpty();
  }

  @Test
  public void selectByComponentUuidsAndMetricKeys_returns_empty_list_if_metric_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), singletonList(measure.getComponentUuid()), singletonList("_other_"));

    assertThat(selected).isEmpty();
  }

  @Test
  public void selectByComponentUuidsAndMetricKeys_returns_empty_list_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), singletonList("_missing_"), singletonList(metric.getKey()));

    assertThat(selected).isEmpty();
  }

  @Test
  public void selectByComponentUuidAndMetricKey() {
    LiveMeasureDto measure = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure);

    Optional<LiveMeasureDto> selected = underTest.selectByComponentUuidAndMetricKey(db.getSession(), measure.getComponentUuid(), metric.getKey());

    assertThat(selected).isNotEmpty();
    assertThat(selected.get()).isEqualToComparingFieldByField(measure);
  }

  @Test
  public void selectByComponentUuidAndMetricKey_return_empty_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure);

    assertThat(underTest.selectByComponentUuidAndMetricKey(db.getSession(), "_missing_", metric.getKey())).isEmpty();
  }

  @Test
  public void selectByComponentUuidAndMetricKey_return_empty_if_metric_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure);

    assertThat(underTest.selectByComponentUuidAndMetricKey(db.getSession(), measure.getComponentUuid(), "_missing_")).isEmpty();
  }

  @Test
  public void selectMeasure() {
    MetricDto metric = db.measures().insertMetric();
    LiveMeasureDto stored = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), stored);

    // metric exists but not component
    assertThat(underTest.selectMeasure(db.getSession(), "_missing_", metric.getKey())).isEmpty();

    // component exists but not metric
    assertThat(underTest.selectMeasure(db.getSession(), stored.getComponentUuid(), "_missing_")).isEmpty();

    // component and metric don't match
    assertThat(underTest.selectMeasure(db.getSession(), "_missing_", "_missing_")).isEmpty();

    // matches
    assertThat(underTest.selectMeasure(db.getSession(), stored.getComponentUuid(), metric.getKey()).get())
      .isEqualToComparingFieldByField(stored);
  }

  @Test
  public void selectTreeByQuery() {
    List<LiveMeasureDto> results = new ArrayList<>();
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    underTest.insert(db.getSession(), newLiveMeasure(file, metric).setValue(3.14));

    underTest.selectTreeByQuery(db.getSession(), project,
      MeasureTreeQuery.builder()
        .setMetricIds(singleton(metric.getId()))
        .setStrategy(MeasureTreeQuery.Strategy.LEAVES).build(),
      context -> results.add(context.getResultObject()));

    assertThat(results).hasSize(1);
    LiveMeasureDto result = results.get(0);
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getMetricId()).isEqualTo(metric.getId());
    assertThat(result.getValue()).isEqualTo(3.14);
  }

  @Test
  public void scrollSelectByComponentUuidAndMetricKeys_for_non_empty_metric_set() {
    List<LiveMeasureDto> results = new ArrayList<>();
    MetricDto metric = db.measures().insertMetric();
    MetricDto metric2 = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    underTest.insert(db.getSession(), newLiveMeasure(project, metric).setValue(3.14));
    underTest.insert(db.getSession(), newLiveMeasure(project, metric2).setValue(4.54));
    underTest.insert(db.getSession(), newLiveMeasure(project2, metric).setValue(99.99));
    underTest.scrollSelectByComponentUuidAndMetricKeys(db.getSession(), project.uuid(), Sets.newSet(metric.getKey(), metric2.getKey()),
      context -> results.add(context.getResultObject()));

    assertThat(results).hasSize(2);
    LiveMeasureDto result = results.stream().filter(lm -> lm.getMetricId() == metric.getId()).findFirst().get();
    assertThat(result.getComponentUuid()).isEqualTo(project.uuid());
    assertThat(result.getMetricId()).isEqualTo(metric.getId());
    assertThat(result.getValue()).isEqualTo(3.14);
    LiveMeasureDto result2 = results.stream().filter(lm -> lm.getMetricId() == metric2.getId()).findFirst().get();
    assertThat(result2.getComponentUuid()).isEqualTo(project.uuid());
    assertThat(result2.getMetricId()).isEqualTo(metric2.getId());
    assertThat(result2.getValue()).isEqualTo(4.54);
  }

  @Test
  public void scrollSelectByComponentUuidAndMetricKeys_for_empty_metric_set() {
    List<LiveMeasureDto> results = new ArrayList<>();
    ComponentDto project = db.components().insertPrivateProject();
    underTest.scrollSelectByComponentUuidAndMetricKeys(db.getSession(), project.uuid(), Sets.newSet(),
      context -> results.add(context.getResultObject()));

    assertThat(results).isEmpty();
  }

  @Test
  public void selectTreeByQuery_with_empty_results() {
    List<LiveMeasureDto> results = new ArrayList<>();
    underTest.selectTreeByQuery(db.getSession(), newPrivateProjectDto(db.getDefaultOrganization()),
      MeasureTreeQuery.builder().setStrategy(MeasureTreeQuery.Strategy.LEAVES).build(),
      context -> results.add(context.getResultObject()));

    assertThat(results).isEmpty();
  }

  @Test
  public void selectMeasure_map_fields() {
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    underTest.insert(db.getSession(), newLiveMeasure(file, metric).setValue(3.14).setVariation(0.1).setData("text_value"));

    LiveMeasureDto result = underTest.selectMeasure(db.getSession(), file.uuid(), metric.getKey()).orElseThrow(() -> new IllegalArgumentException("Measure not found"));

    assertThat(result).as("Fail to map fields of %s", result.toString()).extracting(
      LiveMeasureDto::getProjectUuid, LiveMeasureDto::getComponentUuid, LiveMeasureDto::getMetricId, LiveMeasureDto::getValue, LiveMeasureDto::getVariation,
      LiveMeasureDto::getDataAsString, LiveMeasureDto::getTextValue)
      .contains(project.uuid(), file.uuid(), metric.getId(), 3.14, 0.1, "text_value", "text_value");
  }

  @Test
  public void countNcloc() {
    OrganizationDto organization = db.organizations().insert();
    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey("ncloc").setValueType(INT.toString()));
    MetricDto lines = db.measures().insertMetric(m -> m.setKey("lines").setValueType(INT.toString()));

    ComponentDto simpleProject = db.components().insertMainBranch(organization);
    db.measures().insertLiveMeasure(simpleProject, ncloc, m -> m.setValue(10d));

    ComponentDto projectWithBiggerLongLivingBranch = db.components().insertMainBranch(organization);
    ComponentDto bigLongLivingLongBranch = db.components().insertProjectBranch(projectWithBiggerLongLivingBranch, b -> b.setBranchType(BranchType.LONG));
    db.measures().insertLiveMeasure(projectWithBiggerLongLivingBranch, ncloc, m -> m.setValue(100d));
    db.measures().insertLiveMeasure(bigLongLivingLongBranch, ncloc, m -> m.setValue(200d));

    ComponentDto projectWithLinesButNoLoc = db.components().insertMainBranch(organization);
    db.measures().insertLiveMeasure(projectWithLinesButNoLoc, lines, m -> m.setValue(365d));
    db.measures().insertLiveMeasure(projectWithLinesButNoLoc, ncloc, m -> m.setValue(0d));

    SumNclocDbQuery query = SumNclocDbQuery.builder()
      .setOnlyPrivateProjects(false)
      .setOrganizationUuid(organization.getUuid())
      .build();
    long result = underTest.sumNclocOfBiggestLongLivingBranch(db.getSession(), query);

    assertThat(result).isEqualTo(10L + 200L);
  }

  @Test
  public void countNcloc_empty() {
    db.measures().insertMetric(m -> m.setKey("ncloc").setValueType(INT.toString()));
    db.measures().insertMetric(m -> m.setKey("lines").setValueType(INT.toString()));
    SumNclocDbQuery query = SumNclocDbQuery.builder()
      .setOnlyPrivateProjects(false)
      .setOrganizationUuid(db.getDefaultOrganization().getUuid())
      .build();
    long result = underTest.sumNclocOfBiggestLongLivingBranch(db.getSession(), query);

    assertThat(result).isEqualTo(0L);
  }

  @Test
  public void countNcloc_and_exclude_project() {
    OrganizationDto organization = db.organizations().insert();
    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey("ncloc").setValueType(INT.toString()));

    ComponentDto simpleProject = db.components().insertMainBranch(organization);
    db.measures().insertLiveMeasure(simpleProject, ncloc, m -> m.setValue(10d));

    ComponentDto projectWithBiggerLongLivingBranch = db.components().insertMainBranch(organization);
    ComponentDto bigLongLivingBranch = db.components().insertProjectBranch(projectWithBiggerLongLivingBranch, b -> b.setBranchType(BranchType.LONG));
    db.measures().insertLiveMeasure(projectWithBiggerLongLivingBranch, ncloc, m -> m.setValue(100d));
    db.measures().insertLiveMeasure(bigLongLivingBranch, ncloc, m -> m.setValue(200d));

    ComponentDto projectToExclude = db.components().insertMainBranch(organization);
    ComponentDto projectToExcludeBranch = db.components().insertProjectBranch(projectToExclude, b -> b.setBranchType(BranchType.LONG));
    db.measures().insertLiveMeasure(projectToExclude, ncloc, m -> m.setValue(300d));
    db.measures().insertLiveMeasure(projectToExcludeBranch, ncloc, m -> m.setValue(400d));

    SumNclocDbQuery query = SumNclocDbQuery.builder()
      .setOrganizationUuid(organization.getUuid())
      .setProjectUuidToExclude(projectToExclude.uuid())
      .setOnlyPrivateProjects(false)
      .build();
    long result = underTest.sumNclocOfBiggestLongLivingBranch(db.getSession(), query);

    assertThat(result).isEqualTo(10L + 200L);
  }

  @Test
  public void insert_data() {
    byte[] data = "text_value".getBytes(StandardCharsets.UTF_8);
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    LiveMeasureDto measure = newLiveMeasure(file, metric).setData(data);

    underTest.insert(db.getSession(), measure);

    LiveMeasureDto result = underTest.selectMeasure(db.getSession(), file.uuid(), metric.getKey()).orElseThrow(() -> new IllegalArgumentException("Measure not found"));
    assertThat(new String(result.getData(), StandardCharsets.UTF_8)).isEqualTo("text_value");
    assertThat(result.getDataAsString()).isEqualTo("text_value");
  }

  @Test
  public void insertOrUpdate() {
    // insert
    LiveMeasureDto dto = newLiveMeasure();
    underTest.insertOrUpdate(db.getSession(), dto);
    verifyPersisted(dto);
    verifyTableSize(1);

    // update
    dto.setValue(dto.getValue() + 1);
    dto.setVariation(dto.getVariation() + 10);
    dto.setData(dto.getDataAsString() + "_new");
    underTest.insertOrUpdate(db.getSession(), dto);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void deleteByComponentUuidExcludingMetricIds() {
    LiveMeasureDto measure1 = newLiveMeasure().setComponentUuid("C1").setMetricId(1);
    LiveMeasureDto measure2 = newLiveMeasure().setComponentUuid("C1").setMetricId(2);
    LiveMeasureDto measure3 = newLiveMeasure().setComponentUuid("C1").setMetricId(3);
    LiveMeasureDto measureOtherComponent = newLiveMeasure().setComponentUuid("C2").setMetricId(3);
    underTest.insertOrUpdate(db.getSession(), measure1);
    underTest.insertOrUpdate(db.getSession(), measure2);
    underTest.insertOrUpdate(db.getSession(), measure3);
    underTest.insertOrUpdate(db.getSession(), measureOtherComponent);

    int count = underTest.deleteByComponentUuidExcludingMetricIds(db.getSession(), "C1", Arrays.asList(1, 2));

    verifyTableSize(3);
    verifyPersisted(measure1);
    verifyPersisted(measure2);
    verifyPersisted(measureOtherComponent);
    assertThat(count).isEqualTo(1);
  }

  @Test
  public void deleteByComponentUuidExcludingMetricIds_with_empty_metrics() {
    LiveMeasureDto measure1 = newLiveMeasure().setComponentUuid("C1").setMetricId(1);
    LiveMeasureDto measure2 = newLiveMeasure().setComponentUuid("C1").setMetricId(2);
    LiveMeasureDto measureOnOtherComponent = newLiveMeasure().setComponentUuid("C2").setMetricId(2);
    underTest.insertOrUpdate(db.getSession(), measure1);
    underTest.insertOrUpdate(db.getSession(), measure2);
    underTest.insertOrUpdate(db.getSession(), measureOnOtherComponent);

    int count = underTest.deleteByComponentUuidExcludingMetricIds(db.getSession(), "C1", Collections.emptyList());

    assertThat(count).isEqualTo(2);
    verifyTableSize(1);
    verifyPersisted(measureOnOtherComponent);
  }

  @Test
  public void upsert_inserts_or_updates_row() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    // insert
    LiveMeasureDto dto = newLiveMeasure();
    int count = underTest.upsert(db.getSession(), asList(dto));
    verifyPersisted(dto);
    verifyTableSize(1);
    assertThat(count).isEqualTo(1);

    // update
    dto.setValue(dto.getValue() + 1);
    dto.setVariation(dto.getVariation() + 10);
    dto.setData(dto.getDataAsString() + "_new");
    count = underTest.upsert(db.getSession(), asList(dto));
    assertThat(count).isEqualTo(1);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void upsert_does_not_update_row_if_values_are_not_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    LiveMeasureDto dto = newLiveMeasure();
    underTest.upsert(db.getSession(), asList(dto));

    // update
    int count = underTest.upsert(db.getSession(), asList(dto));
    assertThat(count).isEqualTo(0);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void upsert_updates_row_if_lob_data_is_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    LiveMeasureDto dto = newLiveMeasure().setData(RandomStringUtils.random(10_000));
    underTest.upsert(db.getSession(), asList(dto));

    // update
    dto.setData(RandomStringUtils.random(dto.getDataAsString().length() + 10));
    int count = underTest.upsert(db.getSession(), asList(dto));
    assertThat(count).isEqualTo(1);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void upsert_does_not_update_row_if_lob_data_is_not_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setData(RandomStringUtils.random(10_000));
    underTest.upsert(db.getSession(), asList(dto));

    // update
    int count = underTest.upsert(db.getSession(), asList(dto));
    assertThat(count).isEqualTo(0);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void upsert_updates_row_if_lob_data_is_removed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    LiveMeasureDto dto = newLiveMeasure().setData(RandomStringUtils.random(10_000));
    underTest.upsert(db.getSession(), asList(dto));

    // update
    dto.setData((String) null);
    int count = underTest.upsert(db.getSession(), asList(dto));
    assertThat(count).isEqualTo(1);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void upsert_updates_row_if_variation_is_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setVariation(40.0);
    underTest.upsert(db.getSession(), asList(dto));

    // update
    dto.setVariation(50.0);
    int count = underTest.upsert(db.getSession(), asList(dto));
    assertThat(count).isEqualTo(1);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void upsert_updates_row_if_variation_is_removed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setVariation(40.0);
    underTest.upsert(db.getSession(), asList(dto));

    // update
    dto.setVariation(null);
    int count = underTest.upsert(db.getSession(), asList(dto));
    assertThat(count).isEqualTo(1);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void upsert_updates_row_if_variation_is_added() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setVariation(null);
    underTest.upsert(db.getSession(), asList(dto));

    // update
    dto.setVariation(40.0);
    int count = underTest.upsert(db.getSession(), asList(dto));
    assertThat(count).isEqualTo(1);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void upsert_updates_row_if_value_is_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setValue(40.0);
    underTest.upsert(db.getSession(), asList(dto));

    // update
    dto.setValue(50.0);
    int count = underTest.upsert(db.getSession(), asList(dto));
    assertThat(count).isEqualTo(1);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void upsert_updates_row_if_value_is_removed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setValue(40.0);
    underTest.upsert(db.getSession(), asList(dto));

    // update
    dto.setValue(null);
    int count = underTest.upsert(db.getSession(), asList(dto));
    assertThat(count).isEqualTo(1);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void upsert_updates_row_if_value_is_added() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setValue(null);
    underTest.upsert(db.getSession(), asList(dto));

    // update
    dto.setValue(40.0);
    int count = underTest.upsert(db.getSession(), asList(dto));
    assertThat(count).isEqualTo(1);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void upsert_multiple_rows() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    // insert 30
    List<LiveMeasureDto> inserted = new ArrayList<>();
    IntStream.range(0, 30).forEach(i -> inserted.add(newLiveMeasure()));
    int result = underTest.upsert(db.getSession(), inserted);
    verifyTableSize(30);
    assertThat(result).isEqualTo(30);

    // update 10 with new values, update 5 without any change and insert new 50
    List<LiveMeasureDto> upserted = new ArrayList<>();
    IntStream.range(0, 10).forEach(i -> {
      LiveMeasureDto d = inserted.get(i);
      upserted.add(d.setValue(d.getValue() + 123));
    });
    upserted.addAll(inserted.subList(10, 15));
    IntStream.range(0, 50).forEach(i -> upserted.add(newLiveMeasure()));
    result = underTest.upsert(db.getSession(), upserted);
    verifyTableSize(80);
    assertThat(result).isEqualTo(60);
  }

  private void verifyTableSize(int expectedSize) {
    assertThat(db.countRowsOfTable(db.getSession(), "live_measures")).isEqualTo(expectedSize);
  }

  private void verifyPersisted(LiveMeasureDto dto) {
    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricIds(db.getSession(), singletonList(dto.getComponentUuid()), singletonList(dto.getMetricId()));
    assertThat(selected).hasSize(1);
    assertThat(selected.get(0)).isEqualToComparingOnlyGivenFields(dto,
      // do not compare the field "uuid", which is used only for insert, not select
      "componentUuid", "projectUuid", "metricId", "value", "textValue", "data", "variation");
  }
}
