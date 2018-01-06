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
package org.sonar.db.metric;

import java.util.Arrays;
import java.util.List;
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
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbSession dbSession = db.getSession();

  private MetricDao underTest = new MetricDao();

  @Test
  public void select_by_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    MetricDto result = underTest.selectByKey(dbSession, "coverage");
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
    result = underTest.selectByKey(dbSession, "disabled");
    assertThat(result.getId()).isEqualTo(3);
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void select_or_fail_by_key() {
    expectedException.expect(RowNotFoundException.class);

    underTest.selectOrFailByKey(dbSession, "unknown");
  }

  @Test
  public void get_manual_metric() {
    db.prepareDbUnit(getClass(), "manual_metric.xml");

    MetricDto result = underTest.selectByKey(dbSession, "manual");
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
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectEnabled(dbSession)).hasSize(2);
  }

  @Test
  public void find_all() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectAll(dbSession)).extracting("id").containsExactly(2, 3, 1);
  }

  @Test
  public void insert() {
    underTest.insert(dbSession, new MetricDto()
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

    MetricDto result = underTest.selectByKey(dbSession, "coverage");
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
    underTest.insert(dbSession, new MetricDto()
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
    dbSession.commit();

    assertThat(db.countRowsOfTable("metrics")).isEqualTo(2);
  }

  @Test
  public void selectById() {
    MetricDto metric = underTest.insert(dbSession, newMetricDto());

    MetricDto result = underTest.selectById(dbSession, metric.getId());

    assertThat(result).isNotNull();
  }

  @Test
  public void selectOrFailById() {
    MetricDto metric = underTest.insert(dbSession, newMetricDto());

    MetricDto result = underTest.selectOrFailById(dbSession, metric.getId());

    assertThat(result).isNotNull();
  }

  @Test
  public void fail_when_no_id_selectOrFailById() {
    expectedException.expect(RowNotFoundException.class);

    underTest.selectOrFailById(dbSession, 42L);
  }

  @Test
  public void selectByIds() {
    MetricDto metric1 = underTest.insert(dbSession, newMetricDto());
    MetricDto metric2 = underTest.insert(dbSession, newMetricDto());

    List<MetricDto> result = underTest.selectByIds(dbSession, newHashSet(metric1.getId(), metric2.getId()));

    assertThat(result).hasSize(2);
  }

  @Test
  public void update() {
    MetricDto metric = underTest.insert(dbSession, newMetricDto().setKey("first-key"));

    underTest.update(dbSession, metric.setKey("second-key"));

    MetricDto result = underTest.selectByKey(dbSession, "second-key");
    assertThat(result).isNotNull();
  }

  @Test
  public void countEnabled() {
    underTest.insert(dbSession, newMetricDto().setEnabled(true).setUserManaged(true));
    underTest.insert(dbSession, newMetricDto().setEnabled(true).setUserManaged(true));
    underTest.insert(dbSession, newMetricDto().setEnabled(false));

    int result = underTest.countEnabled(dbSession, true);

    assertThat(result).isEqualTo(2);
  }

  @Test
  public void selectDomains() {
    underTest.insert(dbSession, newMetricDto().setDomain("first-domain").setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setDomain("second-domain").setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setDomain("second-domain").setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setDomain("third-domain").setEnabled(true));

    List<String> domains = underTest.selectEnabledDomains(dbSession);

    assertThat(domains).hasSize(3).containsOnly("first-domain", "second-domain", "third-domain");
  }

  @Test
  public void selectByKeys() {
    underTest.insert(dbSession, newMetricDto().setKey("first-key"));
    underTest.insert(dbSession, newMetricDto().setKey("second-key"));
    underTest.insert(dbSession, newMetricDto().setKey("third-key"));

    List<MetricDto> result = underTest.selectByKeys(dbSession, Arrays.asList("first-key", "second-key", "third-key"));

    assertThat(result).hasSize(3)
      .extracting("key").containsOnly("first-key", "second-key", "third-key");
  }

  @Test
  public void disableByIds() {
    MetricDto metric1 = underTest.insert(dbSession, newMetricDto().setEnabled(true).setUserManaged(true));
    MetricDto metric2 = underTest.insert(dbSession, newMetricDto().setEnabled(true).setUserManaged(true));

    underTest.disableCustomByIds(dbSession, Arrays.asList(metric1.getId(), metric2.getId()));

    List<MetricDto> result = underTest.selectByIds(dbSession, newHashSet(metric1.getId(), metric2.getId()));
    assertThat(result).hasSize(2);
    assertThat(result).extracting("enabled").containsOnly(false);
  }

  @Test
  public void disableByKey() {
    underTest.insert(dbSession, newMetricDto().setKey("metric-key").setEnabled(true).setUserManaged(true));

    boolean updated = underTest.disableCustomByKey(dbSession, "metric-key");
    assertThat(updated).isTrue();

    MetricDto result = underTest.selectByKey(dbSession, "metric-key");
    assertThat(result.isEnabled()).isFalse();

    // disable again -> zero rows are touched
    updated = underTest.disableCustomByKey(dbSession, "metric-key");
    assertThat(updated).isFalse();
  }

  @Test
  public void selectOrFailByKey() {
    underTest.insert(dbSession, newMetricDto().setKey("metric-key"));

    MetricDto result = underTest.selectOrFailByKey(dbSession, "metric-key");

    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo("metric-key");
  }

  @Test
  public void selectEnabled_with_paging_and_custom() {
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setUserManaged(false).setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(false));

    List<MetricDto> result = underTest.selectEnabled(dbSession, true, 0, 100);

    assertThat(result).hasSize(3);
  }

  @Test
  public void selectAvailableByComponentUuid() {
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(true).setKey("metric-key"));
    underTest.insert(dbSession, newMetricDto().setUserManaged(false).setEnabled(true).setKey("another-metric-key"));
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(false).setKey("third-metric-key"));

    List<MetricDto> result = underTest.selectAvailableCustomMetricsByComponentUuid(dbSession, "project-uuid");

    assertThat(result).hasSize(1)
      .extracting("key").containsOnly("metric-key");
  }
}
