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
package org.sonar.server.qualitygate;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_GREATER_THAN;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_LESS_THAN;

public class RegisterQualityGatesTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  QualityGates qualityGates = mock(QualityGates.class);
  RegisterQualityGates task = new RegisterQualityGates(dbClient,
    new QualityGateUpdater(dbClient),
    new QualityGateConditionsUpdater(dbClient),
    dbClient.loadedTemplateDao(),
    qualityGates);

  @Test
  public void register_default_gate() {
    MetricDto newReliability = dbClient.metricDao().insert(dbSession, newMetricDto().setKey(NEW_RELIABILITY_RATING_KEY).setValueType(INT.name()).setHidden(false));
    MetricDto newSecurity = dbClient.metricDao().insert(dbSession, newMetricDto().setKey(NEW_SECURITY_RATING_KEY).setValueType(INT.name()).setHidden(false));
    MetricDto newMaintainability = dbClient.metricDao().insert(dbSession, newMetricDto().setKey(NEW_MAINTAINABILITY_RATING_KEY).setValueType(PERCENT.name()).setHidden(false));
    MetricDto newCoverage = dbClient.metricDao().insert(dbSession, newMetricDto().setKey(NEW_COVERAGE_KEY).setValueType(PERCENT.name()).setHidden(false));
    dbSession.commit();

    task.start();

    assertThat(dbClient.loadedTemplateDao().countByTypeAndKey("QUALITY_GATE", "SonarQube way", dbSession)).isEqualTo(1);
    QualityGateDto qualityGateDto = dbClient.qualityGateDao().selectByName(dbSession, "SonarQube way");
    assertThat(qualityGateDto).isNotNull();
    assertThat(dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGateDto.getId()))
      .extracting(QualityGateConditionDto::getMetricId, QualityGateConditionDto::getOperator, QualityGateConditionDto::getWarningThreshold,
        QualityGateConditionDto::getErrorThreshold, QualityGateConditionDto::getPeriod)
      .containsOnly(
        tuple(newReliability.getId().longValue(), OPERATOR_GREATER_THAN, null, "1", 1),
        tuple(newSecurity.getId().longValue(), OPERATOR_GREATER_THAN, null, "1", 1),
        tuple(newMaintainability.getId().longValue(), OPERATOR_GREATER_THAN, null, "1", 1),
        tuple(newCoverage.getId().longValue(), OPERATOR_LESS_THAN, null, "80", 1));
    verify(qualityGates).setDefault(any(DbSession.class), anyLong());

    task.stop();
  }

  @Test
  public void does_not_register_default_gate_if_already_executed() {
    String templateType = "QUALITY_GATE";
    String templateName = "SonarQube way";
    dbClient.loadedTemplateDao().insert(new LoadedTemplateDto(templateName, templateType), dbSession);
    dbSession.commit();

    task.start();

    assertThat(dbClient.qualityGateDao().selectAll(dbSession)).isEmpty();
    verifyZeroInteractions(qualityGates);
  }
}
