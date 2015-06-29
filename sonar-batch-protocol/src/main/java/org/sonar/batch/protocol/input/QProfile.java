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
package org.sonar.batch.protocol.input;

import java.util.Date;

public class QProfile {

  private final String key;
  private final String name;
  private final String language;
  private final Date rulesUpdatedAt;

  public QProfile(String key, String name, String language, Date rulesUpdatedAt) {
    this.key = key;
    this.name = name;
    this.language = language;
    this.rulesUpdatedAt = rulesUpdatedAt;
  }

  public String key() {
    return key;
  }

  public String name() {
    return name;
  }

  public String language() {
    return language;
  }

  public Date rulesUpdatedAt() {
    return rulesUpdatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    QProfile qProfile = (QProfile) o;
    return key.equals(qProfile.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }
}
