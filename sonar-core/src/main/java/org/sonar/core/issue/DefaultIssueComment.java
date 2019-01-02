/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.core.issue;

import java.io.Serializable;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.util.Uuids;

/**
 * PLUGINS MUST NOT BE USED THIS CLASS
 *
 * @since 3.6
 */
public class DefaultIssueComment implements Serializable {

  private String issueKey;
  private String userUuid;
  private Date createdAt;
  private Date updatedAt;
  private String key;
  private String markdownText;
  private boolean isNew;

  public static DefaultIssueComment create(String issueKey, @Nullable String userUuid, String markdownText) {
    DefaultIssueComment comment = new DefaultIssueComment();
    comment.setIssueKey(issueKey);
    comment.setKey(Uuids.create());
    Date now = new Date();
    comment.setUserUuid(userUuid);
    comment.setMarkdownText(markdownText);
    comment.setCreatedAt(now).setUpdatedAt(now);
    comment.setNew(true);
    return comment;
  }

  public String markdownText() {
    return markdownText;
  }

  public DefaultIssueComment setMarkdownText(String s) {
    this.markdownText = s;
    return this;
  }

  public String issueKey() {
    return issueKey;
  }

  public DefaultIssueComment setIssueKey(String s) {
    this.issueKey = s;
    return this;
  }

  public String key() {
    return key;
  }

  public DefaultIssueComment setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * The user uuid who created the comment. Null if it was automatically generated during project scan.
   */
  @CheckForNull
  public String userUuid() {
    return userUuid;
  }

  public DefaultIssueComment setUserUuid(@Nullable String userUuid) {
    this.userUuid = userUuid;
    return this;
  }

  public Date createdAt() {
    return createdAt;
  }

  public DefaultIssueComment setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public DefaultIssueComment setUpdatedAt(@Nullable Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public boolean isNew() {
    return isNew;
  }

  public DefaultIssueComment setNew(boolean b) {
    isNew = b;
    return this;
  }

}
