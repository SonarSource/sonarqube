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

import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;
import org.sonar.core.review.ReviewQuery;
import org.sonar.plugins.core.timemachine.ViolationTrackingDecorator;

import com.google.common.collect.Lists;

public class ReviewsMeasuresDecoratorTest {

  @Test
  public void testDependsUponViolationTracking() throws Exception {
    ReviewsMeasuresDecorator decorator = new ReviewsMeasuresDecorator(null);
    assertEquals(decorator.dependsUponViolationTracking(), ViolationTrackingDecorator.class);
  }

  @Test
  public void shouldExecuteOnProject() throws Exception {
    ReviewsMeasuresDecorator decorator = new ReviewsMeasuresDecorator(null);
    Project project = new Project("foo");
    project.setLatestAnalysis(true);
    assertThat(decorator.shouldExecuteOnProject(project), is(true));
  }

  @Test
  public void shouldDecorateOnlyFiles() throws Exception {
    ReviewsMeasuresDecorator decorator = new ReviewsMeasuresDecorator(null);
    DecoratorContext context = mock(DecoratorContext.class);
    Resource<?> resource = new Project("Foo");
    decorator.decorate(resource, context);
    verify(context, never()).saveMeasure(any(Metric.class), anyDouble());
  }

  @Test
  public void shouldComputeReviewMetrics() throws Exception {
    ReviewDao reviewDao = mock(ReviewDao.class);
    when(reviewDao.countByQuery(argThat(openReviewQueryMatcher()))).thenReturn(10);
    when(reviewDao.countByQuery(argThat(unassignedReviewQueryMatcher()))).thenReturn(2);
    when(reviewDao.countByQuery(argThat(plannedReviewQueryMatcher()))).thenReturn(3);
    when(reviewDao.countByQuery(argThat(falsePositiveReviewQueryMatcher()))).thenReturn(4);

    ReviewsMeasuresDecorator decorator = new ReviewsMeasuresDecorator(reviewDao);
    Resource<?> resource = new File("foo").setId(1);
    DecoratorContext context = mock(DecoratorContext.class);
    List<Violation> violations = mock(List.class);
    when(violations.size()).thenReturn(35);
    when(context.getViolations()).thenReturn(violations);
    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.ACTIVE_REVIEWS, 10d);
    verify(context).saveMeasure(CoreMetrics.UNASSIGNED_REVIEWS, 2d);
    verify(context).saveMeasure(CoreMetrics.UNPLANNED_REVIEWS, 7d);
    verify(context).saveMeasure(CoreMetrics.FALSE_POSITIVE_REVIEWS, 4d);
    verify(context).saveMeasure(CoreMetrics.VIOLATIONS_WITHOUT_REVIEW, 21d);
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
}
