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
package org.sonar.core.workflow;

import com.google.common.annotations.VisibleForTesting;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertiesMapper;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.review.ReviewCommentDto;
import org.sonar.core.review.ReviewCommentMapper;
import org.sonar.core.review.ReviewDto;
import org.sonar.core.review.ReviewMapper;
import org.sonar.api.workflow.Comment;
import org.sonar.api.workflow.internal.DefaultReview;
import java.util.Date;
import java.util.List;

public class ReviewDatabaseStore implements ReviewStore, ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(ReviewDatabaseStore.class);

  private MyBatis mybatis;

  public ReviewDatabaseStore(MyBatis mb) {
    this.mybatis = mb;
  }

  public void store(DefaultReview review) {
    store(review, new Date());
  }

  @VisibleForTesting
  void store(DefaultReview review, Date now) {
    if (review.getReviewId() == null) {
      LOG.error("Review has no id. Violation id is: " + review.getViolationId());
      return;
    }

    SqlSession session = mybatis.openSession();
    ReviewMapper mapper = session.getMapper(ReviewMapper.class);
    ReviewCommentMapper commentMapper = session.getMapper(ReviewCommentMapper.class);
    try {
      ReviewDto dto = mapper.findById(review.getReviewId());
      dto.setResolution(review.getResolution());
      dto.setStatus(review.getStatus());
      dto.setData(KeyValueFormat.format(review.getProperties()));
      dto.setUpdatedAt(now);
      mapper.update(dto);

      for (Comment comment : review.getNewComments()) {
        ReviewCommentDto commentDto = new ReviewCommentDto();
        commentDto.setReviewId(dto.getId());
        commentDto.setText(comment.getMarkdownText());
        commentDto.setCreatedAt(now);
        commentDto.setUpdatedAt(now);
        commentDto.setUserId(comment.getUserId());
        commentMapper.insert(commentDto);
      }
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void completeProjectSettings(Long projectId, Settings settings, List<String> propertyKeys) {
    if (propertyKeys.isEmpty()) {
      return;
    }

    SqlSession session = mybatis.openSession();
    PropertiesMapper mapper = session.getMapper(PropertiesMapper.class);
    try {
      List<PropertyDto> dtos = mapper.selectSetOfResourceProperties(projectId, propertyKeys);
      for (PropertyDto dto : dtos) {
        settings.setProperty(dto.getKey(), dto.getValue());
      }

    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
