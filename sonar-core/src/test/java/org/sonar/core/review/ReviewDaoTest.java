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
package org.sonar.core.review;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.DaoTestCase;

import com.google.common.collect.Lists;

public class ReviewDaoTest extends DaoTestCase {

  private ReviewDao dao;

  @Before
  public void createDao() {
    dao = new ReviewDao(getMyBatis());
  }

  @Test
  public void shouldSelectById() {
    setupData("shared");

    ReviewDto reviewDto = dao.selectById(100L);
    assertThat(reviewDto.getId(), is(100L));
    assertThat(reviewDto.getStatus(), is("OPEN"));
    assertThat(reviewDto.getResolution(), is(nullValue()));
    assertThat(reviewDto.getProjectId(), is(20));
    assertThat(reviewDto.getViolationPermanentId(), is(1));
    assertThat(reviewDto.getSeverity(), is("BLOCKER"));
    assertThat(reviewDto.getUserId(), is(300));
    assertThat(reviewDto.getResourceId(), is(400));
    assertThat(reviewDto.getRuleId(), is(500));
    assertThat(reviewDto.getManualViolation(), is(true));
  }

  @Test
  public void shouldReturnNullIfIdNotFound() {
    setupData("shared");

    assertNull(dao.selectById(12345L));
  }

  @Test
  public void shouldSelectByQuery() {
    setupData("shared");

    List<ReviewDto> reviewDtos = dao.selectByQuery(ReviewQuery.create().setResourceId(400));
    assertThat(reviewDtos.size(), is(2));
    for (ReviewDto reviewDto : reviewDtos) {
      assertThat(reviewDto.getId(), anyOf(is(100L), is(101L)));
      assertThat(reviewDto.getResourceId(), is(400));
    }
  }

  @Test
  public void shouldSelectByQueryWithStatuses() {
    setupData("shared");

    List<ReviewDto> reviewDtos = dao.selectByQuery(ReviewQuery.create().addStatus(ReviewDto.STATUS_OPEN)
        .addStatus(ReviewDto.STATUS_REOPENED));
    assertThat(reviewDtos.size(), is(3));
    for (ReviewDto reviewDto : reviewDtos) {
      assertThat(reviewDto.getId(), anyOf(is(100L), is(102L), is(103L)));
    }
  }

  @Test
  public void shouldSelectByQueryWithResolutions() {
    setupData("shared");

    List<ReviewDto> reviewDtos = dao.selectByQuery(ReviewQuery.create().addResolution(ReviewDto.RESOLUTION_FALSE_POSITIVE)
        .addResolution(ReviewDto.RESOLUTION_FIXED));
    assertThat(reviewDtos.size(), is(2));
    for (ReviewDto reviewDto : reviewDtos) {
      assertThat(reviewDto.getId(), anyOf(is(101L), is(104L)));
    }
  }

  @Test
  public void shouldSelectByQueryWithNoAssignee() {
    setupData("shared");

    List<ReviewDto> reviewDtos = dao.selectByQuery(ReviewQuery.create().setNoAssignee());
    assertThat(reviewDtos.size(), is(2));
    for (ReviewDto reviewDto : reviewDtos) {
      assertThat(reviewDto.getId(), anyOf(is(101L), is(103L)));
    }
  }

  @Test
  public void shouldSelectByQueryWithPlanned() {
    setupData("shared");

    List<ReviewDto> reviewDtos = dao.selectByQuery(ReviewQuery.create().setPlanned());
    assertThat(reviewDtos.size(), is(2));
    for (ReviewDto reviewDto : reviewDtos) {
      assertThat(reviewDto.getId(), anyOf(is(100L), is(101L)));
    }
  }

  @Test
  public void shouldCountByQuery() {
    setupData("shared");

    Integer count = dao.countByQuery(ReviewQuery.create().addStatus(ReviewDto.STATUS_OPEN)
        .addStatus(ReviewDto.STATUS_REOPENED));
    assertThat(count, is(3));
  }

  @Test
  public void shouldSelectByQueryWithBooleanCriteria() {
    setupData("shared");

    List<ReviewDto> reviewDtos = dao.selectByQuery(ReviewQuery.create().setResourceId(400).setManualViolation(true));
    assertThat(reviewDtos.size(), is(1));
    assertThat(reviewDtos.get(0).getId(), is(100L));
    assertThat(reviewDtos.get(0).getManualViolation(), is(Boolean.TRUE));
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

    // test select query
    List<ReviewDto> reviewDtos = dao.selectByQuery(query);

    assertThat(reviewDtos.size(), is(3));
    assertThat(reviewDtos, hasItem(new ReviewMatcherByViolationPermanentId(100)));
    assertThat(reviewDtos, hasItem(new ReviewMatcherByViolationPermanentId(1300)));
    assertThat(reviewDtos, hasItem(new ReviewMatcherByViolationPermanentId(3200)));
    
    // and test count query
    assertThat(dao.countByQuery(query), is(3));
  }

  static class ReviewMatcherByViolationPermanentId extends BaseMatcher<ReviewDto> {
    Integer expectedId;

    ReviewMatcherByViolationPermanentId(Integer expectedId) {
      this.expectedId = expectedId;
    }

    public boolean matches(Object o) {
      ReviewDto reviewDto = (ReviewDto) o;
      return expectedId.equals(reviewDto.getViolationPermanentId());
    }

    public void describeTo(Description description) {
      description.appendText("violationPermanentId").appendValue(expectedId);
    }
  }
}
