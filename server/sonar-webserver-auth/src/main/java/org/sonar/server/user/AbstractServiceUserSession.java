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
package org.sonar.server.user;

/**
 * The session a caller that is a <em>service</em> rather than a person runs as: logged in and
 * attributable by name, holding no permission at all, and backed by no user account. Subclasses
 * supply only the login.
 *
 * <p>Being logged in is what satisfies {@code sonar.forceAuthentication}. Holding no permission — see
 * {@link PermissionlessUserSession} — is a floor, not a fence: such a caller can still reach any
 * endpoint that performs no permission check of its own. Bounding that is the job of the
 * {@link org.sonar.server.authentication.ServiceAuthentication} that authenticated it.
 */
public abstract class AbstractServiceUserSession extends PermissionlessUserSession {

  @Override
  public String getName() {
    return getLogin();
  }

  /**
   * There is no {@code users} row behind this session, so there is no uuid to report; asking for one
   * is a bug rather than an empty value to tolerate.
   */
  @Override
  public String getUuid() {
    throw new IllegalStateException(getClass().getSimpleName() + " does not contain a uuid.");
  }

  @Override
  public boolean isLoggedIn() {
    return true;
  }

  @Override
  public boolean isActive() {
    return true;
  }
}
