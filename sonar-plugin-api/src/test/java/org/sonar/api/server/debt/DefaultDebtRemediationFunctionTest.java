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
package org.sonar.api.server.debt;

import org.junit.Test;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class DefaultDebtRemediationFunctionTest {

  @Test
  public void create_linear() {
    DebtRemediationFunction function = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, "10h", null);
    assertThat(function.type()).isEqualTo(DefaultDebtRemediationFunction.Type.LINEAR);
    assertThat(function.gapMultiplier()).isEqualTo("10h");
    assertThat(function.baseEffort()).isNull();
  }

  @Test
  public void create_linear_with_offset() {
    DebtRemediationFunction function = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "10h", "5min");
    assertThat(function.type()).isEqualTo(DefaultDebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(function.gapMultiplier()).isEqualTo("10h");
    assertThat(function.baseEffort()).isEqualTo("5min");
  }

  @Test
  public void create_constant_per_issue() {
    DebtRemediationFunction function = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "10h");
    assertThat(function.type()).isEqualTo(DefaultDebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(function.gapMultiplier()).isNull();
    assertThat(function.baseEffort()).isEqualTo("10h");
  }

  @Test
  public void sanitize_remediation_coefficient_and_offset() {
    DebtRemediationFunction function = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "  1  h   ", "  10   min");

    assertThat(function.gapMultiplier()).isEqualTo("1h");
    assertThat(function.baseEffort()).isEqualTo("10min");
  }

  @Test
  public void fail_to_when_no_type() {
    try {
      new DefaultDebtRemediationFunction(null, "5min", "10h");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Remediation function type cannot be null");
    }
  }

  @Test
  public void fail_to_create_linear_when_no_coefficient() {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, null, "10h");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Linear functions must only have a non empty gap multiplier");
    }
  }

  @Test
  public void fail_to_create_linear_when_offset() {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, "5min", "10h");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Linear functions must only have a non empty gap multiplier");
    }
  }

  @Test
  public void fail_to_create_constant_per_issue_when_no_offset() {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, "10h", null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Constant/issue functions must only have a non empty base effort");
    }
  }

  @Test
  public void fail_to_create_constant_per_issue_when_coefficient() {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, "5min", "10h");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Constant/issue functions must only have a non empty base effort");
    }
  }

  @Test
  public void fail_to_create_linear_with_offset_when_no_coefficient() {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "", "10h");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Linear with offset functions must have both non null gap multiplier and base effort");
    }
  }

  @Test
  public void fail_to_create_linear_with_offset_when_no_offset() {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "5min", "");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Linear with offset functions must have both non null gap multiplier and base effort");
    }
  }

  @Test
  public void test_equals_and_hashcode() {
    DebtRemediationFunction function = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "10h", "5min");
    DebtRemediationFunction functionWithSameValue = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "10h", "5min");
    DebtRemediationFunction functionWithDifferentType = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "5min");

    assertThat(function).isEqualTo(function);
    assertThat(function).isEqualTo(functionWithSameValue);
    assertThat(function).isNotEqualTo(functionWithDifferentType);

    assertThat(function).isNotEqualTo(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "11h", "5min"));
    assertThat(function).isNotEqualTo(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "10h", "6min"));
    assertThat(function).isNotEqualTo(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, "10h", null));
    assertThat(function).isNotEqualTo(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "6min"));

    assertThat(function.hashCode()).isEqualTo(function.hashCode());
    assertThat(function.hashCode()).isEqualTo(functionWithSameValue.hashCode());
    assertThat(function.hashCode()).isNotEqualTo(functionWithDifferentType.hashCode());
  }

  @Test
  public void test_to_string() {
    assertThat(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "10h", "5min").toString())
      .isEqualTo("DebtRemediationFunction{type=LINEAR_OFFSET, gap multiplier=10h, base effort=5min}");
  }

  @Test
  public void fail_if_bad_coefficient_format() {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, "foo", null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Invalid gap multiplier: foo (Duration 'foo' is invalid, it should use the following sample format : 2d 10h 15min)");
    }

  }
}
