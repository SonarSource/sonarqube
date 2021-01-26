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
package org.sonar.alm.client.gitlab;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.util.LinkedList;
import java.util.List;

public class Project {
  // https://docs.gitlab.com/ee/api/projects.html#get-single-project
  // https://docs.gitlab.com/ee/api/projects.html#list-all-projects
  @SerializedName("id")
  private long id;

  @SerializedName("name")
  private final String name;

  @SerializedName("name_with_namespace")
  private String nameWithNamespace;

  @SerializedName("path")
  private String path;

  @SerializedName("path_with_namespace")
  private final String pathWithNamespace;

  @SerializedName("web_url")
  private String webUrl;

  public Project(String name, String pathWithNamespace) {
    this.name = name;
    this.pathWithNamespace = pathWithNamespace;
  }

  public Project() {
    // http://stackoverflow.com/a/18645370/229031
    this(0, "", "", "", "", "");
  }

  public Project(long id, String name, String nameWithNamespace, String path, String pathWithNamespace,
    String webUrl) {
    this.id = id;
    this.name = name;
    this.nameWithNamespace = nameWithNamespace;
    this.path = path;
    this.pathWithNamespace = pathWithNamespace;
    this.webUrl = webUrl;
  }


  public static Project parseJson(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, Project.class);
  }

  public static List<Project> parseJsonArray(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, new TypeToken<LinkedList<Project>>() {
    }.getType());
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getNameWithNamespace() {
    return nameWithNamespace;
  }

  public String getPath() {
    return path;
  }

  public String getPathWithNamespace() {
    return pathWithNamespace;
  }

  public String getWebUrl() {
    return webUrl;
  }

}
