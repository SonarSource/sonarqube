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
package org.sonar.db.metric;

import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.metric.MetricTesting.newMetricDto;


public class MetricDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  DbSession session;

  MetricDao underTest;

  @Before
  public void createDao() {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    underTest = new MetricDao();
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void select_by_key() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    MetricDto result = underTest.selectByKey(session, "coverage");
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
    assertThat(result.getDecimalScale()).isEqualTo(3);

    // Disabled metrics are returned
    result = underTest.selectByKey(session, "disabled");
    assertThat(result.getId()).isEqualTo(3);
    assertThat(result.isEnabled()).isFalse();
  }

  @Test(expected = RowNotFoundException.class)
  public void select_or_fail_by_key() {
    underTest.selectOrFailByKey(session, "unknown");
  }

  @Test
  public void get_manual_metric() {
    dbTester.prepareDbUnit(getClass(), "manual_metric.xml");

    MetricDto result = underTest.selectByKey(session, "manual");
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

    assertThat(underTest.selectEnabled(session)).hasSize(2);
  }

  @Test
  public void find_all() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectAll(session)).extracting("id").containsExactly(2, 3, 1);
  }

  @Test
  public void insert() {
    underTest.insert(session, new MetricDto()
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

    MetricDto result = underTest.selectByKey(session, "coverage");
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
    underTest.insert(session, new MetricDto()
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

  @Test
  public void selectById() {
    MetricDto metric = underTest.insert(session, newMetricDto());

    MetricDto result = underTest.selectById(session, metric.getId());

    assertThat(result).isNotNull();
  }

  @Test
  public void selectOrFailById() {
    MetricDto metric = underTest.insert(session, newMetricDto());

    MetricDto result = underTest.selectOrFailById(session, metric.getId());

    assertThat(result).isNotNull();
  }

  @Test
  public void fail_when_no_id_selectOrFailById() {
    thrown.expect(RowNotFoundException.class);

    underTest.selectOrFailById(session, 42L);
  }

  @Test
  public void selectByIds() {
    MetricDto metric1 = underTest.insert(session, newMetricDto());
    MetricDto metric2 = underTest.insert(session, newMetricDto());

    List<MetricDto> result = underTest.selectByIds(session, newHashSet(metric1.getId(), metric2.getId()));

    assertThat(result).hasSize(2);
  }

  @Test
  public void update() {
    MetricDto metric = underTest.insert(session, newMetricDto().setKey("first-key"));

    underTest.update(session, metric.setKey("second-key"));

    MetricDto result = underTest.selectByKey(session, "second-key");
    assertThat(result).isNotNull();
  }

  @Test
  public void countEnabled() {
    underTest.insert(session, newMetricDto().setEnabled(true).setUserManaged(true));
    underTest.insert(session, newMetricDto().setEnabled(true).setUserManaged(true));
    underTest.insert(session, newMetricDto().setEnabled(false));

    int result = underTest.countEnabled(session, true);

    assertThat(result).isEqualTo(2);
  }

  @Test
  public void selectDomains() {
    underTest.insert(session, newMetricDto().setDomain("first-domain").setEnabled(true));
    underTest.insert(session, newMetricDto().setDomain("second-domain").setEnabled(true));
    underTest.insert(session, newMetricDto().setDomain("second-domain").setEnabled(true));
    underTest.insert(session, newMetricDto().setDomain("third-domain").setEnabled(true));

    List<String> domains = underTest.selectEnabledDomains(session);

    assertThat(domains).hasSize(3).containsOnly("first-domain", "second-domain", "third-domain");
  }

  @Test
  public void selectByKeys() {
    underTest.insert(session, newMetricDto().setKey("first-key"));
    underTest.insert(session, newMetricDto().setKey("second-key"));
    underTest.insert(session, newMetricDto().setKey("third-key"));

    List<MetricDto> result = underTest.selectByKeys(session, Arrays.asList("first-key", "second-key", "third-key"));

    assertThat(result).hasSize(3)
      .extracting("key").containsOnly("first-key", "second-key", "third-key");
  }

  @Test
  public void disableByIds() {
    MetricDto metric1 = underTest.insert(session, newMetricDto().setEnabled(true).setUserManaged(true));
    MetricDto metric2 = underTest.insert(session, newMetricDto().setEnabled(true).setUserManaged(true));

    underTest.disableCustomByIds(session, Arrays.asList(metric1.getId(), metric2.getId()));

    List<MetricDto> result = underTest.selectByIds(session, newHashSet(metric1.getId(), metric2.getId()));
    assertThat(result).hasSize(2);
    assertThat(result).extracting("enabled").containsOnly(false);
  }

  @Test
  public void disableByKey() {
    underTest.insert(session, newMetricDto().setKey("metric-key").setEnabled(true).setUserManaged(true));

    boolean updated = underTest.disableCustomByKey(session, "metric-key");
    assertThat(updated).isTrue();

    MetricDto result = underTest.selectByKey(session, "metric-key");
    assertThat(result.isEnabled()).isFalse();

    // disable again -> zero rows are touched
    updated = underTest.disableCustomByKey(session, "metric-key");
    assertThat(updated).isFalse();
  }

  @Test
  public void selectOrFailByKey() {
    underTest.insert(session, newMetricDto().setKey("metric-key"));

    MetricDto result = underTest.selectOrFailByKey(session, "metric-key");

    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo("metric-key");
  }

  @Test
  public void selectEnabled_with_paging_and_custom() {
    underTest.insert(session, newMetricDto().setUserManaged(true).setEnabled(true));
    underTest.insert(session, newMetricDto().setUserManaged(true).setEnabled(true));
    underTest.insert(session, newMetricDto().setUserManaged(true).setEnabled(true));
    underTest.insert(session, newMetricDto().setUserManaged(false).setEnabled(true));
    underTest.insert(session, newMetricDto().setUserManaged(true).setEnabled(false));

    List<MetricDto> result = underTest.selectEnabled(session, true, 0, 100);

    assertThat(result).hasSize(3);
  }

  @Test
  public void selectAvailableByComponentUuid() {
    underTest.insert(session, newMetricDto().setUserManaged(true).setEnabled(true).setKey("metric-key"));
    underTest.insert(session, newMetricDto().setUserManaged(false).setEnabled(true).setKey("another-metric-key"));
    underTest.insert(session, newMetricDto().setUserManaged(true).setEnabled(false).setKey("third-metric-key"));

    List<MetricDto> result = underTest.selectAvailableCustomMetricsByComponentUuid(session, "project-uuid");

    assertThat(result).hasSize(1)
      .extracting("key").containsOnly("metric-key");

  }
}
