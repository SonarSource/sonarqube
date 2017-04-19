/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotTesting;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.UNIT_TEST_FILE;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTreeQuery.Strategy.CHILDREN;
import static org.sonar.db.measure.MeasureTreeQuery.Strategy.LEAVES;

public class MeasureDaoTest {

  private static final int COVERAGE_METRIC_ID = 10;
  private static final int COMPLEXITY_METRIC_ID = 11;
  private static final int NCLOC_METRIC_ID = 12;
  private static final long A_PERSON_ID = 444L;
  private static final String LAST_ANALYSIS_UUID = "A1";
  private static final String OTHER_ANALYSIS_UUID = "A2";
  private static final String PREVIOUS_ANALYSIS_UUID = "previous analysis UUID";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private MeasureDao underTest = db.getDbClient().measureDao();

  @Test
  public void test_inserted_and_selected_columns() {
    ComponentDto project = db.components().insertPrivateProject();
    insertAnalysis(LAST_ANALYSIS_UUID, project.uuid(), true);
    db.components().insertComponent(newFileDto(project).setUuid("C4"));

    MeasureDto inserted = new MeasureDto()
      .setAnalysisUuid(LAST_ANALYSIS_UUID)
      .setMetricId(2)
      .setDeveloperId(3L)
      .setComponentUuid("C4")
      .setValue(5.0d)
      .setData("data")
      .setVariation(1d)
      .setAlertStatus("alert")
      .setAlertText("alert-text");
    underTest.insert(db.getSession(), inserted);
    db.commit();

    MeasureDto selected = underTest.selectSingle(db.getSession(), MeasureQuery.builder()
      .setComponentUuid(inserted.getComponentUuid())
      .setPersonId(inserted.getDeveloperId())
      .build()).get();
    assertThat(selected.getAnalysisUuid()).isEqualTo(inserted.getAnalysisUuid());
    assertThat(selected.getMetricId()).isEqualTo(inserted.getMetricId());
    assertThat(selected.getDeveloperId()).isEqualTo(inserted.getDeveloperId());
    assertThat(selected.getComponentUuid()).isEqualTo(inserted.getComponentUuid());
    assertThat(selected.getValue()).isEqualTo(inserted.getValue());
    assertThat(selected.getData()).isEqualTo(inserted.getData());
    assertThat(selected.getVariation()).isEqualTo(inserted.getVariation());
    assertThat(selected.getAlertStatus()).isEqualTo(inserted.getAlertStatus());
    assertThat(selected.getAlertText()).isEqualTo(inserted.getAlertText());
  }

  @Test
  public void selectByQuery() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project1));
    db.components().insertComponent(newFileDto(module).setUuid("C1"));
    db.components().insertComponent(newFileDto(module).setUuid("C2"));
    insertAnalysis(LAST_ANALYSIS_UUID, project1.uuid(), true);
    insertAnalysis(OTHER_ANALYSIS_UUID, project1.uuid(), false);

    String project2LastAnalysisUuid = "P2_LAST_ANALYSIS";
    ComponentDto project2 = db.components().insertPrivateProject();
    insertAnalysis(project2LastAnalysisUuid, project2.uuid(), true);

    // project 1
    insertMeasure("P1_M1", LAST_ANALYSIS_UUID, project1.uuid(), NCLOC_METRIC_ID);
    insertMeasure("P1_M2", LAST_ANALYSIS_UUID, project1.uuid(), COVERAGE_METRIC_ID);
    insertMeasure("P1_M3", OTHER_ANALYSIS_UUID, project1.uuid(), NCLOC_METRIC_ID);
    // project 2
    insertMeasure("P2_M1", project2LastAnalysisUuid, project2.uuid(), NCLOC_METRIC_ID);
    insertMeasure("P2_M2", project2LastAnalysisUuid, project2.uuid(), COVERAGE_METRIC_ID);
    // component C1
    insertMeasure("M1", OTHER_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID);
    insertMeasure("M2", LAST_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID);
    insertMeasure("M3", LAST_ANALYSIS_UUID, "C1", COVERAGE_METRIC_ID);
    insertMeasureOnPerson("M4", LAST_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID, A_PERSON_ID);
    insertMeasureOnPerson("M5", OTHER_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID, 123L);
    // component C2
    insertMeasure("M6", LAST_ANALYSIS_UUID, "C2", NCLOC_METRIC_ID);
    db.commit();

    verifyZeroMeasures(MeasureQuery.builder().setComponentUuids(project1.uuid(), emptyList()));
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("MISSING_COMPONENT"));
    verifyZeroMeasures(MeasureQuery.builder().setProjectUuids(emptyList()));
    verifyZeroMeasures(MeasureQuery.builder().setProjectUuids(singletonList("MISSING_COMPONENT")));

    // all measures of component C1 of last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1"), "M2", "M3");
    // all measures of component C1 of non last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(OTHER_ANALYSIS_UUID), "M1");
    // all measures of component C1 of last analysis by UUID
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(LAST_ANALYSIS_UUID), "M2", "M3");

    // ncloc measure of component C1 of last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setMetricId(NCLOC_METRIC_ID), "M2");
    // ncloc measure of component C1 of non last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(OTHER_ANALYSIS_UUID).setMetricId(NCLOC_METRIC_ID), "M1");
    // ncloc measure of component C1 of last analysis by UUID
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(LAST_ANALYSIS_UUID).setMetricId(NCLOC_METRIC_ID), "M2");

    // multiple measures of component C1 of last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setMetricIds(asList(NCLOC_METRIC_ID, COVERAGE_METRIC_ID)), "M2", "M3");
    // multiple measures of component C1 of non last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(OTHER_ANALYSIS_UUID).setMetricIds(asList(NCLOC_METRIC_ID, COVERAGE_METRIC_ID)), "M1");
    // multiple measures of component C1 of last analysis by UUID
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(LAST_ANALYSIS_UUID).setMetricIds(asList(NCLOC_METRIC_ID, COVERAGE_METRIC_ID)), "M2", "M3");

    // missing measure of component C1 of last analysis
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("C1").setMetricId(COMPLEXITY_METRIC_ID));
    // missing measure of component C1 of non last analysis
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(OTHER_ANALYSIS_UUID).setMetricId(COMPLEXITY_METRIC_ID));
    // missing measure of component C1 of last analysis by UUID
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(LAST_ANALYSIS_UUID).setMetricId(COMPLEXITY_METRIC_ID));

    // ncloc measures of components C1, C2 and C3 (which does not exist) of last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuids(project1.uuid(), asList("C1", "C2", "C3")), "M2", "M3", "M6");
    // ncloc measures of components C1, C2 and C3 (which does not exist) of non last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuids(project1.uuid(), asList("C1", "C2", "C3")).setAnalysisUuid(OTHER_ANALYSIS_UUID), "M1");
    // ncloc measures of components C1, C2 and C3 (which does not exist) of last analysis by UUID
    verifyMeasures(MeasureQuery.builder().setComponentUuids(project1.uuid(), asList("C1", "C2", "C3")).setAnalysisUuid(LAST_ANALYSIS_UUID), "M2", "M3", "M6");

    // measures of missing developer of component C1 of last analysis
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("C1").setPersonId(123L));
    // measures of missing developer of component C1 of non last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(OTHER_ANALYSIS_UUID).setPersonId(123L), "M5");
    // measures of missing developer of component C1 of last analysis by UUID
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(LAST_ANALYSIS_UUID).setPersonId(123L));

    // developer measures of component C1 of last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setPersonId(A_PERSON_ID), "M4");
    // developer measures of component C1 of non last analysis
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(OTHER_ANALYSIS_UUID).setPersonId(A_PERSON_ID));
    // developer measures of component C1 of last analysis by UUID
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(LAST_ANALYSIS_UUID).setPersonId(A_PERSON_ID), "M4");

    // projects measures of last analysis
    verifyMeasures(MeasureQuery.builder().setProjectUuids(singletonList(project1.uuid())).setMetricId(NCLOC_METRIC_ID), "P1_M1");
    verifyMeasures(MeasureQuery.builder().setProjectUuids(asList(project1.uuid(), project2.uuid())).setMetricIds(asList(NCLOC_METRIC_ID, COVERAGE_METRIC_ID)),
      "P1_M1", "P1_M2", "P2_M1", "P2_M2", "P2_M2");
    verifyMeasures(MeasureQuery.builder().setProjectUuids(asList(project1.uuid(), project2.uuid(), "UNKNOWN")).setMetricId(NCLOC_METRIC_ID), "P1_M1", "P2_M1");

    // projects measures of none last analysis
    verifyMeasures(MeasureQuery.builder().setProjectUuids(singletonList(project1.uuid())).setMetricId(NCLOC_METRIC_ID).setAnalysisUuid(OTHER_ANALYSIS_UUID), "P1_M3");
    verifyMeasures(MeasureQuery.builder().setProjectUuids(asList(project1.uuid(), project2.uuid())).setMetricId(NCLOC_METRIC_ID).setAnalysisUuid(OTHER_ANALYSIS_UUID), "P1_M3");
  }

  @Test
  public void selectSingle() {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertComponent(newFileDto(project).setUuid("C1"));
    insertAnalysis(LAST_ANALYSIS_UUID, project.uuid(), true);
    insertMeasure("M1", LAST_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID);
    insertMeasure("M2", LAST_ANALYSIS_UUID, "C1", COMPLEXITY_METRIC_ID);
    db.commit();

    assertThat(selectSingle(MeasureQuery.builder().setComponentUuids(project.uuid(), emptyList()))).isNotPresent();
    assertThat(selectSingle(MeasureQuery.builder().setComponentUuid("MISSING_COMPONENT"))).isNotPresent();

    // select a single measure
    assertThat(selectSingle(MeasureQuery.builder().setComponentUuid("C1").setMetricId(NCLOC_METRIC_ID))).isPresent();

    // select multiple measures -> fail
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("expected one element");
    selectSingle(MeasureQuery.builder().setComponentUuid("C1"));
  }

  @Test
  public void select_tree_by_query() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module1 = db.components().insertComponent(newModuleDto(project));
    ComponentDto module2 = db.components().insertComponent(newModuleDto(project));
    ComponentDto file1 = db.components().insertComponent(newFileDto(module1).setUuid("C1").setName("File One"));
    db.components().insertComponent(newFileDto(module2).setUuid("C2").setName("File Two").setQualifier(UNIT_TEST_FILE));
    insertAnalysis(LAST_ANALYSIS_UUID, project.uuid(), true);

    // project
    insertMeasure("PROJECT_M1", LAST_ANALYSIS_UUID, project.uuid(), NCLOC_METRIC_ID);
    // module 1
    insertMeasure("MODULE_M1", LAST_ANALYSIS_UUID, module1.uuid(), NCLOC_METRIC_ID);
    // component C1
    insertMeasure("M2", LAST_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID);
    insertMeasure("M3", LAST_ANALYSIS_UUID, "C1", COVERAGE_METRIC_ID);
    insertMeasureOnPerson("M4", LAST_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID, A_PERSON_ID);
    // component C2
    insertMeasure("M6", LAST_ANALYSIS_UUID, "C2", NCLOC_METRIC_ID);
    db.commit();

    // Children measures of project
    verifyMeasures(project, MeasureTreeQuery.builder().setStrategy(CHILDREN), "PROJECT_M1", "MODULE_M1");

    // Children measures of module 1
    verifyMeasures(module1, MeasureTreeQuery.builder().setStrategy(CHILDREN), "M2", "M3", "MODULE_M1");

    // Children measure on file => only measures from itself
    verifyMeasures(file1, MeasureTreeQuery.builder().setStrategy(CHILDREN), "M2", "M3");

    // Leaves measures of project
    verifyMeasures(project, MeasureTreeQuery.builder().setStrategy(LEAVES), "PROJECT_M1", "MODULE_M1", "M2", "M3", "M6");

    // Leaves measures of module 1
    verifyMeasures(module1, MeasureTreeQuery.builder().setStrategy(LEAVES), "MODULE_M1", "M2", "M3");

    // Leaves measures of project by metric ids
    verifyMeasures(project, MeasureTreeQuery.builder().setMetricIds(asList(NCLOC_METRIC_ID)).setStrategy(LEAVES), "PROJECT_M1", "MODULE_M1", "M2",
      "M6");

    // Leaves measure on file
    verifyMeasures(file1, MeasureTreeQuery.builder().setStrategy(LEAVES), "M2", "M3");

    // Leaves measures of project matching name
    verifyMeasures(project, MeasureTreeQuery.builder().setNameOrKeyQuery("OnE").setStrategy(LEAVES), "M2", "M3");

    // Leaves measures of project matching qualifiers
    verifyMeasures(project, MeasureTreeQuery.builder().setQualifiers(asList(FILE)).setStrategy(LEAVES), "M2", "M3");
    verifyMeasures(project, MeasureTreeQuery.builder().setQualifiers(asList(FILE, UNIT_TEST_FILE)).setStrategy(LEAVES), "M2", "M3", "M6");
  }

  @Test
  public void select_tree_by_query_use_only_latest_analysis() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file1 = db.components().insertComponent(newFileDto(project).setUuid("C1").setName("File One"));
    db.components().insertComponent(newFileDto(project).setUuid("C2").setName("File Two").setQualifier(UNIT_TEST_FILE));
    insertAnalysis(LAST_ANALYSIS_UUID, project.uuid(), true);
    insertAnalysis(OTHER_ANALYSIS_UUID, project.uuid(), false);

    // project
    insertMeasure("PROJECT_M1", LAST_ANALYSIS_UUID, project.uuid(), NCLOC_METRIC_ID);
    insertMeasure("PROJECT_M2", OTHER_ANALYSIS_UUID, project.uuid(), NCLOC_METRIC_ID);
    // component C1
    insertMeasure("M2", LAST_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID);
    insertMeasure("M3", LAST_ANALYSIS_UUID, "C1", COVERAGE_METRIC_ID);
    insertMeasure("M4", OTHER_ANALYSIS_UUID, "C1", COVERAGE_METRIC_ID);
    // component C2
    insertMeasure("M5", LAST_ANALYSIS_UUID, "C2", NCLOC_METRIC_ID);
    insertMeasure("M6", OTHER_ANALYSIS_UUID, "C2", NCLOC_METRIC_ID);
    db.commit();

    // Children measures of project
    verifyMeasures(project, MeasureTreeQuery.builder().setStrategy(CHILDREN), "PROJECT_M1", "M2", "M3", "M5");

    // Children measure on file => only measures from itself
    verifyMeasures(file1, MeasureTreeQuery.builder().setStrategy(CHILDREN), "M2", "M3");

    // Leaves measures of project
    verifyMeasures(project, MeasureTreeQuery.builder().setStrategy(LEAVES), "PROJECT_M1", "M2", "M3", "M5");

    // Leaves measure on file
    verifyMeasures(file1, MeasureTreeQuery.builder().setStrategy(LEAVES), "M2", "M3");
  }

  @Test
  public void select_past_measures_with_several_analyses() {
    ComponentDto project = db.components().insertPrivateProject();
    long lastAnalysisDate = parseDate("2017-01-25").getTime();
    long previousAnalysisDate = lastAnalysisDate - 10_000_000_000L;
    long oldAnalysisDate = lastAnalysisDate - 100_000_000_000L;
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setUuid(LAST_ANALYSIS_UUID).setCreatedAt(lastAnalysisDate));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setUuid(OTHER_ANALYSIS_UUID).setCreatedAt(previousAnalysisDate).setLast(false));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setUuid("OLD_ANALYSIS_UUID").setCreatedAt(oldAnalysisDate).setLast(false));
    db.commit();

    // project
    insertMeasure("PROJECT_M1", LAST_ANALYSIS_UUID, project.uuid(), NCLOC_METRIC_ID);
    insertMeasure("PROJECT_M2", OTHER_ANALYSIS_UUID, project.uuid(), NCLOC_METRIC_ID);
    insertMeasure("PROJECT_M3", "OLD_ANALYSIS_UUID", project.uuid(), NCLOC_METRIC_ID);
    db.commit();

    // Measures of project for last and previous analyses
    List<MeasureDto> result = underTest.selectPastMeasures(db.getSession(),
      new PastMeasureQuery(project.uuid(), singletonList(NCLOC_METRIC_ID), previousAnalysisDate, lastAnalysisDate + 1_000L));

    assertThat(result).hasSize(2).extracting(MeasureDto::getData).containsOnly("PROJECT_M1", "PROJECT_M2");
  }

  @Test
  public void selectByComponentsAndMetrics() {
    ComponentDto project1 = db.components().insertPrivateProject(db.getDefaultOrganization(), "P1");
    ComponentDto module = db.components().insertComponent(newModuleDto(project1));
    db.components().insertComponent(newFileDto(module).setUuid("C1"));
    db.components().insertComponent(newFileDto(module).setUuid("C2"));
    insertAnalysis(LAST_ANALYSIS_UUID, project1.uuid(), true);
    insertAnalysis(OTHER_ANALYSIS_UUID, project1.uuid(), false);

    String project2LastAnalysisUuid = "P2_LAST_ANALYSIS";
    ComponentDto project2 = db.components().insertPrivateProject(db.getDefaultOrganization(), "P2");
    insertAnalysis(project2LastAnalysisUuid, project2.uuid(), true);

    // project 1
    insertMeasure("P1_M1", LAST_ANALYSIS_UUID, project1.uuid(), NCLOC_METRIC_ID);
    insertMeasure("P1_M2", LAST_ANALYSIS_UUID, project1.uuid(), COVERAGE_METRIC_ID);
    insertMeasure("P1_M3", OTHER_ANALYSIS_UUID, project1.uuid(), NCLOC_METRIC_ID);
    // project 2
    insertMeasure("P2_M1", project2LastAnalysisUuid, project2.uuid(), NCLOC_METRIC_ID);
    insertMeasure("P2_M2", project2LastAnalysisUuid, project2.uuid(), COVERAGE_METRIC_ID);
    // component C1
    insertMeasure("M1", OTHER_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID);
    insertMeasure("M2", LAST_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID);
    insertMeasure("M3", LAST_ANALYSIS_UUID, "C1", COVERAGE_METRIC_ID);
    insertMeasureOnPerson("M4", LAST_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID, A_PERSON_ID);
    insertMeasureOnPerson("M5", OTHER_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID, 123L);
    // component C2
    insertMeasure("M6", LAST_ANALYSIS_UUID, "C2", NCLOC_METRIC_ID);
    db.commit();

    assertThat(underTest.selectByComponentsAndMetrics(db.getSession(), Collections.emptyList(), Collections.emptyList())).isEmpty();

    // Measures of component C1
    assertThat(underTest.selectByComponentsAndMetrics(db.getSession(), singletonList("C1"), singletonList(NCLOC_METRIC_ID))).extracting(MeasureDto::getData).containsOnly("M2");
    assertThat(underTest.selectByComponentsAndMetrics(db.getSession(), singletonList("C1"), asList(NCLOC_METRIC_ID, COVERAGE_METRIC_ID))).extracting(MeasureDto::getData)
      .containsOnly("M2", "M3");

    // ncloc measures of components C1, C2
    assertThat(underTest.selectByComponentsAndMetrics(db.getSession(), asList("C1", "C2"), asList(NCLOC_METRIC_ID, COVERAGE_METRIC_ID))).extracting(MeasureDto::getData)
      .containsOnly("M2", "M3", "M6");

    // projects measures of last analysis
    assertThat(underTest.selectByComponentsAndMetrics(db.getSession(), singletonList("P1"), singletonList(NCLOC_METRIC_ID))).extracting(MeasureDto::getData)
      .containsOnly("P1_M1");
    assertThat(underTest.selectByComponentsAndMetrics(db.getSession(), asList("P1", "P2"), asList(NCLOC_METRIC_ID, COVERAGE_METRIC_ID))).extracting(MeasureDto::getData)
      .containsOnly("P1_M1", "P1_M2", "P2_M1", "P2_M2");
  }

  private Optional<MeasureDto> selectSingle(MeasureQuery.Builder query) {
    return underTest.selectSingle(db.getSession(), query.build());
  }

  private void verifyMeasures(MeasureQuery.Builder query, String... expectedIds) {
    List<MeasureDto> measures = underTest.selectByQuery(db.getSession(), query.build());
    assertThat(measures).extracting(MeasureDto::getData).containsOnly(expectedIds);
  }

  private void verifyZeroMeasures(MeasureQuery.Builder query) {
    assertThat(underTest.selectByQuery(db.getSession(), query.build())).isEmpty();
  }

  private void verifyMeasures(ComponentDto baseComponent, MeasureTreeQuery.Builder measureQuery, String... expectedIds) {
    List<MeasureDto> measures = new ArrayList<>();
    underTest.selectTreeByQuery(db.getSession(), baseComponent, measureQuery.build(), result -> measures.add((MeasureDto) result.getResultObject()));
    assertThat(measures).extracting(MeasureDto::getData).containsOnly(expectedIds);
  }

  private void insertMeasure(String id, String analysisUuid, String componentUuid, int metricId) {
    insertMeasure(id, analysisUuid, componentUuid, null, metricId);
  }

  private void insertMeasure(String id, String analysisUuid, String componentUuid, @Nullable Long developerId, int metricId) {
    MeasureDto measure = MeasureTesting.newMeasure()
      .setAnalysisUuid(analysisUuid)
      .setComponentUuid(componentUuid)
      .setMetricId(metricId)
      // as ids can't be forced when inserting measures, the field "data"
      // is used to store a virtual id. It is used then in assertions.
      .setData(id)
      .setDeveloperId(developerId);
    db.getDbClient().measureDao().insert(db.getSession(), measure);
  }

  private String insertComponent(String scope, String qualifier, boolean enabled) {
    String uuid = UuidFactoryImpl.INSTANCE.create();
    ComponentDto componentDto = new ComponentDto()
      .setOrganizationUuid("org1")
      .setUuid(uuid)
      .setScope(scope)
      .setQualifier(qualifier)
      .setProjectUuid("don't care")
      .setRootUuid("don't care")
      .setUuidPath("don't care")
      .setKey("kee_" + uuid)
      .setEnabled(enabled);
    db.getDbClient().componentDao().insert(db.getSession(), componentDto);
    return uuid;
  }

  private void insertMeasureOnPerson(String id, String analysisUuid, String componentUuid, int metricId, long personId) {
    MeasureDto measure = MeasureTesting.newMeasure()
      .setAnalysisUuid(analysisUuid)
      .setComponentUuid(componentUuid)
      .setMetricId(metricId)
      .setDeveloperId(personId)
      // as ids can't be forced when inserting measures, the field "data"
      // is used to store a virtual id. It is used then in assertions.
      .setData(id);
    db.getDbClient().measureDao().insert(db.getSession(), measure);
  }

  private SnapshotDto insertAnalysis(String uuid, String projectUuid, boolean isLast) {
    return db.getDbClient().snapshotDao().insert(db.getSession(), SnapshotTesting.newSnapshot()
      .setUuid(uuid)
      .setComponentUuid(projectUuid)
      .setLast(isLast));
  }

  // TODO test selectPastMeasures

}
