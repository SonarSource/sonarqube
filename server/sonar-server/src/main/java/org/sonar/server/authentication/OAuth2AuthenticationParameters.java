/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Optional;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;

/**
 * This class is used to store some parameters during the OAuth2 authentication process, by using a cookie.
 *
 * Parameters are read from the request during {@link InitFilter#init(FilterConfig)}
 * and reset when {@link OAuth2IdentityProvider.CallbackContext#redirectToRequestedPage()} is called.
 */
public interface OAuth2AuthenticationParameters {

  void init(HttpServletRequest request, HttpServletResponse response);

  Optional<String> getReturnTo(HttpServletRequest request);

  Optional<Boolean> getAllowEmailShift(HttpServletRequest request);

  void delete(HttpServletRequest request, HttpServletResponse response);

}
