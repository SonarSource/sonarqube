/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class Repository {

  @SerializedName("slug")
  private String slug;

  @SerializedName("name")
  private String name;

  @SerializedName("id")
  private long id;

  @SerializedName("project")
  private Project project;

  @SerializedName("links")
  private Links links;

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

  @CheckForNull
  public String getSelfHref() {
    if (links == null || links.self == null || links.self.isEmpty()) {
      return null;
    }
    return links.self.get(0).href;
  }

  public Repository setLinks(@Nullable Links links) {
    this.links = links;
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

  public static class Links {

    @SerializedName("self")
    private List<Link> self;

    public Links() {
      // http://stackoverflow.com/a/18645370/229031
    }

    public Links(List<Link> self) {
      this.self = self;
    }
  }

  public static class Link {

    @SerializedName("href")
    private String href;

    public Link() {
      // http://stackoverflow.com/a/18645370/229031
    }

    public Link(String href) {
      this.href = href;
    }
  }
}
