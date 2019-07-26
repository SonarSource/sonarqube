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
package org.sonar.ce.task.projectanalysis.metric;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricRepositoryImplTest {
  private static final String SOME_KEY = "some_key";
  private static final long SOME_ID = 156;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private MetricRepositoryImpl underTest = new MetricRepositoryImpl(dbClient);

  @Test(expected = NullPointerException.class)
  public void getByKey_throws_NPE_if_arg_is_null() {
    underTest.getByKey(null);
  }

  @Test
  public void getByKey_throws_ISE_if_start_has_not_been_called() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Metric cache has not been initialized");

    underTest.getByKey(SOME_KEY);
  }

  @Test
  public void getByKey_throws_ISE_of_Metric_does_not_exist() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Metric with key '%s' does not exist", SOME_KEY));

    underTest.start();

    underTest.getByKey(SOME_KEY);
  }

  @Test
  public void getByKey_throws_ISE_of_Metric_is_disabled() {
    dbTester.measures().insertMetric(t -> t.setKey("complexity").setEnabled(false));

    underTest.start();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Metric with key '%s' does not exist", "complexity"));

    underTest.getByKey("complexity");
  }

  @Test
  public void getByKey_find_enabled_Metrics() {
    MetricDto ncloc = dbTester.measures().insertMetric(t -> t.setKey("ncloc").setEnabled(true));
    MetricDto coverage = dbTester.measures().insertMetric(t -> t.setKey("coverage").setEnabled(true));

    underTest.start();

    assertThat(underTest.getByKey("ncloc").getId()).isEqualTo(ncloc.getId());
    assertThat(underTest.getByKey("coverage").getId()).isEqualTo(coverage.getId());
  }

  @Test
  public void getById_throws_ISE_if_start_has_not_been_called() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Metric cache has not been initialized");

    underTest.getById(SOME_ID);
  }

  @Test
  public void getById_throws_ISE_of_Metric_does_not_exist() {
    underTest.start();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Metric with id '%s' does not exist", SOME_ID));

    underTest.getById(SOME_ID);
  }

  @Test
  public void getById_throws_ISE_of_Metric_is_disabled() {
    dbTester.measures().insertMetric(t -> t.setKey("complexity").setEnabled(false));

    underTest.start();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Metric with id '%s' does not exist", SOME_ID));

    underTest.getById(SOME_ID);
  }

  @Test
  public void getById_find_enabled_Metrics() {
    MetricDto ncloc = dbTester.measures().insertMetric(t -> t.setKey("ncloc").setEnabled(true));
    MetricDto coverage = dbTester.measures().insertMetric(t -> t.setKey("coverage").setEnabled(true));

    underTest.start();

    assertThat(underTest.getById(ncloc.getId()).getKey()).isEqualTo("ncloc");
    assertThat(underTest.getById(coverage.getId()).getKey()).isEqualTo("coverage");
  }

  @Test
  public void getOptionalById_throws_ISE_if_start_has_not_been_called() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Metric cache has not been initialized");

    underTest.getOptionalById(SOME_ID);
  }

  @Test
  public void getOptionalById_returns_empty_of_Metric_does_not_exist() {
    underTest.start();

    assertThat(underTest.getOptionalById(SOME_ID)).isEmpty();
  }

  @Test
  public void getOptionalById_returns_empty_of_Metric_is_disabled() {
    dbTester.measures().insertMetric(t -> t.setKey("complexity").setEnabled(false));

    underTest.start();

    assertThat(underTest.getOptionalById(SOME_ID)).isEmpty();
  }

  @Test
  public void getOptionalById_find_enabled_Metrics() {
    MetricDto ncloc = dbTester.measures().insertMetric(t -> t.setKey("ncloc").setEnabled(true));
    MetricDto coverage = dbTester.measures().insertMetric(t -> t.setKey("coverage").setEnabled(true));

    underTest.start();

    assertThat(underTest.getOptionalById(ncloc.getId()).get().getKey()).isEqualTo("ncloc");
    assertThat(underTest.getOptionalById(coverage.getId()).get().getKey()).isEqualTo("coverage");
  }

  @Test
  public void get_all_metrics() {
    List<MetricDto> enabledMetrics = IntStream.range(0, 1 + new Random().nextInt(12))
      .mapToObj(i -> dbTester.measures().insertMetric(t -> t.setKey("key_enabled_" + i).setEnabled(true)))
      .collect(Collectors.toList());
    IntStream.range(0, 1 + new Random().nextInt(12))
      .forEach(i -> dbTester.measures().insertMetric(t -> t.setKey("key_disabled_" + i).setEnabled(false)));

    underTest.start();
    assertThat(underTest.getAll())
      .extracting(Metric::getKey)
      .containsOnly(enabledMetrics.stream().map(MetricDto::getKey).toArray(String[]::new));
  }

}
