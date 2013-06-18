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

import org.sonar.core.issue.DefaultIssueFilter;

import javax.annotation.Nullable;

import java.util.Date;

/**
 * @since 3.7
 */
public class IssueFilterDto {

  private Long id;
  private String name;
  private String userLogin;
  private boolean shared;
  private String description;
  private String data;
  private Date createdAt;
  private Date updatedAt;

  public Long getId() {
    return id;
  }

  public IssueFilterDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public IssueFilterDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public IssueFilterDto setUserLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public Boolean isShared() {
    return shared;
  }

  public IssueFilterDto setShared(@Nullable Boolean shared) {
    this.shared = shared;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public IssueFilterDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public String getData() {
    return data;
  }

  public IssueFilterDto setData(String data) {
    this.data = data;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public IssueFilterDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public IssueFilterDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public DefaultIssueFilter toIssueFilter() {
    return new DefaultIssueFilter()
      .setId(id)
      .setName(name)
      .setUser(userLogin)
      .setDescription(description)
      .setShared(shared)
      .setData(data)
      .setCreatedAt(createdAt)
      .setUpdatedAt(updatedAt);
  }

  public static IssueFilterDto toIssueFilter(DefaultIssueFilter issueFilter) {
    return new IssueFilterDto()
      .setId(issueFilter.id())
      .setName(issueFilter.name())
      .setUserLogin(issueFilter.user())
      .setDescription(issueFilter.description())
      .setShared(issueFilter.shared())
      .setData(issueFilter.data())
      .setCreatedAt(issueFilter.createdAt())
      .setUpdatedAt(issueFilter.updatedAt());
  }
}
