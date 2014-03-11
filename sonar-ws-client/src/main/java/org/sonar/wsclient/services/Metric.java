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
package org.sonar.wsclient.services;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class Metric extends Model {

  private String key;
  private String name;
  private Integer direction;
  private String domain;
  private String description;
  private String type;
  private Boolean userManaged;
  private Boolean hidden;

  @CheckForNull
  public String getKey() {
    return key;
  }

  public Metric setKey(@Nullable String key) {
    this.key = key;
    return this;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  public Metric setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public Integer getDirection() {
    return direction;
  }

  public Metric setDirection(@Nullable Integer direction) {
    this.direction = direction;
    return this;
  }

  @CheckForNull
  public String getDomain() {
    return domain;
  }

  public Metric setDomain(@Nullable String domain) {
    this.domain = domain;
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public Metric setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  @CheckForNull
  public String getType() {
    return type;
  }

  public Metric setType(@Nullable String type) {
    this.type = type;
    return this;
  }

  @CheckForNull
  public Boolean getHidden() {
    return hidden;
  }

  public Metric setHidden(@Nullable Boolean hidden) {
    this.hidden = hidden;
    return this;
  }

  @CheckForNull
  public Boolean getUserManaged() {
    return userManaged;
  }

  public Metric setUserManaged(@Nullable Boolean userManaged) {
    this.userManaged = userManaged;
    return this;
  }

  @Override
  public String toString() {
    return new StringBuilder()
      .append(name)
      .append("(")
      .append(key)
      .append(")")
      .toString();
  }
}
