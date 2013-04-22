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
package org.sonar.wsclient.services;

public class Favourite extends Model {

  private Integer id;
  private String key;
  private String name;
  private String scope;
  private String qualifier;
  private String language;

  public Integer getId() {
    return id;
  }

  public Favourite setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getKey() {
    return key;
  }

  public Favourite setKey(String key) {
    this.key = key;
    return this;
  }

  public String getName() {
    return name;
  }

  public Favourite setName(String name) {
    this.name = name;
    return this;
  }

  public String getScope() {
    return scope;
  }

  public Favourite setScope(String scope) {
    this.scope = scope;
    return this;
  }

  public String getQualifier() {
    return qualifier;
  }

  public Favourite setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public Favourite setLanguage(String language) {
    this.language = language;
    return this;
  }
}
