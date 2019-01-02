/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.server.authentication;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

/**
 * Entry-point to define a new Identity provider.
 * Only one of this two interfaces can be used :
 * <ul>
 *   <li>{@link OAuth2IdentityProvider} for OAuth2 authentication</li>
 *   <li>{@link BaseIdentityProvider} for other kind of authentication</li>
 * </ul>
 *
 * @since 5.4
 */
@ServerSide
@ExtensionPoint
public interface IdentityProvider {

  /**
   * Unique key of provider, for example "github".
   * Must not be blank.
   */
  String getKey();

  /**
   * Name displayed in login form.
   * Must not be blank.
   */
  String getName();

  /**
   * Display information for the login form
   */
  Display getDisplay();

  /**
   * Is the provider fully configured and enabled ? If {@code true}, then
   * the provider is available in login form.
   */
  boolean isEnabled();

  /**
   * Can users sign-up (connecting with their account for the first time) ? If {@code true},
   * then users can register and create their account into SonarQube, else only already
   * registered users can login.
   */
  boolean allowsUsersToSignUp();

}
