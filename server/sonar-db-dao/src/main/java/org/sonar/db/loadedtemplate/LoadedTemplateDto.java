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
package org.sonar.db.loadedtemplate;

import java.util.Objects;

public final class LoadedTemplateDto {

  public static final String QUALITY_GATE_TYPE = "QUALITY_GATE";
  public static final String ONE_SHOT_TASK_TYPE = "ONE_SHOT_TASK";

  private Long id;
  private String key;
  private String type;

  public LoadedTemplateDto() {
  }

  public LoadedTemplateDto(String key, String type) {
    this.key = key;
    this.type = type;
  }

  public Long getId() {
    return id;
  }

  public LoadedTemplateDto setId(Long l) {
    this.id = l;
    return this;
  }

  public String getKey() {
    return key;
  }

  public LoadedTemplateDto setKey(String key) {
    this.key = key;
    return this;
  }

  public String getType() {
    return type;
  }

  public LoadedTemplateDto setType(String type) {
    this.type = type;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LoadedTemplateDto other = (LoadedTemplateDto) o;
    return Objects.equals(id, other.id) && Objects.equals(key, other.key) && Objects.equals(type, other.type);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
