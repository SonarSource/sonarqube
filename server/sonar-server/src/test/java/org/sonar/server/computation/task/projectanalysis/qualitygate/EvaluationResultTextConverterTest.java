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
package org.sonar.server.computation.task.projectanalysis.qualitygate;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.Durations;
import org.sonar.core.timemachine.Periods;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricImpl;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.Level.ERROR;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.Level.WARN;

@RunWith(DataProviderRunner.class)
public class EvaluationResultTextConverterTest {
  private static final Metric INT_METRIC = new MetricImpl(1, "key", "int_metric_name", Metric.MetricType.INT);
  private static final Metric SOME_VARIATION_METRIC = new MetricImpl(2, "new_variation_of_trololo", "variation_of_trololo_name", Metric.MetricType.INT);
  private static final Condition EQ_10_CONDITION = new Condition(INT_METRIC, Condition.Operator.EQUALS.getDbValue(), "10", null, false);
  private static final EvaluationResult OK_EVALUATION_RESULT = new EvaluationResult(Measure.Level.OK, null);
  private static final String ERROR_THRESHOLD = "error_threshold";
  private static final String WARNING_THRESHOLD = "warning_threshold";
  private static final String SOME_MODE = "mode";
  private static final String SOME_ANALYSIS_UUID = "u1";

  @Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule();

  private I18n i18n = mock(I18n.class);
  private Durations durations = mock(Durations.class);
  private Periods periods = mock(Periods.class);
  private EvaluationResultTextConverter underTest = new EvaluationResultTextConverterImpl(i18n, durations, periods, periodsHolder);

  @Test(expected = NullPointerException.class)
  public void evaluate_throws_NPE_if_Condition_arg_is_null() {
    underTest.asText(null, OK_EVALUATION_RESULT);
  }

  @Test(expected = NullPointerException.class)
  public void evaluate_throws_NPE_if_EvaluationResult_arg_is_null() {
    underTest.asText(EQ_10_CONDITION, null);
  }

  @Test
  public void evaluate_returns_null_if_EvaluationResult_has_level_OK() {
    assertThat(underTest.asText(EQ_10_CONDITION, OK_EVALUATION_RESULT)).isNull();
  }

  @DataProvider
  public static Object[][] all_operators_for_error_warning_levels() {
    List<Object[]> res = new ArrayList<>();
    for (Condition.Operator operator : Condition.Operator.values()) {
      for (Measure.Level level : ImmutableList.of(ERROR, WARN)) {
        res.add(new Object[] {operator, level});
      }
    }
    return res.toArray(new Object[res.size()][2]);
  }

  @Test
  @UseDataProvider("all_operators_for_error_warning_levels")
  public void evaluate_returns_msg_of_metric_plus_operator_plus_threshold_for_level_argument(Condition.Operator operator, Measure.Level level) {
    String metricMsg = "int_metric_msg";

    when(i18n.message(Locale.ENGLISH, "metric." + INT_METRIC.getKey() + ".name", INT_METRIC.getName()))
      .thenReturn(metricMsg);

    Condition condition = new Condition(INT_METRIC, operator.getDbValue(), ERROR_THRESHOLD, WARNING_THRESHOLD, false);

    assertThat(underTest.asText(condition, new EvaluationResult(level, null)))
      .isEqualTo(metricMsg + " " + toSign(operator) + " " + getThreshold(level));
  }

  private String getThreshold(Measure.Level level) {
    return level == ERROR ? ERROR_THRESHOLD : WARNING_THRESHOLD;
  }

  @Test
  @UseDataProvider("all_operators_for_error_warning_levels")
  public void evaluate_does_not_add_variation_if_metric_starts_with_variation_prefix_but_period_is_null(Condition.Operator operator, Measure.Level level) {
    String metricMsg = "trololo_metric_msg";

    when(i18n.message(Locale.ENGLISH, "metric." + SOME_VARIATION_METRIC.getKey() + ".name", SOME_VARIATION_METRIC.getName()))
      .thenReturn(metricMsg);

    Condition condition = new Condition(SOME_VARIATION_METRIC, operator.getDbValue(), ERROR_THRESHOLD, WARNING_THRESHOLD, false);

    assertThat(underTest.asText(condition, new EvaluationResult(level, null)))
      .isEqualTo(metricMsg + " " + toSign(operator) + " " + getThreshold(level));
  }

  @Test
  @UseDataProvider("all_operators_for_error_warning_levels")
  public void evaluate_adds_only_period_if_metric_starts_with_new_prefix(Condition.Operator operator, Measure.Level level) {
    String metricMsg = "trololo_metric_msg";
    int periodIndex = 1;
    String periodLabel = "periodLabel";

    when(i18n.message(Locale.ENGLISH, "metric." + SOME_VARIATION_METRIC.getKey() + ".name", SOME_VARIATION_METRIC.getName()))
      .thenReturn(metricMsg);

    Date date = new Date();
    Period period = new Period(SOME_MODE, null, date.getTime(), SOME_ANALYSIS_UUID);
    periodsHolder.setPeriod(period);
    when(periods.label(period.getMode(), period.getModeParameter(), date)).thenReturn(periodLabel);

    Condition condition = new Condition(SOME_VARIATION_METRIC, operator.getDbValue(), ERROR_THRESHOLD, WARNING_THRESHOLD, true);

    assertThat(underTest.asText(condition, new EvaluationResult(level, null)))
      .isEqualTo(metricMsg + " " + toSign(operator) + " " + (getThreshold(level)) + " " + periodLabel);
  }

  @Test
  @UseDataProvider("all_operators_for_error_warning_levels")
  public void evaluate_adds_variation_and_period_if_metric_does_not_starts_with_variation_prefix(Condition.Operator operator, Measure.Level level) {
    String metricMsg = "trololo_metric_msg";
    String variationMsg = "_variation_";
    int periodIndex = 1;
    String periodLabel = "periodLabel";

    when(i18n.message(Locale.ENGLISH, "metric." + INT_METRIC.getKey() + ".name", INT_METRIC.getName()))
      .thenReturn(metricMsg);
    when(i18n.message(Locale.ENGLISH, "variation", "variation")).thenReturn(variationMsg);

    Date date = new Date();
    Period period = new Period(SOME_MODE, null, date.getTime(), SOME_ANALYSIS_UUID);
    periodsHolder.setPeriod(period);
    when(periods.label(period.getMode(), period.getModeParameter(), date)).thenReturn(periodLabel);

    Condition condition = new Condition(INT_METRIC, operator.getDbValue(), ERROR_THRESHOLD, WARNING_THRESHOLD, true);

    assertThat(underTest.asText(condition, new EvaluationResult(level, null)))
      .isEqualTo(metricMsg + " " + variationMsg + " " + toSign(operator) + " " + (getThreshold(level)) + " " + periodLabel);
  }

  private static String toSign(Condition.Operator operator) {
    switch (operator) {
      case EQUALS:
        return "=";
      case NOT_EQUALS:
        return "!=";
      case GREATER_THAN:
        return ">";
      case LESS_THAN:
        return "<";
      default:
        throw new IllegalArgumentException("Unsupported operator");
    }
  }
}
