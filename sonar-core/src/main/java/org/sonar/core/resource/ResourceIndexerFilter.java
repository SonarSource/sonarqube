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
package org.sonar.core.resource;

public final class ResourceIndexerFilter {
  private boolean enabled = true;
  private String[] scopes = null;
  private String[] qualifiers = null;

  private ResourceIndexerFilter() {
  }

  public static ResourceIndexerFilter create() {
    return new ResourceIndexerFilter();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String[] getScopes() {
    return scopes;
  }

  public String[] getQualifiers() {
    return qualifiers;
  }

  public ResourceIndexerFilter setScopes(String[] scopes) {
    this.scopes = scopes;
    return this;
  }

  public ResourceIndexerFilter setQualifiers(String[] qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }
}
