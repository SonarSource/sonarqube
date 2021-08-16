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

import org.apache.commons.lang.ObjectUtils;

public class ProjectNewValue extends NewValue{

  private final String uuid;
  private String name;
  private String description;
  private Boolean isPrivate;

  public ProjectNewValue(String uuid, boolean isPrivate, String name, String description) {
    this.uuid = uuid;
    this.isPrivate = isPrivate;
    this.name = name;
    this.description = description;
  }

  public String getUuid() {
    return uuid;
  }

  public String getDescription() {
    return description;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"projectUuid\": ", this.uuid, true);
    addField(sb, "\"description\": ", this.description, true);
    addField(sb, "\"name\": ", this.name, true);
    addField(sb, "\"isPrivate\": ", ObjectUtils.toString(this.isPrivate), false);
    endString(sb);
    return sb.toString();
  }
}
