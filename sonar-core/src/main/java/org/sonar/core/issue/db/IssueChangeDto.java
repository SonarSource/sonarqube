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
package org.sonar.core.issue.db;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.FieldDiffs;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Date;

/**
 * @since 3.6
 */
public final class IssueChangeDto implements Serializable {

  public static final String TYPE_FIELD_CHANGE = "diff";
  public static final String TYPE_COMMENT = "comment";

  private Long id;
  private String kee;
  private String issueKey;
  private String userLogin;
  private String changeType;
  private String changeData;
  private Date createdAt;
  private Date updatedAt;

  public Long getId() {
    return id;
  }

  public IssueChangeDto setId(Long id) {
    this.id = id;
    return this;
  }

  @CheckForNull
  public String getKey() {
    return kee;
  }

  public IssueChangeDto setKey(@Nullable String key) {
    this.kee = key;
    return this;
  }

  public String getIssueKey() {
    return issueKey;
  }

  public IssueChangeDto setIssueKey(String s) {
    this.issueKey = s;
    return this;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public IssueChangeDto setUserLogin(@Nullable String userLogin) {
    this.userLogin = userLogin;
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

  public static IssueChangeDto of(DefaultIssueComment comment) {
    IssueChangeDto dto = newDto(comment.issueKey());
    dto.setKey(comment.key());
    dto.setChangeType(IssueChangeDto.TYPE_COMMENT);
    dto.setChangeData(comment.markdownText());
    dto.setUserLogin(comment.userLogin());
    return dto;
  }

  public static IssueChangeDto of(String issueKey, FieldDiffs diffs) {
    IssueChangeDto dto = newDto(issueKey);
    dto.setChangeType(IssueChangeDto.TYPE_FIELD_CHANGE);
    dto.setChangeData(diffs.toString());
    dto.setUserLogin(diffs.userLogin());
    return dto;
  }

  private static IssueChangeDto newDto(String issueKey) {
    IssueChangeDto dto = new IssueChangeDto();
    dto.setIssueKey(issueKey);

    // technical dates - do not use the context date
    Date now = new Date();
    dto.setCreatedAt(now);
    dto.setUpdatedAt(new Date());
    return dto;
  }

  public DefaultIssueComment toComment() {
    return new DefaultIssueComment()
      .setMarkdownText(changeData)
      .setKey(kee)
      .setCreatedAt(createdAt)
      .setUpdatedAt(updatedAt)
      .setUserLogin(userLogin)
      .setIssueKey(issueKey)
      .setNew(false);
  }

  public FieldDiffs toFieldDiffs() {
    return FieldDiffs.parse(changeData)
      .setUserLogin(userLogin)
      .setCreatedAt(createdAt)
      .setUpdatedAt(updatedAt)
      .setIssueKey(issueKey);
  }
}
