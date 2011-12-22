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
package org.sonar.core.review;

import com.google.common.collect.Lists;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.DaoTestCase;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

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
    assertThat(reviewDto.getResolution(), is("RESOLVE"));
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
  public void shouldSelectByResource() {
    setupData("shared");

    List<ReviewDto> reviewDtos = dao.selectByResource(400);
    assertThat(reviewDtos.size(), is(2));
    for (ReviewDto reviewDto : reviewDtos) {
      assertThat(reviewDto.getId(), anyOf(is(100L), is(101L)));
      assertThat(reviewDto.getResourceId(), is(400));
    }
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
  public void shouldSelectByQuery_booleanCriteria() {
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

    List<ReviewDto> reviewDtos = dao.selectByQuery(query);

    assertThat(reviewDtos.size(), is(3));
    assertThat(reviewDtos, hasItem(new ReviewMatcherByViolationPermanentId(100)));
    assertThat(reviewDtos, hasItem(new ReviewMatcherByViolationPermanentId(1300)));
    assertThat(reviewDtos, hasItem(new ReviewMatcherByViolationPermanentId(3200)));
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
