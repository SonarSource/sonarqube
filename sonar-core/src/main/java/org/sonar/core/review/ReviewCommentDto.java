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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.Date;

/**
 * @since 3.1
 */
public final class ReviewCommentDto {

  private Long id;
  private Long reviewId;
  private Long userId;
  private String text;
  private Date createdAt;
  private Date updatedAt;

  public Long getId() {
    return id;
  }

  public ReviewCommentDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public ReviewCommentDto setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  public Long getReviewId() {
    return reviewId;
  }

  public ReviewCommentDto setReviewId(Long reviewId) {
    this.reviewId = reviewId;
    return this;
  }

  public String getText() {
    return text;
  }

  public ReviewCommentDto setText(String text) {
    this.text = text;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public ReviewCommentDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public ReviewCommentDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ReviewCommentDto reviewDto = (ReviewCommentDto) o;
    return !(id != null ? !id.equals(reviewDto.id) : reviewDto.id != null);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
