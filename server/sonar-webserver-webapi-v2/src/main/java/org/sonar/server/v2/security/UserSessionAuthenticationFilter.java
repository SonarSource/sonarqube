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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security filter that bridges UserSession (from ThreadLocal) into Spring SecurityContext.
 * This filter runs after the UserSessionFilter (servlet filter) has already initialized the UserSession.
 *
 * <p><strong>Architecture:</strong> This filter makes SecurityContext the single source of truth
 * for user identity in /api/v2/* endpoints:</p>
 * <ol>
 *   <li>Reads the original UserSession from ThreadLocal (set by UserSessionFilter)</li>
 *   <li>Creates standard Spring Security PreAuthenticatedAuthenticationToken and stores in SecurityContext</li>
 *   <li>Replaces ThreadLocal with SecurityContextBackedUserSession for backwards compatibility</li>
 * </ol>
 *
 * <p>This eliminates dual ThreadLocal storage and makes SecurityContext the authoritative source.</p>
 */
public class UserSessionAuthenticationFilter extends OncePerRequestFilter {

  private final ThreadLocalUserSession threadLocalUserSession;

  public UserSessionAuthenticationFilter(ThreadLocalUserSession threadLocalUserSession) {
    this.threadLocalUserSession = threadLocalUserSession;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {

    if (threadLocalUserSession.hasSession()) {
      UserSession userSession = threadLocalUserSession.get();

      PreAuthenticatedAuthenticationToken authentication = createStandardAuthentication(userSession);
      SecurityContextHolder.getContext().setAuthentication(authentication);

      threadLocalUserSession.set(new SecurityContextBackedUserSession());
    } else {
      // No UserSession - set anonymous authentication for Spring Security
      // This ensures authorization checks work correctly (allows `permitAll()`)
      AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
        "anonymous",
        "anonymousUser",
        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
      );
      SecurityContextHolder.getContext().setAuthentication(anonymous);
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Create standard Spring Security PreAuthenticatedAuthenticationToken with user information.
   * User is already authenticated by UserSessionInitializer, so we use PreAuthenticatedAuthenticationToken.
   *
   * @param userSession the authenticated user session
   * @return PreAuthenticatedAuthenticationToken with SonarUserDetails as principal
   */
  private static PreAuthenticatedAuthenticationToken createStandardAuthentication(UserSession userSession) {
    Collection<GrantedAuthority> authorities = buildAuthorities(userSession);
    SonarUserDetails userDetails = new SonarUserDetails(userSession, authorities);

    return getPreAuthenticatedAuthenticationToken(userSession, userDetails, authorities);
  }

  private static @NonNull PreAuthenticatedAuthenticationToken getPreAuthenticatedAuthenticationToken(UserSession userSession, SonarUserDetails userDetails,
    Collection<GrantedAuthority> authorities) {
    String login = userSession.getLogin();
    PreAuthenticatedAuthenticationToken token;
    if (userSession.isLoggedIn()) {
      token = new PreAuthenticatedAuthenticationToken(userDetails, null, authorities) {
        @Override
        public @NonNull String getName() {
          return login;
        }
      };
    } else {
      token = new PreAuthenticatedAuthenticationToken(userDetails, null) {
        @Override
        public @NonNull String getName() {
          return login;
        }
      };
      token.setAuthenticated(false);
    }
    return token;
  }

  /**
   * Build Spring Security authorities from UserSession permissions.
   *
   * @param userSession the user session
   * @return collection of granted authorities
   */
  private static Collection<GrantedAuthority> buildAuthorities(UserSession userSession) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    // Add system administrator role
    if (userSession.isSystemAdministrator()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // Add authenticated user role
    if (userSession.isLoggedIn()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // Convert UserSession groups to Spring Security authorities
    List<GrantedAuthority> groupAuthorities = userSession.getGroups().stream()
      .map(group -> new SimpleGrantedAuthority("GROUP_" + group.getName()))
      .collect(Collectors.toList());
    authorities.addAll(groupAuthorities);

    return authorities;
  }
}
