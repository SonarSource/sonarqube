/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.qualitygate;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.Durations;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.core.i18n.I18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.ERROR;

@RunWith(DataProviderRunner.class)
public class EvaluationResultTextConverterTest {
  private static final Metric INT_METRIC = new MetricImpl(1, "key", "int_metric_name", Metric.MetricType.INT);
  private static final Metric SOME_VARIATION_METRIC = new MetricImpl(2, "new_variation_of_trololo", "variation_of_trololo_name", Metric.MetricType.INT);
  private static final Condition LT_10_CONDITION = new Condition(INT_METRIC, Condition.Operator.LESS_THAN.getDbValue(), "10");
  private static final EvaluationResult OK_EVALUATION_RESULT = new EvaluationResult(Measure.Level.OK, null);
  private static final String ERROR_THRESHOLD = "error_threshold";

  private I18n i18n = mock(I18n.class);
  private Durations durations = mock(Durations.class);
  private EvaluationResultTextConverter underTest = new EvaluationResultTextConverterImpl(i18n, durations);

  @Test(expected = NullPointerException.class)
  public void evaluate_throws_NPE_if_Condition_arg_is_null() {
    underTest.asText(null, OK_EVALUATION_RESULT);
  }

  @Test(expected = NullPointerException.class)
  public void evaluate_throws_NPE_if_EvaluationResult_arg_is_null() {
    underTest.asText(LT_10_CONDITION, null);
  }

  @Test
  public void evaluate_returns_null_if_EvaluationResult_has_level_OK() {
    assertThat(underTest.asText(LT_10_CONDITION, OK_EVALUATION_RESULT)).isNull();
  }

  @DataProvider
  public static Object[][] all_operators_for_error_levels() {
    List<Object[]> res = new ArrayList<>();
    for (Condition.Operator operator : Condition.Operator.values()) {
      res.add(new Object[] {operator, ERROR});
    }
    return res.toArray(new Object[res.size()][2]);
  }

  @Test
  @UseDataProvider("all_operators_for_error_levels")
  public void evaluate_returns_msg_of_metric_plus_operator_plus_threshold_for_level_argument(Condition.Operator operator, Measure.Level level) {
    String metricMsg = "int_metric_msg";

    when(i18n.message(Locale.ENGLISH, "metric." + INT_METRIC.getKey() + ".name", INT_METRIC.getName()))
      .thenReturn(metricMsg);

    Condition condition = new Condition(INT_METRIC, operator.getDbValue(), ERROR_THRESHOLD);

    assertThat(underTest.asText(condition, new EvaluationResult(level, null)))
      .isEqualTo(metricMsg + " " + toSign(operator) + " " + ERROR_THRESHOLD);
  }

  @Test
  @UseDataProvider("all_operators_for_error_levels")
  public void evaluate_does_not_add_variation_if_metric_starts_with_variation_prefix_but_period_is_null(Condition.Operator operator, Measure.Level level) {
    String metricMsg = "trololo_metric_msg";

    when(i18n.message(Locale.ENGLISH, "metric." + SOME_VARIATION_METRIC.getKey() + ".name", SOME_VARIATION_METRIC.getName()))
      .thenReturn(metricMsg);

    Condition condition = new Condition(SOME_VARIATION_METRIC, operator.getDbValue(), ERROR_THRESHOLD);

    assertThat(underTest.asText(condition, new EvaluationResult(level, null)))
      .isEqualTo(metricMsg + " " + toSign(operator) + " " + ERROR_THRESHOLD);
  }

  private static String toSign(Condition.Operator operator) {
    switch (operator) {
      case GREATER_THAN:
        return ">";
      case LESS_THAN:
        return "<";
      default:
        throw new IllegalArgumentException("Unsupported operator");
    }
  }
}
