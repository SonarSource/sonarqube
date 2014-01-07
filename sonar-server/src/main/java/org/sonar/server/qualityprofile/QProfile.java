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

package org.sonar.server.qualityprofile;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.core.qualityprofile.db.QualityProfileDto;

public class QProfile {

  private Integer id;
  private String name;
  private String language;
  private String parent;
  private Integer version;
  private boolean used;

  public Integer id() {
    return id;
  }

  public QProfile setId(Integer id) {
    this.id = id;
    return this;
  }

  public String name() {
    return name;
  }

  public QProfile setName(String name) {
    this.name = name;
    return this;
  }

  public String language() {
    return language;
  }

  public QProfile setLanguage(String language) {
    this.language = language;
    return this;
  }

  public String parent() {
    return parent;
  }

  public QProfile setParent(String parent) {
    this.parent = parent;
    return this;
  }

  public Integer version() {
    return version;
  }

  public QProfile setVersion(Integer version) {
    this.version = version;
    return this;
  }

  public boolean used() {
    return used;
  }

  public QProfile setUsed(boolean used) {
    this.used = used;
    return this;
  }

  public boolean isInherited(){
    return parent != null;
  }

  public static QProfile from(QualityProfileDto dto) {
    return new QProfile()
      .setId(dto.getId())
      .setName(dto.getName())
      .setLanguage(dto.getLanguage())
      .setParent(dto.getParent())
      .setVersion(dto.getVersion())
      .setUsed(dto.isUsed());
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this).toString();
  }
}
