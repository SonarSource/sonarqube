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
package org.sonar.plugins.core.sensors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;
import org.sonar.core.review.ReviewQuery;
import org.sonar.plugins.core.timemachine.ViolationTrackingDecorator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
    when(reviewDao.selectByQuery(argThat(openReviewQueryMatcher()))).thenReturn(createListOf10Reviews());
    when(reviewDao.countByQuery(argThat(unassignedReviewQueryMatcher()))).thenReturn(2);
    when(reviewDao.countByQuery(argThat(plannedReviewQueryMatcher()))).thenReturn(3);
    when(reviewDao.countByQuery(argThat(falsePositiveReviewQueryMatcher()))).thenReturn(4);

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
  public void testDependsUponViolationTracking() throws Exception {
    ReviewsMeasuresDecorator decorator = new ReviewsMeasuresDecorator(null, null);
    assertEquals(decorator.dependsUponViolationTracking(), ViolationTrackingDecorator.class);
  }

  @Test
  public void shouldExecuteOnProject() throws Exception {
    ReviewsMeasuresDecorator decorator = new ReviewsMeasuresDecorator(null, null);
    Project project = new Project("foo");
    project.setLatestAnalysis(true);
    assertThat(decorator.shouldExecuteOnProject(project), is(true));
  }

  @Test
  public void shouldDecoratePersistableResource() throws Exception {
    ReviewsMeasuresDecorator decorator = new ReviewsMeasuresDecorator(null, null);
    DecoratorContext context = mock(DecoratorContext.class);
    Resource<?> resource = mock(Resource.class);
    when(resource.getScope()).thenReturn(Scopes.BLOCK_UNIT);
    decorator.decorate(resource, context);
    verify(context, never()).saveMeasure(any(Metric.class), anyDouble());
  }

  @Test
  public void shouldNotDecorateUnitTest() throws Exception {
    ReviewsMeasuresDecorator decorator = new ReviewsMeasuresDecorator(null, null);
    DecoratorContext context = mock(DecoratorContext.class);
    Resource<?> resource = mock(Resource.class);
    when(resource.getScope()).thenReturn(Scopes.FILE);
    when(resource.getQualifier()).thenReturn(Qualifiers.UNIT_TEST_FILE);
    decorator.decorate(resource, context);
    verify(context, never()).saveMeasure(any(Metric.class), anyDouble());
  }

  @Test
  public void shouldComputeReviewsMetricsOnFile() throws Exception {
    Resource<?> resource = new File("foo").setId(1);
    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.ACTIVE_REVIEWS, 10d);
    verify(context).saveMeasure(CoreMetrics.UNASSIGNED_REVIEWS, 2d);
    verify(context).saveMeasure(CoreMetrics.UNPLANNED_REVIEWS, 7d);
    verify(context).saveMeasure(CoreMetrics.FALSE_POSITIVE_REVIEWS, 4d);
    verify(context).saveMeasure(CoreMetrics.UNREVIEWED_VIOLATIONS, 35d - 10d);
  }

  @Test
  public void shouldComputeReviewsMetricsOnProject() throws Exception {
    when(context.getChildrenMeasures(CoreMetrics.ACTIVE_REVIEWS)).thenReturn(
        Lists.newArrayList(new Measure(CoreMetrics.ACTIVE_REVIEWS, 7d)));
    when(context.getChildrenMeasures(CoreMetrics.UNASSIGNED_REVIEWS)).thenReturn(
        Lists.newArrayList(new Measure(CoreMetrics.UNASSIGNED_REVIEWS, 1d)));
    when(context.getChildrenMeasures(CoreMetrics.UNPLANNED_REVIEWS)).thenReturn(
        Lists.newArrayList(new Measure(CoreMetrics.UNPLANNED_REVIEWS, 2d)));
    when(context.getChildrenMeasures(CoreMetrics.FALSE_POSITIVE_REVIEWS)).thenReturn(
        Lists.newArrayList(new Measure(CoreMetrics.FALSE_POSITIVE_REVIEWS, 2d)));

    Resource<?> resource = new Project("foo").setId(1);
    decorator.decorate(resource, context);

    // As same values used for #shouldComputeReviewMetricsOnFile, we just add the children measures to verify
    verify(context).saveMeasure(CoreMetrics.ACTIVE_REVIEWS, 10d + 7d);
    verify(context).saveMeasure(CoreMetrics.UNASSIGNED_REVIEWS, 2d + 1d);
    verify(context).saveMeasure(CoreMetrics.UNPLANNED_REVIEWS, 7d + 2d);
    verify(context).saveMeasure(CoreMetrics.FALSE_POSITIVE_REVIEWS, 4d + 2d);
    verify(context).saveMeasure(CoreMetrics.UNREVIEWED_VIOLATIONS, 35d - (10d + 7d));
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

  private List<ReviewDto> createListOf10Reviews() {
    List<ReviewDto> reviews = Lists.newArrayList();
    for (int i = 1; i < 11; i++) {
      reviews.add(new ReviewDto().setViolationPermanentId(i));
    }
    return reviews;
  }

  private BaseMatcher<ReviewQuery> openReviewQueryMatcher() {
    return new BaseMatcher<ReviewQuery>() {
      public boolean matches(Object o) {
        ReviewQuery query = (ReviewQuery) o;
        if (query == null) {
          return false;
        }
        return Lists.newArrayList(ReviewDto.STATUS_OPEN, ReviewDto.STATUS_REOPENED).equals(query.getStatuses())
          && query.getNoAssignee() == null && query.getPlanned() == null;
      }

      public void describeTo(Description description) {
      }
    };
  }

  private BaseMatcher<ReviewQuery> unassignedReviewQueryMatcher() {
    return new BaseMatcher<ReviewQuery>() {
      public boolean matches(Object o) {
        ReviewQuery query = (ReviewQuery) o;
        if (query == null) {
          return false;
        }
        return Lists.newArrayList(ReviewDto.STATUS_OPEN, ReviewDto.STATUS_REOPENED).equals(query.getStatuses())
          && query.getNoAssignee() == Boolean.TRUE;
      }

      public void describeTo(Description description) {
      }
    };
  }

  private BaseMatcher<ReviewQuery> plannedReviewQueryMatcher() {
    return new BaseMatcher<ReviewQuery>() {
      public boolean matches(Object o) {
        ReviewQuery query = (ReviewQuery) o;
        if (query == null) {
          return false;
        }
        return Lists.newArrayList(ReviewDto.STATUS_OPEN, ReviewDto.STATUS_REOPENED).equals(query.getStatuses())
          && query.getPlanned() == Boolean.TRUE;
      }

      public void describeTo(Description description) {
      }
    };
  }

  private BaseMatcher<ReviewQuery> falsePositiveReviewQueryMatcher() {
    return new BaseMatcher<ReviewQuery>() {
      public boolean matches(Object o) {
        ReviewQuery query = (ReviewQuery) o;
        if (query == null) {
          return false;
        }
        return Lists.newArrayList(ReviewDto.RESOLUTION_FALSE_POSITIVE).equals(query.getResolutions());
      }

      public void describeTo(Description description) {
      }
    };
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
