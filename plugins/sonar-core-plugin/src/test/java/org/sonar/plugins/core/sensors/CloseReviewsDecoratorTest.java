/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import junit.framework.ComparisonFailure;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.security.UserFinder;
import org.sonar.jpa.entity.Review;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CloseReviewsDecoratorTest extends AbstractDbUnitTestCase {

  private NotificationManager notificationManager;
  private CloseReviewsDecorator reviewsDecorator;

  @Before
  public void setUp() {
    Project project = mock(Project.class);
    when(project.getRoot()).thenReturn(project);
    notificationManager = mock(NotificationManager.class);
    reviewsDecorator = new CloseReviewsDecorator(project, null, getSession(), notificationManager, mock(UserFinder.class));
  }

  @Test
  public void testShouldExecuteOnProject() throws Exception {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(true);
    assertTrue(reviewsDecorator.shouldExecuteOnProject(project));
  }

  @Test
  public void shouldCloseReviewWithoutCorrespondingViolation() throws Exception {
    setupData("fixture");

    int count = reviewsDecorator.closeReviewsOnResolvedViolations(null, 666, 222);

    assertThat(count, is(3));
    verify(notificationManager, times(3)).scheduleForSending(any(Notification.class));
    checkTables("shouldCloseReviewWithoutCorrespondingViolation", new String[]{"updated_at"}, "reviews");

    try {
      checkTables("shouldCloseReviewWithoutCorrespondingViolation", "reviews");
      fail("'updated_at' columns are identical whereas they should be different.");
    } catch (ComparisonFailure e) {
      // "updated_at" column must be different, so the comparison should raise this exception
    }
  }

  @Test
  public void shouldReopenResolvedReviewWithNonFixedViolation() throws Exception {
    setupData("fixture");

    // First we close the reviews for which the violations have been fixed (this is because we use the same "fixture"...)
    reviewsDecorator.closeReviewsOnResolvedViolations(null, 666, 222);

    // And now we reopen the reviews that still have a violation
    int count = reviewsDecorator.reopenReviewsOnUnresolvedViolations(null, 666);

    assertThat(count, is(1));
    verify(notificationManager, times(4)).scheduleForSending(any(Notification.class));
    checkTables("shouldReopenResolvedReviewWithNonFixedViolation", new String[]{"updated_at"}, "reviews");

    try {
      checkTables("shouldReopenResolvedReviewWithNonFixedViolation", "reviews");
      fail("'updated_at' columns are identical whereas they should be different.");
    } catch (ComparisonFailure e) {
      // "updated_at" column must be different, so the comparison should raise this exception
    }
  }

  @Test
  public void shouldCloseResolvedManualViolations() throws Exception {
    setupData("shouldCloseResolvedManualViolations");

    int count = reviewsDecorator.closeReviewsOnResolvedViolations(null, 555, 11);
    assertThat(count, is(1));

    verify(notificationManager, times(1)).scheduleForSending(any(Notification.class));

    getSession().commit();
    assertThat(getSession().getSingleResult(Review.class, "id", 1L).getStatus(), is("CLOSED")); // automatic violation not changed
    assertThat(getSession().getSingleResult(Review.class, "id", 2L).getStatus(), is("CLOSED")); // manual violation resolved -> closed
    assertThat(getSession().getSingleResult(Review.class, "id", 3L).getStatus(), is("OPEN"));   // manual violation not changed
  }
}
