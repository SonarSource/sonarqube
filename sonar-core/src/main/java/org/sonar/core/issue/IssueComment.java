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
package org.sonar.core.issue;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class IssueComment implements Serializable {

  private String userLogin;
  private Date createdAt, updatedAt;
  private String key;
  private String text;
  private boolean isNew;

  public String text() {
    return text;
  }

  public IssueComment setText(String s) {
    this.text = s;
    return this;
  }

  public String key() {
    return key;
  }

  public IssueComment setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * The user who created the comment. Null if it was automatically generated during project scan.
   */
  @CheckForNull
  public String userLogin() {
    return userLogin;
  }

  public IssueComment setUserLogin(@Nullable String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public Date createdAt() {
    return createdAt;
  }

  public IssueComment setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public IssueComment setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public boolean isNew() {
    return isNew;
  }

  public IssueComment setNew(boolean b) {
    isNew = b;
    return this;
  }

  public static IssueComment create(@Nullable String login, String text) {
    IssueComment comment = new IssueComment();
    comment.setKey(UUID.randomUUID().toString());
    Date now = new Date();
    comment.setUserLogin(login);
    comment.setText(text);
    comment.setCreatedAt(now).setUpdatedAt(now);
    comment.setNew(true);
    return comment;
  }
}
