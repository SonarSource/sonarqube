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

package org.sonar.api.server.rule;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class DefaultDebtRemediationFunctionTest {

  @Test
  public void create_linear() throws Exception {
    DebtRemediationFunction function = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, "10h", null);
    assertThat(function.type()).isEqualTo(DefaultDebtRemediationFunction.Type.LINEAR);
    assertThat(function.factor()).isEqualTo("10h");
    assertThat(function.offset()).isNull();
  }

  @Test
  public void create_linear_with_offset() throws Exception {
    DebtRemediationFunction function = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "10h", "5min");
    assertThat(function.type()).isEqualTo(DefaultDebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(function.factor()).isEqualTo("10h");
    assertThat(function.offset()).isEqualTo("5min");
  }

  @Test
  public void create_constant_per_issue() throws Exception {
    DebtRemediationFunction function = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "10h");
    assertThat(function.type()).isEqualTo(DefaultDebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(function.factor()).isNull();
    assertThat(function.offset()).isEqualTo("10h");
  }

  @Test
  public void sanitize_remediation_factor_and_offset() {
    DebtRemediationFunction function = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "  1  h   ", "  10   mi n");

    assertThat(function.factor()).isEqualTo("1h");
    assertThat(function.offset()).isEqualTo("10min");
  }

  @Test
  public void fail_to_create_linear_when_no_factor() throws Exception {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, null, "10h");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(DefaultDebtRemediationFunction.ValidationException.class);
    }
  }

  @Test
  public void fail_to_create_linear_when_offset() throws Exception {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, "5min", "10h");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(DefaultDebtRemediationFunction.ValidationException.class);
    }
  }

  @Test
  public void fail_to_create_constant_per_issue_when_no_offset() throws Exception {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, "10h", null);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(DefaultDebtRemediationFunction.ValidationException.class);
    }
  }

  @Test
  public void fail_to_create_constant_per_issue_when_factor() throws Exception {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, "5min", "10h");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(DefaultDebtRemediationFunction.ValidationException.class);
    }
  }

  @Test
  public void fail_to_create_linear_with_offset_when_no_factor() throws Exception {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, null, "10h");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(DefaultDebtRemediationFunction.ValidationException.class);
    }
  }

  @Test
  public void fail_to_create_linear_with_offset_when_no_offset() throws Exception {
    try {
      new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "5min", null);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(DefaultDebtRemediationFunction.ValidationException.class);
    }
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
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
  public void test_to_string() throws Exception {
    assertThat(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "10h", "5min").toString()).isNotNull();
  }

}
