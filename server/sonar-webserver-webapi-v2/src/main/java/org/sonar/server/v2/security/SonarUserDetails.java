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

import java.util.Collection;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.sonar.server.user.UserSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security {@link UserDetails} implementation that wraps a SonarQube {@link UserSession}.
 *
 * <p>This bridges SonarQube's authentication model with Spring Security's standard UserDetails interface,
 * allowing SecurityContext to use standard Spring Security types while preserving access to the full UserSession.</p>
 *
 * <p>The wrapped UserSession is stored for backward compatibility with code that needs access to
 * UserSession-specific methods (permissions, groups, etc.).</p>
 */
public class SonarUserDetails implements UserDetails {

  private final UserSession userSession;
  private final Collection<? extends GrantedAuthority> authorities;

  /**
   * Creates a new SonarUserDetails wrapping the given UserSession.
   *
   * @param userSession the user session to wrap
   * @param authorities the Spring Security authorities for this user
   */
  public SonarUserDetails(UserSession userSession, Collection<? extends GrantedAuthority> authorities) {
    this.userSession = userSession;
    this.authorities = authorities;
  }

  /**
   * Get the wrapped UserSession for backward compatibility with code that needs
   * UserSession-specific functionality.
   *
   * @return the underlying UserSession
   */
  public UserSession getUserSession() {
    return userSession;
  }

  /**
   * Returns the user's login.
   *
   * @return the login
   */
  public String getLogin() {
    return userSession.getLogin();
  }

  /**
   * Returns the user's display name.
   *
   * @return the user's name
   */
  public String getName() {
    return userSession.getName();
  }

  /**
   * Returns whether the user is a system administrator.
   *
   * @return true if admin, false otherwise
   */
  public boolean isSystemAdministrator() {
    return userSession.isSystemAdministrator();
  }

  // UserDetails interface methods

  @Override
  public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return null;
  }

  /**
   * Returns the user's UUID as the username.
   * <p>
   * This aligns with Cloud's JWT authentication where the 'sub' claim contains the user UUID.
   * The unified sonar-spring module's SecurityContextUtils.extractUserId() calls getUsername()
   * to get the user ID, which should be the UUID for consistency across Server and Cloud.
   * </p>
   * <p>
   * The login is accessed via the custom getLogin() method, which our enterprise-specific
   * SecurityContextUtils uses when extracting from SonarUserDetails.
   * </p>
   *
   * @return the user UUID
   */
  @Override
  public @NonNull String getUsername() {
    return Objects.requireNonNull(userSession.getUuid());
  }

  @Override
  public boolean isEnabled() {
    return userSession.isActive();
  }
}
