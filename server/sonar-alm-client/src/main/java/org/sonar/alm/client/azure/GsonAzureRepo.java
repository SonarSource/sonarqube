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
package org.sonar.alm.client.azure;

import com.google.gson.annotations.SerializedName;

public class GsonAzureRepo {
  @SerializedName("id")
  private String id;

  @SerializedName("name")
  private String name;

  @SerializedName("url")
  private String url;

  @SerializedName("project")
  private GsonAzureProject project;

  public GsonAzureRepo() {
    // http://stackoverflow.com/a/18645370/229031
  }

  public GsonAzureRepo(String id, String name, String url, GsonAzureProject project) {
    this.id = id;
    this.name = name;
    this.url = url;
    this.project = project;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public GsonAzureProject getProject() {
    return project;
  }
}
