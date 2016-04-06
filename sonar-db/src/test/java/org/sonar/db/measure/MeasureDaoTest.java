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
package org.sonar.db.measure;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricTesting;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDevProjectCopy;
import static org.sonar.db.component.ComponentTesting.newDeveloper;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;

public class MeasureDaoTest {

  private static final int SNAPSHOT_ID = 5;
  private static final long DEVELOPER_ID = 333L;
  private static final int AUTHORS_BY_LINE_METRIC_ID = 10;
  private static final int COVERAGE_LINE_HITS_DATA_METRIC_ID = 11;
  private static final int NCLOC_METRIC_ID = 12;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  final DbClient dbClient = db.getDbClient();
  final DbSession dbSession = db.getSession();

  MeasureDao underTest = dbClient.measureDao();
  ComponentDbTester componentDb = new ComponentDbTester(db);

  @Test
  public void get_value_by_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    MeasureDto result = underTest.selectByComponentKeyAndMetricKey(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc");
    assertThat(result.getId()).isEqualTo(22);
    assertThat(result.getValue()).isEqualTo(10d);
    assertThat(result.getData()).isNull();
    assertThat(result.getVariation(1)).isEqualTo(1d);
    assertThat(result.getVariation(2)).isEqualTo(2d);
    assertThat(result.getVariation(3)).isEqualTo(3d);
    assertThat(result.getVariation(4)).isEqualTo(4d);
    assertThat(result.getVariation(5)).isEqualTo(-5d);
    assertThat(result.getAlertStatus()).isEqualTo("OK");
    assertThat(result.getAlertText()).isEqualTo("Green");
  }

  @Test
  // TODO the string must be longer than 4000 char to be persisted in the data field
  public void get_data_by_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    MeasureDto result = underTest.selectByComponentKeyAndMetricKey(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java", "authors_by_line");
    assertThat(result.getId()).isEqualTo(20);
    assertThat(result.getData()).isEqualTo("0123456789012345678901234567890123456789");
  }

  @Test
  public void get_text_value_by_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    MeasureDto result = underTest.selectByComponentKeyAndMetricKey(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java", "coverage_line_hits_data");
    assertThat(result.getId()).isEqualTo(21);
    assertThat(result.getData()).isEqualTo("36=1;37=1;38=1;39=1;43=1;48=1;53=1");
  }

  @Test
  public void select_by_component_key_and_metrics() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<MeasureDto> results = underTest.selectByComponentKeyAndMetricKeys(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java",
      newArrayList("ncloc", "authors_by_line"));
    assertThat(results).hasSize(2);

    results = underTest.selectByComponentKeyAndMetricKeys(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java", newArrayList("ncloc"));
    assertThat(results).hasSize(1);

    MeasureDto result = results.get(0);
    assertThat(result.getId()).isEqualTo(22);
    assertThat(result.getValue()).isEqualTo(10d);
    assertThat(result.getMetricKey()).isEqualTo("ncloc");
    assertThat(result.getVariation(1)).isEqualTo(1d);
    assertThat(result.getVariation(2)).isEqualTo(2d);
    assertThat(result.getVariation(3)).isEqualTo(3d);
    assertThat(result.getVariation(4)).isEqualTo(4d);
    assertThat(result.getVariation(5)).isEqualTo(-5d);
  }

  @Test
  public void select_by_snapshotId_and_metrics() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<MeasureDto> results = underTest.selectBySnapshotIdAndMetricKeys(SNAPSHOT_ID, ImmutableSet.of("ncloc", "authors_by_line"), dbSession);
    assertThat(results).hasSize(2);

    Optional<MeasureDto> optional = FluentIterable.from(results).filter(new Predicate<MeasureDto>() {
      @Override
      public boolean apply(@Nullable MeasureDto input) {
        return input.getId() == 22;
      }
    }).first();
    assertThat(optional).isPresent();

    MeasureDto result = optional.get();
    assertThat(result.getId()).isEqualTo(22);
    assertThat(result.getMetricId()).isEqualTo(12);
    assertThat(result.getMetricKey()).isNull();
    assertThat(result.getValue()).isEqualTo(10d);
    assertThat(result.getVariation(1)).isEqualTo(1d);
    assertThat(result.getVariation(2)).isEqualTo(2d);
    assertThat(result.getVariation(3)).isEqualTo(3d);
    assertThat(result.getVariation(4)).isEqualTo(4d);
    assertThat(result.getVariation(5)).isEqualTo(-5d);
  }

  @Test
  public void find_by_component_key_and_metric() {
    db.prepareDbUnit(getClass(), "shared.xml");

    MeasureDto result = underTest.selectByComponentKeyAndMetricKey(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc");
    assertThat(result.getId()).isEqualTo(22);
    assertThat(result.getValue()).isEqualTo(10d);
    assertThat(result.getMetricKey()).isEqualTo("ncloc");
    assertThat(result.getVariation(1)).isEqualTo(1d);
    assertThat(result.getVariation(2)).isEqualTo(2d);
    assertThat(result.getVariation(3)).isEqualTo(3d);
    assertThat(result.getVariation(4)).isEqualTo(4d);
    assertThat(result.getVariation(5)).isEqualTo(-5d);

    assertThat(underTest.selectByComponentKeyAndMetricKey(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java", "unknown")).isNull();
  }

  @Test
  public void exists_by_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.existsByKey(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc")).isTrue();
    assertThat(underTest.existsByKey(dbSession, "org.struts:struts-core:src/org/struts/RequestContext.java", "unknown")).isFalse();
  }

  @Test
  public void select_past_measures_by_component_uuid_and_root_snapshot_id_and_metric_keys() {
    db.prepareDbUnit(getClass(), "past_measures.xml");

    Map<Long, PastMeasureDto> fileMeasures = pastMeasuresById(underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(dbSession, "CDEF", 1000L, ImmutableSet.of(1, 2)));
    assertThat(fileMeasures).hasSize(2);

    PastMeasureDto fileMeasure1 = fileMeasures.get(5L);
    assertThat(fileMeasure1.getValue()).isEqualTo(5d);
    assertThat(fileMeasure1.getMetricId()).isEqualTo(1);
    assertThat(fileMeasure1.getPersonId()).isNull();

    PastMeasureDto fileMeasure2 = fileMeasures.get(6L);
    assertThat(fileMeasure2.getValue()).isEqualTo(60d);
    assertThat(fileMeasure2.getMetricId()).isEqualTo(2);

    Map<Long, PastMeasureDto> projectMeasures = pastMeasuresById(underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(dbSession, "ABCD", 1000L, ImmutableSet.of(1, 2)));
    assertThat(projectMeasures).hasSize(2);

    PastMeasureDto projectMeasure1 = projectMeasures.get(1L);
    assertThat(projectMeasure1.getValue()).isEqualTo(60d);
    assertThat(projectMeasure1.getMetricId()).isEqualTo(1);

    PastMeasureDto projectMeasure2 = projectMeasures.get(2L);
    assertThat(projectMeasure2.getValue()).isEqualTo(80d);
    assertThat(projectMeasure2.getMetricId()).isEqualTo(2);

    assertThat(underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(dbSession, "UNKNOWN", 1000L, ImmutableSet.of(1, 2))).isEmpty();
    assertThat(underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(dbSession, "CDEF", 987654L, ImmutableSet.of(1, 2))).isEmpty();
    assertThat(underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(dbSession, "CDEF", 1000L, ImmutableSet.of(123, 456))).isEmpty();
  }

  @Test
  public void select_past_measures_ignore_measures_with_person_id() {
    db.prepareDbUnit(getClass(), "past_measures_with_person_id.xml");

    List<PastMeasureDto> measures = underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(dbSession, "ABCD", 1000L, ImmutableSet.of(1));
    assertThat(measures).hasSize(1);

    Map<Long, PastMeasureDto> pastMeasuresById = pastMeasuresById(measures);

    PastMeasureDto measure1 = pastMeasuresById.get(1L);
    assertThat(measure1.getPersonId()).isNull();
  }

  @Test
  public void select_by_snapshot_and_metric_keys() throws Exception {
    db.prepareDbUnit(getClass(), "select_by_snapshot_and_metric_keys.xml");

    List<MeasureDto> results = underTest.selectBySnapshotIdAndMetricKeys(SNAPSHOT_ID, newHashSet("ncloc", "authors_by_line"), dbSession);
    assertThat(results).hasSize(2);

    results = underTest.selectBySnapshotIdAndMetricKeys(SNAPSHOT_ID, newHashSet("ncloc"), dbSession);
    assertThat(results).hasSize(1);

    MeasureDto result = results.get(0);
    assertThat(result.getId()).isEqualTo(22);
    assertThat(result.getValue()).isEqualTo(10d);
    assertThat(result.getVariation(1)).isEqualTo(1d);
    assertThat(result.getVariation(2)).isEqualTo(2d);
    assertThat(result.getVariation(3)).isEqualTo(3d);
    assertThat(result.getVariation(4)).isEqualTo(4d);
    assertThat(result.getVariation(5)).isEqualTo(-5d);

    assertThat(underTest.selectBySnapshotIdAndMetricKeys(123, newHashSet("ncloc"), dbSession)).isEmpty();
    assertThat(underTest.selectBySnapshotIdAndMetricKeys(SNAPSHOT_ID, Collections.<String>emptySet(), dbSession)).isEmpty();
  }

  @Test
  public void selectByDeveloperForSnapshotAndMetrics_when_there_is_no_measure_for_developer_returns_empty() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<MeasureDto> measureDtos = underTest.selectByDeveloperForSnapshotAndMetrics(dbSession,
      DEVELOPER_ID, SNAPSHOT_ID,
      ImmutableList.of(AUTHORS_BY_LINE_METRIC_ID, COVERAGE_LINE_HITS_DATA_METRIC_ID, NCLOC_METRIC_ID));

    assertThat(measureDtos).isEmpty();
  }

  @Test
  public void selectByDeveloperForSnapshotAndMetrics_returns_only_measures_for_developer() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectByDeveloperForSnapshotAndMetrics(dbSession,
      DEVELOPER_ID, SNAPSHOT_ID,
      ImmutableList.of(AUTHORS_BY_LINE_METRIC_ID, COVERAGE_LINE_HITS_DATA_METRIC_ID, NCLOC_METRIC_ID));

    assertThat(measureDtos).extracting("id").containsOnly(30L, 31L, 32L);
  }

  @Test
  public void selectByDeveloperForSnapshotAndMetrics_returns_only_measures_for_developer_and_specified_metric_id() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectByDeveloperForSnapshotAndMetrics(dbSession,
      DEVELOPER_ID, SNAPSHOT_ID,
      ImmutableList.of(NCLOC_METRIC_ID));

    assertThat(measureDtos).extracting("id").containsOnly(32L);
  }

  @Test
  public void selectBySnapshotAndMetrics_returns_empty_when_single_metric_id_does_not_exist() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectBySnapshotAndMetrics(dbSession,
      SNAPSHOT_ID,
      ImmutableList.of(666));

    assertThat(measureDtos).isEmpty();
  }

  @Test
  public void selectBySnapshotAndMetrics_returns_only_measures_not_for_developer() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectBySnapshotAndMetrics(dbSession,
      SNAPSHOT_ID,
      ImmutableList.of(AUTHORS_BY_LINE_METRIC_ID, COVERAGE_LINE_HITS_DATA_METRIC_ID, NCLOC_METRIC_ID));

    assertThat(measureDtos).extracting("id").containsOnly(20L, 21L, 22L);
  }

  @Test
  public void selectBySnapshotAndMetrics_returns_only_measures_not_for_developer_and_with_specified_metric_id() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectBySnapshotAndMetrics(dbSession,
      SNAPSHOT_ID,
      ImmutableList.of(NCLOC_METRIC_ID));

    assertThat(measureDtos).extracting("id").containsOnly(22L);
  }

  @Test
  public void selectByDeveloperForSnapshotAndMetrics_returns_empty_when_single_metric_id_does_not_exist() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectByDeveloperForSnapshotAndMetrics(dbSession,
      DEVELOPER_ID, SNAPSHOT_ID,
      ImmutableList.of(666));

    assertThat(measureDtos).isEmpty();
  }

  @Test
  public void selectByDeveloperForSnapshotAndMetrics_returns_empty_when_snapshotId_does_not_exist() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectByDeveloperForSnapshotAndMetrics(dbSession,
      DEVELOPER_ID, 10,
      ImmutableList.of(AUTHORS_BY_LINE_METRIC_ID, COVERAGE_LINE_HITS_DATA_METRIC_ID, NCLOC_METRIC_ID));

    assertThat(measureDtos).isEmpty();
  }

  @Test
  public void selectSnapshotIdsAndMetricIds() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, MetricTesting.newMetricDto());
    ComponentDto project = newProjectDto();
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    SnapshotDto fileSnapshot = componentDb.insertComponentAndSnapshot(newFileDto(project, "file-uuid"), projectSnapshot);
    ComponentDto developer = newDeveloper("Ray Bradbury");
    SnapshotDto developerSnapshot = componentDb.insertDeveloperAndSnapshot(developer);
    componentDb.insertComponentAndSnapshot(newDevProjectCopy("project-copy-uuid", project, developer), developerSnapshot);
    underTest.insert(dbSession,
      newMeasureDto(metric, developerSnapshot.getId()).setDeveloperId(developer.getId()),
      newMeasureDto(metric, projectSnapshot.getId()),
      newMeasureDto(metric, fileSnapshot.getId()));
    dbSession.commit();

    List<MeasureDto> result = underTest.selectBySnapshotIdsAndMetricIds(dbSession,
      newArrayList(developerSnapshot.getId(), projectSnapshot.getId(), fileSnapshot.getId()),
      singletonList(metric.getId()));

    assertThat(result)
      .hasSize(2)
      .extracting("snapshotId")
      .containsOnly(projectSnapshot.getId(), fileSnapshot.getId())
      .doesNotContain(developerSnapshot.getId());
  }

  @Test
  public void selectDeveloperAndSnapshotIdsAndMetricIds() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, MetricTesting.newMetricDto());
    ComponentDto project = newProjectDto();
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    SnapshotDto fileSnapshot = componentDb.insertComponentAndSnapshot(newFileDto(project, "file-uuid"), projectSnapshot);
    ComponentDto developer = newDeveloper("Ray Bradbury");
    SnapshotDto developerSnapshot = componentDb.insertDeveloperAndSnapshot(developer);
    componentDb.insertComponentAndSnapshot(newDevProjectCopy("project-copy-uuid", project, developer), developerSnapshot);

    underTest.insert(dbSession,
      newMeasureDto(metric, developerSnapshot.getId()).setDeveloperId(developer.getId()),
      newMeasureDto(metric, projectSnapshot.getId()).setDeveloperId(developer.getId()),
      newMeasureDto(metric, projectSnapshot.getId()).setDeveloperId(null),
      newMeasureDto(metric, fileSnapshot.getId()).setDeveloperId(developer.getId()));
    dbSession.commit();

    List<MeasureDto> result = underTest.selectByDeveloperAndSnapshotIdsAndMetricIds(dbSession,
      developer.getId(),
      newArrayList(developerSnapshot.getId(), projectSnapshot.getId(), fileSnapshot.getId()),
      singletonList(metric.getId()));

    assertThat(result)
      .hasSize(3)
      .extracting("snapshotId")
      .containsOnly(developerSnapshot.getId(), projectSnapshot.getId(), fileSnapshot.getId());
    assertThat(result)
      .extracting("developerId")
      .containsOnly(developer.getId());
  }

  @Test
  public void selectProjectMeasuresByDeveloperForMetrics_returns_empty_on_empty_db() {
    assertThat(underTest.selectProjectMeasuresByDeveloperForMetrics(dbSession, DEVELOPER_ID, ImmutableList.of(NCLOC_METRIC_ID, AUTHORS_BY_LINE_METRIC_ID))).isEmpty();
  }

  @Test
  public void selectProjectMeasuresByDeveloperForMetrics_returns_empty_when_no_measure_for_developer() {
    long otherDeveloperId = 666l;

    ComponentDto projectDto = insertProject("aa");
    SnapshotDto snapshotDto = insertSnapshot(projectDto, true);
    insertMeasure(snapshotDto, DEVELOPER_ID, NCLOC_METRIC_ID, 12d);

    List<MeasureDto> measureDtos = underTest.selectProjectMeasuresByDeveloperForMetrics(dbSession, DEVELOPER_ID, ImmutableList.of(NCLOC_METRIC_ID));
    assertThat(measureDtos).hasSize(1);
    MeasureDto measureDto = measureDtos.iterator().next();
    assertThat(measureDto.getId()).isNotNull();
    assertThat(measureDto.getMetricId()).isEqualTo(NCLOC_METRIC_ID);
    assertThat(measureDto.getSnapshotId()).isEqualTo(snapshotDto.getId());
    assertThat(measureDto.getComponentId()).isEqualTo(projectDto.getId());
    assertThat(measureDto.getDeveloperId()).isEqualTo(DEVELOPER_ID);

    assertThat(underTest.selectProjectMeasuresByDeveloperForMetrics(dbSession, otherDeveloperId, ImmutableList.of(NCLOC_METRIC_ID))).isEmpty();
    assertThat(underTest.selectProjectMeasuresByDeveloperForMetrics(dbSession, DEVELOPER_ID, ImmutableList.of(AUTHORS_BY_LINE_METRIC_ID))).isEmpty();
  }

  @Test
  public void selectProjectMeasuresByDeveloperForMetrics_returns_ignores_measure_of_non_last_snapshot() {
    long otherDeveloperId = 666l;

    ComponentDto projectDto = insertProject("aa");
    SnapshotDto nonLastSnapshotDto = insertSnapshot(projectDto, false);
    insertMeasure(nonLastSnapshotDto, DEVELOPER_ID, NCLOC_METRIC_ID, 12d);
    SnapshotDto lastSnapshotDto = insertSnapshot(projectDto, true);
    insertMeasure(lastSnapshotDto, otherDeveloperId, NCLOC_METRIC_ID, 15d);

    assertThat(underTest.selectProjectMeasuresByDeveloperForMetrics(dbSession, DEVELOPER_ID, ImmutableList.of(NCLOC_METRIC_ID))).hasSize(0);

    List<MeasureDto> measureDtos = underTest.selectProjectMeasuresByDeveloperForMetrics(dbSession, otherDeveloperId, ImmutableList.of(NCLOC_METRIC_ID));
    assertThat(measureDtos).hasSize(1);
    MeasureDto measureDto = measureDtos.iterator().next();
    assertThat(measureDto.getMetricId()).isEqualTo(NCLOC_METRIC_ID);
    assertThat(measureDto.getSnapshotId()).isEqualTo(lastSnapshotDto.getId());
    assertThat(measureDto.getComponentId()).isEqualTo(projectDto.getId());
    assertThat(measureDto.getDeveloperId()).isEqualTo(otherDeveloperId);
    assertThat(measureDto.getValue()).isEqualTo(15d);
  }

  @Test
  public void selectProjectMeasuresByDeveloperForMetrics_returns_ignores_snapshots_of_any_component_but_project() {
    ComponentDto projectDto = insertProject("aa");
    insertSnapshot(projectDto, true);
    ComponentDto moduleDto = insertComponent(ComponentTesting.newModuleDto(projectDto));
    insertMeasure(insertSnapshot(moduleDto, true), DEVELOPER_ID, NCLOC_METRIC_ID, 15d);
    ComponentDto dirDto = insertComponent(ComponentTesting.newDirectory(moduleDto, "toto"));
    insertMeasure(insertSnapshot(dirDto, true), DEVELOPER_ID, NCLOC_METRIC_ID, 25d);
    ComponentDto fileDto = insertComponent(ComponentTesting.newFileDto(moduleDto, "tutu"));
    insertMeasure(insertSnapshot(fileDto, true), DEVELOPER_ID, NCLOC_METRIC_ID, 35d);

    assertThat(underTest.selectProjectMeasuresByDeveloperForMetrics(dbSession, DEVELOPER_ID, ImmutableList.of(NCLOC_METRIC_ID))).isEmpty();
  }

  private ComponentDto insertComponent(ComponentDto moduleDto) {
    dbClient.componentDao().insert(dbSession, moduleDto);
    dbSession.commit();
    return moduleDto;
  }

  private ComponentDto insertProject(String uuid) {
    ComponentDto projectDto = newProjectDto(uuid);
    return insertComponent(projectDto);
  }

  private SnapshotDto insertSnapshot(ComponentDto componentDto, boolean last) {
    SnapshotDto snapshotDto = new SnapshotDto().setComponentId(componentDto.getId()).setLast(last).setQualifier(componentDto.qualifier()).setScope(componentDto.scope());
    dbClient.snapshotDao().insert(dbSession, snapshotDto);
    dbSession.commit();
    return snapshotDto;
  }

  private MeasureDto insertMeasure(SnapshotDto snapshotDto, Long developerId, int metricId, double value) {
    MeasureDto measureDto = new MeasureDto().setMetricId(metricId).setValue(value).setSnapshotId(snapshotDto.getId()).setDeveloperId(developerId);
    dbClient.measureDao().insert(dbSession, measureDto);
    dbSession.commit();
    return measureDto;
  }

  @Test
  public void insert() {
    db.prepareDbUnit(getClass(), "empty.xml");

    underTest.insert(dbSession, new MeasureDto()
      .setSnapshotId(2L)
      .setMetricId(3)
      .setDeveloperId(23L)
      .setRuleId(5)
      .setComponentId(6L)
      .setValue(2.0d)
      .setData("measure-value")
      .setVariation(1, 1.0d)
      .setVariation(2, 2.0d)
      .setVariation(3, 3.0d)
      .setVariation(4, 4.0d)
      .setVariation(5, 5.0d)
      .setAlertStatus("alert")
      .setAlertText("alert-text")
      .setDescription("measure-description"));
    dbSession.commit();

    db.assertDbUnit(getClass(), "insert-result.xml", new String[] {"id"}, "project_measures");
  }

  @Test
  public void insert_measures() {
    db.prepareDbUnit(getClass(), "empty.xml");

    underTest.insert(dbSession, new MeasureDto()
      .setSnapshotId(2L)
      .setMetricId(3)
      .setComponentId(6L)
      .setValue(2.0d),
      new MeasureDto()
        .setSnapshotId(3L)
        .setMetricId(4)
        .setComponentId(6L)
        .setValue(4.0d));
    dbSession.commit();

    assertThat(db.countRowsOfTable("project_measures")).isEqualTo(2);
  }

  private static Map<Long, PastMeasureDto> pastMeasuresById(List<PastMeasureDto> pastMeasures) {
    return FluentIterable.from(pastMeasures).uniqueIndex(new Function<PastMeasureDto, Long>() {
      @Nullable
      @Override
      public Long apply(PastMeasureDto input) {
        return input.getId();
      }
    });
  }

  private static Map<Long, MeasureDto> measuresById(List<MeasureDto> pastMeasures) {
    return FluentIterable.from(pastMeasures).uniqueIndex(new Function<MeasureDto, Long>() {
      @Nullable
      @Override
      public Long apply(MeasureDto input) {
        return input.getId();
      }
    });
  }
}
