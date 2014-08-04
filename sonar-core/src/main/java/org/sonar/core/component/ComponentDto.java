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
package org.sonar.core.component;

import org.sonar.api.component.Component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ComponentDto extends AuthorizedComponentDto implements Component {

  private String path;
  private String name;
  private String longName;
  private String qualifier;
  private String scope;
  private String language;
  private Long projectId;
  private Long subProjectId;
  private boolean enabled = true;

  public ComponentDto setId(Long id) {
    super.setId(id);
    return this;
  }

  public ComponentDto setKey(String key) {
    super.setKey(key);
    return this;
  }

  @CheckForNull
  @Override
  public String path() {
    return path;
  }

  public ComponentDto setPath(@Nullable String path) {
    this.path = path;
    return this;
  }

  @Override
  public String name() {
    return name;
  }

  public ComponentDto setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public String longName() {
    return longName;
  }

  public ComponentDto setLongName(String longName) {
    this.longName = longName;
    return this;
  }

  @Override
  public String qualifier() {
    return qualifier;
  }

  public ComponentDto setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  public String scope() {
    return scope;
  }

  public ComponentDto setScope(String scope) {
    this.scope = scope;
    return this;
  }

  @CheckForNull
  public String language() {
    return language;
  }

  public ComponentDto setLanguage(@Nullable String language) {
    this.language = language;
    return this;
  }

  public Long projectId() {
    return projectId;
  }

  public ComponentDto setProjectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  @CheckForNull
  public Long subProjectId() {
    return subProjectId;
  }

  public ComponentDto setSubProjectId(@Nullable Long subProjectId) {
    this.subProjectId = subProjectId;
    return this;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public ComponentDto setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

}
