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
package org.sonar.server.computation.metric;

import javax.annotation.CheckForNull;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.metric.persistence.MetricDao;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class MetricRepositoryImplTest {
  private static final String SOME_KEY = "some key";
  private static final String SOME_NAME = "the short name";

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  private DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new MetricDao());
  private MetricRepository underTest = new MetricRepositoryImpl(dbClient);

  @CheckForNull
  private DbSession dbSession;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
  }

  @After
  public void tearDown() throws Exception {
    if (dbSession != null) {
      dbSession.close();
    }
  }

  @Test(expected = NullPointerException.class)
  public void findByKey_throws_NPE_if_arg_is_null() {
    underTest.getByKey(null);
  }

  @Test(expected = IllegalStateException.class)
  public void findByKey_throws_ISE_of_Metric_does_not_exist() {
    underTest.getByKey(SOME_KEY);
  }

  @Test
  public void verify_mapping_and_valueType_conversion_from_DB() {
    dbSession = dbClient.openSession(false);

    for (Metric.MetricType metricType : Metric.MetricType.values()) {
      verify_mapping_and_valueType_conversion_from_DB_impl(metricType.name(), metricType);
    }
  }

  private void verify_mapping_and_valueType_conversion_from_DB_impl(String valueType, Metric.MetricType expected) {
    MetricDto metricDto = new MetricDto().setId(SOME_KEY.hashCode()).setKey(SOME_KEY + valueType).setShortName(SOME_NAME).setValueType(valueType);

    dbClient.metricDao().insert(dbSession, metricDto);
    dbSession.commit();

    Metric metric = underTest.getByKey(metricDto.getKey());

    assertThat(metric.getId()).isEqualTo(metricDto.getId());
    assertThat(metric.getKey()).isEqualTo(metricDto.getKey());
    assertThat(metric.getName()).isEqualTo(metricDto.getShortName());
    assertThat(metric.getType()).isEqualTo(expected);
  }

  @Test(expected = IllegalArgumentException.class)
  public void findByKey_throws_IAE_if_valueType_can_not_be_parsed() {
    MetricDto metricDto = new MetricDto().setKey(SOME_KEY).setShortName(SOME_NAME).setValueType("trololo");

    dbSession = dbClient.openSession(false);
    dbClient.metricDao().insert(dbSession, metricDto);
    dbSession.commit();

    underTest.getByKey(SOME_KEY);
  }
}
