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

import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;

public class LiveMeasureDaoTest {

  private static final int A_METRIC_ID = 42;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private LiveMeasureDao underTest = db.getDbClient().liveMeasureDao();

  @Test
  public void test_selectByComponentUuids() {
    LiveMeasureDto measure1 = newLiveMeasure().setMetricId(A_METRIC_ID);
    LiveMeasureDto measure2 = newLiveMeasure().setMetricId(A_METRIC_ID);
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuids(db.getSession(), asList(measure1.getComponentUuid(), measure2.getComponentUuid()), singletonList(A_METRIC_ID));
    assertThat(selected)
      .extracting(LiveMeasureDto::getComponentUuid, LiveMeasureDto::getProjectUuid, LiveMeasureDto::getMetricId, LiveMeasureDto::getValue, LiveMeasureDto::getDataAsString)
      .containsExactlyInAnyOrder(
        Tuple.tuple(measure1.getComponentUuid(), measure1.getProjectUuid(), measure1.getMetricId(), measure1.getValue(), measure1.getDataAsString()),
        Tuple.tuple(measure2.getComponentUuid(), measure2.getProjectUuid(), measure2.getMetricId(), measure2.getValue(), measure2.getDataAsString()));
  }

  @Test
  public void selectByComponentUuids_returns_empty_list_if_metric_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricId(10);
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuids(db.getSession(), singletonList(measure.getComponentUuid()), singletonList(222));

    assertThat(selected).isEmpty();
  }

  @Test
  public void selectByComponentUuids_returns_empty_list_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure();
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuids(db.getSession(), singletonList("_missing_"), singletonList(measure.getMetricId()));

    assertThat(selected).isEmpty();
  }

  @Test
  public void test_selectMeasure() {
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
  public void test_insertOrUpdate() {
    // insert
    LiveMeasureDto dto = newLiveMeasure();
    underTest.insertOrUpdate(db.getSession(), dto, "foo");
    verifyPersisted(dto);
    verifyTableSize(1);

    // update
    dto.setValue(dto.getValue() + 1);
    dto.setVariation(dto.getVariation() + 10);
    dto.setData(dto.getDataAsString() + "_new");
    underTest.insertOrUpdate(db.getSession(), dto, "foo");
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void deleteByProjectUuidExcludingMarker() {
    LiveMeasureDto measure1 = newLiveMeasure().setProjectUuid("P1");
    LiveMeasureDto measure2 = newLiveMeasure().setProjectUuid("P1");
    LiveMeasureDto measure3DifferentMarker = newLiveMeasure().setProjectUuid("P1");
    LiveMeasureDto measure4NoMarker = newLiveMeasure().setProjectUuid("P1");
    LiveMeasureDto measure5OtherProject = newLiveMeasure().setProjectUuid("P2");
    underTest.insertOrUpdate(db.getSession(), measure1, "foo");
    underTest.insertOrUpdate(db.getSession(), measure2, "foo");
    underTest.insertOrUpdate(db.getSession(), measure3DifferentMarker, "bar");
    underTest.insertOrUpdate(db.getSession(), measure4NoMarker, null);
    underTest.insertOrUpdate(db.getSession(), measure5OtherProject, "foo");

    underTest.deleteByProjectUuidExcludingMarker(db.getSession(), "P1", "foo");

    verifyTableSize(3);
    verifyPersisted(measure1);
    verifyPersisted(measure2);
    verifyPersisted(measure5OtherProject);
  }

  private void verifyTableSize(int expectedSize) {
    assertThat(db.countRowsOfTable(db.getSession(), "live_measures")).isEqualTo(expectedSize);
  }

  private void verifyPersisted(LiveMeasureDto dto) {
    List<LiveMeasureDto> selected = underTest.selectByComponentUuids(db.getSession(), singletonList(dto.getComponentUuid()), singletonList(dto.getMetricId()));
    assertThat(selected).hasSize(1);
    assertThat(selected.get(0)).isEqualToComparingFieldByField(dto);
  }
}
