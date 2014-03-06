
/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.collect.Lists;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.Alert;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.timemachine.Periods;

import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LegacyQualityGateVerifierTest {

  LegacyQualityGateVerifier verifier;
  DecoratorContext context;
  ProjectAlerts projectAlerts;

  Measure measureClasses;
  Measure measureCoverage;
  Measure measureComplexity;
  Resource project;
  Snapshot snapshot;
  Periods periods;
  I18n i18n;
  Durations durations;

  @Before
  public void before() {
    context = mock(DecoratorContext.class);
    periods = mock(Periods.class);
    i18n = mock(I18n.class);
    durations = mock(Durations.class);
    when(i18n.message(any(Locale.class), eq("variation"), eq("variation"))).thenReturn("variation");

    measureClasses = new Measure(CoreMetrics.CLASSES, 20d);
    measureCoverage = new Measure(CoreMetrics.COVERAGE, 35d);
    measureComplexity = new Measure(CoreMetrics.COMPLEXITY, 50d);

    when(context.getMeasure(CoreMetrics.CLASSES)).thenReturn(measureClasses);
    when(context.getMeasure(CoreMetrics.COVERAGE)).thenReturn(measureCoverage);
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(measureComplexity);

    snapshot = mock(Snapshot.class);
    projectAlerts = new ProjectAlerts();
    verifier = new LegacyQualityGateVerifier(snapshot, projectAlerts, periods, i18n, durations);
    project = new Project("foo");
  }

  @Test
  public void should_always_be_executed() {
    assertThat(verifier.shouldExecuteOnProject(new Project("foo"))).isTrue();
  }

  @Test
  public void test_toString() {
    assertThat(verifier.toString()).isEqualTo("LegacyQualityGateVerifier");
  }

  @Test
  public void generates_alert_status() {
    assertThat(verifier.generatesAlertStatus()).isEqualTo(CoreMetrics.ALERT_STATUS);
  }

  @Test
  public void depends_on_variations() {
    assertThat(verifier.dependsOnVariations()).isEqualTo(DecoratorBarriers.END_OF_TIME_MACHINE);
  }

  @Test
  public void depends_upon_metrics() {
    projectAlerts.addAll(Lists.newArrayList(new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "20")));
    assertThat(verifier.dependsUponMetrics()).containsOnly(CoreMetrics.CLASSES);
  }

  @Test
  public void ok_when_no_alerts() {
    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "20"),
      new Alert(null, CoreMetrics.COVERAGE, Alert.OPERATOR_GREATER, null, "35.0")));

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.ALERT_STATUS, Metric.Level.OK.toString())));
    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.OK)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.OK)));
  }

  @Test
  public void check_root_modules_only() {
    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "20"),
      new Alert(null, CoreMetrics.COVERAGE, Alert.OPERATOR_GREATER, null, "35.0")));

    verifier.decorate(File.create("src/Foo.php"), context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void generate_warnings() {
    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "100"),
      new Alert(null, CoreMetrics.COVERAGE, Alert.OPERATOR_SMALLER, null, "95.0"))); // generates warning because coverage 35% < 95%

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.WARN, null)));

    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.OK)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.WARN)));

  }

  @Test
  public void globalStatusShouldBeErrorIfWarningsAndErrors() {
    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_SMALLER, null, "100"), // generates warning because classes 20 < 100
      new Alert(null, CoreMetrics.COVERAGE, Alert.OPERATOR_SMALLER, "50.0", "80.0"))); // generates error because coverage 35% < 50%

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, null)));

    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.WARN)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.ERROR)));
  }

  @Test
  public void globalLabelShouldAggregateAllLabels() {
    when(i18n.message(any(Locale.class), eq("metric.classes.name"), anyString())).thenReturn("Classes");
    when(i18n.message(any(Locale.class), eq("metric.coverage.name"), anyString())).thenReturn("Coverages");
    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_SMALLER, null, "10000"), // there are 20 classes, error threshold is higher =>
      // alert
      new Alert(null, CoreMetrics.COVERAGE, Alert.OPERATOR_SMALLER, "50.0", "80.0"))); // coverage is 35%, warning threshold is higher =>
    // alert

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, "Classes < 10000, Coverages < 50.0")));
  }

  @Test
  public void alertLabelUsesL10nMetricName() {
    Metric metric = new Metric.Builder("rating", "Rating", Metric.ValueType.INT).create();

    // metric name is declared in l10n bundle
    when(i18n.message(any(Locale.class), eq("metric.rating.name"), anyString())).thenReturn("THE RATING");

    when(context.getMeasure(metric)).thenReturn(new Measure(metric, 4d));
    projectAlerts.addAll(Lists.newArrayList(new Alert(null, metric, Alert.OPERATOR_SMALLER, "10", null)));
    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, "THE RATING < 10")));
  }

  @Test
  public void alertLabelUsesMetricNameIfMissingL10nBundle() {
    // the third argument is Metric#getName()
    when(i18n.message(any(Locale.class), eq("metric.classes.name"), eq("Classes"))).thenReturn("Classes");
    projectAlerts.addAll(Lists.newArrayList(
      // there are 20 classes, error threshold is higher => alert
      new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_SMALLER, "10000", null)
    ));

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, "Classes < 10000")));
  }

  @Test
  public void shouldBeOkIfPeriodVariationIsEnough() {
    measureClasses.setVariation1(0d);
    measureCoverage.setVariation2(50d);
    measureComplexity.setVariation3(2d);

    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "10", 1), // ok because no variation
      new Alert(null, CoreMetrics.COVERAGE, Alert.OPERATOR_SMALLER, null, "40.0", 2), // ok because coverage increases of 50%, which is more
      // than 40%
      new Alert(null, CoreMetrics.COMPLEXITY, Alert.OPERATOR_GREATER, null, "5", 3) // ok because complexity increases of 2, which is less
      // than 5
    ));

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

    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "30", 1), // generates warning because classes increases of 40,
      // which is greater than 30
      new Alert(null, CoreMetrics.COVERAGE, Alert.OPERATOR_SMALLER, null, "10.0", 2), // generates warning because coverage increases of 5%,
      // which is smaller than 10%
      new Alert(null, CoreMetrics.COMPLEXITY, Alert.OPERATOR_GREATER, null, "60", 3) // generates warning because complexity increases of
      // 70, which is smaller than 60
    ));

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.WARN, null)));

    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.WARN)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.WARN)));
    verify(context).saveMeasure(argThat(hasLevel(measureComplexity, Metric.Level.WARN)));
  }

  @Test
  public void shouldBeOkIfVariationIsNull() {
    measureClasses.setVariation1(null);

    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "10", 1)));

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

    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, ratingMetric, Alert.OPERATOR_GREATER, null, "100", 1)
    ));

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.OK, null)));
    verify(context).saveMeasure(argThat(hasLevel(measureRatingMetric, Metric.Level.OK)));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldAllowOnlyVariationPeriodOneGlobalPeriods() {
    measureClasses.setVariation4(40d);

    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "30", 4)
    ));

    verifier.decorate(project, context);
  }

  @Test(expected = NotImplementedException.class)
  public void shouldNotAllowPeriodVariationAlertOnStringMetric() {
    Measure measure = new Measure(CoreMetrics.SCM_AUTHORS_BY_LINE, 100d);
    measure.setVariation1(50d);
    when(context.getMeasure(CoreMetrics.SCM_AUTHORS_BY_LINE)).thenReturn(measure);

    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, CoreMetrics.SCM_AUTHORS_BY_LINE, Alert.OPERATOR_GREATER, null, "30", 1)
    ));

    verifier.decorate(project, context);
  }

  @Test
  public void shouldLabelAlertContainsPeriod() {
    measureClasses.setVariation1(40d);

    when(i18n.message(any(Locale.class), eq("metric.classes.name"), anyString())).thenReturn("Classes");
    when(periods.label(snapshot, 1)).thenReturn("since someday");

    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "30", 1) // generates warning because classes increases of 40,
      // which is greater than 30
    ));

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.WARN, "Classes variation > 30 since someday")));
  }

  @Test
  public void shouldLabelAlertForNewMetricDoNotContainsVariationWord() {
    Metric newMetric = new Metric.Builder("new_metric_key", "New Metric", Metric.ValueType.INT).create();
    Measure measure = new Measure(newMetric, 15d);
    measure.setVariation1(50d);
    when(context.getMeasure(newMetric)).thenReturn(measure);
    measureClasses.setVariation1(40d);

    when(i18n.message(any(Locale.class), eq("metric.new_metric_key.name"), anyString())).thenReturn("New Measure");
    when(periods.label(snapshot, 1)).thenReturn("since someday");

    projectAlerts.addAll(Lists.newArrayList(
      new Alert(null, newMetric, Alert.OPERATOR_GREATER, null, "30", 1) // generates warning because classes increases of 40, which is
      // greater than 30
    ));

    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.WARN, "New Measure > 30 since someday")));
  }

  @Test
  public void alert_on_work_duration() {
    Metric metric = new Metric.Builder("tech_debt", "Debt", Metric.ValueType.WORK_DUR).create();

    // metric name is declared in l10n bundle
    when(i18n.message(any(Locale.class), eq("metric.tech_debt.name"), anyString())).thenReturn("The Debt");
    when(durations.format(any(Locale.class), eq(Duration.create(3600L)), eq(Durations.DurationFormat.SHORT))).thenReturn("1h");

    when(context.getMeasure(metric)).thenReturn(new Measure(metric, 1800d));
    projectAlerts.addAll(Lists.newArrayList(new Alert(null, metric, Alert.OPERATOR_SMALLER, "3600", null)));
    verifier.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, "The Debt < 1h")));
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

}
