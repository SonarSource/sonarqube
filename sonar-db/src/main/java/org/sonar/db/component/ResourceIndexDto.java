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
package org.sonar.db.component;

public final class ResourceIndexDto {
  private Long id;
  private String key;
  private int position;
  private int nameSize;
  private long resourceId;
  private long rootProjectId;
  private String qualifier;

  public Long getId() {
    return id;
  }

  public ResourceIndexDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getKey() {
    return key;
  }

  public ResourceIndexDto setKey(String key) {
    this.key = key;
    return this;
  }

  public int getPosition() {
    return position;
  }

  public ResourceIndexDto setPosition(int i) {
    this.position = i;
    return this;
  }

  public long getResourceId() {
    return resourceId;
  }

  public ResourceIndexDto setResourceId(long i) {
    this.resourceId = i;
    return this;
  }

  public long getRootProjectId() {
    return rootProjectId;
  }

  public ResourceIndexDto setRootProjectId(long i) {
    this.rootProjectId = i;
    return this;
  }

  public int getNameSize() {
    return nameSize;
  }

  public ResourceIndexDto setNameSize(int i) {
    this.nameSize = i;
    return this;
  }

  public String getQualifier() {
    return qualifier;
  }

  public ResourceIndexDto setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }
}
