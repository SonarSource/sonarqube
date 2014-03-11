/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import java.util.Date;

public class DefaultIssueFilter {

  private Long id;
  private String name;
  private String user;
  private boolean shared = false;
  private String description;
  private String data;
  private Date createdAt;
  private Date updatedAt;

  public DefaultIssueFilter() {

  }

  public static DefaultIssueFilter create(String name) {
    DefaultIssueFilter issueFilter = new DefaultIssueFilter();
    Date now = new Date();
    issueFilter.setName(name);
    issueFilter.setCreatedAt(now).setUpdatedAt(now);
    return issueFilter;
  }

  public Long id() {
    return id;
  }

  public DefaultIssueFilter setId(Long id) {
    this.id = id;
    return this;
  }

  public String name() {
    return name;
  }

  public DefaultIssueFilter setName(String name) {
    this.name = name;
    return this;
  }

  public String user() {
    return user;
  }

  public DefaultIssueFilter setUser(String user) {
    this.user = user;
    return this;
  }

  public boolean shared() {
    return shared;
  }

  public DefaultIssueFilter setShared(boolean shared) {
    this.shared = shared;
    return this;
  }

  public String description() {
    return description;
  }

  public DefaultIssueFilter setDescription(String description) {
    this.description = description;
    return this;
  }

  public String data() {
    return data;
  }

  public DefaultIssueFilter setData(String data) {
    this.data = data;
    return this;
  }

  public Date createdAt() {
    return createdAt;
  }

  public DefaultIssueFilter setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public DefaultIssueFilter setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

}
