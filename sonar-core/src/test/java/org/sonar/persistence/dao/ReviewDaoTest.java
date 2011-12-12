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
package org.sonar.persistence.dao;

import com.google.common.collect.Lists;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.persistence.model.Review;
import org.sonar.persistence.model.ReviewQuery;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ReviewDaoTest extends DaoTestCase {

  private ReviewDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new ReviewDao(getMyBatis());
  }

  @Test
  public void shouldSelectById() throws Exception {
    setupData("shared");

    Review review = dao.selectById(100L);
    assertThat(review.getId(), is(100L));
    assertThat(review.getStatus(), is("OPEN"));
    assertThat(review.getResolution(), is("RESOLVE"));
    assertThat(review.getProjectId(), is(20));
    assertThat(review.getViolationPermanentId(), is(1));
    assertThat(review.getSeverity(), is("BLOCKER"));
    assertThat(review.getUserId(), is(300));
    assertThat(review.getResourceId(), is(400));
    assertThat(review.getRuleId(), is(500));
    assertThat(review.getManualViolation(), is(true));
  }

  @Test
  public void shouldReturnNullIfIdNotFound() throws Exception {
    setupData("shared");

    assertNull(dao.selectById(12345L));
  }

  @Test
  public void shouldSelectByResource() throws Exception {
    setupData("shared");

    List<Review> reviews = dao.selectByResource(400);
    assertThat(reviews.size(), is(2));
    for (Review review : reviews) {
      assertThat(review.getId(), anyOf(is(100L), is(101L)));
      assertThat(review.getResourceId(), is(400));
    }
  }

  @Test
  public void shouldSelectByQuery() throws Exception {
    setupData("shared");

    List<Review> reviews = dao.selectByQuery(ReviewQuery.create().setResourceId(400));
    assertThat(reviews.size(), is(2));
    for (Review review : reviews) {
      assertThat(review.getId(), anyOf(is(100L), is(101L)));
      assertThat(review.getResourceId(), is(400));
    }
  }

  @Test
  public void shouldSelectByQuery_booleanCriteria() throws Exception {
    setupData("shared");

    List<Review> reviews = dao.selectByQuery(ReviewQuery.create().setResourceId(400).setManualViolation(true));
    assertThat(reviews.size(), is(1));
    assertThat(reviews.get(0).getId(), is(100L));
    assertThat(reviews.get(0).getManualViolation(), is(Boolean.TRUE));
  }

  /**
   * Oracle limitation of IN statements....
   */
  @Test
  public void shouldPartitionFiltersOnPermanentId() {
    setupData("shouldPartitionFiltersOnPermanentId");
    List<Integer> permanentIds = Lists.newArrayList();
    for (int index = 1; index < 3500; index++) {
      permanentIds.add(index);
    }
    ReviewQuery query = ReviewQuery.create().setViolationPermanentIds(permanentIds);

    List<Review> reviews = dao.selectByQuery(query);

    assertThat(reviews.size(), is(3));
    assertThat(reviews, hasItem(new ReviewMatcherByViolationPermanentId(100)));
    assertThat(reviews, hasItem(new ReviewMatcherByViolationPermanentId(1300)));
    assertThat(reviews, hasItem(new ReviewMatcherByViolationPermanentId(3200)));
  }

  static class ReviewMatcherByViolationPermanentId extends BaseMatcher<Review> {
    Integer expectedId;

    ReviewMatcherByViolationPermanentId(Integer expectedId) {
      this.expectedId = expectedId;
    }

    public boolean matches(Object o) {
      Review review = (Review) o;
      System.out.println(review.getViolationPermanentId());

      return expectedId.equals(review.getViolationPermanentId());
    }

    public void describeTo(Description description) {
      description.appendText("violationPermanentId").appendValue(expectedId);
    }
  }
}
