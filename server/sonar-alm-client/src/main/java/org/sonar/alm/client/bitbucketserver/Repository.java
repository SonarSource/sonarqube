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

public class Repository {

  @SerializedName("slug")
  private String slug;

  @SerializedName("name")
  private String name;

  @SerializedName("id")
  private long id;

  @SerializedName("project")
  private Project project;

  public Repository() {
    // http://stackoverflow.com/a/18645370/229031
  }

  public Repository(String slug, String name, long id, Project project) {
    this.slug = slug;
    this.name = name;
    this.id = id;
    this.project = project;
  }

  public String getSlug() {
    return slug;
  }

  public Repository setSlug(String slug) {
    this.slug = slug;
    return this;
  }

  public String getName() {
    return name;
  }

  public Repository setName(String name) {
    this.name = name;
    return this;
  }

  public long getId() {
    return id;
  }

  public Repository setId(long id) {
    this.id = id;
    return this;
  }

  public Project getProject() {
    return project;
  }

  public Repository setProject(Project project) {
    this.project = project;
    return this;
  }

  @Override
  public String toString() {
    return "{" +
      "slug='" + slug + '\'' +
      ", name='" + name + '\'' +
      ", id=" + id +
      ", project=" + project +
      '}';
  }
}
