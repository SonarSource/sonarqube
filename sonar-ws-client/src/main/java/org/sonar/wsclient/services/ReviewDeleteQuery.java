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
package org.sonar.wsclient.services;

/**
 * @since 2.9
 */
public class ReviewDeleteQuery extends DeleteQuery<Review> {

  private Long reviewId;
  private Long commentId;

  public ReviewDeleteQuery() {
  }

  /**
   * Builds a request that will delete the comment of a review. For the moment, only the last comment can be deleted, and only the author of
   * this comment is allowed to do so.
   * 
   * @param reviewId
   *          the id of the review
   * @param commentId
   *          the id of the comment to delete
   */
  public static ReviewDeleteQuery deleteCommentQuery(Long reviewId, Long commentId) {
    ReviewDeleteQuery query = new ReviewDeleteQuery();
    query.reviewId = reviewId;
    query.commentId = commentId;
    return query;
  }

  public Long getReviewId() {
    return reviewId;
  }

  public ReviewDeleteQuery setReviewId(Long reviewId) {
    this.reviewId = reviewId;
    return this;
  }

  public Long getCommentId() {
    return commentId;
  }

  public ReviewDeleteQuery setCommentId(Long commentId) {
    this.commentId = commentId;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder();
    url.append(ReviewQuery.BASE_URL);
    url.append("/");
    url.append('?');
    appendUrlParameter(url, "id", getReviewId());
    appendUrlParameter(url, "comment_id", getCommentId());
    return url.toString();
  }
}
