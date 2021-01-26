/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.alm.client.bitbucketserver;

import com.google.gson.annotations.SerializedName;

public class Project {

  @SerializedName("key")
  private String key;

  @SerializedName("name")
  private String name;

  @SerializedName("id")
  private long id;

  public Project() {
    // http://stackoverflow.com/a/18645370/229031
  }

  public Project(String key, String name, long id) {
    this.key = key;
    this.name = name;
    this.id = id;
  }

  public String getKey() {
    return key;
  }

  public Project setKey(String key) {
    this.key = key;
    return this;
  }

  public String getName() {
    return name;
  }

  public Project setName(String name) {
    this.name = name;
    return this;
  }

  public long getId() {
    return id;
  }

  public Project setId(long id) {
    this.id = id;
    return this;
  }

  @Override
  public String toString() {
    return "{" +
      "key='" + key + '\'' +
      ", name='" + name + '\'' +
      ", id=" + id +
      '}';
  }
}
