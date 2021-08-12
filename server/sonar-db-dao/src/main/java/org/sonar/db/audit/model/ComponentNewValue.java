/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;

public class ComponentNewValue implements NewValue {

  private String componentUuid;
  private String componentName;
  @Nullable
  private String description;
  private String rootComponentUuid;
  private String path;
  private String key;
  private Boolean isPrivate;
  private Boolean isEnabled;
  private String prefix;

  public ComponentNewValue(String componentUuid, String name, String key, @Nullable String qualifier) {
    this.componentUuid = componentUuid;
    this.componentName = name;
    this.key = key;
    this.generateComponentPrefix(qualifier);
  }

  public ComponentNewValue(String rootComponentUuid, boolean isPrivate, @Nullable String qualifier) {
    this.rootComponentUuid = rootComponentUuid;
    this.isPrivate = isPrivate;
    this.generateComponentPrefix(qualifier);
  }

  public ComponentNewValue(String componentUuid, String name, boolean isPrivate, String qualifier) {
    this.componentUuid = componentUuid;
    this.componentName = name;
    this.isPrivate = isPrivate;
    this.generateComponentPrefix(qualifier);
  }

  public ComponentNewValue(String uuid, String name, String key, boolean enabled, String path, @Nullable String qualifier) {
    this.componentUuid = uuid;
    this.componentName = name;
    this.isEnabled = enabled;
    this.path = path;
    this.key = key;
    this.generateComponentPrefix(qualifier);
  }

  public ComponentNewValue(String uuid, boolean isPrivate, String name, String key, @Nullable String description, @Nullable String qualifier) {
    this.componentUuid = uuid;
    this.isPrivate = isPrivate;
    this.componentName = name;
    this.key = key;
    this.description = description;
    this.generateComponentPrefix(qualifier);
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

  public String getKey() {
    return key;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  private void generateComponentPrefix(String qualifier) {
    if (qualifier == null) {
      this.prefix = "component";
      return ;
    }
    switch (qualifier) {
      case VIEW:
        this.prefix = "portfolio";
        break;
      case APP:
        this.prefix = "application";
        break;
      case PROJECT:
        this.prefix = "project";
        break;
      default:
        this.prefix = "component";
        break;
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"" + this.prefix + "Uuid\": ", this.componentUuid, true);
    addField(sb, "\"rootComponentUuid\": ", this.rootComponentUuid, true);
    addField(sb, "\"" + this.prefix + "Name\": ", this.componentName, true);
    addField(sb, "\"description\": ", this.description, true);
    addField(sb, "\"key\": ", this.key, true);
    addField(sb, "\"path\": ", this.path, true);
    addField(sb, "\"isPrivate\": ", ObjectUtils.toString(this.isPrivate), false);
    addField(sb, "\"isEnabled\": ", ObjectUtils.toString(this.isEnabled), false);
    endString(sb);
    return sb.toString();
  }

}
