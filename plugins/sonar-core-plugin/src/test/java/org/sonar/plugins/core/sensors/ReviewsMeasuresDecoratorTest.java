/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Method;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReviewsMeasuresDecoratorTest {

  private ReviewDao reviewDao;
  private ReviewsMeasuresDecorator decorator;
  private DecoratorContext context;

  private Date rightNow;
  private Date tenDaysAgo;
  private Date fiveDaysAgo;

  @Before
  public void setUp() {
    reviewDao = mock(ReviewDao.class);

    rightNow = new Date();
    tenDaysAgo = DateUtils.addDays(rightNow, -10);
    fiveDaysAgo = DateUtils.addDays(rightNow, -5);

    PastSnapshot pastSnapshot = mock(PastSnapshot.class);
    when(pastSnapshot.getIndex()).thenReturn(1);
    when(pastSnapshot.getTargetDate()).thenReturn(fiveDaysAgo);

    PastSnapshot pastSnapshot2 = mock(PastSnapshot.class);
    when(pastSnapshot2.getIndex()).thenReturn(2);
    when(pastSnapshot2.getTargetDate()).thenReturn(tenDaysAgo);

    TimeMachineConfiguration timeMachineConfiguration = mock(TimeMachineConfiguration.class);
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(Arrays.asList(pastSnapshot, pastSnapshot2));

    decorator = new ReviewsMeasuresDecorator(reviewDao, timeMachineConfiguration);
    context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.VIOLATIONS)).thenReturn(new Measure(CoreMetrics.VIOLATIONS, 35d));
  }

  @Test
  public void shouldExecuteOnProject() {
    ReviewsMeasuresDecorator decorator = new ReviewsMeasuresDecorator(null, null);
    Project project = new Project("foo");
    project.setLatestAnalysis(true);
    assertThat(decorator.shouldExecuteOnProject(project), is(true));
  }

  @Test
  public void shouldNotDecoratePersistableResource() throws Exception {
    ReviewsMeasuresDecorator decorator = new ReviewsMeasuresDecorator(null, null);
    DecoratorContext context = mock(DecoratorContext.class);
    Resource<?> resource = Method.createMethod("foo", null).setId(1);
    decorator.decorate(resource, context);
    verify(context, never()).saveMeasure(any(Metric.class), anyDouble());
  }

  /**
   * SONAR-3746
   */
  @Test
  public void shouldDecorateUnitTest() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    File resource = new File("foo");
    resource.setQualifier(Qualifiers.UNIT_TEST_FILE);
    resource.setId(1);
    decorator.decorate(resource, context);
    verify(context, atLeast(1)).saveMeasure(any(Metric.class), anyDouble());
  }

  @Test
  public void shouldTrackNewViolationsWithoutReview() throws Exception {
    Resource<?> resource = new File("foo").setId(1);
    Violation v1 = Violation.create((Rule) null, resource).setPermanentId(1); // test the null case for the created_at date
    Violation v2 = Violation.create((Rule) null, resource).setPermanentId(2).setCreatedAt(rightNow);
    Violation v3 = Violation.create((Rule) null, resource).setPermanentId(3).setCreatedAt(fiveDaysAgo);
    Violation v4 = Violation.create((Rule) null, resource).setPermanentId(4).setCreatedAt(fiveDaysAgo);
    Violation v5 = Violation.create((Rule) null, resource).setPermanentId(5).setCreatedAt(fiveDaysAgo);
    Violation v6 = Violation.create((Rule) null, resource).setPermanentId(6).setCreatedAt(tenDaysAgo);
    when(context.getViolations()).thenReturn(Arrays.asList(v1, v2, v3, v4, v5, v6));

    Map<Integer, ReviewDto> openReviewsByViolationPermanentIds = Maps.newHashMap();
    openReviewsByViolationPermanentIds.put(1, new ReviewDto());
    openReviewsByViolationPermanentIds.put(3, new ReviewDto());

    decorator.trackNewViolationsWithoutReview(context, openReviewsByViolationPermanentIds);
    verify(context).saveMeasure(argThat(new IsVariationMeasure(CoreMetrics.NEW_UNREVIEWED_VIOLATIONS, 1.0, 3.0)));
  }

  private class IsVariationMeasure extends BaseMatcher<Measure> {
    private Metric metric = null;
    private Double var1 = null;
    private Double var2 = null;

    public IsVariationMeasure(Metric metric, Double var1, Double var2) {
      this.metric = metric;
      this.var1 = var1;
      this.var2 = var2;
    }

    public boolean matches(Object o) {
      if (!(o instanceof Measure)) {
        return false;
      }
      Measure m = (Measure) o;
      return ObjectUtils.equals(metric, m.getMetric()) &&
        ObjectUtils.equals(var1, m.getVariation1()) &&
        ObjectUtils.equals(var2, m.getVariation2()) &&
        !(m instanceof RuleMeasure);
    }

    public void describeTo(Description o) {
    }
  }
}
