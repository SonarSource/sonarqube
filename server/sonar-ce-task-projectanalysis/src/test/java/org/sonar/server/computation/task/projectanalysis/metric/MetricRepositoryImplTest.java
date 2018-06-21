/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.metric;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;

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
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Metric with key '%s' does not exist", "complexity"));

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.start();
    underTest.getByKey("complexity");
  }

  @Test
  public void getByKey_find_enabled_Metrics() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.start();
    assertThat(underTest.getByKey("ncloc").getId()).isEqualTo(1);
    assertThat(underTest.getByKey("coverage").getId()).isEqualTo(2);
  }

  @Test
  public void getById_throws_ISE_if_start_has_not_been_called() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Metric cache has not been initialized");

    underTest.getById(SOME_ID);
  }

  @Test
  public void getById_throws_ISE_of_Metric_does_not_exist() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Metric with id '%s' does not exist", SOME_ID));

    underTest.start();
    underTest.getById(SOME_ID);
  }

  @Test
  public void getById_throws_ISE_of_Metric_is_disabled() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Metric with id '%s' does not exist", 100));

    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.start();
    underTest.getById(100);
  }

  @Test
  public void getById_find_enabled_Metrics() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.start();
    assertThat(underTest.getById(1).getKey()).isEqualTo("ncloc");
    assertThat(underTest.getById(2).getKey()).isEqualTo("coverage");
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
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.start();

    assertThat(underTest.getOptionalById(100)).isEmpty();
  }

  @Test
  public void getOptionalById_find_enabled_Metrics() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.start();

    assertThat(underTest.getOptionalById(1).get().getKey()).isEqualTo("ncloc");
    assertThat(underTest.getOptionalById(2).get().getKey()).isEqualTo("coverage");
  }

  @Test
  public void get_all_metrics() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.start();
    assertThat(underTest.getAll()).extracting("key").containsOnly("ncloc", "coverage", "sqale_index", "development_cost");
  }

}
