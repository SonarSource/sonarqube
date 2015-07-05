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

final class ResourceIndexQuery {
  private long rootProjectId;
  private String[] scopes = null;
  private String[] qualifiers = null;
  private boolean nonIndexedOnly = false;

  private ResourceIndexQuery() {
  }

  public static ResourceIndexQuery create() {
    return new ResourceIndexQuery();
  }

  public String[] getScopes() {
    return scopes;
  }

  public String[] getQualifiers() {
    return qualifiers;
  }

  public ResourceIndexQuery setScopes(String[] scopes) {
    this.scopes = scopes;
    return this;
  }

  public ResourceIndexQuery setQualifiers(String[] qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public long getRootProjectId() {
    return rootProjectId;
  }

  public ResourceIndexQuery setRootProjectId(long i) {
    this.rootProjectId = i;
    return this;
  }

  public boolean isNonIndexedOnly() {
    return nonIndexedOnly;
  }

  public ResourceIndexQuery setNonIndexedOnly(boolean b) {
    this.nonIndexedOnly = b;
    return this;
  }
}
