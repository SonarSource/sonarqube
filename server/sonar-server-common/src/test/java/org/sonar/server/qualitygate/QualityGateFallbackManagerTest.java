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

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.metric.SoftwareQualitiesMetrics;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class QualityGateFallbackManagerTest {

  QualityGateFallbackManager underTest = new QualityGateFallbackManager();

  @Test
  void getFallbackCondition_whenConditionIsSoftwareQualityMetricWithEquivalence_shouldReturnExpectedCondition() {
    Condition condition = new Condition(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY, Condition.Operator.GREATER_THAN, "3");
    Optional<Condition> fallbackCondition = underTest.getFallbackCondition(condition);
    assertThat(fallbackCondition).isPresent()
      .get()
      .extracting(Condition::getMetricKey, Condition::getOperator, Condition::getErrorThreshold)
      .containsExactly(CoreMetrics.SECURITY_RATING_KEY, Condition.Operator.GREATER_THAN, "3");
  }

  @Test
  void getFallbackCondition_whenConditionIsStandardMetricWithEquivalence_shouldNotReturnCondition() {
    Condition condition = new Condition(CoreMetrics.SECURITY_RATING_KEY, Condition.Operator.GREATER_THAN, "3");
    Optional<Condition> fallbackCondition = underTest.getFallbackCondition(condition);
    assertThat(fallbackCondition).isEmpty();
  }

  @Test
  void getFallbackCondition_whenConditionIsAnyMetricWithoutEquivalence_shouldNotReturnCondition() {
    Condition condition = new Condition(CoreMetrics.LINES_KEY, Condition.Operator.GREATER_THAN, "3");
    Optional<Condition> fallbackCondition = underTest.getFallbackCondition(condition);
    assertThat(fallbackCondition).isEmpty();
  }

}
