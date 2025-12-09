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
package org.sonar.server.common.component;

import javax.annotation.Nullable;
import org.sonar.db.project.CreationMethod;

public record ComponentCreationParameters(NewComponent newComponent,
                                          @Nullable String userUuid,
                                          @Nullable String userLogin,
                                          @Nullable String mainBranchName,
                                          boolean isManaged,
                                          CreationMethod creationMethod) {

  public static ProjectCreationDataBuilder builder() {
    return new ProjectCreationDataBuilder();
  }

  public static final class ProjectCreationDataBuilder {
    private NewComponent newComponent;
    private String userUuid = null;
    private String userLogin = null;
    private String mainBranchName = null;
    private boolean isManaged = false;
    private CreationMethod creationMethod;

    public ProjectCreationDataBuilder newComponent(NewComponent newComponent) {
      this.newComponent = newComponent;
      return this;
    }

    public ProjectCreationDataBuilder userUuid(@Nullable String userUuid) {
      this.userUuid = userUuid;
      return this;
    }

    public ProjectCreationDataBuilder userLogin(@Nullable String userLogin) {
      this.userLogin = userLogin;
      return this;
    }

    public ProjectCreationDataBuilder mainBranchName(@Nullable String mainBranchName) {
      this.mainBranchName = mainBranchName;
      return this;
    }

    public ProjectCreationDataBuilder isManaged(boolean isManaged) {
      this.isManaged = isManaged;
      return this;
    }

    public ProjectCreationDataBuilder creationMethod(CreationMethod creationMethod) {
      this.creationMethod = creationMethod;
      return this;
    }

    public ComponentCreationParameters build() {
      return new ComponentCreationParameters(newComponent, userUuid, userLogin, mainBranchName, isManaged, creationMethod);
    }
  }
}
