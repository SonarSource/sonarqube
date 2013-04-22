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
package org.sonar.core.review;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Date;

public class ReviewCommentDaoTest extends AbstractDaoTestCase {

  private ReviewCommentDao dao;

  @Before
  public void createDao() {
    dao = new ReviewCommentDao(getMyBatis());
  }

  @Test
  public void shouldFindReviewById() {
    setupData("insert");

    ReviewCommentDto reviewCommentDto = new ReviewCommentDto();
    reviewCommentDto.setReviewId(12L);
    reviewCommentDto.setUserId(8L);
    reviewCommentDto.setText("Hello");
    Date today = new Date();
    reviewCommentDto.setCreatedAt(today);
    reviewCommentDto.setUpdatedAt(today);

    dao.insert(reviewCommentDto);

    checkTables("insert", new String[] {"id", "created_at", "updated_at"}, "review_comments");
  }
}
