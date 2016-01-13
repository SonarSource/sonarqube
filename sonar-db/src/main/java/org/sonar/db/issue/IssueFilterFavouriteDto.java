/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Date;

/**
 * @since 3.7
 */
public class IssueFilterFavouriteDto {

  private Long id;
  private String userLogin;
  private Long issueFilterId;
  private Date createdAt;

  public Long getId() {
    return id;
  }

  public IssueFilterFavouriteDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public IssueFilterFavouriteDto setUserLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public Long getIssueFilterId() {
    return issueFilterId;
  }

  public IssueFilterFavouriteDto setIssueFilterId(Long issueFilterId) {
    this.issueFilterId = issueFilterId;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public IssueFilterFavouriteDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

}
