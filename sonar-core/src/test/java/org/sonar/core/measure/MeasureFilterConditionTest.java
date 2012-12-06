/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.measure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric;

import static org.fest.assertions.Assertions.assertThat;

public class MeasureFilterConditionTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void create_operator_from_code() {
    assertThat(MeasureFilterCondition.Operator.fromCode("eq")).isEqualTo(MeasureFilterCondition.Operator.EQUALS);
    assertThat(MeasureFilterCondition.Operator.fromCode("lte")).isEqualTo(MeasureFilterCondition.Operator.LESS_OR_EQUALS);
  }

  @Test
  public void fail_if_operator_code_not_found() {
    thrown.expect(IllegalArgumentException.class);
    MeasureFilterCondition.Operator.fromCode("xxx");
  }

  @Test
  public void operator_sql() {
    assertThat(MeasureFilterCondition.Operator.EQUALS.getSql()).isEqualTo("=");
    assertThat(MeasureFilterCondition.Operator.LESS_OR_EQUALS.getSql()).isEqualTo("<=");
    assertThat(MeasureFilterCondition.Operator.GREATER.getSql()).isEqualTo(">");
  }

  @Test
  public void value_condition() {
    Metric ncloc = new Metric.Builder("ncloc", "NCLOC", Metric.ValueType.INT).create();
    ncloc.setId(123);
    MeasureFilterCondition condition = new MeasureFilterCondition(ncloc, MeasureFilterCondition.Operator.GREATER, 10.0);

    assertThat(condition.metric()).isEqualTo(ncloc);
    assertThat(condition.operator()).isEqualTo(MeasureFilterCondition.Operator.GREATER);
    assertThat(condition.period()).isNull();
    assertThat(condition.value()).isEqualTo(10.0);
    assertThat(condition.valueColumn()).isEqualTo("pm.value");
    assertThat(condition.toString()).isNotEmpty();
    assertThat(condition.appendSqlCondition(new StringBuilder()).toString()).isEqualTo(" pm.metric_id=123 AND pm.value>10.0");
  }

  @Test
  public void variation_condition() {
    Metric ncloc = new Metric.Builder("ncloc", "NCLOC", Metric.ValueType.INT).create();
    ncloc.setId(123);
    MeasureFilterCondition condition = new MeasureFilterCondition(ncloc, MeasureFilterCondition.Operator.LESS_OR_EQUALS, 10.0);
    condition.setPeriod(3);

    assertThat(condition.metric()).isEqualTo(ncloc);
    assertThat(condition.operator()).isEqualTo(MeasureFilterCondition.Operator.LESS_OR_EQUALS);
    assertThat(condition.period()).isEqualTo(3);
    assertThat(condition.value()).isEqualTo(10.0);
    assertThat(condition.valueColumn()).isEqualTo("pm.variation_value_3");
    assertThat(condition.toString()).isNotEmpty();
    assertThat(condition.appendSqlCondition(new StringBuilder()).toString()).isEqualTo(" pm.metric_id=123 AND pm.variation_value_3<=10.0");
  }
}
