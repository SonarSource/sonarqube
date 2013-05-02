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

import java.io.Serializable;
import java.util.Date;

public abstract class IssueChange implements Serializable {
  private String userLogin;
  private Date createdAt, updatedAt;

  public String userLogin() {
    return userLogin;
  }

  public IssueChange setUserLogin(String s) {
    this.userLogin = s;
    return this;
  }

  public Date createdAt() {
    return createdAt;
  }

  public IssueChange setCreatedAt(Date d) {
    this.createdAt = d;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public IssueChange setUpdatedAt(Date d) {
    this.updatedAt = d;
    return this;
  }
}
