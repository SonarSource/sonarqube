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
package org.sonar.server.qualitygate;

import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.qualitygate.ShortLivingBranchQualityGate.Condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ShortLivingBranchQualityGateTest {
  @Test
  public void defines_3_conditions() {
    assertThat(ShortLivingBranchQualityGate.CONDITIONS)
      .extracting(Condition::getMetricKey, Condition::getOperator, Condition::getErrorThreshold, Condition::getWarnThreshold, Condition::isOnLeak)
      .containsExactly(
        tuple(CoreMetrics.BUGS_KEY, "GT", "0", null, false),
        tuple(CoreMetrics.VULNERABILITIES_KEY, "GT", "0", null, false),
        tuple(CoreMetrics.CODE_SMELLS_KEY, "GT", "0", null, false));
  }

}
