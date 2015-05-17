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
package org.sonar.api.resources;

/**
 * @deprecated in 4.2. Only file system components are managed by SQ core (files/directories).
 */
@Deprecated
public class Method extends Resource {

  public static final String SCOPE = Scopes.BLOCK_UNIT;

  protected String qualifier;
  protected Language language;

  protected Method(String key, String qualifier, Language language) {
    setKey(key);
    this.qualifier = qualifier;
    this.language = language;
  }

  @Override
  public String getName() {
    return getKey();
  }

  @Override
  public String getLongName() {
    return getKey();
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public final Language getLanguage() {
    return language;
  }

  @Override
  public final String getScope() {
    return SCOPE;
  }

  @Override
  public final String getQualifier() {
    return qualifier;
  }

  @Override
  public Resource getParent() {
    return null;
  }

  @Override
  public final boolean matchFilePattern(String antPattern) {
    return false;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Method)) {
      return false;
    }
    Method method = (Method) o;
    if (!getKey().equals(method.getKey())) {
      return false;
    }
    if (!qualifier.equals(method.qualifier)) {
      return false;
    }
    return true;
  }

  @Override
  public final int hashCode() {
    int result = super.hashCode();
    result = 31 * result + qualifier.hashCode();
    result = 31 * result + getKey().hashCode();
    return result;
  }

  public static Method createMethod(String key, Language language) {
    return new Method(key, Qualifiers.METHOD, language);
  }
}
