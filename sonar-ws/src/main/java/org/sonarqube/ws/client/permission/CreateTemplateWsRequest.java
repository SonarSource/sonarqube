/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.permission;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class CreateTemplateWsRequest {
  private String description;
  private String name;
  private String projectKeyPattern;
  private String organization;

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public CreateTemplateWsRequest setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public String getName() {
    return name;
  }

  public CreateTemplateWsRequest setName(String name) {
    this.name = requireNonNull(name);
    return this;
  }

  @CheckForNull
  public String getProjectKeyPattern() {
    return projectKeyPattern;
  }

  public CreateTemplateWsRequest setProjectKeyPattern(@Nullable String projectKeyPattern) {
    this.projectKeyPattern = projectKeyPattern;
    return this;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public CreateTemplateWsRequest setOrganization(@Nullable String s) {
    this.organization = s;
    return this;
  }
}
