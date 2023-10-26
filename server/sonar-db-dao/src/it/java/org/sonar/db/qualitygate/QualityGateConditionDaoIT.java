/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.qualitygate;

import java.util.Collection;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

public class QualityGateConditionDaoIT {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = dbTester.getSession();
  private QualityGateConditionDao underTest = dbTester.getDbClient().gateConditionDao();

  @Test
  public void testInsert() {
    QualityGateConditionDto newCondition = insertQGCondition("1", "2", "GT", "20");

    assertThat(newCondition.getUuid()).isNotNull();
    QualityGateConditionDto actual = underTest.selectByUuid(newCondition.getUuid(), dbSession);
    assertEquals(actual, newCondition);
  }

  @Test
  public void testSelectForQualityGate() {
    String qg1Uuid = "1";
    String qg2Uuid = "2";
    int qg1Conditions = 2 + new Random().nextInt(5);
    int qg2Conditions = 10 + new Random().nextInt(5);

    IntStream.range(0, qg1Conditions).forEach(i -> insertQGCondition(qg1Uuid));
    IntStream.range(0, qg2Conditions).forEach(i -> insertQGCondition(qg2Uuid));

    Collection<QualityGateConditionDto> conditions = underTest.selectForQualityGate(dbSession, qg1Uuid);
    assertThat(conditions).hasSize(qg1Conditions);
    assertThat(conditions)
      .extracting("uuid")
      .containsExactly(conditions.stream()
        .sorted(Comparator.comparing(QualityGateConditionDto::getCreatedAt))
        .map(QualityGateConditionDto::getUuid).toArray());

    conditions = underTest.selectForQualityGate(dbSession, qg2Uuid);
    assertThat(conditions).hasSize(qg2Conditions);
    assertThat(conditions)
      .extracting("uuid")
      .containsExactly(conditions.stream()
        .sorted(Comparator.comparing(QualityGateConditionDto::getCreatedAt))
        .map(QualityGateConditionDto::getUuid)
        .toArray());

    assertThat(underTest.selectForQualityGate(dbSession, "5")).isEmpty();
  }

  @Test
  public void selectAll() {
    MetricDto metric = dbTester.measures().insertMetric(t -> t.setEnabled(true));
    QualityGateConditionDto condition1 = insertQGCondition("uuid1", metric.getUuid());
    QualityGateConditionDto condition2 = insertQGCondition("uuid2", metric.getUuid());
    QualityGateConditionDto condition3 = insertQGCondition("uuid3", metric.getUuid());

    assertThat(underTest.selectAll(dbSession))
      .extracting(QualityGateConditionDto::getUuid, QualityGateConditionDto::getMetricUuid)
      .containsOnly(tuple(condition1.getUuid(), condition1.getMetricUuid()),
        tuple(condition2.getUuid(), condition2.getMetricUuid()),
        tuple(condition3.getUuid(), condition3.getMetricUuid()));
  }

  @Test
  public void testSelectByUuid() {
    QualityGateConditionDto condition = insertQGCondition("1", "2", "GT", "20");

    assertEquals(underTest.selectByUuid(condition.getUuid(), dbSession), condition);
    assertThat(underTest.selectByUuid("uuid1", dbSession)).isNull();
  }

  @Test
  public void testDelete() {
    QualityGateConditionDto condition1 = insertQGCondition("2");
    QualityGateConditionDto condition2 = insertQGCondition("3");

    underTest.delete(condition1, dbSession);
    dbSession.commit();

    assertThat(underTest.selectByUuid(condition1.getUuid(), dbSession)).isNull();
    assertThat(underTest.selectByUuid(condition2.getUuid(), dbSession)).isNotNull();
  }

  @Test
  public void testUpdate() {
    QualityGateConditionDto condition1 = insertQGCondition("2");
    QualityGateConditionDto condition2 = insertQGCondition("3");

    QualityGateConditionDto newCondition1 = new QualityGateConditionDto()
      .setUuid(condition1.getUuid())
      .setQualityGateUuid(condition1.getQualityGateUuid())
      .setMetricUuid("7")
      .setOperator(">")
      .setErrorThreshold("80");
    underTest.update(newCondition1, dbSession);
    dbSession.commit();


    assertEquals(underTest.selectByUuid(condition1.getUuid(), dbSession), newCondition1);
    assertEquals(underTest.selectByUuid(condition2.getUuid(), dbSession), condition2);
  }

  @Test
  public void shouldCleanConditions() {
    MetricDto enabledMetric = dbTester.measures().insertMetric(t -> t.setEnabled(true));
    MetricDto disabledMetric = dbTester.measures().insertMetric(t -> t.setEnabled(false));
    QualityGateConditionDto condition1 = insertQGCondition("1", enabledMetric.getUuid());
    QualityGateConditionDto condition2 = insertQGCondition("1", disabledMetric.getUuid());
    QualityGateConditionDto condition3 = insertQGCondition("1", "299");

    underTest.deleteConditionsWithInvalidMetrics(dbTester.getSession());
    dbTester.commit();


    assertThat(underTest.selectByUuid(condition1.getUuid(), dbSession)).isNotNull();
    assertThat(underTest.selectByUuid(condition2.getUuid(), dbSession)).isNull();
    assertThat(underTest.selectByUuid(condition3.getUuid(), dbSession)).isNull();
  }

  private QualityGateConditionDto insertQGCondition(String qualityGateUuid) {
    return insertQGCondition(qualityGateUuid, randomAlphabetic(2));
  }

  private QualityGateConditionDto insertQGCondition(String qualityGateUuid, String metricUuid) {
    return insertQGCondition(qualityGateUuid, metricUuid, randomAlphabetic(2), randomAlphabetic(3));
  }

  private QualityGateConditionDto insertQGCondition(String qualityGateUuid, String metricUuid, String operator, String threshold) {
    QualityGateConditionDto res = new QualityGateConditionDto()
      .setUuid(Uuids.create())
      .setQualityGateUuid(qualityGateUuid)
      .setMetricUuid(metricUuid)
      .setOperator(operator)
      .setErrorThreshold(threshold);
    underTest.insert(res, dbTester.getSession());
    dbTester.commit();
    return res;
  }

  private void assertEquals(QualityGateConditionDto actual, QualityGateConditionDto expected) {
    assertThat(actual.getQualityGateUuid()).isEqualTo(expected.getQualityGateUuid());
    assertThat(actual.getMetricUuid()).isEqualTo(expected.getMetricUuid());
    assertThat(actual.getOperator()).isEqualTo(expected.getOperator());
    assertThat(actual.getErrorThreshold()).isEqualTo(expected.getErrorThreshold());
  }
}
