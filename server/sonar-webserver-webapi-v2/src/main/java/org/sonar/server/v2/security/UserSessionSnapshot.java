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
package org.sonar.server.v2.security;

import javax.annotation.Nullable;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Captures the user session and the {@link SecurityContext} of the current thread before a nested call goes through the
 * Web API v2 security filter chain, and restores them on {@link #close()}.
 *
 * <p>When the caller is itself a Web API v2 request, its thread-local session is a {@link SecurityContextBackedUserSession}.
 * That session is replaced by the real {@link UserSession} it delegates to, because {@link UserSessionAuthenticationFilter}
 * would otherwise wrap it again, and the filter chain installs a new empty {@link SecurityContext} it can't read from.</p>
 */
public final class UserSessionSnapshot implements AutoCloseable {

  private final ThreadLocalUserSession threadLocalUserSession;
  @Nullable
  private final UserSession userSession;
  @Nullable
  private final SecurityContext securityContext;

  private UserSessionSnapshot(ThreadLocalUserSession threadLocalUserSession, @Nullable UserSession userSession,
    @Nullable SecurityContext securityContext) {
    this.threadLocalUserSession = threadLocalUserSession;
    this.userSession = userSession;
    this.securityContext = securityContext;
  }

  public static UserSessionSnapshot take(ThreadLocalUserSession threadLocalUserSession) {
    UserSession userSession = threadLocalUserSession.hasSession() ? threadLocalUserSession.get() : null;
    SecurityContext securityContext = SecurityContextHolder.getContext();
    Authentication authentication = securityContext.getAuthentication();
    if (userSession instanceof SecurityContextBackedUserSession && authentication != null
      && authentication.getPrincipal() instanceof SonarUserDetails sonarUserDetails) {
      threadLocalUserSession.set(sonarUserDetails.getUserSession());
    }
    return new UserSessionSnapshot(threadLocalUserSession, userSession, authentication == null ? null : securityContext);
  }

  @Override
  public void close() {
    if (securityContext == null) {
      SecurityContextHolder.clearContext();
    } else {
      SecurityContextHolder.setContext(securityContext);
    }
    if (userSession == null) {
      threadLocalUserSession.unload();
    } else {
      threadLocalUserSession.set(userSession);
    }
  }
}
