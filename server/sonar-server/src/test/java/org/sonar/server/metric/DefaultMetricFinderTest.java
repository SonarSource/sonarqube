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
package org.sonar.server.metric;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.TestDBSessions;
import org.sonar.db.metric.MetricDao;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;


public class DefaultMetricFinderTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DefaultMetricFinder finder;

  @Before
  public void setUp() {
    dbTester.prepareDbUnit(DefaultMetricFinderTest.class, "shared.xml");
    finder = new DefaultMetricFinder(new DbClient(dbTester.database(), dbTester.myBatis(), new TestDBSessions(dbTester.myBatis()), new MetricDao()));
  }

  @Test
  public void shouldFindAll() {
    assertThat(finder.findAll().size(), is(2));
  }

  @Test
  public void shouldFindByKeys() {
    assertThat(finder.findAll(Arrays.asList("ncloc", "foo", "coverage")).size(), is(2));
  }

  @Test
  public void shouldFindById() {
    assertThat(finder.findById(1).getKey(), is("ncloc"));
    assertThat(finder.findById(3), nullValue());
  }

  @Test
  public void shouldFindByKey() {
    assertThat(finder.findByKey("ncloc").getKey(), is("ncloc"));
    assertThat(finder.findByKey("disabled"), nullValue());
  }
}
