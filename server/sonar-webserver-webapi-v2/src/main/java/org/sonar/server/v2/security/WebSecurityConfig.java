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

import org.sonar.server.user.ThreadLocalUserSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for SonarQube Web API v2.
 * <p>
 * This configuration integrates Spring Security's declarative authorization with SonarQube's existing
 * authentication system, enabling the use of method-level security annotations like {@code @PreAuthorize}
 * alongside traditional manual authorization checks.
 * </p>
 *
 * <h3>Scope</h3>
 * <p>
 * This security configuration applies ONLY to {@code /api/v2/**} endpoints. Legacy {@code /api/*} endpoints
 * are not affected and continue to use ThreadLocalUserSession directly.
 * </p>
 *
 * <h3>Architecture Overview</h3>
 * <p>
 * API v2 uses a two-layer authentication and authorization architecture:
 * </p>
 * <ol>
 *   <li><b>Layer 1 - Servlet Filter (UserSessionInitializer):</b> Authenticates requests and populates
 *       ThreadLocal UserSession. This runs BEFORE Spring Security.</li>
 *   <li><b>Layer 2 - Spring Security:</b> Bridges UserSession to Spring SecurityContext. Authorization is
 *       primarily handled by controllers using {@code @RequireAuthentication}, Spring Security annotations, or
 *       manual permission checks, consistent with API v1 behavior.</li>
 * </ol>
 *
 * <h3>Integration with UserSessionInitializer</h3>
 * <p>
 * API v2 requests go through {@code UserSessionInitializer} for authentication (populating ThreadLocal
 * UserSession), then through Spring Security (bridging to SecurityContext). Public endpoints that should
 * be accessible without authentication must be added to {@code UserSessionInitializer.SKIPPED_URLS} to
 * bypass the servlet-level {@code force_authentication} check.
 * </p>
 * <p>
 * <strong>Note for extensions:</strong> Currently, SKIPPED_URLS is hardcoded in the server, which limits
 * extensibility. Extensions cannot easily add their own public API v2 endpoints. A future improvement
 * could use annotations (e.g., {@code @PublicEndpoint}) to allow extensions to mark endpoints as public.
 * </p>
 *
 * <h3>Filter Chain</h3>
 * <p>
 * The {@link UserSessionAuthenticationFilter} bridges SonarQube's ThreadLocal UserSession into Spring Security's
 * SecurityContext. This filter runs early in the Spring Security filter chain, before authorization checks.
 * For authenticated requests, it creates a {@link UserSessionAuthentication} containing the UserSession.
 * For unauthenticated requests on public endpoints, it creates an AnonymousAuthenticationToken.
 * </p>
 *
 * <h3>Authorization Approach</h3>
 * <p>
 * Spring Security is configured with a permissive default ({@code permitAll()}), delegating authorization
 * to controllers. This mirrors API v1 behavior where endpoints handle their own authorization checks.
 * Controllers should use annotations or manual checks as needed:
 * </p>
 * <ul>
 *   <li>{@code @PreAuthorize("isAuthenticated()")} - Requires any authenticated user</li>
 *   <li>{@code @PreAuthorize("hasRole('ADMIN')")} - Requires system administrator</li>
 *   <li>{@code @RequireAuthentication} - Custom annotation equivalent to isAuthenticated()</li>
 *   <li>Manual checks via {@code UserSession} - For fine-grained permission checks</li>
 * </ul>
 *
 * <h3>Path Matching</h3>
 * <p>
 * Spring Security's DelegatingFilterProxy is registered at servlet container level and sees full paths
 * (e.g., {@code /api/v2/system/liveness}), not servlet-relative paths (e.g., {@code /system/liveness}).
 * All requestMatchers must use full paths prefixed with {@link #API_V2_PATH}.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, proxyTargetClass = true)
public class WebSecurityConfig {

  private static final String API_V2_PATH = "/api/v2";

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, ThreadLocalUserSession threadLocalUserSession) {

    http
      .securityMatcher(API_V2_PATH + "/**")
      .csrf(AbstractHttpConfigurer::disable)
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable)
      .logout(AbstractHttpConfigurer::disable)
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .anonymous(anonymous -> anonymous
        .principal("anonymousUser")
        .authorities("ROLE_ANONYMOUS"))
      .addFilterBefore(new UserSessionAuthenticationFilter(threadLocalUserSession),
        UsernamePasswordAuthenticationFilter.class)
      .authorizeHttpRequests(authz -> authz
        .anyRequest().permitAll()
      );

    return http.build();
  }
}
