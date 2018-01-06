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
package org.sonar.scanner.rule;

import com.google.common.base.MoreObjects;
import java.util.Date;

import javax.annotation.concurrent.Immutable;

@Immutable
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

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public String getLanguage() {
    return language;
  }

  public Date getRulesUpdatedAt() {
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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("key", key)
      .add("name", name)
      .add("language", language)
      .add("rulesUpdatedAt", rulesUpdatedAt)
      .toString();
  }

  public static class Builder {
    private String key;
    private String name;
    private String language;
    private Date rulesUpdatedAt;

    public String getKey() {
      return key;
    }

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public String getName() {
      return name;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public String getLanguage() {
      return language;
    }

    public Builder setLanguage(String language) {
      this.language = language;
      return this;
    }

    public Date getRulesUpdatedAt() {
      return rulesUpdatedAt;
    }

    public Builder setRulesUpdatedAt(Date d) {
      this.rulesUpdatedAt = d;
      return this;
    }

    public QProfile build() {
      return new QProfile(key, name, language, rulesUpdatedAt);
    }
  }
}
