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
package org.sonar.server.authentication;

import javax.annotation.CheckForNull;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.HttpFilter;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.sonar.server.authentication.AuthenticationError.handleError;

public abstract class AuthenticationFilter extends HttpFilter {

  static final String CALLBACK_PATH = "/oauth2/callback/";
  private final IdentityProviderRepository identityProviderRepository;

  protected AuthenticationFilter(IdentityProviderRepository identityProviderRepository) {
    this.identityProviderRepository = identityProviderRepository;
  }

  /**
   * @return the {@link IdentityProvider} for the key extracted in the request if is exists, or {@code null}, in which
   *         case the request is fully handled and caller should not handle it
   */
  @CheckForNull
  IdentityProvider resolveProviderOrHandleResponse(HttpRequest request, HttpResponse response, String path) {
    String requestUri = request.getRequestURI();
    String providerKey = extractKeyProvider(requestUri, request.getContextPath() + path);
    if (providerKey == null) {
      handleError(request, response, "No provider key found in URI");
      return null;
    }
    try {
      return identityProviderRepository.getEnabledByKey(providerKey);
    } catch (Exception e) {
      handleError(e, request, response, format("Failed to retrieve IdentityProvider for key '%s'", providerKey));
      return null;
    }
  }

  @CheckForNull
  private static String extractKeyProvider(String requestUri, String context) {
    if (requestUri.contains(context)) {
      String key = requestUri.replace(context, "");
      if (!isNullOrEmpty(key)) {
        return key;
      }
    }
    return null;
  }
}
