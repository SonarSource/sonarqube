/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.System2;
import org.sonar.core.metric.SoftwareQualitiesMetrics;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateDto;

class QualityGateConditionsValidatorIT {

  @RegisterExtension
  public DbTester db = DbTester.create(System2.INSTANCE);

  public QualityGateConditionsValidator underTest = new QualityGateConditionsValidator(db.getDbClient());

  @Test
  void hasConditionsMismatch_hasMismatchForStandardMode_shouldReturnExpectedValue() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    MetricDto mqrMetric = db.measures().insertMetric(m -> m.setKey(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY));
    db.qualityGates().addCondition(qualityGateDto, mqrMetric);

    Assertions.assertThat(underTest.hasConditionsMismatch(false)).isTrue();
    Assertions.assertThat(underTest.hasConditionsMismatch(true)).isFalse();
  }

  @Test
  void hasConditionsMismatch_hasMismatchForMQRMode_shouldReturnExpectedValue() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    MetricDto mqrMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.CODE_SMELLS_KEY));
    db.qualityGates().addCondition(qualityGateDto, mqrMetric);

    Assertions.assertThat(underTest.hasConditionsMismatch(true)).isTrue();
    Assertions.assertThat(underTest.hasConditionsMismatch(false)).isFalse();
  }

  @Test
  void hasConditionsMismatch_hasNoMismatchFromAnyModes_shouldReturnExpectedValue() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    MetricDto mqrMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.COVERAGE_KEY));
    db.qualityGates().addCondition(qualityGateDto, mqrMetric);

    Assertions.assertThat(underTest.hasConditionsMismatch(true)).isFalse();
    Assertions.assertThat(underTest.hasConditionsMismatch(false)).isFalse();
  }
}
