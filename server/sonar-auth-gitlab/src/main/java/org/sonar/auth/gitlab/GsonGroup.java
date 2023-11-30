/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.auth.gitlab;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * Lite representation of JSON response of GET https://docs.gitlab.com/ee/api/groups.html
 */
public class GsonGroup {

  @SerializedName("id")
  private String id;
  @SerializedName("full_path")
  private String fullPath;
  @SerializedName("description")
  private String description;

  public GsonGroup() {
    // http://stackoverflow.com/a/18645370/229031
    this("", "", "");
  }

  private GsonGroup(String id, String fullPath, String description) {
    this.id = id;
    this.fullPath = fullPath;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public String getFullPath() {
    return fullPath;
  }

  public String getDescription() {
    return description;
  }

  static List<GsonGroup> parse(String json) {
    Type collectionType = new TypeToken<Collection<GsonGroup>>() {
    }.getType();
    Gson gson = new Gson();
    return gson.fromJson(json, collectionType);
  }

}
