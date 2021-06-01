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

public class ProjectDetails extends Project {
  // https://docs.gitlab.com/ee/api/projects.html#get-single-project

  private Permissions permissions;

  public ProjectDetails(String name, String pathWithNamespace) {
    super(name, pathWithNamespace);
  }

  public ProjectDetails() {
    // http://stackoverflow.com/a/18645370/229031
    this(0, "", "", "", "", "");
  }

  public ProjectDetails(long id, String name, String nameWithNamespace, String path, String pathWithNamespace,
    String webUrl) {
    super(id, name, nameWithNamespace, path, pathWithNamespace, webUrl);
  }

  public Permissions getPermissions() {
    return permissions;
  }

  public ProjectDetails setPermissions(Permissions permissions) {
    this.permissions = permissions;
    return this;
  }
}
