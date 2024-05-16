/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.internal.util.collections.Sets;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.metric.MetricDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;

class LiveMeasureDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final LiveMeasureDao underTest = db.getDbClient().liveMeasureDao();
  private MetricDto metric;

  private int branchId = 0;

  @BeforeEach
  void setUp() {
    metric = db.measures().insertMetric();
  }

  @Test
  void selectByComponentUuidsAndMetricUuids() {
    LiveMeasureDto measure1 = newLiveMeasure().setMetricUuid(metric.getUuid());
    LiveMeasureDto measure2 = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricUuids(db.getSession(),
      asList(measure1.getComponentUuid(), measure2.getComponentUuid()), singletonList(metric.getUuid()));
    assertThat(selected)
      .extracting(LiveMeasureDto::getComponentUuid, LiveMeasureDto::getProjectUuid, LiveMeasureDto::getMetricUuid,
        LiveMeasureDto::getValue, LiveMeasureDto::getDataAsString)
      .containsExactlyInAnyOrder(
        tuple(measure1.getComponentUuid(), measure1.getProjectUuid(), measure1.getMetricUuid(), measure1.getValue(),
          measure1.getDataAsString()),
        tuple(measure2.getComponentUuid(), measure2.getProjectUuid(), measure2.getMetricUuid(), measure2.getValue(),
          measure2.getDataAsString()));

    assertThat(underTest.selectByComponentUuidsAndMetricUuids(db.getSession(), emptyList(), singletonList(metric.getUuid()))).isEmpty();
    assertThat(underTest.selectByComponentUuidsAndMetricUuids(db.getSession(), singletonList(measure1.getComponentUuid()), emptyList())).isEmpty();
  }

  @Test
  void selectByComponentUuidsAndMetricUuids_returns_empty_list_if_metric_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    String otherMetricUuid = metric.getUuid() + "other";
    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricUuids(db.getSession(),
      singletonList(measure.getComponentUuid()), singletonList(otherMetricUuid));

    assertThat(selected).isEmpty();
  }

  @Test
  void selectByComponentUuidsAndMetricUuids_returns_empty_list_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure();
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricUuids(db.getSession(), singletonList("_missing_"),
      singletonList(measure.getMetricUuid()));

    assertThat(selected).isEmpty();
  }

  @Test
  void selectByComponentUuidsAndMetricKeys() {
    LiveMeasureDto measure1 = newLiveMeasure().setMetricUuid(metric.getUuid());
    LiveMeasureDto measure2 = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), asList(measure1.getComponentUuid(),
        measure2.getComponentUuid()),
      singletonList(metric.getKey()));
    assertThat(selected)
      .extracting(LiveMeasureDto::getComponentUuid, LiveMeasureDto::getProjectUuid, LiveMeasureDto::getMetricUuid,
        LiveMeasureDto::getValue, LiveMeasureDto::getDataAsString)
      .containsExactlyInAnyOrder(
        tuple(measure1.getComponentUuid(), measure1.getProjectUuid(), measure1.getMetricUuid(), measure1.getValue(),
          measure1.getDataAsString()),
        tuple(measure2.getComponentUuid(), measure2.getProjectUuid(), measure2.getMetricUuid(), measure2.getValue(),
          measure2.getDataAsString()));

    assertThat(underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), emptyList(), singletonList(metric.getKey()))).isEmpty();
    assertThat(underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), singletonList(measure1.getComponentUuid()), emptyList())).isEmpty();
  }

  @Test
  void selectByComponentUuidsAndMetricKeys_returns_empty_list_if_metric_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricKeys(db.getSession(),
      singletonList(measure.getComponentUuid()), singletonList("_other_"));

    assertThat(selected).isEmpty();
  }

  @Test
  void selectByComponentUuidsAndMetricKeys_returns_empty_list_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), singletonList("_missing_"),
      singletonList(metric.getKey()));

    assertThat(selected).isEmpty();
  }

  @Test
  void selectForProjectsByMetricUuids() {
    MetricDto metric = db.measures().insertMetric();
    MetricDto metric2 = db.measures().insertMetric();
    BranchDto projectBranch = db.components().insertPrivateProject().getMainBranchDto();
    BranchDto project2Branch = db.components().insertPrivateProject().getMainBranchDto();
    underTest.insert(db.getSession(), newLiveMeasure(projectBranch, metric).setValue(3.14).setData((String) null));
    underTest.insert(db.getSession(), newLiveMeasure(projectBranch, metric2).setValue(4.54).setData((String) null));
    underTest.insert(db.getSession(), newLiveMeasure(project2Branch, metric).setValue(99.99).setData((String) null));

    List<ProjectMainBranchLiveMeasureDto> selected = underTest.selectForProjectMainBranchesByMetricUuids(db.getSession(),
      List.of(metric.getUuid(), metric2.getUuid()));
    assertThat(selected)
      .extracting(ProjectMainBranchLiveMeasureDto::getProjectUuid, ProjectMainBranchLiveMeasureDto::getMetricUuid,
        ProjectMainBranchLiveMeasureDto::getValue, ProjectMainBranchLiveMeasureDto::getTextValue)
      .containsExactlyInAnyOrder(
        tuple(projectBranch.getProjectUuid(), metric.getUuid(), 3.14, null),
        tuple(projectBranch.getProjectUuid(), metric2.getUuid(), 4.54, null),
        tuple(project2Branch.getProjectUuid(), metric.getUuid(), 99.99, null));
  }

  @Test
  void selectForProjectMainBranchesByMetricUuids_whenMultipleBranches_shouldRetrieveMetricsFromMainBranch() {
    MetricDto metric = db.measures().insertMetric();
    ProjectData projectData = db.components().insertPrivateProject();
    BranchDto mainBranch = projectData.getMainBranchDto();
    BranchDto branch1 = db.components().insertProjectBranch(projectData.getProjectDto());
    BranchDto branch2 = db.components().insertProjectBranch(projectData.getProjectDto());
    underTest.insert(db.getSession(), newLiveMeasure(mainBranch, metric).setValue(3.14).setData((String) null));
    underTest.insert(db.getSession(), newLiveMeasure(branch1, metric).setValue(4.54).setData((String) null));
    underTest.insert(db.getSession(), newLiveMeasure(branch2, metric).setValue(99.99).setData((String) null));

    List<ProjectMainBranchLiveMeasureDto> selected = underTest.selectForProjectMainBranchesByMetricUuids(db.getSession(),
      List.of(metric.getUuid()));
    assertThat(selected)
      .extracting(ProjectMainBranchLiveMeasureDto::getProjectUuid, ProjectMainBranchLiveMeasureDto::getMetricUuid,
        ProjectMainBranchLiveMeasureDto::getValue, ProjectMainBranchLiveMeasureDto::getTextValue)
      .containsExactlyInAnyOrder(
        tuple(mainBranch.getProjectUuid(), metric.getUuid(), 3.14, null));
  }

  @Test
  void selectForProjectsByMetricUuids_whenMetricDoesNotMatch_shouldReturnEmptyList() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    underTest.insert(db.getSession(), newLiveMeasure(project, metric).setValue(3.14).setData((String) null));
    List<ProjectMainBranchLiveMeasureDto> selected = underTest.selectForProjectMainBranchesByMetricUuids(db.getSession(), singletonList(
      "_other_"));
    assertThat(selected).isEmpty();
  }

  @Test
  void selectForProjectsByMetricUuids_shouldReturnProjectWithTRKQualifierOnly() {
    MetricDto metric = db.measures().insertMetric();
    ProjectData application = db.components().insertPrivateApplication();
    ProjectData project = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    db.components().addApplicationProject(application, project, project2);
    underTest.insert(db.getSession(), newLiveMeasure(application.getMainBranchComponent(), metric).setValue(3.14).setData((String) null));
    underTest.insert(db.getSession(), newLiveMeasure(project.getMainBranchComponent(), metric).setValue(4.54).setData((String) null));
    underTest.insert(db.getSession(), newLiveMeasure(project2.getMainBranchComponent(), metric).setValue(5.56).setData((String) null));

    List<ProjectMainBranchLiveMeasureDto> selected = underTest.selectForProjectMainBranchesByMetricUuids(db.getSession(),
      List.of(metric.getUuid()));

    assertThat(selected)
      .extracting(ProjectMainBranchLiveMeasureDto::getProjectUuid)
      .containsExactlyInAnyOrder(project.projectUuid(), project2.projectUuid());
  }

  @Test
  void selectByComponentUuidAndMetricKey() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    Optional<LiveMeasureDto> selected = underTest.selectMeasure(db.getSession(), measure.getComponentUuid(), metric.getKey());

    assertThat(selected).isNotEmpty();
    assertThat(selected.get()).isEqualToComparingFieldByField(measure);
  }

  @Test
  void selectByComponentUuidAndMetricKey_return_empty_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    assertThat(underTest.selectMeasure(db.getSession(), "_missing_", metric.getKey())).isEmpty();
  }

  @Test
  void selectByComponentUuidAndMetricKey_return_empty_if_metric_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    assertThat(underTest.selectMeasure(db.getSession(), measure.getComponentUuid(), "_missing_")).isEmpty();
  }

  @Test
  void selectByComponentUuidAndMetricKeys() {
    MetricDto metric2 = db.measures().insertMetric();

    LiveMeasureDto measure1 = newLiveMeasure().setMetricUuid(metric.getUuid()).setValue(1.0).setComponentUuid("uuid");
    LiveMeasureDto measure2 = newLiveMeasure().setMetricUuid(metric2.getUuid()).setValue(2.0).setComponentUuid("uuid");

    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidAndMetricKeys(db.getSession(), "uuid", asList(metric.getKey(),
      metric2.getKey()));

    assertThat(selected).hasSize(2);
    assertThat(selected).extracting(LiveMeasureDto::getMetricUuid, LiveMeasureDto::getValue)
      .containsExactlyInAnyOrder(tuple(metric.getUuid(), measure1.getValue()), tuple(metric2.getUuid(), measure2.getValue()));
  }

  @Test
  void selectByComponentUuidAndMetricKeys_return_empty_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    assertThat(underTest.selectByComponentUuidAndMetricKeys(db.getSession(), "_missing_", singletonList(metric.getKey()))).isEmpty();
  }

  @Test
  void selectByComponentUuidAndMetricKeys_return_empty_if_no_metric_matches() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    assertThat(underTest.selectByComponentUuidAndMetricKeys(db.getSession(), measure.getComponentUuid(), singletonList("_missing_"))).isEmpty();
  }

  @Test
  void selectMeasure() {
    MetricDto metric = db.measures().insertMetric();
    LiveMeasureDto stored = newLiveMeasure().setMetricUuid(metric.getUuid());
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
  void selectTreeByQuery() {
    List<LiveMeasureDto> results = new ArrayList<>();
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    underTest.insert(db.getSession(), newLiveMeasure(file, metric).setValue(3.14));

    underTest.selectTreeByQuery(db.getSession(), project,
      MeasureTreeQuery.builder()
        .setMetricUuids(singleton(metric.getUuid()))
        .setStrategy(MeasureTreeQuery.Strategy.LEAVES).build(),
      context -> results.add(context.getResultObject()));

    assertThat(results).hasSize(1);
    LiveMeasureDto result = results.get(0);
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getMetricUuid()).isEqualTo(metric.getUuid());
    assertThat(result.getValue()).isEqualTo(3.14);
  }

  @Test
  void scrollSelectByComponentUuidAndMetricKeys_for_non_empty_metric_set() {
    List<LiveMeasureDto> results = new ArrayList<>();
    MetricDto metric = db.measures().insertMetric();
    MetricDto metric2 = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    underTest.insert(db.getSession(), newLiveMeasure(project, metric).setValue(3.14));
    underTest.insert(db.getSession(), newLiveMeasure(project, metric2).setValue(4.54));
    underTest.insert(db.getSession(), newLiveMeasure(project2, metric).setValue(99.99));
    underTest.scrollSelectByComponentUuidAndMetricKeys(db.getSession(), project.uuid(), Sets.newSet(metric.getKey(), metric2.getKey()),
      context -> results.add(context.getResultObject()));

    assertThat(results).hasSize(2);
    LiveMeasureDto result = results.stream().filter(lm -> lm.getMetricUuid().equals(metric.getUuid())).findFirst().get();
    assertThat(result.getComponentUuid()).isEqualTo(project.uuid());
    assertThat(result.getMetricUuid()).isEqualTo(metric.getUuid());
    assertThat(result.getValue()).isEqualTo(3.14);
    LiveMeasureDto result2 = results.stream().filter(lm -> lm.getMetricUuid().equals(metric2.getUuid())).findFirst().get();
    assertThat(result2.getComponentUuid()).isEqualTo(project.uuid());
    assertThat(result2.getMetricUuid()).isEqualTo(metric2.getUuid());
    assertThat(result2.getValue()).isEqualTo(4.54);
  }

  @Test
  void scrollSelectByComponentUuidAndMetricKeys_for_empty_metric_set() {
    List<LiveMeasureDto> results = new ArrayList<>();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    underTest.scrollSelectByComponentUuidAndMetricKeys(db.getSession(), project.uuid(), Sets.newSet(),
      context -> results.add(context.getResultObject()));

    assertThat(results).isEmpty();
  }

  @Test
  void selectTreeByQuery_with_empty_results() {
    List<LiveMeasureDto> results = new ArrayList<>();
    underTest.selectTreeByQuery(db.getSession(), newPrivateProjectDto(),
      MeasureTreeQuery.builder().setStrategy(MeasureTreeQuery.Strategy.LEAVES).build(),
      context -> results.add(context.getResultObject()));

    assertThat(results).isEmpty();
  }

  @Test
  void selectMeasure_map_fields() {
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    underTest.insert(db.getSession(), newLiveMeasure(file, metric).setValue(3.14).setData("text_value"));

    LiveMeasureDto result =
      underTest.selectMeasure(db.getSession(), file.uuid(), metric.getKey()).orElseThrow(() -> new IllegalArgumentException("Measure not " +
        "found"));

    assertThat(result).as("Fail to map fields of %s", result.toString()).extracting(
        LiveMeasureDto::getProjectUuid, LiveMeasureDto::getComponentUuid, LiveMeasureDto::getMetricUuid, LiveMeasureDto::getValue,
        LiveMeasureDto::getDataAsString, LiveMeasureDto::getTextValue)
      .contains(project.uuid(), file.uuid(), metric.getUuid(), 3.14, "text_value", "text_value");
  }

  @Test
  void countNcloc() {
    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey("ncloc").setValueType(INT.toString()));
    MetricDto lines = db.measures().insertMetric(m -> m.setKey("lines").setValueType(INT.toString()));

    ProjectData simpleProject = db.components().insertPublicProject();
    db.measures().insertLiveMeasure(simpleProject, ncloc, m -> m.setValue(10d));

    ProjectData projectWithBiggerBranch = db.components().insertPublicProject();
    BranchDto bigBranch = db.components().insertProjectBranch(projectWithBiggerBranch.getProjectDto(),
      b -> b.setBranchType(BranchType.BRANCH));
    db.measures().insertLiveMeasure(projectWithBiggerBranch, ncloc, m -> m.setValue(100d));
    db.measures().insertLiveMeasure(bigBranch, ncloc, m -> m.setValue(200d));

    ProjectData projectWithLinesButNoLoc = db.components().insertPublicProject();
    db.measures().insertLiveMeasure(projectWithLinesButNoLoc, lines, m -> m.setValue(365d));
    db.measures().insertLiveMeasure(projectWithLinesButNoLoc, ncloc, m -> m.setValue(0d));

    assertThat(underTest.findNclocOfBiggestBranchForProject(db.getSession(), simpleProject.projectUuid())).isEqualTo(10L);
    assertThat(underTest.findNclocOfBiggestBranchForProject(db.getSession(), projectWithBiggerBranch.projectUuid())).isEqualTo(200L);
    assertThat(underTest.findNclocOfBiggestBranchForProject(db.getSession(), projectWithLinesButNoLoc.projectUuid())).isZero();
  }

  @Test
  void get_branch_with_max_ncloc_per_project() {
    Map<String, MetricDto> metrics = setupMetrics();
    MetricDto ncloc = metrics.get("ncloc");
    setupProjectsWithLoc(ncloc, metrics.get("ncloc_language_distribution"), metrics.get("lines"));

    Map<String, LargestBranchNclocDto> results = underTest.getLargestBranchNclocPerProject(db.getSession(), ncloc.getUuid())
      .stream()
      .collect(toMap(largestBranchNclocDto -> largestBranchNclocDto.getProjectKey() + " " + largestBranchNclocDto.getBranchName(),
        identity(), (a, b) -> a));

    assertThat(results).hasSize(5);
    assertLocForProject(results.get("projectWithTieOnBranchSize main"), DEFAULT_MAIN_BRANCH_NAME, 250);
    assertLocForProject(results.get("projectWithTieOnOtherBranches tieBranch1"), "tieBranch1", 230);
    assertLocForProject(results.get("projectWithBranchBiggerThanMaster notMasterBranch"), "notMasterBranch", 200);
    assertLocForProject(results.get("simpleProject main"), DEFAULT_MAIN_BRANCH_NAME, 10);
    assertLocForProject(results.get("projectWithLinesButNoLoc main"), DEFAULT_MAIN_BRANCH_NAME, 0);
  }

  @Test
  void get_loc_language_distribution() {
    Map<String, MetricDto> metrics = setupMetrics();
    MetricDto ncloc = metrics.get("ncloc");
    MetricDto nclocLanguageDistribution = metrics.get("ncloc_language_distribution");
    setupProjectsWithLoc(ncloc, nclocLanguageDistribution, metrics.get("lines"));

    List<ProjectLocDistributionDto> results = underTest.selectLargestBranchesLocDistribution(db.getSession(), ncloc.getUuid(),
      nclocLanguageDistribution.getUuid());

    String firstBranchOfProjectUuid =
      db.getDbClient().branchDao().selectByProjectUuid(db.getSession(), "projectWithTieOnOtherBranches").stream()
      .filter(branchDto -> !branchDto.isMain())
      .map(BranchDto::getUuid)
      .sorted()
      .findFirst().orElseThrow();

    assertThat(results)
      .containsExactlyInAnyOrder(
        new ProjectLocDistributionDto("projectWithTieOnBranchSize", getMainBranchUuid("projectWithTieOnBranchSize"), "java=250;js=0"),
        new ProjectLocDistributionDto("projectWithTieOnOtherBranches", firstBranchOfProjectUuid, "java=230;js=0"),
        new ProjectLocDistributionDto("projectWithBranchBiggerThanMaster", getBranchUuid("projectWithBranchBiggerThanMaster",
          "notMasterBranch"), "java=100;js=100"),
        new ProjectLocDistributionDto("simpleProject", getMainBranchUuid("simpleProject"), "java=10;js=0"),
        new ProjectLocDistributionDto("projectWithLinesButNoLoc", getMainBranchUuid("projectWithLinesButNoLoc"), "java=0;js=0"));
  }

  private String getBranchUuid(String projectUuid, String branchKey) {
    return db.getDbClient().branchDao().selectByBranchKey(db.getSession(), projectUuid, branchKey).get().getUuid();
  }

  private String getMainBranchUuid(String projectUuid) {
    return db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), projectUuid).get().getUuid();
  }

  @Test
  void countNcloc_empty() {
    db.measures().insertMetric(m -> m.setKey("ncloc").setValueType(INT.toString()));
    db.measures().insertMetric(m -> m.setKey("lines").setValueType(INT.toString()));
    long result = underTest.findNclocOfBiggestBranchForProject(db.getSession(), "non-existing-project-uuid");

    assertThat(result).isZero();
  }

  @Test
  void insert_data() {
    byte[] data = "text_value".getBytes(StandardCharsets.UTF_8);
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    LiveMeasureDto measure = newLiveMeasure(file, metric).setData(data);

    underTest.insert(db.getSession(), measure);

    LiveMeasureDto result =
      underTest.selectMeasure(db.getSession(), file.uuid(), metric.getKey()).orElseThrow(() -> new IllegalArgumentException("Measure not " +
        "found"));
    assertThat(new String(result.getData(), StandardCharsets.UTF_8)).isEqualTo("text_value");
    assertThat(result.getDataAsString()).isEqualTo("text_value");
  }

  @Test
  void insertOrUpdate() {
    // insert
    LiveMeasureDto dto = newLiveMeasure();
    underTest.insertOrUpdate(db.getSession(), dto);
    verifyPersisted(dto);
    verifyTableSize(1);

    // update
    dto.setValue(dto.getValue() + 1);
    dto.setData(dto.getDataAsString() + "_new");
    underTest.insertOrUpdate(db.getSession(), dto);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void deleteByComponentUuidExcludingMetricUuids() {
    LiveMeasureDto measure1 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("1");
    LiveMeasureDto measure2 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("2");
    LiveMeasureDto measure3 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("3");
    LiveMeasureDto measureOtherComponent = newLiveMeasure().setComponentUuid("C2").setMetricUuid("3");
    underTest.insertOrUpdate(db.getSession(), measure1);
    underTest.insertOrUpdate(db.getSession(), measure2);
    underTest.insertOrUpdate(db.getSession(), measure3);
    underTest.insertOrUpdate(db.getSession(), measureOtherComponent);

    underTest.deleteByComponentUuidExcludingMetricUuids(db.getSession(), "C1", Arrays.asList("1", "2"));

    verifyTableSize(3);
    verifyPersisted(measure1);
    verifyPersisted(measure2);
    verifyPersisted(measureOtherComponent);
  }

  @Test
  void deleteByComponentUuid() {
    LiveMeasureDto measure1 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("1");
    LiveMeasureDto measure2 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("2");
    LiveMeasureDto measure3 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("3");
    LiveMeasureDto measureOtherComponent = newLiveMeasure().setComponentUuid("C2").setMetricUuid("3");
    underTest.insertOrUpdate(db.getSession(), measure1);
    underTest.insertOrUpdate(db.getSession(), measure2);
    underTest.insertOrUpdate(db.getSession(), measure3);
    underTest.insertOrUpdate(db.getSession(), measureOtherComponent);

    underTest.deleteByComponent(db.getSession(), "C1");

    verifyTableSize(1);
    verifyPersisted(measureOtherComponent);
  }

  @Test
  void deleteByComponentUuidExcludingMetricUuids_with_empty_metrics() {
    LiveMeasureDto measure1 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("1");
    LiveMeasureDto measure2 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("2");
    LiveMeasureDto measureOnOtherComponent = newLiveMeasure().setComponentUuid("C2").setMetricUuid("2");
    underTest.insertOrUpdate(db.getSession(), measure1);
    underTest.insertOrUpdate(db.getSession(), measure2);
    underTest.insertOrUpdate(db.getSession(), measureOnOtherComponent);

    underTest.deleteByComponentUuidExcludingMetricUuids(db.getSession(), "C1", Collections.emptyList());

    verifyTableSize(1);
    verifyPersisted(measureOnOtherComponent);
  }

  @Test
  void countProjectsHavingMeasure() {
    MetricDto metric1 = db.measures().insertMetric();
    MetricDto metric2 = db.measures().insertMetric();
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    db.measures().insertLiveMeasure(project1, metric1);
    db.measures().insertLiveMeasure(project2, metric1);
    db.measures().insertLiveMeasure(project1, metric2);

    assertThat(underTest.countProjectsHavingMeasure(db.getSession(), metric1.getKey())).isEqualTo(2);
    assertThat(underTest.countProjectsHavingMeasure(db.getSession(), metric2.getKey())).isOne();
    assertThat(underTest.countProjectsHavingMeasure(db.getSession(), "unknown")).isZero();
  }

  @Test
  void upsert_inserts_or_updates_row() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    // insert
    LiveMeasureDto dto = newLiveMeasure();
    int count = underTest.upsert(db.getSession(), dto);
    verifyPersisted(dto);
    verifyTableSize(1);
    assertThat(count).isOne();

    // update
    dto.setValue(dto.getValue() + 1);
    dto.setData(dto.getDataAsString() + "_new");
    count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_does_not_update_row_if_values_are_not_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    LiveMeasureDto dto = newLiveMeasure();
    underTest.upsert(db.getSession(), dto);

    // update
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isZero();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_updates_row_if_lob_data_is_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    LiveMeasureDto dto = newLiveMeasure().setData(RandomStringUtils.random(10_000));
    underTest.upsert(db.getSession(), dto);

    // update
    dto.setData(RandomStringUtils.random(dto.getDataAsString().length() + 10));
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_does_not_update_row_if_lob_data_is_not_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setData(RandomStringUtils.random(10_000));
    underTest.upsert(db.getSession(), dto);

    // update
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isZero();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_updates_row_if_lob_data_is_removed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    LiveMeasureDto dto = newLiveMeasure().setData(RandomStringUtils.random(10_000));
    underTest.upsert(db.getSession(), dto);

    // update
    dto.setData((String) null);
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_updates_row_if_value_is_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setValue(40.0);
    underTest.upsert(db.getSession(), dto);

    // update
    dto.setValue(50.0);
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_updates_row_if_value_is_removed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setValue(40.0);
    underTest.upsert(db.getSession(), dto);

    // update
    dto.setValue(null);
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_updates_row_if_value_is_added() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setValue(null);
    underTest.upsert(db.getSession(), dto);

    // update
    dto.setValue(40.0);
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_multiple_rows() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    // insert 30
    List<LiveMeasureDto> inserted = new ArrayList<>();
    IntStream.range(0, 30).forEach(i -> inserted.add(newLiveMeasure()));
    for (LiveMeasureDto dto : inserted) {
      underTest.upsert(db.getSession(), dto);
    }
    verifyTableSize(30);

    // update 10 with new values, update 5 without any change and insert new 50
    List<LiveMeasureDto> upserted = new ArrayList<>();
    IntStream.range(0, 10).forEach(i -> {
      LiveMeasureDto d = inserted.get(i);
      upserted.add(d.setValue(d.getValue() + 123));
    });
    upserted.addAll(inserted.subList(10, 15));
    IntStream.range(0, 50).forEach(i -> upserted.add(newLiveMeasure()));
    for (LiveMeasureDto dto : upserted) {
      underTest.upsert(db.getSession(), dto);
    }
    db.getSession().commit();
    verifyTableSize(80);
  }

  private void verifyTableSize(int expectedSize) {
    assertThat(db.countRowsOfTable(db.getSession(), "live_measures")).isEqualTo(expectedSize);
  }

  private void verifyPersisted(LiveMeasureDto dto) {
    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricUuids(db.getSession(), singletonList(dto.getComponentUuid())
      , singletonList(dto.getMetricUuid()));
    assertThat(selected).hasSize(1);
    assertThat(selected.get(0)).isEqualToComparingOnlyGivenFields(dto,
      // do not compare the field "uuid", which is used only for insert, not select
      "componentUuid", "projectUuid", "metricUuid", "value", "textValue", "data");
  }

  private Map<String, MetricDto> setupMetrics() {
    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey("ncloc").setValueType(INT.toString()));
    MetricDto nclocDistribution = db.measures().insertMetric(m -> m.setKey("ncloc_language_distribution").setValueType(DATA.toString()));
    MetricDto lines = db.measures().insertMetric(m -> m.setKey("lines").setValueType(INT.toString()));
    return Map.of("ncloc", ncloc,
      "ncloc_language_distribution", nclocDistribution,
      "lines", lines);
  }

  private void setupProjectsWithLoc(MetricDto ncloc, MetricDto nclocDistribution, MetricDto lines) {
    ProjectData simpleProject = addProjectWithMeasure("simpleProject", ncloc, 10d);
    addMeasureToMainBranch(simpleProject, nclocDistribution, "java=10;js=0");

    ProjectData projectWithBranchBiggerThanMaster = addProjectWithMeasure("projectWithBranchBiggerThanMaster", ncloc, 100d);
    addMeasureToMainBranch(projectWithBranchBiggerThanMaster, nclocDistribution, "java=100;js=0");

    BranchDto notMasterBranch = addBranchToProjectWithMeasure(projectWithBranchBiggerThanMaster, "notMasterBranch", ncloc, 200d);
    addMeasureToBranch(notMasterBranch, nclocDistribution, "java=100;js=100");

    ProjectData projectWithLinesButNoLoc = addProjectWithMeasure("projectWithLinesButNoLoc", lines, 365d);
    addMeasureToMainBranch(projectWithLinesButNoLoc, nclocDistribution, "java=0;js=0");
    addMeasureToBranch(projectWithLinesButNoLoc.getMainBranchDto(), ncloc, 0d, false);

    ProjectData projectWithTieOnBranchSize = addProjectWithMeasure("projectWithTieOnBranchSize", ncloc, 250d);
    addMeasureToMainBranch(projectWithTieOnBranchSize, nclocDistribution, "java=250;js=0");
    BranchDto tieBranch = addBranchToProjectWithMeasure(projectWithTieOnBranchSize, "tieBranch", ncloc, 250d);
    addMeasureToBranch(tieBranch, nclocDistribution, "java=250;js=0");

    ProjectData projectWithTieOnOtherBranches = addProjectWithMeasure("projectWithTieOnOtherBranches", ncloc, 220d);
    addMeasureToMainBranch(projectWithTieOnOtherBranches, nclocDistribution, "java=220;js=0");
    BranchDto tieBranch1 = addBranchToProjectWithMeasure(projectWithTieOnOtherBranches, "tieBranch1", ncloc, 230d);
    addMeasureToBranch(tieBranch1, nclocDistribution, "java=230;js=0");
    BranchDto tieBranch2 = addBranchToProjectWithMeasure(projectWithTieOnOtherBranches, "tieBranch2", ncloc, 230d);
    addMeasureToBranch(tieBranch2, nclocDistribution, "java=230;js=0");
  }

  private ProjectData addProjectWithMeasure(String projectKey, MetricDto metric, double metricValue) {
    ProjectData project = db.components().insertPublicProject(projectKey, p -> p.setKey(projectKey));
    addMeasureToBranch(project.getMainBranchDto(), metric, metricValue, true);
    return project;
  }

  private void addMeasureToBranch(BranchDto component, MetricDto metric, double metricValue, boolean addSnapshot) {
    db.measures().insertLiveMeasure(component, metric, m -> m.setValue(metricValue));
    if (addSnapshot) {
      db.components().insertSnapshot(component, t -> t.setLast(true));
    }
  }

  private void addMeasureToMainBranch(ProjectData projectData, MetricDto metric, String metricValue) {
    addMeasureToBranch(projectData.getMainBranchDto(), metric, metricValue);
  }

  private void addMeasureToBranch(BranchDto component, MetricDto metric, String metricValue) {
    db.measures().insertLiveMeasure(component, metric, m -> m.setData(metricValue));
  }

  private BranchDto addBranchToProjectWithMeasure(ProjectData project, String branchKey, MetricDto metric, double metricValue) {
    BranchDto branch = db.components()
      .insertProjectBranch(project.getProjectDto(), b -> b.setBranchType(BranchType.BRANCH).setKey(branchKey),
        branchDto -> branchDto.setUuid("uuid_" + branchId++));
    addMeasureToBranch(branch, metric, metricValue, true);
    return branch;
  }

  private void assertLocForProject(LargestBranchNclocDto result, String branchKey, long linesOfCode) {
    assertThat(result.getBranchName()).isEqualTo(branchKey);
    assertThat(result.getLoc()).isEqualTo(linesOfCode);
  }
}
