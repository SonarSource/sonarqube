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
package org.sonar.server.notifications.reviews;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.User;
import org.sonar.jpa.entity.Review;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

public class ReviewsNotificationManagerTest extends AbstractDbUnitTestCase {

  private ReviewsNotificationManager manager;

  @Before
  public void setUp() {
    setupData(getClass().getResourceAsStream("fixture.xml"));
    manager = new ReviewsNotificationManager(getSessionFactory(), null);
  }

  @Test
  public void shouldGetReviewById() {
    Review review = manager.getReviewById(3L);
    assertThat(review.getUserId(), is(1));
    assertThat(review.getAssigneeId(), is(2));
    assertThat(review.getTitle(), is("Review #3"));
  }

  @Test
  public void shouldGetUserById() {
    User user = manager.getUserById(1);
    assertThat(user.getLogin(), is("simon"));
    assertThat(user.getName(), is("Simon Brandhof"));
    assertThat(user.getEmail(), is("simon.brandhof@sonarsource.com"));
  }

}
