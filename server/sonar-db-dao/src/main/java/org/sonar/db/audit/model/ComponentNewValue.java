/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.audit.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;

import static java.util.Objects.requireNonNull;

public class ComponentNewValue extends NewValue {
  private final String componentUuid;
  private final String componentKey;
  private final String componentName;
  private final String description;
  private final Boolean isPrivate;
  private final String qualifier;
  private Boolean isEnabled;
  private String path;

  public ComponentNewValue(ProjectDto project) {
    this(project.getUuid(), project.getName(), project.getKey(), project.isPrivate(), project.getDescription(), project.getQualifier());
  }

  public ComponentNewValue(ComponentDto component) {
    this(component.uuid(), component.name(), component.getKey(), component.isPrivate(), component.description(), component.qualifier());
  }

  public ComponentNewValue(String componentUuid, String componentName, String componentKey, String qualifier) {
    this(componentUuid, componentName, componentKey, null, null, qualifier);
  }

  public ComponentNewValue(String componentUuid, String componentName, String componentKey, boolean isPrivate, String qualifier) {
    this(componentUuid, isPrivate, componentName, componentKey, null, qualifier);
  }

  public ComponentNewValue(String uuid, String name, String key, boolean enabled, String path, String qualifier) {
    this(uuid, name, key, null, null, qualifier);
    this.isEnabled = enabled;
    this.path = path;
  }

  public ComponentNewValue(String uuid, @Nullable Boolean isPrivate, String name, String key, @Nullable String description, String qualifier) {
    this(uuid, name, key, isPrivate, description, qualifier);
  }

  private ComponentNewValue(String uuid, String name, String key, @Nullable Boolean isPrivate, @Nullable String description, String qualifier) {
    this.componentUuid = requireNonNull(uuid);
    this.componentName = name;
    this.componentKey = key;
    this.isPrivate = isPrivate;
    this.description = description;
    this.qualifier = qualifier;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public String getComponentName() {
    return componentName;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public String getComponentKey() {
    return componentKey;
  }

  @CheckForNull
  public Boolean isPrivate() {
    return isPrivate;
  }

  public String getQualifier() {
    return qualifier;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"componentUuid\": ", this.componentUuid, true);
    addField(sb, "\"componentKey\": ", this.componentKey, true);
    addField(sb, "\"componentName\": ", this.componentName, true);
    addField(sb, "\"qualifier\": ", getQualifier(qualifier), true);
    addField(sb, "\"description\": ", this.description, true);
    addField(sb, "\"path\": ", this.path, true);
    addField(sb, "\"isPrivate\": ", ObjectUtils.toString(this.isPrivate), false);
    addField(sb, "\"isEnabled\": ", ObjectUtils.toString(this.isEnabled), false);
    endString(sb);
    return sb.toString();
  }

}
