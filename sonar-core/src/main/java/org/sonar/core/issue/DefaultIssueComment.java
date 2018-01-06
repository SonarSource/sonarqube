/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.api.issue.IssueComment;
import org.sonar.core.util.Uuids;

/**
 * PLUGINS MUST NOT BE USED THIS CLASS, EXCEPT FOR UNIT TESTING.
 *
 * @since 3.6
 */
public class DefaultIssueComment implements Serializable, IssueComment {

  private String issueKey;
  private String userLogin;
  private Date createdAt;
  private Date updatedAt;
  private String key;
  private String markdownText;
  private boolean isNew;

  public static DefaultIssueComment create(String issueKey, @Nullable String login, String markdownText) {
    DefaultIssueComment comment = new DefaultIssueComment();
    comment.setIssueKey(issueKey);
    comment.setKey(Uuids.create());
    Date now = new Date();
    comment.setUserLogin(login);
    comment.setMarkdownText(markdownText);
    comment.setCreatedAt(now).setUpdatedAt(now);
    comment.setNew(true);
    return comment;
  }

  @Override
  public String markdownText() {
    return markdownText;
  }

  public DefaultIssueComment setMarkdownText(String s) {
    this.markdownText = s;
    return this;
  }

  @Override
  public String issueKey() {
    return issueKey;
  }

  public DefaultIssueComment setIssueKey(String s) {
    this.issueKey = s;
    return this;
  }

  @Override
  public String key() {
    return key;
  }

  public DefaultIssueComment setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * The user who created the comment. Null if it was automatically generated during project scan.
   */
  @Override
  @CheckForNull
  public String userLogin() {
    return userLogin;
  }

  public DefaultIssueComment setUserLogin(@Nullable String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  @Override
  public Date createdAt() {
    return createdAt;
  }

  public DefaultIssueComment setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @Override
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
