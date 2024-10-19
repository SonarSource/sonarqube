/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.issue;

import java.io.Serializable;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * @since 3.6
 */
public final class IssueChangeDto implements Serializable {

  public static final String TYPE_FIELD_CHANGE = "diff";
  public static final String TYPE_COMMENT = "comment";

  private String uuid;
  private String kee;
  private String issueKey;
  private String projectUuid;


  /**
   * The column USER_LOGIN hasn't been renamed to USER_UUID because we don't know yet the cost of renaming a column on a table that can contain huge number of data
   */
  private String userUuid;
  private String changeType;
  private String changeData;

  // technical dates
  private Long createdAt;
  private Long updatedAt;

  // functional date
  @Nullable
  private Long issueChangeCreationDate;

  public IssueChangeDto() {
    // nothing to do
  }

  public static IssueChangeDto of(DefaultIssueComment comment, String projectUuid) {
    IssueChangeDto dto = newDto(comment.issueKey());
    dto.setKey(comment.key());
    dto.setChangeType(IssueChangeDto.TYPE_COMMENT);
    dto.setChangeData(comment.markdownText());
    dto.setUserUuid(comment.userUuid());
    Date createdAt = requireNonNull(comment.createdAt(), "Comment created at must not be null");
    dto.setIssueChangeCreationDate(createdAt.getTime());
    dto.setProjectUuid(projectUuid);
    return dto;
  }

  public static IssueChangeDto of(String issueKey, FieldDiffs diffs, String projectUuid) {
    IssueChangeDto dto = newDto(issueKey);
    dto.setChangeType(IssueChangeDto.TYPE_FIELD_CHANGE);
    dto.setChangeData(diffs.toEncodedString());
    dto.setUserUuid(diffs.userUuid().orElse(null));
    Date createdAt = requireNonNull(diffs.creationDate(), "Diffs created at must not be null");
    dto.setIssueChangeCreationDate(createdAt.getTime());
    dto.setProjectUuid(projectUuid);
    return dto;
  }

  private static IssueChangeDto newDto(String issueKey) {
    IssueChangeDto dto = new IssueChangeDto();
    dto.setIssueKey(issueKey);

    // technical dates - do not use the context date
    dto.setCreatedAt(System2.INSTANCE.now());
    dto.setUpdatedAt(System2.INSTANCE.now());
    return dto;
  }

  public String getUuid() {
    return uuid;
  }

  public IssueChangeDto setUuid(String uuid) {
    this.uuid = uuid;
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

  @CheckForNull
  public String getUserUuid() {
    return userUuid;
  }

  public IssueChangeDto setUserUuid(@Nullable String userUuid) {
    this.userUuid = userUuid;
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

  public Long getCreatedAt() {
    return createdAt;
  }

  public IssueChangeDto setCreatedAt(Long createdAt) {
    this.createdAt = checkNotNull(createdAt);
    return this;
  }

  public Long getUpdatedAt() {
    return updatedAt;
  }

  public IssueChangeDto setUpdatedAt(@Nullable Long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Long getIssueChangeCreationDate() {
    // Old comments do not have functional creation date as this column has been added later
    return issueChangeCreationDate == null ? createdAt : issueChangeCreationDate;
  }

  public IssueChangeDto setIssueChangeCreationDate(long issueChangeCreationDate) {
    this.issueChangeCreationDate = issueChangeCreationDate;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public DefaultIssueComment toComment() {
    return new DefaultIssueComment()
      .setMarkdownText(changeData)
      .setKey(kee)
      .setCreatedAt(new Date(getIssueChangeCreationDate()))
      .setUpdatedAt(updatedAt == null ? null : new Date(updatedAt))
      .setUserUuid(userUuid)
      .setIssueKey(issueKey)
      .setNew(false);
  }

  public FieldDiffs toFieldDiffs() {
    return FieldDiffs.parse(changeData)
      .setUserUuid(userUuid)
      .setCreationDate(new Date(getIssueChangeCreationDate()))
      .setIssueKey(issueKey);
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public IssueChangeDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }
}
