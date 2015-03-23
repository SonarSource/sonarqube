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

package org.sonar.server.measure.persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class MetricDaoTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  DbSession session;

  MetricDao dao;

  @Before
  public void createDao() {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dao = new MetricDao();
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void get_by_key() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    MetricDto result = dao.selectByKey(session, "coverage");
    assertThat(result.getId()).isEqualTo(2);
    assertThat(result.getKey()).isEqualTo("coverage");
    assertThat(result.getShortName()).isEqualTo("Coverage");
    assertThat(result.getDescription()).isEqualTo("Coverage by unit tests");
    assertThat(result.getDomain()).isEqualTo("Tests");
    assertThat(result.getValueType()).isEqualTo("PERCENT");
    assertThat(result.getOrigin()).isEqualTo("JAV");
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
    result = dao.selectByKey(session, "disabled");
    assertThat(result.getId()).isEqualTo(3);
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void find_all_enabled() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(dao.selectEnabled(session)).hasSize(2);
  }

  @Test
  public void insert() throws Exception {
    dao.insert(session, new MetricDto()
      .setId(1)
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
      .setOrigin("JAV")
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .setEnabled(true));

    MetricDto result = dao.selectByKey(session, "coverage");
    assertThat(result.getId()).isNotNull();
    assertThat(result.getKey()).isEqualTo("coverage");
    assertThat(result.getShortName()).isEqualTo("Coverage");
    assertThat(result.getDescription()).isEqualTo("Coverage by unit tests");
    assertThat(result.getDomain()).isEqualTo("Tests");
    assertThat(result.getValueType()).isEqualTo("PERCENT");
    assertThat(result.getOrigin()).isEqualTo("JAV");
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
}
