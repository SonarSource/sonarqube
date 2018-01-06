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
package org.sonar.server.qualityprofile;

import javax.annotation.Nullable;

public class QProfileName {
  private final String lang;
  private final String name;

  public QProfileName(String lang, String name) {
    this.lang = lang;
    this.name = name;
  }

  public String getLanguage() {
    return lang;
  }

  public String getName() {
    return name;
  }

  public static QProfileName createFor(String lang, String name){
    return new QProfileName(lang, name);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QProfileName that = (QProfileName) o;
    if (!lang.equals(that.lang)) {
      return false;
    }
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    int result = lang.hashCode();
    result = 31 * result + name.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return String.format("%s/%s", lang, name);
  }
}
