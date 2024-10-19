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
package org.sonar.server.permission.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.sonar.server.exceptions.BadRequestException.checkRequest;

/**
 * Reference to a project <b>as defined by web service callers</b>. It allows to reference a project
 * by its (functional) key or by its (technical) uuid.
 *
 * <p>Factory methods guarantee that the project id and project key are not provided at the same time.</p>
 */
public class ProjectWsRef {
  private static final String MSG_ID_OR_KEY_MUST_BE_PROVIDED = "Project id or project key can be provided, not both.";
  private final String uuid;
  private final String key;

  private ProjectWsRef(@Nullable String uuid, @Nullable String key) {
    validateUuidAndKeyPair(uuid, key);
    this.uuid = uuid;
    this.key = key;
  }

  public static void validateUuidAndKeyPair(@Nullable String uuid, @Nullable String key) {
    checkRequest(uuid != null ^ key != null, MSG_ID_OR_KEY_MUST_BE_PROVIDED);
  }

  public static ProjectWsRef newWsProjectRef(@Nullable String uuid, @Nullable String key) {
    return new ProjectWsRef(uuid, key);
  }

  @CheckForNull
  public String uuid() {
    return this.uuid;
  }

  @CheckForNull
  public String key() {
    return this.key;
  }
}
