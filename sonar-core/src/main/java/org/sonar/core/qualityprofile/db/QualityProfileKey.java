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
package org.sonar.core.qualityprofile.db;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.io.Serializable;

/**
 * Created by gamars on 05/05/14.
 *
 * @since 4.4
 */
public class QualityProfileKey implements Serializable{
  private final String name, lang;

  protected QualityProfileKey(String name, String lang) {
    this.lang = lang;
    this.name = name;
  }

  /**
   * Create a key. Parameters are NOT null.
   */
  public static QualityProfileKey of(String name, String lang) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name must be set");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(lang), "Lang must be set");
    return new QualityProfileKey(name, lang);
  }

  /**
   * Create a key from a string representation (see {@link #toString()}. An {@link IllegalArgumentException} is raised
   * if the format is not valid.
   */
  public static QualityProfileKey parse(String s) {
    String[] split = s.split(":");
    Preconditions.checkArgument(split.length == 3, "Bad format of activeRule key: " + s);
    return QualityProfileKey.of(split[0], split[1]);
  }

  /**
   * Never null
   */
  public String lang() {
    return lang;
  }

  /**
   * Never null
   */
  public String name() {
    return name;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualityProfileKey qualityProfileKey = (QualityProfileKey) o;
    if (!lang.equals(qualityProfileKey.lang)) {
      return false;
    }
    if (!name.equals(qualityProfileKey.name)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + lang.hashCode();
    return result;
  }

  /**
   * Format is "qProfile:lang", for example "Java:javascript"
   */
  @Override
  public String toString() {
    return String.format("%s:%s", name, lang);
  }
}
