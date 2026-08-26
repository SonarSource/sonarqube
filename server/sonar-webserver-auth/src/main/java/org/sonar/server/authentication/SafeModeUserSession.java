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
package org.sonar.server.authentication;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.server.user.PermissionlessUserSession;

/**
 * The session requests run as while the server is in safe mode: nameless, not logged in, not active,
 * and holding no permission at all (see {@link PermissionlessUserSession}).
 */
@Immutable
public class SafeModeUserSession extends PermissionlessUserSession {

  @CheckForNull
  @Override
  public String getLogin() {
    return null;
  }

  @CheckForNull
  @Override
  public String getUuid() {
    return null;
  }

  @CheckForNull
  @Override
  public String getName() {
    return null;
  }

  @Override
  public boolean isLoggedIn() {
    return false;
  }

  @Override
  public boolean isActive() {
    return false;
  }
}
