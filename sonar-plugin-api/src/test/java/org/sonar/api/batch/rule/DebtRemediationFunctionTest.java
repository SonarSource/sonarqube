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

package org.sonar.api.batch.rule;

import org.junit.Test;
import org.sonar.api.server.rule.DebtRemediationFunction;

import static org.fest.assertions.Assertions.assertThat;

public class DebtRemediationFunctionTest {

  @Test
  public void create_linear() throws Exception {
    org.sonar.api.server.rule.DebtRemediationFunction function = org.sonar.api.server.rule.DebtRemediationFunction.createLinear("10h");
    assertThat(function.type()).isEqualTo(org.sonar.api.server.rule.DebtRemediationFunction.Type.LINEAR);
    assertThat(function.factor()).isEqualTo("10h");
    assertThat(function.offset()).isNull();
  }

  @Test
  public void create_linear_with_offset() throws Exception {
    org.sonar.api.server.rule.DebtRemediationFunction function = org.sonar.api.server.rule.DebtRemediationFunction.createLinearWithOffset("10h", "5min");
    assertThat(function.type()).isEqualTo(org.sonar.api.server.rule.DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(function.factor()).isEqualTo("10h");
    assertThat(function.offset()).isEqualTo("5min");
  }

  @Test
  public void create_constant_per_issue() throws Exception {
    org.sonar.api.server.rule.DebtRemediationFunction function = org.sonar.api.server.rule.DebtRemediationFunction.createConstantPerIssue("10h");
    assertThat(function.type()).isEqualTo(org.sonar.api.server.rule.DebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(function.factor()).isNull();
    assertThat(function.offset()).isEqualTo("10h");
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    org.sonar.api.server.rule.DebtRemediationFunction function = org.sonar.api.server.rule.DebtRemediationFunction.createLinearWithOffset("10h", "5min");
    org.sonar.api.server.rule.DebtRemediationFunction functionWithSameValue = org.sonar.api.server.rule.DebtRemediationFunction.createLinearWithOffset("10h", "5min");
    org.sonar.api.server.rule.DebtRemediationFunction functionWithDifferentType = org.sonar.api.server.rule.DebtRemediationFunction.createConstantPerIssue("5min");

    assertThat(function).isEqualTo(function);
    assertThat(function).isEqualTo(functionWithSameValue);
    assertThat(function).isNotEqualTo(functionWithDifferentType);
    assertThat(function).isNotEqualTo(org.sonar.api.server.rule.DebtRemediationFunction.createLinearWithOffset("11h", "5min"));
    assertThat(function).isNotEqualTo(org.sonar.api.server.rule.DebtRemediationFunction.createLinearWithOffset("10h", "6min"));
    assertThat(function).isNotEqualTo(org.sonar.api.server.rule.DebtRemediationFunction.createLinear("10h"));
    assertThat(function).isNotEqualTo(org.sonar.api.server.rule.DebtRemediationFunction.createConstantPerIssue("6min"));

    assertThat(function.hashCode()).isEqualTo(function.hashCode());
    assertThat(function.hashCode()).isEqualTo(functionWithSameValue.hashCode());
    assertThat(function.hashCode()).isNotEqualTo(functionWithDifferentType.hashCode());
  }

  @Test
  public void test_to_string() throws Exception {
    assertThat(DebtRemediationFunction.createLinearWithOffset("10h", "5min").toString()).isNotNull();
  }
}
