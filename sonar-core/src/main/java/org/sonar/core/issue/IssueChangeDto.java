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
package org.sonar.core.issue;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.Date;

/**
 * @since 3.6
 */
public final class IssueChangeDto {

  private Long id;
  private String issueUuid;
  private Long userId;
  private String changeType;
  private String changeData;
  private String message;
  private Date createdAt;
  private Date updatedAt;

  public Long getId() {
    return id;
  }

  public IssueChangeDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getIssueUuid() {
    return issueUuid;
  }

  public IssueChangeDto setIssueUuid(String issueUuid) {
    this.issueUuid = issueUuid;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public IssueChangeDto setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  public String getChangeType() {
    return changeType;
  }

  public IssueChangeDto setChangeType(String changeType) {
    this.changeType = changeType;
    return this;
  }

  public String getChangeData() {
    return changeData;
  }

  public IssueChangeDto setChangeData(String changeData) {
    this.changeData = changeData;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public IssueChangeDto setMessage(String message) {
    this.message = message;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public IssueChangeDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public IssueChangeDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
