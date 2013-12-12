/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.qualityprofile;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Key of a quality profile. Unique among all the quality profile repositories.
 *
 */
public class QProfileKey {

  private final String name, language;

  private QProfileKey(String name, String language) {
    this.name = name;
    this.language = language;
  }

  /**
   * Create a key. Parameters are NOT null.
   */
  public static QProfileKey of(String name, String language) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name must be set");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(language), "Language must be set");
    return new QProfileKey(name, language);
  }

  /**
   * Create a key from a string representation (see {@link #toString()}. An {@link IllegalArgumentException} is raised
   * if the format is not valid.
   */
  public static QProfileKey parse(String s) {
    String[] split = s.split("_");
    Preconditions.checkArgument(split.length == 2, "Bad format of quality profile key: " + s);
    return QProfileKey.of(split[0], split[1]);
  }

  public String name() {
    return name;
  }

  public String language() {
    return language;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QProfileKey key = (QProfileKey) o;
    if (!name.equals(key.name)) {
      return false;
    }
    if (!language.equals(key.language)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + language.hashCode();
    return result;
  }

  /**
   * Format is "name_language", for example "Sonar Way_java:java"
   */
  @Override
  public String toString() {
    return String.format("%s_%s", name, language);
  }

}
