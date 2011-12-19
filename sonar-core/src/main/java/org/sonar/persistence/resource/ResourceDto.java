/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.persistence.resource;

public final class ResourceDto {

  private Integer id;
  private String name;
  private String longName;
  private Integer rootId;

  public Integer getId() {
    return id;
  }

  public ResourceDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public ResourceDto setName(String name) {
    this.name = name;
    return this;
  }

  public Integer getRootId() {
    return rootId;
  }

  public ResourceDto setRootId(Integer rootId) {
    this.rootId = rootId;
    return this;
  }

  public String getLongName() {
    return longName;
  }

  public ResourceDto setLongName(String longName) {
    this.longName = longName;
    return this;
  }
}
