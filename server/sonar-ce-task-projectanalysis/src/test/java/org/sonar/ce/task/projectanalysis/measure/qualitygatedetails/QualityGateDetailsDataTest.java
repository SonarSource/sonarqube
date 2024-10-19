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
package org.sonar.ce.task.projectanalysis.measure.qualitygatedetails;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QualityGateDetailsDataTest {
  @Test
  public void constructor_throws_NPE_if_Level_arg_is_null() {
    assertThatThrownBy(() -> new QualityGateDetailsData(null, Collections.emptyList(), false))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void constructor_throws_NPE_if_Iterable_arg_is_null() {
    assertThatThrownBy(() -> new QualityGateDetailsData(Measure.Level.OK, null, false))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void verify_json_when_there_is_no_condition() {
    String actualJson = new QualityGateDetailsData(Measure.Level.OK, Collections.emptyList(), false).toJson();

    JsonAssert.assertJson(actualJson).isSimilarTo("{" +
      "\"level\":\"OK\"," +
      "\"conditions\":[]" +
      "}");
  }

  @Test
  public void verify_json_for_each_type_of_condition() {
    String value = "actualValue";
    Condition condition = new Condition(new MetricImpl("1", "key1", "name1", Metric.MetricType.STRING), Condition.Operator.GREATER_THAN.getDbValue(), "errorTh");
    ImmutableList<EvaluatedCondition> evaluatedConditions = ImmutableList.of(
      new EvaluatedCondition(condition, Measure.Level.OK, value),
      new EvaluatedCondition(condition, Measure.Level.ERROR, value));
    String actualJson = new QualityGateDetailsData(Measure.Level.OK, evaluatedConditions, false).toJson();

    JsonAssert.assertJson(actualJson).isSimilarTo("{" +
      "\"level\":\"OK\"," +
      "\"conditions\":[" +
      "  {" +
      "    \"metric\":\"key1\"," +
      "    \"op\":\"GT\"," +
      "    \"error\":\"errorTh\"," +
      "    \"actual\":\"actualValue\"," +
      "    \"level\":\"OK\"" +
      "  }," +
      "  {" +
      "    \"metric\":\"key1\"," +
      "    \"op\":\"GT\"," +
      "    \"error\":\"errorTh\"," +
      "    \"actual\":\"actualValue\"," +
      "    \"level\":\"ERROR\"" +
      "  }" +
      "]" +
      "}");
  }

  @Test
  public void verify_json_for_condition_on_leak_metric() {
    String value = "actualValue";
    Condition condition = new Condition(new MetricImpl("1", "new_key1", "name1", Metric.MetricType.STRING), Condition.Operator.GREATER_THAN.getDbValue(), "errorTh");
    ImmutableList<EvaluatedCondition> evaluatedConditions = ImmutableList.of(
      new EvaluatedCondition(condition, Measure.Level.OK, value),
      new EvaluatedCondition(condition, Measure.Level.ERROR, value));
    String actualJson = new QualityGateDetailsData(Measure.Level.OK, evaluatedConditions, false).toJson();

    JsonAssert.assertJson(actualJson).isSimilarTo("{" +
      "\"level\":\"OK\"," +
      "\"conditions\":[" +
      "  {" +
      "    \"metric\":\"new_key1\"," +
      "    \"op\":\"GT\"," +
      "    \"error\":\"errorTh\"," +
      "    \"actual\":\"actualValue\"," +
      "    \"period\":1," +
      "    \"level\":\"OK\"" +
      "  }," +
      "  {" +
      "    \"metric\":\"new_key1\"," +
      "    \"op\":\"GT\"," +
      "    \"error\":\"errorTh\"," +
      "    \"actual\":\"actualValue\"," +
      "    \"period\":1," +
      "    \"level\":\"ERROR\"" +
      "  }" +
      "]" +
      "}");
  }

  @Test
  public void verify_json_for_small_leak() {
    String actualJson = new QualityGateDetailsData(Measure.Level.OK, Collections.emptyList(), false).toJson();
    JsonAssert.assertJson(actualJson).isSimilarTo("{\"ignoredConditions\": false}");

    String actualJson2 = new QualityGateDetailsData(Measure.Level.OK, Collections.emptyList(), true).toJson();
    JsonAssert.assertJson(actualJson2).isSimilarTo("{\"ignoredConditions\": true}");
  }

}
