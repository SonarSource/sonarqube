/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.resources;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @deprecated since 5.2 No more design features
 */
@Deprecated
public final class Library extends Resource {

  private String name;
  private String description;
  private String version;

  public Library(String key, String version) {
    setKey(key);
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  public Library setName(String name) {
    this.name = name;
    return this;
  }

  public Library setDescription(String description) {
    this.description = description;
    return this;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getLongName() {
    return null;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Language getLanguage() {
    return null;
  }

  @Override
  public String getScope() {
    return Scopes.PROJECT;
  }

  @Override
  public String getQualifier() {
    return Qualifiers.LIBRARY;
  }

  @Override
  public Resource getParent() {
    return null;
  }

  @Override
  public boolean matchFilePattern(String antPattern) {
    return false;
  }

  public static Library createFromMavenIds(String groupId, String artifactId, String version) {
    return new Library(String.format("%s:%s", groupId, artifactId), version);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Library library = (Library) o;
    if (!getKey().equals(library.getKey())) {
      return false;
    }
    return version.equals(library.version);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + getKey().hashCode();
    result = 31 * result + version.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("key", getKey())
      .append("name", getName())
      .append("version", version)
      .toString();
  }
}
