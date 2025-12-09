/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.alm.setting;

public class ProjectAlmKeyAndProject {

  private String projectUuid;
  private String almId;
  private String url;
  private Boolean monorepo;

  public ProjectAlmKeyAndProject() {
    // keep empty
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public ProjectAlmKeyAndProject setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public String getAlmId() {
    return almId;
  }

  public ProjectAlmKeyAndProject setAlmId(String almId) {
    this.almId = almId;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public ProjectAlmKeyAndProject setUrl(String url) {
    this.url = url;
    return this;
  }

  public Boolean getMonorepo() {
    return monorepo;
  }
  public ProjectAlmKeyAndProject setMonorepo(Boolean monorepo) {
    this.monorepo = monorepo;
    return this;
  }
}
