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
public class QProfileKey implements Serializable{
  private final String qProfile, lang;

  protected QProfileKey(String qProfile, String lang) {
    this.lang = lang;
    this.qProfile = qProfile;
  }

  /**
   * Create a key. Parameters are NOT null.
   */
  public static QProfileKey of(String qProfile, String lang) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(qProfile), "QProfile must be set");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(lang), "Lang must be set");
    return new QProfileKey(qProfile, lang);
  }

  /**
   * Create a key from a string representation (see {@link #toString()}. An {@link IllegalArgumentException} is raised
   * if the format is not valid.
   */
  public static QProfileKey parse(String s) {
    String[] split = s.split(":");
    Preconditions.checkArgument(split.length == 3, "Bad format of activeRule key: " + s);
    return QProfileKey.of(split[0], split[1]);
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
  public String qProfile() {
    return qProfile;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QProfileKey qProfileKey = (QProfileKey) o;
    if (!lang.equals(qProfileKey.lang)) {
      return false;
    }
    if (!qProfile.equals(qProfileKey.qProfile)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = qProfile.hashCode();
    result = 31 * result + lang.hashCode();
    return result;
  }

  /**
   * Format is "qProfile:lang", for example "Java:javascript"
   */
  @Override
  public String toString() {
    return String.format("%s:%s", qProfile, lang);
  }
}
