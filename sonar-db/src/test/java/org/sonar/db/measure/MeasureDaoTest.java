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
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

@Category(DbTests.class)
public class MeasureDaoTest {

  private static final int SNAPSHOT_ID = 5;
  private static final long DEVELOPER_ID = 333L;
  private static final int AUTHORS_BY_LINE_METRIC_ID = 10;
  private static final int COVERAGE_LINE_HITS_DATA_METRIC_ID = 11;
  private static final int NCLOC_METRIC_ID = 12;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  MeasureDao underTest = db.getDbClient().measureDao();

  @Test
  public void get_value_by_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    MeasureDto result = underTest.selectByComponentKeyAndMetricKey(db.getSession(), "org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc");
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

    MeasureDto result = underTest.selectByComponentKeyAndMetricKey(db.getSession(), "org.struts:struts-core:src/org/struts/RequestContext.java", "authors_by_line");
    assertThat(result.getId()).isEqualTo(20);
    assertThat(result.getData()).isEqualTo("0123456789012345678901234567890123456789");
  }

  @Test
  public void get_text_value_by_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    MeasureDto result = underTest.selectByComponentKeyAndMetricKey(db.getSession(), "org.struts:struts-core:src/org/struts/RequestContext.java", "coverage_line_hits_data");
    assertThat(result.getId()).isEqualTo(21);
    assertThat(result.getData()).isEqualTo("36=1;37=1;38=1;39=1;43=1;48=1;53=1");
  }

  @Test
  public void select_by_component_key_and_metrics() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<MeasureDto> results = underTest.selectByComponentKeyAndMetricKeys(db.getSession(), "org.struts:struts-core:src/org/struts/RequestContext.java",
        newArrayList("ncloc", "authors_by_line"));
    assertThat(results).hasSize(2);

    results = underTest.selectByComponentKeyAndMetricKeys(db.getSession(), "org.struts:struts-core:src/org/struts/RequestContext.java", newArrayList("ncloc"));
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

    List<MeasureDto> results = underTest.selectBySnapshotIdAndMetricKeys(SNAPSHOT_ID, ImmutableSet.of("ncloc", "authors_by_line"), db.getSession());
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

    MeasureDto result = underTest.selectByComponentKeyAndMetricKey(db.getSession(), "org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc");
    assertThat(result.getId()).isEqualTo(22);
    assertThat(result.getValue()).isEqualTo(10d);
    assertThat(result.getMetricKey()).isEqualTo("ncloc");
    assertThat(result.getVariation(1)).isEqualTo(1d);
    assertThat(result.getVariation(2)).isEqualTo(2d);
    assertThat(result.getVariation(3)).isEqualTo(3d);
    assertThat(result.getVariation(4)).isEqualTo(4d);
    assertThat(result.getVariation(5)).isEqualTo(-5d);

    assertThat(underTest.selectByComponentKeyAndMetricKey(db.getSession(), "org.struts:struts-core:src/org/struts/RequestContext.java", "unknown")).isNull();
  }

  @Test
  public void exists_by_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.existsByKey(db.getSession(), "org.struts:struts-core:src/org/struts/RequestContext.java", "ncloc")).isTrue();
    assertThat(underTest.existsByKey(db.getSession(), "org.struts:struts-core:src/org/struts/RequestContext.java", "unknown")).isFalse();
  }

  @Test
  public void select_past_measures_by_component_uuid_and_root_snapshot_id_and_metric_keys() {
    db.prepareDbUnit(getClass(), "past_measures.xml");

    List<PastMeasureDto> fileMeasures = underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(db.getSession(), "CDEF", 1000L, ImmutableSet.of(1, 2));
    assertThat(fileMeasures).hasSize(2);

    PastMeasureDto fileMeasure1 = fileMeasures.get(0);
    assertThat(fileMeasure1.getValue()).isEqualTo(5d);
    assertThat(fileMeasure1.getMetricId()).isEqualTo(1);
    assertThat(fileMeasure1.getRuleId()).isNull();
    assertThat(fileMeasure1.getCharacteristicId()).isNull();
    assertThat(fileMeasure1.getPersonId()).isNull();

    PastMeasureDto fileMeasure2 = fileMeasures.get(1);
    assertThat(fileMeasure2.getValue()).isEqualTo(60d);
    assertThat(fileMeasure2.getMetricId()).isEqualTo(2);

    List<PastMeasureDto> projectMeasures = underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(db.getSession(), "ABCD", 1000L, ImmutableSet.of(1, 2));
    assertThat(projectMeasures).hasSize(2);

    PastMeasureDto projectMeasure1 = projectMeasures.get(0);
    assertThat(projectMeasure1.getValue()).isEqualTo(60d);
    assertThat(projectMeasure1.getMetricId()).isEqualTo(1);

    PastMeasureDto projectMeasure2 = projectMeasures.get(1);
    assertThat(projectMeasure2.getValue()).isEqualTo(80d);
    assertThat(projectMeasure2.getMetricId()).isEqualTo(2);

    assertThat(underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(db.getSession(), "UNKNOWN", 1000L, ImmutableSet.of(1, 2))).isEmpty();
    assertThat(underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(db.getSession(), "CDEF", 987654L, ImmutableSet.of(1, 2))).isEmpty();
    assertThat(underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(db.getSession(), "CDEF", 1000L, ImmutableSet.of(123, 456))).isEmpty();
  }

  @Test
  public void select_past_measures_on_rule_by_component_uuid_and_root_snapshot_id_and_metric_keys() {
    db.prepareDbUnit(getClass(), "past_measures_with_rule_id.xml");
    db.getSession().commit();

    List<PastMeasureDto> measures = underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(db.getSession(), "ABCD", 1000L, ImmutableSet.of(1));
    assertThat(measures).hasSize(3);

    Map<Long, PastMeasureDto> pastMeasuresById = pastMeasuresById(measures);

    PastMeasureDto measure1 = pastMeasuresById.get(1L);
    assertThat(measure1.getValue()).isEqualTo(60d);
    assertThat(measure1.getMetricId()).isEqualTo(1);
    assertThat(measure1.getRuleId()).isNull();
    assertThat(measure1.getCharacteristicId()).isNull();
    assertThat(measure1.getPersonId()).isNull();

    PastMeasureDto measure2 = pastMeasuresById.get(2L);
    assertThat(measure2.getValue()).isEqualTo(20d);
    assertThat(measure2.getMetricId()).isEqualTo(1);
    assertThat(measure2.getRuleId()).isEqualTo(30);
    assertThat(measure2.getCharacteristicId()).isNull();
    assertThat(measure2.getPersonId()).isNull();

    PastMeasureDto measure3 = pastMeasuresById.get(3L);
    assertThat(measure3.getValue()).isEqualTo(40d);
    assertThat(measure3.getMetricId()).isEqualTo(1);
    assertThat(measure3.getRuleId()).isEqualTo(31);
    assertThat(measure3.getCharacteristicId()).isNull();
    assertThat(measure3.getPersonId()).isNull();
  }

  @Test
  public void select_past_measures_on_characteristic_by_component_uuid_and_root_snapshot_id_and_metric_keys() {
    db.prepareDbUnit(getClass(), "past_measures_with_characteristic_id.xml");

    List<PastMeasureDto> measures = underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(db.getSession(), "ABCD", 1000L, ImmutableSet.of(1));
    assertThat(measures).hasSize(3);

    Map<Long, PastMeasureDto> pastMeasuresById = pastMeasuresById(measures);

    PastMeasureDto measure1 = pastMeasuresById.get(1L);
    assertThat(measure1.getValue()).isEqualTo(60d);
    assertThat(measure1.getMetricId()).isEqualTo(1);
    assertThat(measure1.getRuleId()).isNull();
    assertThat(measure1.getCharacteristicId()).isNull();
    assertThat(measure1.getPersonId()).isNull();

    PastMeasureDto measure2 = pastMeasuresById.get(2L);
    assertThat(measure2.getValue()).isEqualTo(20d);
    assertThat(measure2.getMetricId()).isEqualTo(1);
    assertThat(measure2.getRuleId()).isNull();
    assertThat(measure2.getCharacteristicId()).isEqualTo(10);
    assertThat(measure2.getPersonId()).isNull();

    PastMeasureDto measure3 = pastMeasuresById.get(3L);
    assertThat(measure3.getValue()).isEqualTo(40d);
    assertThat(measure3.getMetricId()).isEqualTo(1);
    assertThat(measure3.getRuleId()).isNull();
    assertThat(measure3.getCharacteristicId()).isEqualTo(11);
    assertThat(measure3.getPersonId()).isNull();
  }

  @Test
  public void select_past_measures_ignore_measures_with_person_id() {
    db.prepareDbUnit(getClass(), "past_measures_with_person_id.xml");

    List<PastMeasureDto> measures = underTest.selectByComponentUuidAndProjectSnapshotIdAndMetricIds(db.getSession(), "ABCD", 1000L, ImmutableSet.of(1));
    assertThat(measures).hasSize(1);

    Map<Long, PastMeasureDto> pastMeasuresById = pastMeasuresById(measures);

    PastMeasureDto measure1 = pastMeasuresById.get(1L);
    assertThat(measure1.getPersonId()).isNull();
  }

  @Test
  public void select_by_snapshot_and_metric_keys() throws Exception {
    db.prepareDbUnit(getClass(), "select_by_snapshot_and_metric_keys.xml");

    List<MeasureDto> results = underTest.selectBySnapshotIdAndMetricKeys(SNAPSHOT_ID, newHashSet("ncloc", "authors_by_line"), db.getSession());
    assertThat(results).hasSize(2);

    results = underTest.selectBySnapshotIdAndMetricKeys(SNAPSHOT_ID, newHashSet("ncloc"), db.getSession());
    assertThat(results).hasSize(1);

    MeasureDto result = results.get(0);
    assertThat(result.getId()).isEqualTo(22);
    assertThat(result.getValue()).isEqualTo(10d);
    assertThat(result.getVariation(1)).isEqualTo(1d);
    assertThat(result.getVariation(2)).isEqualTo(2d);
    assertThat(result.getVariation(3)).isEqualTo(3d);
    assertThat(result.getVariation(4)).isEqualTo(4d);
    assertThat(result.getVariation(5)).isEqualTo(-5d);

    assertThat(underTest.selectBySnapshotIdAndMetricKeys(123, newHashSet("ncloc"), db.getSession())).isEmpty();
    assertThat(underTest.selectBySnapshotIdAndMetricKeys(SNAPSHOT_ID, Collections.<String>emptySet(), db.getSession())).isEmpty();
  }

  @Test
  public void select_by_snapshot_and_metric_keys_return_measures_with_rule_id() throws Exception {
    db.prepareDbUnit(getClass(), "select_by_snapshot_and_metric_keys_with_rule_id.xml");

    List<MeasureDto> results = underTest.selectBySnapshotIdAndMetricKeys(SNAPSHOT_ID, newHashSet("ncloc"), db.getSession());
    assertThat(results).hasSize(3);

    Map<Long, MeasureDto> measuresById = measuresById(results);
    assertThat(measuresById.get(1L).getRuleId()).isNull();
    assertThat(measuresById.get(2L).getRuleId()).isEqualTo(30);
    assertThat(measuresById.get(3L).getRuleId()).isEqualTo(31);
  }

  @Test
  public void select_by_snapshot_and_metric_keys_return_measures_with_characteristic_id() throws Exception {
    db.prepareDbUnit(getClass(), "select_by_snapshot_and_metric_keys_with_characteristic_id.xml");

    List<MeasureDto> results = underTest.selectBySnapshotIdAndMetricKeys(SNAPSHOT_ID, newHashSet("ncloc"), db.getSession());
    assertThat(results).hasSize(3);

    Map<Long, MeasureDto> measuresById = measuresById(results);
    assertThat(measuresById.get(1L).getCharacteristicId()).isNull();
    assertThat(measuresById.get(2L).getCharacteristicId()).isEqualTo(10);
    assertThat(measuresById.get(3L).getCharacteristicId()).isEqualTo(11);
  }

  @Test
  public void selectByDeveloperForSnapshotAndMetrics_when_there_is_no_measure_for_developer_returns_empty() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<MeasureDto> measureDtos = underTest.selectByDeveloperForSnapshotAndMetrics(db.getSession(),
        DEVELOPER_ID, SNAPSHOT_ID,
        ImmutableList.of(AUTHORS_BY_LINE_METRIC_ID, COVERAGE_LINE_HITS_DATA_METRIC_ID, NCLOC_METRIC_ID));

    assertThat(measureDtos).isEmpty();
  }

  @Test
  public void selectByDeveloperForSnapshotAndMetrics_returns_only_measures_for_developer() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectByDeveloperForSnapshotAndMetrics(db.getSession(),
        DEVELOPER_ID, SNAPSHOT_ID,
        ImmutableList.of(AUTHORS_BY_LINE_METRIC_ID, COVERAGE_LINE_HITS_DATA_METRIC_ID, NCLOC_METRIC_ID));

    assertThat(measureDtos).extracting("id").containsOnly(30L, 31L, 32L);
  }

  @Test
  public void selectByDeveloperForSnapshotAndMetrics_returns_only_measures_for_developer_and_specified_metric_id() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectByDeveloperForSnapshotAndMetrics(db.getSession(),
        DEVELOPER_ID, SNAPSHOT_ID,
        ImmutableList.of(NCLOC_METRIC_ID));

    assertThat(measureDtos).extracting("id").containsOnly(32L);
  }

  @Test
  public void selectBySnapshotAndMetrics_returns_empty_when_single_metric_id_does_not_exist() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectBySnapshotAndMetrics(db.getSession(),
        SNAPSHOT_ID,
        ImmutableList.of(666));

    assertThat(measureDtos).isEmpty();
  }

  @Test
  public void selectBySnapshotAndMetrics_returns_only_measures_not_for_developer() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectBySnapshotAndMetrics(db.getSession(),
        SNAPSHOT_ID,
        ImmutableList.of(AUTHORS_BY_LINE_METRIC_ID, COVERAGE_LINE_HITS_DATA_METRIC_ID, NCLOC_METRIC_ID));

    assertThat(measureDtos).extracting("id").containsOnly(20L, 21L, 22L);
  }

  @Test
  public void selectBySnapshotAndMetrics_returns_only_measures_not_for_developer_and_with_specified_metric_id() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectBySnapshotAndMetrics(db.getSession(),
        SNAPSHOT_ID,
        ImmutableList.of(NCLOC_METRIC_ID));

    assertThat(measureDtos).extracting("id").containsOnly(22L);
  }

  @Test
  public void selectByDeveloperForSnapshotAndMetrics_returns_empty_when_single_metric_id_does_not_exist() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectByDeveloperForSnapshotAndMetrics(db.getSession(),
        DEVELOPER_ID, SNAPSHOT_ID,
        ImmutableList.of(666));

    assertThat(measureDtos).isEmpty();
  }

  @Test
  public void selectByDeveloperForSnapshotAndMetrics_returns_empty_when_snapshotId_does_not_exist() {
    db.prepareDbUnit(getClass(), "with_some_measures_for_developer.xml");

    List<MeasureDto> measureDtos = underTest.selectByDeveloperForSnapshotAndMetrics(db.getSession(),
        DEVELOPER_ID, 10,
        ImmutableList.of(AUTHORS_BY_LINE_METRIC_ID, COVERAGE_LINE_HITS_DATA_METRIC_ID, NCLOC_METRIC_ID));

    assertThat(measureDtos).isEmpty();
  }

  @Test
  public void insert() {
    db.prepareDbUnit(getClass(), "empty.xml");

    underTest.insert(db.getSession(), new MeasureDto()
        .setSnapshotId(2L)
        .setMetricId(3)
        .setCharacteristicId(4)
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
        .setDescription("measure-description")
    );
    db.getSession().commit();

    db.assertDbUnit(getClass(), "insert-result.xml", new String[]{"id"}, "project_measures");
  }

  @Test
  public void insert_measures() {
    db.prepareDbUnit(getClass(), "empty.xml");

    underTest.insert(db.getSession(), new MeasureDto()
        .setSnapshotId(2L)
        .setMetricId(3)
        .setComponentId(6L)
        .setValue(2.0d),
      new MeasureDto()
        .setSnapshotId(3L)
        .setMetricId(4)
        .setComponentId(6L)
        .setValue(4.0d)
    );
    db.getSession().commit();

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
