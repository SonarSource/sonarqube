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

import com.google.common.base.Preconditions;

import java.util.Locale;

/**
 * Inherit this class to define a new language like PLSQL, PHP or C#
 *
 * @since 1.10
 */
public abstract class AbstractLanguage implements Language {
  private final String key;
  private String name;

  /**
   * Better to use AbstractLanguage(key, name). In this case, key and name will be the same
   * 
   * @param key The key of the language. Must not be more than 20 chars.
   */
  public AbstractLanguage(String key) {
    this(key, key);
  }

  /**
   * Should be the constructor used to build an AbstractLanguage.
   *
   * @param key the key that will be used to retrieve the language. Must not be more than 20 chars. This key is important as it will be used to teint rules repositories...
   * @param name the display name of the language in the interface
   */
  public AbstractLanguage(String key, String name) {
    Preconditions.checkArgument(key.length() <= 20, "The following language key exceeds 20 characters: '" + key + "'");
    this.key = key.toLowerCase(Locale.ENGLISH);
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getKey() {
    return key;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets the language name
   */
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof Language)) {
      return false;
    }
    Language that = (Language) o;
    return key.equals(that.getKey());

  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }
}
