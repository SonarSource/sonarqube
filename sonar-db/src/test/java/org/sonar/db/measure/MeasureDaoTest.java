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

import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.SnapshotTesting;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class MeasureDaoTest {

  private static final int COVERAGE_METRIC_ID = 10;
  private static final int COMPLEXITY_METRIC_ID = 11;
  private static final int NCLOC_METRIC_ID = 12;
  private static final long A_PERSON_ID = 444L;
  public static final String LAST_ANALYSIS_UUID = "A1";
  public static final String OTHER_ANALYSIS_UUID = "A2";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private MeasureDao underTest = db.getDbClient().measureDao();

  @Test
  public void test_inserted_and_selected_columns() {
    insertAnalysis(LAST_ANALYSIS_UUID, true);

    MeasureDto inserted = new MeasureDto()
      .setAnalysisUuid(LAST_ANALYSIS_UUID)
      .setMetricId(2)
      .setDeveloperId(3L)
      .setComponentUuid("C4")
      .setValue(5.0d)
      .setData("data")
      .setVariation(1, 1.0d)
      .setVariation(2, 2.0d)
      .setVariation(3, 3.0d)
      .setVariation(4, 4.0d)
      .setVariation(5, 5.0d)
      .setAlertStatus("alert")
      .setAlertText("alert-text")
      .setDescription("measure-description");
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
    assertThat(selected.getVariation(1)).isEqualTo(inserted.getVariation(1));
    assertThat(selected.getVariation(2)).isEqualTo(inserted.getVariation(2));
    assertThat(selected.getVariation(3)).isEqualTo(inserted.getVariation(3));
    assertThat(selected.getVariation(4)).isEqualTo(inserted.getVariation(4));
    assertThat(selected.getVariation(5)).isEqualTo(inserted.getVariation(5));
    assertThat(selected.getAlertStatus()).isEqualTo(inserted.getAlertStatus());
    assertThat(selected.getAlertText()).isEqualTo(inserted.getAlertText());
  }

  @Test
  public void selectByQuery() {
    insertAnalysis(LAST_ANALYSIS_UUID, true);
    insertAnalysis(OTHER_ANALYSIS_UUID, false);
    // component C1
    insertMeasure("M1", OTHER_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID);
    insertMeasure("M2", LAST_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID);
    insertMeasure("M3", LAST_ANALYSIS_UUID, "C1", COVERAGE_METRIC_ID);
    insertMeasureOnPerson("M4", LAST_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID, A_PERSON_ID);
    insertMeasureOnPerson("M5", OTHER_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID, 123L);
    // component C2
    insertMeasure("M6", LAST_ANALYSIS_UUID, "C2", NCLOC_METRIC_ID);
    db.commit();

    verifyZeroMeasures(MeasureQuery.builder().setComponentUuids(emptyList()));
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("MISSING_COMPONENT"));

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
    verifyMeasures(MeasureQuery.builder().setComponentUuids(asList("C1", "C2", "C3")), "M2", "M3", "M6");
    // ncloc measures of components C1, C2 and C3 (which does not exist) of non last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuids(asList("C1", "C2", "C3")).setAnalysisUuid(OTHER_ANALYSIS_UUID), "M1");
    // ncloc measures of components C1, C2 and C3 (which does not exist) of last analysis by UUID
    verifyMeasures(MeasureQuery.builder().setComponentUuids(asList("C1", "C2", "C3")).setAnalysisUuid(LAST_ANALYSIS_UUID), "M2", "M3", "M6");

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
  }

  @Test
  public void selectSingle() {
    insertAnalysis(LAST_ANALYSIS_UUID, true);
    insertMeasure("M1", LAST_ANALYSIS_UUID, "C1", NCLOC_METRIC_ID);
    insertMeasure("M2", LAST_ANALYSIS_UUID, "C1", COMPLEXITY_METRIC_ID);
    db.commit();

    assertThat(selectSingle(MeasureQuery.builder().setComponentUuids(emptyList()))).isNotPresent();
    assertThat(selectSingle(MeasureQuery.builder().setComponentUuid("MISSING_COMPONENT"))).isNotPresent();

    // select a single measure
    assertThat(selectSingle(MeasureQuery.builder().setComponentUuid("C1").setMetricId(NCLOC_METRIC_ID))).isPresent();

    // select multiple measures -> fail
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("expected one element");
    selectSingle(MeasureQuery.builder().setComponentUuid("C1"));
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

  private void insertMeasure(String id, String analysisUuid, String componentUuid, int metricId) {
    MeasureDto measure = MeasureTesting.newMeasure()
      .setAnalysisUuid(analysisUuid)
      .setComponentUuid(componentUuid)
      .setMetricId(metricId)
      // as ids can't be forced when inserting measures, the field "data"
      // is used to store a virtual id. It is used then in assertions.
      .setData(id);
    db.getDbClient().measureDao().insert(db.getSession(), measure);
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

  private void insertAnalysis(String uuid, boolean isLast) {
    db.getDbClient().snapshotDao().insert(db.getSession(), SnapshotTesting.newSnapshot()
      .setUuid(uuid)
      .setLast(isLast));
  }

  // TODO test selectPastMeasures

}
