/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.metric;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MetricRepositoryImplIT {
  private static final String SOME_KEY = "some_key";
  private static final String SOME_UUID = "uuid";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();
  private MetricRepositoryImpl underTest = new MetricRepositoryImpl(dbClient);

  @Test
  public void getByKey_throws_NPE_if_arg_is_null() {
    assertThatThrownBy(() -> underTest.getByKey(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void getByKey_throws_ISE_if_start_has_not_been_called() {
    assertThatThrownBy(() -> underTest.getByKey(SOME_KEY))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Metric cache has not been initialized");
  }

  @Test
  public void getByKey_throws_ISE_of_Metric_does_not_exist() {
    assertThatThrownBy(() -> {
      underTest.start();
      underTest.getByKey(SOME_KEY);
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(String.format("Metric with key '%s' does not exist", SOME_KEY));
  }

  @Test
  public void getByKey_throws_ISE_of_Metric_is_disabled() {
    dbTester.measures().insertMetric(t -> t.setKey("complexity").setEnabled(false));

    underTest.start();

    assertThatThrownBy(() -> underTest.getByKey("complexity"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(String.format("Metric with key '%s' does not exist", "complexity"));
  }

  @Test
  public void getByKey_find_enabled_Metrics() {
    MetricDto ncloc = dbTester.measures().insertMetric(t -> t.setKey("ncloc").setEnabled(true));
    MetricDto coverage = dbTester.measures().insertMetric(t -> t.setKey("coverage").setEnabled(true));

    underTest.start();

    assertThat(underTest.getByKey("ncloc").getUuid()).isEqualTo(ncloc.getUuid());
    assertThat(underTest.getByKey("coverage").getUuid()).isEqualTo(coverage.getUuid());
  }

  @Test
  public void getById_throws_ISE_if_start_has_not_been_called() {
    assertThatThrownBy(() -> underTest.getByUuid(SOME_UUID))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Metric cache has not been initialized");
  }

  @Test
  public void getById_throws_ISE_of_Metric_does_not_exist() {
    underTest.start();

    assertThatThrownBy(() -> underTest.getByUuid(SOME_UUID))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(String.format("Metric with uuid '%s' does not exist", SOME_UUID));
  }

  @Test
  public void getById_throws_ISE_of_Metric_is_disabled() {
    dbTester.measures().insertMetric(t -> t.setKey("complexity").setEnabled(false));

    underTest.start();

    assertThatThrownBy(() -> underTest.getByUuid(SOME_UUID))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(String.format("Metric with uuid '%s' does not exist", SOME_UUID));
  }

  @Test
  public void getById_find_enabled_Metrics() {
    MetricDto ncloc = dbTester.measures().insertMetric(t -> t.setKey("ncloc").setEnabled(true));
    MetricDto coverage = dbTester.measures().insertMetric(t -> t.setKey("coverage").setEnabled(true));

    underTest.start();

    assertThat(underTest.getByUuid(ncloc.getUuid()).getKey()).isEqualTo("ncloc");
    assertThat(underTest.getByUuid(coverage.getUuid()).getKey()).isEqualTo("coverage");
  }

  @Test
  public void getOptionalById_throws_ISE_if_start_has_not_been_called() {
    assertThatThrownBy(() -> underTest.getOptionalByUuid(SOME_UUID))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Metric cache has not been initialized");
  }

  @Test
  public void getOptionalById_returns_empty_of_Metric_does_not_exist() {
    underTest.start();

    assertThat(underTest.getOptionalByUuid(SOME_UUID)).isEmpty();
  }

  @Test
  public void getOptionalById_returns_empty_of_Metric_is_disabled() {
    dbTester.measures().insertMetric(t -> t.setKey("complexity").setEnabled(false));

    underTest.start();

    assertThat(underTest.getOptionalByUuid(SOME_UUID)).isEmpty();
  }

  @Test
  public void getOptionalById_find_enabled_Metrics() {
    MetricDto ncloc = dbTester.measures().insertMetric(t -> t.setKey("ncloc").setEnabled(true));
    MetricDto coverage = dbTester.measures().insertMetric(t -> t.setKey("coverage").setEnabled(true));

    underTest.start();

    assertThat(underTest.getOptionalByUuid(ncloc.getUuid()).get().getKey()).isEqualTo("ncloc");
    assertThat(underTest.getOptionalByUuid(coverage.getUuid()).get().getKey()).isEqualTo("coverage");
  }

  @Test
  public void get_all_metrics() {
    List<MetricDto> enabledMetrics = IntStream.range(0, 1 + new Random().nextInt(12))
      .mapToObj(i -> dbTester.measures().insertMetric(t -> t.setKey("key_enabled_" + i).setEnabled(true)))
      .toList();
    IntStream.range(0, 1 + new Random().nextInt(12))
      .forEach(i -> dbTester.measures().insertMetric(t -> t.setKey("key_disabled_" + i).setEnabled(false)));

    underTest.start();
    assertThat(underTest.getAll())
      .extracting(Metric::getKey)
      .containsOnly(enabledMetrics.stream().map(MetricDto::getKey).toArray(String[]::new));
  }

  @Test
  public void getMetricsByType_givenRatingType_returnRatingMetrics() {
    List<MetricDto> enabledMetrics = IntStream.range(0, 1 + new Random().nextInt(12))
      .mapToObj(i -> dbTester.measures().insertMetric(t -> t.setKey("key_enabled_" + i).setEnabled(true).setValueType("RATING")))
      .toList();

    underTest.start();
    assertThat(underTest.getMetricsByType(Metric.MetricType.RATING))
      .extracting(Metric::getKey)
      .containsOnly(enabledMetrics.stream().map(MetricDto::getKey).toArray(String[]::new));
  }

  @Test
  public void getMetricsByType_givenRatingTypeAndWantedMilisecType_returnEmptyList() {
    IntStream.range(0, 1 + new Random().nextInt(12))
      .mapToObj(i -> dbTester.measures().insertMetric(t -> t.setKey("key_enabled_" + i).setEnabled(true).setValueType("RATING")))
      .toList();

    underTest.start();
    assertThat(underTest.getMetricsByType(Metric.MetricType.MILLISEC)).isEmpty();
  }

  @Test
  public void getMetricsByType_givenOnlyMilisecTypeAndWantedRatingMetrics_returnEmptyList() {
    IntStream.range(0, 1 + new Random().nextInt(12))
      .mapToObj(i -> dbTester.measures().insertMetric(t -> t.setKey("key_enabled_" + i).setEnabled(true).setValueType("MILISEC")));

    underTest.start();
    assertThat(underTest.getMetricsByType(Metric.MetricType.RATING)).isEmpty();
  }

  @Test
  public void getMetricsByType_givenMetricsAreNull_throwException() {
    assertThatThrownBy(() -> underTest.getMetricsByType(Metric.MetricType.RATING))
      .isInstanceOf(IllegalStateException.class);
  }

}
