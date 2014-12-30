/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.batch;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ProjectReferentialsQuery {

  private String projectOrModuleKey;
  private String profileName;
  private boolean preview;

  private ProjectReferentialsQuery() {
    // No direct call
  }

  public boolean isPreview() {
    return preview;
  }

  public ProjectReferentialsQuery setPreview(boolean preview) {
    this.preview = preview;
    return this;
  }

  @CheckForNull
  public String getProfileName() {
    return profileName;
  }

  public ProjectReferentialsQuery setProfileName(@Nullable String profileName) {
    this.profileName = profileName;
    return this;
  }

  public String getModuleKey() {
    return projectOrModuleKey;
  }

  public ProjectReferentialsQuery setModuleKey(String projectOrModuleKey) {
    this.projectOrModuleKey = projectOrModuleKey;
    return this;
  }

  public static ProjectReferentialsQuery create(){
    return new ProjectReferentialsQuery();
  }
}
