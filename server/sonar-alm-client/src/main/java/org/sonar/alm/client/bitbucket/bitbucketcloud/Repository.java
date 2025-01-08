/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.alm.client.bitbucket.bitbucketcloud;

import com.google.gson.annotations.SerializedName;

public class Repository {

  @SerializedName("slug")
  private String slug;

  @SerializedName("name")
  private String name;

  @SerializedName("uuid")
  private String uuid;

  @SerializedName("project")
  private Project project;

  @SerializedName("mainbranch")
  private MainBranch mainBranch;

  public Repository() {
    // http://stackoverflow.com/a/18645370/229031
  }

  public Repository(String uuid, String slug, String name, Project project, MainBranch mainBranch) {
    this.uuid = uuid;
    this.slug = slug;
    this.name = name;
    this.project = project;
    this.mainBranch = mainBranch;
  }

  public String getSlug() {
    return slug;
  }

  public String getName() {
    return name;
  }

  public String getUuid() {
    return uuid;
  }

  public Project getProject() {
    return project;
  }

  public MainBranch getMainBranch() {
    return mainBranch;
  }
}
