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
package org.sonar.core.review;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ReviewDaoTest extends AbstractDaoTestCase {

  private ReviewDao dao;

  @Before
  public void createDao() {
    dao = new ReviewDao(getMyBatis());
  }

  @Test
  public void shouldFindReviewById() {
    setupData("shared");

    ReviewDto review = dao.findById(100L);
    assertThat(review.getId(), is(100L));
    assertThat(review.getStatus(), is("OPEN"));
    assertThat(review.getResolution(), is(nullValue()));
    assertThat(review.getProjectId(), is(20));
    assertThat(review.getViolationPermanentId(), is(1));
    assertThat(review.getSeverity(), is("BLOCKER"));
    assertThat(review.getUserId(), is(300));
    assertThat(review.getResourceId(), is(400));
    assertThat(review.getRuleId(), is(500));
    assertThat(review.getManualViolation(), is(true));
    assertThat(review.getActionPlanId(), is(1));
  }

  @Test
  public void shouldSelectOpenByResourceId() {
    setupData("shared");

    // only a single review is open on this resource
    Collection<ReviewDto> reviews = dao.selectOpenByResourceId(400L);
    assertThat(reviews.size(), is(1));
    ReviewDto review = reviews.iterator().next();
    assertThat(review.getId(), is(100L));
    assertThat(review.getStatus(), is("OPEN"));
    assertThat(review.getResolution(), is(nullValue()));
    assertThat(review.getProjectId(), is(20));
    assertThat(review.getViolationPermanentId(), is(1));
    assertThat(review.getSeverity(), is("BLOCKER"));
    assertThat(review.getUserId(), is(300));
    assertThat(review.getResourceId(), is(400));
    assertThat(review.getRuleId(), is(500));
    assertThat(review.getManualViolation(), is(true));
    assertThat(review.getActionPlanId(), is(1));
  }

  @Test
  public void shouldReturnEmptyCollectionIfResourceNotFound() {
    setupData("shared");
    assertThat(dao.selectOpenByResourceId(123456789L).isEmpty(), is(true));
  }

  @Test
  public void shouldFilterResults() {
    setupData("shared");
    Collection<ReviewDto> reviews = dao.selectOpenByResourceId(401L,
        ReviewPredicates.status(ReviewDto.STATUS_REOPENED));

    assertThat(reviews.size(), is(1));
    ReviewDto review = reviews.iterator().next();
    assertThat(review.getId(), is(103L));
    assertThat(review.getStatus(), is(ReviewDto.STATUS_REOPENED));
  }

  @Test
  public void update() {
    setupData("update");
    Collection<ReviewDto> reviews = dao.selectOpenByResourceId(400L);
    ReviewDto review = reviews.iterator().next();
    review.setLine(1000);
    review.setResolution("NEW_RESOLUTION");
    review.setStatus("NEW_STATUS");
    review.setSeverity("NEW_SEV");
    review.setAssigneeId(1001L);
    review.setManualSeverity(true);
    review.setManualViolation(false);
    review.setTitle("NEW_TITLE");
    review.setCreatedAt(DateUtils.parseDate("2012-05-18"));
    review.setUpdatedAt(DateUtils.parseDate("2012-07-01"));
    review.setData("big=bang");

    dao.update(reviews);

    checkTables("update", "reviews");
  }
}
