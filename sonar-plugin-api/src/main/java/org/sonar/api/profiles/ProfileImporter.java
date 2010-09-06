/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.profiles;

import org.sonar.api.ServerExtension;
import org.sonar.api.utils.ValidationMessages;

import java.io.Reader;

/**
 * @since 2.3
 */
public abstract class ProfileImporter implements ServerExtension {

  private String[] supportedLanguages = new String[0];
  private String key;
  private String name;

  protected ProfileImporter(String key, String name) {
    this.key = key;
    this.name = name;
  }

  public abstract ProfilePrototype importProfile(Reader reader, ValidationMessages messages);

  public final String getKey() {
    return key;
  }

  public final ProfileImporter setKey(String s) {
    this.key = s;
    return this;
  }

  public final String getName() {
    return name;
  }

  public final ProfileImporter setName(String s) {
    this.name = s;
    return this;
  }

  protected final ProfileImporter setSupportedLanguages(String... languages) {
    supportedLanguages = (languages != null ? languages : new String[0]);
    return this;
  }

  /**
   * @return if empty, then any languages are supported.
   */
  public final String[] getSupportedLanguages() {
    return supportedLanguages;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProfileImporter that = (ProfileImporter) o;
    if (key != null ? !key.equals(that.key) : that.key != null) {
      return false;
    }
    return true;
  }

  @Override
  public final int hashCode() {
    return key != null ? key.hashCode() : 0;
  }
}
