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
package org.sonarqube.ws.client.permission;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class UpdateTemplateWsRequest {
  private String id;
  private String description;
  private String name;
  private String projectKeyPattern;

  public String getId() {
    return id;
  }

  public UpdateTemplateWsRequest setId(String id) {
    this.id = requireNonNull(id);
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public UpdateTemplateWsRequest setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  public UpdateTemplateWsRequest setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String getProjectKeyPattern() {
    return projectKeyPattern;
  }

  public UpdateTemplateWsRequest setProjectKeyPattern(@Nullable String projectKeyPattern) {
    this.projectKeyPattern = projectKeyPattern;
    return this;
  }
}
