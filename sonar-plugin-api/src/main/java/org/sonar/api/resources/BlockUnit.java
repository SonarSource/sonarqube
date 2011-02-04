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
package org.sonar.api.resources;

public class BlockUnit extends Resource {

  public static final String SCOPE = Scopes.BLOCK_UNIT;

  protected String qualifier;
  protected Language language;

  protected BlockUnit(String key, String qualifier, Language language) {
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    BlockUnit blockUnit = (BlockUnit) o;
    if (!qualifier.equals(blockUnit.qualifier)) {
      return false;
    }
    return getKey().equals(blockUnit.getKey());
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + getKey().hashCode();
    result = 31 * result + qualifier.hashCode();
    return result;
  }

  public static BlockUnit createMethod(String key, Language language) {
    return new BlockUnit(key, Qualifiers.METHOD, language);
  }
}
