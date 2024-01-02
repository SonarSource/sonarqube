/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue.commonrule;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.db.rule.RuleTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CommonRuleTest {

  private static final String PLUGIN_KEY = "java";
  private static final String QP_KEY = "qp1";


  @Test
  public void test_getMinDensityParam() {
    ActiveRule activeRule = new ActiveRule(RuleTesting.XOO_X1, Severity.MAJOR, ImmutableMap.of("minDensity", "30.5"), 1_000L, PLUGIN_KEY, QP_KEY);
    double minDensity = CommonRule.getMinDensityParam(activeRule, "minDensity");

    assertThat(minDensity).isEqualTo(30.5);
  }

  @Test
  public void getMinDensityParam_fails_if_param_value_is_absent() {
    assertThatThrownBy(() -> {
      ActiveRule activeRule = new ActiveRule(RuleTesting.XOO_X1, Severity.MAJOR, ImmutableMap.of(), 1_000L, PLUGIN_KEY, QP_KEY);
      CommonRule.getMinDensityParam(activeRule, "minDensity");
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Required parameter [minDensity] is missing on rule [xoo:x1]");
  }

  @Test
  public void getMinDensityParam_fails_if_param_value_is_negative() {
    assertThatThrownBy(() -> {
      ActiveRule activeRule = new ActiveRule(RuleTesting.XOO_X1, Severity.MAJOR, ImmutableMap.of("minDensity", "-30.5"), 1_000L, PLUGIN_KEY, QP_KEY);
      CommonRule.getMinDensityParam(activeRule, "minDensity");
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Minimum density of rule [xoo:x1] is incorrect. Got [-30.5] but must be between 0 and 100.");
  }

  @Test
  public void getMinDensityParam_fails_if_param_value_is_greater_than_100() {
    assertThatThrownBy(() -> {
      ActiveRule activeRule = new ActiveRule(RuleTesting.XOO_X1, Severity.MAJOR, ImmutableMap.of("minDensity", "305"), 1_000L, PLUGIN_KEY, QP_KEY);
      CommonRule.getMinDensityParam(activeRule, "minDensity");
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Minimum density of rule [xoo:x1] is incorrect. Got [305] but must be between 0 and 100.");
  }
}
