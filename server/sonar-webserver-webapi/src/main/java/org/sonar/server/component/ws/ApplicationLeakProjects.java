/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.component.ws;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ApplicationLeakProjects {

  @SerializedName("leakProjects")
  private List<LeakProject> projects = new ArrayList<>();

  public ApplicationLeakProjects() {
    // even if empty constructor is not required for Gson, it is strongly recommended:
    // http://stackoverflow.com/a/18645370/229031
  }

  public ApplicationLeakProjects addProject(LeakProject project) {
    this.projects.add(project);
    return this;
  }

  public Optional<LeakProject> getOldestLeak() {
    return projects.stream().min(Comparator.comparingLong(o -> o.leak));
  }

  public static ApplicationLeakProjects parse(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, ApplicationLeakProjects.class);
  }

  public String format() {
    Gson gson = new Gson();
    return gson.toJson(this, ApplicationLeakProjects.class);
  }

  public static class LeakProject {
    @SerializedName("id")
    private String id;
    @SerializedName("leak")
    private long leak;

    public LeakProject() {
      // even if empty constructor is not required for Gson, it is strongly recommended:
      // http://stackoverflow.com/a/18645370/229031
    }

    public LeakProject setId(String id) {
      this.id = id;
      return this;
    }

    public String getId() {
      return id;
    }

    public LeakProject setLeak(long leak) {
      this.leak = leak;
      return this;
    }

    public long getLeak() {
      return leak;
    }
  }

}
