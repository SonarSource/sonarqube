/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.rule;

public class RuleRepositoryDto {

  // do not rename "key" as MyBatis maps it with the db column "kee"
  private String kee;
  private String language;
  private String name;

  public RuleRepositoryDto() {
    // used by MyBatis
  }

  public RuleRepositoryDto(String kee, String language, String name) {
    this.kee = kee;
    this.language = language;
    this.name = name;
  }

  public String getKey() {
    return kee;
  }

  public String getLanguage() {
    return language;
  }

  public String getName() {
    return name;
  }

  public RuleRepositoryDto setKey(String s) {
    this.kee = s;
    return this;
  }

  public RuleRepositoryDto setLanguage(String s) {
    this.language = s;
    return this;
  }

  public RuleRepositoryDto setName(String s) {
    this.name = s;
    return this;
  }
}
