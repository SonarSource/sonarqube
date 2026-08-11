/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.common.almsettings;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.db.DbSession;
import org.sonar.db.project.CreationMethod;
import org.sonar.server.component.ComponentCreationData;

public interface DevOpsProjectCreator {

  /**
   * Must only be called when {@link #permissionsFromDevopsPlatformUnavailableReason()} returns an empty {@link Optional}.
   * Implementations overriding this method must override that one too, otherwise callers keep rejecting the request
   * on the ground that this platform does not support permission checks.
   */
  boolean isScanAllowedUsingPermissionsFromDevopsPlatform();

  /**
   * Tells whether the permissions of the current user can be read from the DevOps platform, and if not, why.
   * {@link #isScanAllowedUsingPermissionsFromDevopsPlatform()} must only be called when this returns an empty {@link Optional}.
   *
   * @return a user-facing explanation of why the DevOps platform cannot be used to resolve the permissions of the current user,
   * or {@link Optional#empty()} when it can be used. The explanation is a clause meant to be embedded in a larger sentence,
   * so it starts in lower case and has no trailing punctuation.
   */
  default Optional<String> permissionsFromDevopsPlatformUnavailableReason() {
    return Optional.of("permissions cannot be checked on this DevOps platform because it does not support permission checks");
  }

  ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, CreationMethod creationMethod, Boolean monorepo, @Nullable String projectKey,
    @Nullable String projectName, boolean allowExisting);

}
