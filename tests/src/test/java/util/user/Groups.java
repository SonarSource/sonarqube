/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package util.user;

import com.google.gson.Gson;
import java.util.List;
import org.sonarqube.qa.util.Tester;

/**
 * @deprecated replaced by {@link Tester}
 */
@Deprecated
public class Groups {

  private List<Group> groups;

  private Groups(List<Group> groups) {
    this.groups = groups;
  }

  public List<Group> getGroups() {
    return groups;
  }

  public static Groups parse(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, Groups.class);
  }

  public static class Group {
    private final String name;

    private Group(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
