/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.component;

/**
 * @since 3.0
 */
public class ResourceQuery {
  private String[] qualifiers = null;
  private String key = null;
  private boolean excludeDisabled = false;

  private ResourceQuery() {
  }

  public static ResourceQuery create() {
    return new ResourceQuery();
  }

  public String[] getQualifiers() {
    return qualifiers;
  }

  public ResourceQuery setQualifiers(String[] qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public String getKey() {
    return key;
  }

  public ResourceQuery setKey(String key) {
    this.key = key;
    return this;
  }

  public boolean isExcludeDisabled() {
    return excludeDisabled;
  }

  public ResourceQuery setExcludeDisabled(boolean b) {
    this.excludeDisabled = b;
    return this;
  }
}
