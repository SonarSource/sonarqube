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

package org.sonar.db.metric;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class MetricDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbSession session;

  MetricDao dao;

  @Before
  public void createDao() {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dao = new MetricDao();
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void get_by_key() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    MetricDto result = dao.selectNullableByKey(session, "coverage");
    assertThat(result.getId()).isEqualTo(2);
    assertThat(result.getKey()).isEqualTo("coverage");
    assertThat(result.getShortName()).isEqualTo("Coverage");
    assertThat(result.getDescription()).isEqualTo("Coverage by unit tests");
    assertThat(result.getDomain()).isEqualTo("Tests");
    assertThat(result.getValueType()).isEqualTo("PERCENT");
    assertThat(result.getDirection()).isEqualTo(1);
    assertThat(result.isQualitative()).isTrue();
    assertThat(result.isUserManaged()).isFalse();
    assertThat(result.getWorstValue()).isEqualTo(0d);
    assertThat(result.getBestValue()).isEqualTo(100d);
    assertThat(result.isOptimizedBestValue()).isFalse();
    assertThat(result.isDeleteHistoricalData()).isFalse();
    assertThat(result.isHidden()).isFalse();
    assertThat(result.isEnabled()).isTrue();

    // Disabled metrics are returned
    result = dao.selectNullableByKey(session, "disabled");
    assertThat(result.getId()).isEqualTo(3);
    assertThat(result.isEnabled()).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void get_nullable_by_key() {
    dao.selectByKey(session, "unknown");
  }

  @Test
  public void get_manual_metric() {
    dbTester.prepareDbUnit(getClass(), "manual_metric.xml");

    MetricDto result = dao.selectNullableByKey(session, "manual");
    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getKey()).isEqualTo("manual");
    assertThat(result.getShortName()).isEqualTo("Manual metric");
    assertThat(result.getDescription()).isEqualTo("Manual metric");
    assertThat(result.getDomain()).isNullOrEmpty();
    assertThat(result.getValueType()).isEqualTo("INT");
    assertThat(result.getDirection()).isEqualTo(0);
    assertThat(result.isQualitative()).isFalse();
    assertThat(result.isUserManaged()).isTrue();
    assertThat(result.getWorstValue()).isNull();
    assertThat(result.getBestValue()).isNull();
    assertThat(result.isOptimizedBestValue()).isFalse();
    assertThat(result.isDeleteHistoricalData()).isFalse();
    assertThat(result.isHidden()).isFalse();
    assertThat(result.isEnabled()).isTrue();
  }

  @Test
  public void find_all_enabled() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(dao.selectEnabled(session)).hasSize(2);
  }

  @Test
  public void insert() {
    dao.insert(session, new MetricDto()
      .setKey("coverage")
      .setShortName("Coverage")
      .setDescription("Coverage by unit tests")
      .setDomain("Tests")
      .setValueType("PERCENT")
      .setQualitative(true)
      .setUserManaged(true)
      .setWorstValue(0d)
      .setBestValue(100d)
      .setOptimizedBestValue(true)
      .setDirection(1)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .setEnabled(true));

    MetricDto result = dao.selectNullableByKey(session, "coverage");
    assertThat(result.getId()).isNotNull();
    assertThat(result.getKey()).isEqualTo("coverage");
    assertThat(result.getShortName()).isEqualTo("Coverage");
    assertThat(result.getDescription()).isEqualTo("Coverage by unit tests");
    assertThat(result.getDomain()).isEqualTo("Tests");
    assertThat(result.getValueType()).isEqualTo("PERCENT");
    assertThat(result.getDirection()).isEqualTo(1);
    assertThat(result.isQualitative()).isTrue();
    assertThat(result.isUserManaged()).isTrue();
    assertThat(result.getWorstValue()).isEqualTo(0d);
    assertThat(result.getBestValue()).isEqualTo(100d);
    assertThat(result.isOptimizedBestValue()).isTrue();
    assertThat(result.isDeleteHistoricalData()).isTrue();
    assertThat(result.isHidden()).isTrue();
    assertThat(result.isEnabled()).isTrue();
  }

  @Test
  public void insert_metrics() {
    dao.insert(session, new MetricDto()
      .setKey("coverage")
      .setShortName("Coverage")
      .setDescription("Coverage by unit tests")
      .setDomain("Tests")
      .setValueType("PERCENT")
      .setQualitative(true)
      .setUserManaged(true)
      .setWorstValue(0d)
      .setBestValue(100d)
      .setOptimizedBestValue(true)
      .setDirection(1)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .setEnabled(true),
      new MetricDto()
        .setKey("ncloc")
        .setShortName("ncloc")
        .setDescription("ncloc")
        .setDomain("Tests")
        .setValueType("INT")
        .setQualitative(true)
        .setUserManaged(true)
        .setWorstValue(0d)
        .setBestValue(100d)
        .setOptimizedBestValue(true)
        .setDirection(1)
        .setHidden(true)
        .setDeleteHistoricalData(true)
        .setEnabled(true));
    session.commit();

    assertThat(dbTester.countRowsOfTable("metrics")).isEqualTo(2);
  }
}
