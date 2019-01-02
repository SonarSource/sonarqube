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
package org.sonar.server.user;

import org.sonar.api.server.ws.Request;

/**
 * Passcode for accessing some web services, usually for connecting
 * monitoring tools without using the credentials
 * of a system administrator.
 *
 * Important - the web services accepting passcode must be listed in
 * {@link org.sonar.server.authentication.UserSessionInitializer#URL_USING_PASSCODE}.
 */
public interface SystemPasscode {

  /**
   * Whether the system passcode is provided by the HTTP request or not.
   * Returns {@code false} if passcode is not configured.
   */
  boolean isValid(Request request);

}
