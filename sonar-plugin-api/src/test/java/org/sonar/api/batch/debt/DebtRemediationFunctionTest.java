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
package org.sonar.api.batch.debt;

import org.junit.Test;
import org.sonar.api.utils.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class DebtRemediationFunctionTest {

  @Test
  public void create_linear() {
    DebtRemediationFunction function = DebtRemediationFunction.createLinear(Duration.create(10));
    assertThat(function.type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(function.coefficient()).isEqualTo(Duration.create(10));
    assertThat(function.offset()).isNull();
  }

  @Test
  public void create_linear_with_offset() {
    DebtRemediationFunction function = DebtRemediationFunction.createLinearWithOffset(Duration.create(10), Duration.create(5));
    assertThat(function.type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(function.coefficient()).isEqualTo(Duration.create(10));
    assertThat(function.offset()).isEqualTo(Duration.create(5));
  }

  @Test
  public void create_constant_per_issue() {
    DebtRemediationFunction function = DebtRemediationFunction.createConstantPerIssue(Duration.create(10));
    assertThat(function.type()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(function.coefficient()).isNull();
    assertThat(function.offset()).isEqualTo(Duration.create(10));
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    DebtRemediationFunction function = DebtRemediationFunction.createLinearWithOffset(Duration.create(10), Duration.create(5));
    DebtRemediationFunction functionWithSameValue = DebtRemediationFunction.createLinearWithOffset(Duration.create(10), Duration.create(5));
    DebtRemediationFunction functionWithDifferentType = DebtRemediationFunction.createConstantPerIssue(Duration.create(5));

    assertThat(function).isEqualTo(function);
    assertThat(function).isEqualTo(functionWithSameValue);
    assertThat(function).isNotEqualTo(functionWithDifferentType);
    assertThat(function).isNotEqualTo(DebtRemediationFunction.createLinearWithOffset(Duration.create(11), Duration.create(5)));
    assertThat(function).isNotEqualTo(DebtRemediationFunction.createLinearWithOffset(Duration.create(10), Duration.create(6)));
    assertThat(function).isNotEqualTo(DebtRemediationFunction.createLinear(Duration.create(10)));
    assertThat(function).isNotEqualTo(DebtRemediationFunction.createConstantPerIssue(Duration.create(6)));

    assertThat(function.hashCode()).isEqualTo(function.hashCode());
    assertThat(function.hashCode()).isEqualTo(functionWithSameValue.hashCode());
    assertThat(function.hashCode()).isNotEqualTo(functionWithDifferentType.hashCode());
  }

  @Test
  public void test_to_string() throws Exception {
    assertThat(DebtRemediationFunction.createLinearWithOffset(Duration.create(10), Duration.create(5)).toString())
      .isEqualTo("DebtRemediationFunction{type=LINEAR_OFFSET, coefficient=Duration[durationInMinutes=10], offset=Duration[durationInMinutes=5]}");
  }
}
