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
package org.sonar.db.component;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ProjectLinkDto {

  public static final String TYPE_HOME_PAGE = "homepage";
  public static final String TYPE_CI = "ci";
  public static final String TYPE_ISSUE_TRACKER = "issue";
  public static final String TYPE_SOURCES = "scm";

  public static final String TYPE_SOURCES_DEV = "scm_dev";

  public static final List<String> PROVIDED_TYPES = ImmutableList.of(TYPE_HOME_PAGE, TYPE_CI, TYPE_ISSUE_TRACKER, TYPE_SOURCES, TYPE_SOURCES_DEV);

  private String uuid;
  private String projectUuid;
  private String type;
  private String name;
  private String href;
  private long createdAt;
  private long updatedAt;

  public String getUuid() {
    return uuid;
  }

  public ProjectLinkDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  public ProjectLinkDto setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public ProjectLinkDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public String getHref() {
    return href;
  }

  public ProjectLinkDto setHref(String href) {
    this.href = href;
    return this;
  }

  public String getType() {
    return type;
  }

  public ProjectLinkDto setType(String type) {
    this.type = type;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public ProjectLinkDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public ProjectLinkDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
