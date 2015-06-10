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
package org.sonar.batch.qualitygate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Locale;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.qualitygate.db.QualityGateConditionDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QualityGateVerifierTest {

  QualityGateVerifier verifier;
  DecoratorContext context;
  QualityGate qualityGate;

  Measure measureClasses;
  Measure measureCoverage;
  Measure measureComplexity;
  Resource project;
  Snapshot snapshot;
  I18n i18n;
  Durations durations;

  @Before
  public void before() {
    context = mock(DecoratorContext.class);
    i18n = mock(I18n.class);
    when(i18n.message(any(Locale.class), eq("variation"), eq("variation"))).thenReturn("variation");
    durations = mock(Durations.class);

    measureClasses = new Measure(CoreMetrics.CLASSES, 20d);
    measureCoverage = new Measure(CoreMetrics.COVERAGE, 35d);
    measureComplexity = new Measure(CoreMetrics.COMPLEXITY, 50d);

    when(context.getMeasure(CoreMetrics.CLASSES)).thenReturn(measureClasses);
    when(context.getMeasure(CoreMetrics.COVERAGE)).thenReturn(measureCoverage);
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(measureComplexity);

    snapshot = mock(Snapshot.class);
    qualityGate = mock(QualityGate.class);
    when(qualityGate.isEnabled()).thenReturn(true);

    project = new Project("foo");

    verifier = new QualityGateVerifier(qualityGate, i18n, durations);
  }

  @Test
  public void should_be_executed_if_quality_gate_is_enabled() {
    assertThat(verifier.shouldExecuteOnProject((Project) project)).isTrue();
    when(qualityGate.isEnabled()).thenReturn(false);
    assertThat(verifier.shouldExecuteOnProject((Project) project)).isFalse();
  }

  @Test
  public void test_toString() {
    assertThat(verifier.toString()).isEqualTo("QualityGateVerifier");
  }

  @Test
  public void generates_quality_gates_status() {
    assertThat(verifier.generatesQualityGateStatus()).isEqualTo(CoreMetrics.ALERT_STATUS);
  }

  @Test
  public void depends_on_variations() {
    assertThat(verifier.dependsOnVariations()).isEqualTo(DecoratorBarriers.END_OF_TIME_MACHINE);
  }

  @Test
  public void depends_upon_metrics() {
    when(qualityGate.conditions()).thenReturn(ImmutableList.of(new ResolvedCondition(null, CoreMetrics.CLASSES)));
    assertThat(verifier.dependsUponMetrics()).containsOnly(CoreMetrics.CLASSES);
  }

  @Test
  public void ok_when_no_alerts() {
    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(CoreMetrics.CLASSES, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "20"),
      mockCondition(CoreMetrics.COVERAGE, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "35.0"));
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.OK)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.OK)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.ALERT_STATUS, Metric.Level.OK.toString())));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.QUALITY_GATE_DETAILS, "{\"level\":\"OK\","
      + "\"conditions\":"
      + "["
      + "{\"metric\":\"classes\",\"op\":\"GT\",\"warning\":\"20\",\"actual\":\"20.0\",\"level\":\"OK\"},"
      + "{\"metric\":\"coverage\",\"op\":\"GT\",\"warning\":\"35.0\",\"actual\":\"35.0\",\"level\":\"OK\"}"
      + "]"
      + "}")));
  }

  @Test
  public void check_root_modules_only() {
    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(CoreMetrics.CLASSES, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "20"),
      mockCondition(CoreMetrics.COVERAGE, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "35.0"));
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(File.create("src/Foo.php"), context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void generate_warnings() {
    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(CoreMetrics.CLASSES, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "100"),
      mockCondition(CoreMetrics.COVERAGE, QualityGateConditionDto.OPERATOR_LESS_THAN, null, "95.0")); // generates warning because coverage
                                                                                                      // 35% < 95%
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.WARN, null)));

    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.OK)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.WARN)));

  }

  @Test
  public void globalStatusShouldBeErrorIfWarningsAndErrors() {
    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(CoreMetrics.CLASSES, QualityGateConditionDto.OPERATOR_LESS_THAN, null, "100"), // generates warning because classes 20 <
                                                                                                   // 100
      mockCondition(CoreMetrics.COVERAGE, QualityGateConditionDto.OPERATOR_LESS_THAN, "50.0", "80.0")); // generates error because coverage
                                                                                                        // 35% < 50%
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, null)));

    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.WARN)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.ERROR)));
  }

  @Test
  public void globalLabelShouldAggregateAllLabels() {
    when(i18n.message(any(Locale.class), eq("metric.classes.name"), anyString())).thenReturn("Classes");
    when(i18n.message(any(Locale.class), eq("metric.coverage.name"), anyString())).thenReturn("Coverages");
    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(CoreMetrics.CLASSES, QualityGateConditionDto.OPERATOR_LESS_THAN, null, "10000"), // there are 20 classes, error
                                                                                                     // threshold is higher => alert
      mockCondition(CoreMetrics.COVERAGE, QualityGateConditionDto.OPERATOR_LESS_THAN, "50.0", "80.0"));// coverage is 35%, warning threshold
                                                                                                       // is higher => alert
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, "Classes < 10000, Coverages < 50.0")));
  }

  @Test
  public void alertLabelUsesL10nMetricName() {
    Metric metric = new Metric.Builder("rating", "Rating", Metric.ValueType.INT).create();

    // metric name is declared in l10n bundle
    when(i18n.message(any(Locale.class), eq("metric.rating.name"), anyString())).thenReturn("THE RATING");

    when(context.getMeasure(metric)).thenReturn(new Measure(metric, 4d));
    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(mockCondition(metric, QualityGateConditionDto.OPERATOR_LESS_THAN, "10", null));
    when(qualityGate.conditions()).thenReturn(conditions);
    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, "THE RATING < 10")));
  }

  @Test
  public void alertLabelUsesMetricNameIfMissingL10nBundle() {
    // the third argument is Metric#getName()
    when(i18n.message(any(Locale.class), eq("metric.classes.name"), eq("Classes"))).thenReturn("Classes");
    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      // there are 20 classes, error threshold is higher => alert
      mockCondition(CoreMetrics.CLASSES, QualityGateConditionDto.OPERATOR_LESS_THAN, "10000", null)
      );
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, "Classes < 10000")));
  }

  @Test
  public void shouldBeOkIfPeriodVariationIsEnough() {
    measureClasses.setVariation1(0d);
    measureCoverage.setVariation2(50d);
    measureComplexity.setVariation3(2d);

    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(CoreMetrics.CLASSES, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "10", 1), // ok because no variation
      mockCondition(CoreMetrics.COVERAGE, QualityGateConditionDto.OPERATOR_LESS_THAN, null, "40.0", 2), // ok because coverage increases of
                                                                                                        // 50%, which is more
      // than 40%
      mockCondition(CoreMetrics.COMPLEXITY, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "5", 3) // ok because complexity increases
                                                                                                         // of 2, which is less
      // than 5
      );
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.OK, null)));

    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.OK)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.OK)));
    verify(context).saveMeasure(argThat(hasLevel(measureComplexity, Metric.Level.OK)));
  }

  @Test
  public void shouldGenerateWarningIfPeriodVariationIsNotEnough() {
    measureClasses.setVariation1(40d);
    measureCoverage.setVariation2(5d);
    measureComplexity.setVariation3(70d);

    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(CoreMetrics.CLASSES, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "30", 1), // generates warning because classes
                                                                                                        // increases of 40,
      // which is greater than 30
      mockCondition(CoreMetrics.COVERAGE, QualityGateConditionDto.OPERATOR_LESS_THAN, null, "10.0", 2), // generates warning because
                                                                                                        // coverage increases of 5%,
      // which is smaller than 10%
      mockCondition(CoreMetrics.COMPLEXITY, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "60", 3) // generates warning because
                                                                                                          // complexity increases of
      // 70, which is smaller than 60
      );
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.WARN, null)));

    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.WARN)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.WARN)));
    verify(context).saveMeasure(argThat(hasLevel(measureComplexity, Metric.Level.WARN)));
  }

  @Test
  public void shouldBeOkIfVariationIsNull() {
    measureClasses.setVariation1(null);

    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(CoreMetrics.CLASSES, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "10", 1));
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.OK, null)));
    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.OK)));
  }

  @Test
  public void shouldVariationPeriodValueCouldBeUsedForRatingMetric() {
    Metric ratingMetric = new Metric.Builder("key_rating_metric", "Rating metric", Metric.ValueType.RATING).create();
    Measure measureRatingMetric = new Measure(ratingMetric, 150d);
    measureRatingMetric.setVariation1(50d);
    when(context.getMeasure(ratingMetric)).thenReturn(measureRatingMetric);

    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(ratingMetric, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "100", 1)
      );
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.OK, null)));
    verify(context).saveMeasure(argThat(hasLevel(measureRatingMetric, Metric.Level.OK)));
  }

  @Test
  public void shouldAllowVariationPeriodOnAllPeriods() {
    measureClasses.setVariation4(40d);

    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(CoreMetrics.CLASSES, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "30", 4)
      );
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.WARN, null)));
    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.WARN)));
  }

  @Test(expected = NotImplementedException.class)
  public void shouldNotAllowPeriodVariationAlertOnStringMetric() {
    Measure measure = new Measure(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION, 100d);
    measure.setVariation1(50d);
    when(context.getMeasure(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION)).thenReturn(measure);

    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "30", 1)
      );
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);
  }

  @Test
  @Ignore("Disabled because snapshot is no more created by the batch")
  public void shouldLabelAlertContainsPeriod() {
    measureClasses.setVariation1(40d);

    when(i18n.message(any(Locale.class), eq("metric.classes.name"), anyString())).thenReturn("Classes");
    // when(periods.label(snapshot, 1)).thenReturn("since someday");

    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(CoreMetrics.CLASSES, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "30", 1) // generates warning because classes
                                                                                                       // increases of 40,
      // which is greater than 30
      );
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.WARN, "Classes variation > 30 since someday")));
  }

  @Test
  @Ignore("Disabled because snapshot is no more created by the batch")
  public void shouldLabelAlertForNewMetricDoNotContainsVariationWord() {
    Metric newMetric = new Metric.Builder("new_metric_key", "New Metric", Metric.ValueType.INT).create();
    Measure measure = new Measure(newMetric, 15d);
    measure.setVariation1(50d);
    when(context.getMeasure(newMetric)).thenReturn(measure);
    measureClasses.setVariation1(40d);

    when(i18n.message(any(Locale.class), eq("metric.new_metric_key.name"), anyString())).thenReturn("New Measure");
    // when(periods.label(snapshot, 1)).thenReturn("since someday");

    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(
      mockCondition(newMetric, QualityGateConditionDto.OPERATOR_GREATER_THAN, null, "30", 1) // generates warning because classes increases
                                                                                             // of 40, which is
      // greater than 30
      );
    when(qualityGate.conditions()).thenReturn(conditions);

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.WARN, "New Measure > 30 since someday")));
  }

  @Test
  public void alert_on_work_duration() {
    Metric metric = new Metric.Builder("tech_debt", "Debt", Metric.ValueType.WORK_DUR).create();

    // metric name is declared in l10n bundle
    when(i18n.message(any(Locale.class), eq("metric.tech_debt.name"), anyString())).thenReturn("The Debt");
    when(durations.format(any(Locale.class), eq(Duration.create(3600L)), eq(Durations.DurationFormat.SHORT))).thenReturn("1h");

    when(context.getMeasure(metric)).thenReturn(new Measure(metric, 7200d));
    ArrayList<ResolvedCondition> conditions = Lists.newArrayList(mockCondition(metric, QualityGateConditionDto.OPERATOR_GREATER_THAN, "3600", null));
    when(qualityGate.conditions()).thenReturn(conditions);
    verifier.decorate(project, context);

    // First call to saveMeasure is for the update of debt
    verify(context).saveMeasure(argThat(matchesMetric(metric, Level.ERROR, "The Debt > 1h")));
    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, "The Debt > 1h")));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.QUALITY_GATE_DETAILS, "{\"level\":\"ERROR\","
      + "\"conditions\":"
      + "["
      + "{\"metric\":\"tech_debt\",\"op\":\"GT\",\"error\":\"3600\",\"actual\":\"7200.0\",\"level\":\"ERROR\"}"
      + "]"
      + "}")));
  }

  private ArgumentMatcher<Measure> matchesMetric(final Metric metric, final Metric.Level alertStatus, final String alertText) {
    return new ArgumentMatcher<Measure>() {
      @Override
      public boolean matches(Object arg) {
        boolean result = ((Measure) arg).getMetric().equals(metric) && ((Measure) arg).getAlertStatus() == alertStatus;
        if (result && alertText != null) {
          result = alertText.equals(((Measure) arg).getAlertText());
        }
        return result;
      }
    };
  }

  private ArgumentMatcher<Measure> hasLevel(final Measure measure, final Metric.Level alertStatus) {
    return new ArgumentMatcher<Measure>() {
      @Override
      public boolean matches(Object arg) {
        return arg == measure && ((Measure) arg).getAlertStatus().equals(alertStatus);
      }
    };
  }

  private ResolvedCondition mockCondition(Metric metric, String operator, String error, String warning) {
    return mockCondition(metric, operator, error, warning, null);
  }

  private ResolvedCondition mockCondition(Metric metric, String operator, String error, String warning, Integer period) {
    ResolvedCondition cond = mock(ResolvedCondition.class);
    when(cond.metric()).thenReturn(metric);
    when(cond.metricKey()).thenReturn(metric.getKey());
    when(cond.operator()).thenReturn(operator);
    when(cond.warningThreshold()).thenReturn(warning);
    when(cond.errorThreshold()).thenReturn(error);
    when(cond.period()).thenReturn(period);
    return cond;
  }

}
