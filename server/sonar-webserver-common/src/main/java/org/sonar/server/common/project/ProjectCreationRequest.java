/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.common.project;

import javax.annotation.Nullable;
import org.sonar.db.project.CreationMethod;

/**
 * Request object for project creation with parameters consolidated to avoid method signature limits.
 *
 * @param projectKey The unique key for the project
 * @param projectName The display name for the project
 * @param mainBranchName The name of the main branch (optional)
 * @param creationMethod How the project is being created
 * @param isPrivate Whether the project should be private (null for default visibility)
 * @param isManaged Whether the project is managed by auto-provisioning
 * @param allowExisting If true, enables idempotent behavior: returns existing project if it matches,
 *                      otherwise throws exception. If false, always attempts to create a new project
 *                      and throws exception if key already exists. Use true for PUT endpoints (create-or-update),
 *                      false for POST endpoints (strict creation).
 */
public record ProjectCreationRequest(
  String projectKey,
  String projectName,
  @Nullable String mainBranchName,
  CreationMethod creationMethod,
  @Nullable Boolean isPrivate,
  boolean isManaged,
  boolean allowExisting) {
}
