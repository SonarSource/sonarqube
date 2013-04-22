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
package org.sonar.wsclient.services;

/**
 * @since 2.9
 */
public final class ReviewUpdateQuery extends UpdateQuery<Review> {

  private long reviewId;
  private String action;
  private String comment;
  private String assignee;
  private String resolution;

  /**
   * Creates query to add comment to review.
   */
  public static ReviewUpdateQuery addComment(long id, String comment) {
    return new ReviewUpdateQuery(id, "add_comment").setComment(comment);
  }

  /**
   * Creates query to reassign review.
   */
  public static ReviewUpdateQuery reassign(long id, String assignee) {
    return new ReviewUpdateQuery(id, "reassign").setAssignee(assignee);
  }

  /**
   * Creates query to resolve review.
   * If resolution "FALSE-POSITIVE", then you must provide comment using {@link #setComment(String)}.
   * Otherwise comment is optional.
   * 
   * @param resolution
   *          can be "FIXED" or "FALSE-POSITIVE"
   */
  public static ReviewUpdateQuery resolve(long id, String resolution) {
    return new ReviewUpdateQuery(id, "resolve").setResolution(resolution);
  }

  /**
   * Creates query to reopen review.
   * If review was resolved as "FALSE-POSITIVE", then you must provide comment using {@link #setComment(String)}.
   * Otherwise comment is optional.
   */
  public static ReviewUpdateQuery reopen(long id) {
    return new ReviewUpdateQuery(id, "reopen");
  }

  private ReviewUpdateQuery(long id, String action) {
    this.reviewId = id;
    this.action = action;
  }

  public long getReviewId() {
    return reviewId;
  }

  public ReviewUpdateQuery setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public String getComment() {
    return comment;
  }

  public String getAssignee() {
    return assignee;
  }

  public ReviewUpdateQuery setAssignee(String userLogin) {
    this.assignee = userLogin;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  /**
   * @param resolution
   *          can be "FIXED" or "FALSE-POSITIVE"
   */
  public ReviewUpdateQuery setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder();
    url.append(ReviewQuery.BASE_URL)
        .append('/').append(action)
        .append('/').append(reviewId)
        .append('?');
    appendUrlParameter(url, "assignee", getAssignee());
    appendUrlParameter(url, "resolution", getResolution());
    return url.toString();
  }

  /**
   * Property {@link #comment} transmitted through request body as content may exceed URL size allowed by the server.
   */
  @Override
  public String getBody() {
    return comment;
  }

  @Override
  public Class<Review> getModelClass() {
    return Review.class;
  }
}
