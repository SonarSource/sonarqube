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

package org.sonar.core.qualityprofile.db;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class QualityProfileDto {

  private Integer id;
  private String name;
  private String language;
  private String parent;
  private Integer version;
  private boolean used;

  public Integer getId() {
    return id;
  }

  public QualityProfileDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public QualityProfileDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public QualityProfileDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  @CheckForNull
  public String getParent() {
    return parent;
  }

  public QualityProfileDto setParent(@Nullable String parent) {
    this.parent = parent;
    return this;
  }

  public Integer getVersion() {
    return version;
  }

  public QualityProfileDto setVersion(Integer version) {
    this.version = version;
    return this;
  }

  public boolean isUsed() {
    return used;
  }

  public QualityProfileDto setUsed(boolean used) {
    this.used = used;
    return this;
  }
}
