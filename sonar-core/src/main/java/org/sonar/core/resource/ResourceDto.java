/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.core.resource;

public final class ResourceDto {

  private Long id;
  private String key;
  private String name;
  private String longName;
  private Integer rootId;
  private String scope;
  private String qualifier;

  public Long getId() {
    return id;
  }

  public ResourceDto setId(Long id) {
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

  public String getKey() {
    return key;
  }

  public ResourceDto setKey(String s) {
    this.key = s;
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

  public String getScope() {
    return scope;
  }

  public ResourceDto setScope(String scope) {
    this.scope = scope;
    return this;
  }

  public String getQualifier() {
    return qualifier;
  }

  public ResourceDto setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ResourceDto other = (ResourceDto) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

}
