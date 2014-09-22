/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.security;

import java.util.Collections;
import java.util.List;

import org.sonar.api.ServerExtension;

/**
 * @since 2.14
 */
public abstract class SecurityRealm implements ServerExtension {

  /**
   * @return unique name of this realm, e.g. "ldap"
   */
  public String getName() {
    return getClass().getSimpleName();
  }

  /**
   * Invoked during server startup and can be used to initialize internal state.
   */
  public void init() {
  }

  /**
   * @return {@link LoginPasswordAuthenticator} associated with this realm, never null
   * @deprecated replaced by doGetAuthenticator in version 3.1
   */
  @Deprecated
  public LoginPasswordAuthenticator getLoginPasswordAuthenticator() {
    return null;
  }

  /**
   * @since 3.1
   * @deprecated replaced by getAuthenticators in version 5.0
   */
  @Deprecated
  public Authenticator doGetAuthenticator() {
    // this method is not overridden when deprecated getLoginPasswordAuthenticator is used
    return new Authenticator() {
      @Override
      public boolean doAuthenticate(Context context) {
        return getLoginPasswordAuthenticator().authenticate(context.getUsername(), context.getPassword());
      }
    };
  }

  /**
   * @return {@link Authenticator}s associated with this realm, empty {@link List} if not supported
   * @since 5.0
   */
  public List<Authenticator> getAuthenticators() {
    // this method is not overridden when deprecated doGetAuthenticator or getLoginPasswordAuthenticator is used
    return Collections.singletonList(doGetAuthenticator());
  }

  /**
   * @return {@link ExternalUsersProvider} associated with this realm, null if not supported
   * @deprecated replaced by getUsersProviders in version 5.0
   */
  @Deprecated
  public ExternalUsersProvider getUsersProvider() {
    return null;
  }

  /**
   * @return {@link ExternalUsersProvider}s associated with this realm, empty {@link List} if not supported
   * @since 5.0
   */
  public List<ExternalUsersProvider> getUsersProviders() {
    // this method is not overridden when deprecated getUsersProvider is used
    if (getUsersProvider() == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(getUsersProvider());
  }

  /**
   * @return {@link ExternalGroupsProvider} associated with this realm, null if not supported
   * @deprecated replaced by getGroupsProviders in version 5.0
   */
  @Deprecated
  public ExternalGroupsProvider getGroupsProvider() {
    return null;
  }

  /**
   * @return {@link ExternalGroupsProvider}s associated with this realm, empty {@link List} if not supported
   * @since 5.0
   */
  public List<ExternalGroupsProvider> getGroupsProviders() {
    // this method is not overridden when deprecated getGroupsProvider is used
    if (getGroupsProvider() == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(getGroupsProvider());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getName()).append(": {");
    
    sb.append("authenticators: [");
    for (Authenticator authenticator : getAuthenticators()) {
      sb.append(authenticator.getClass().getSimpleName()).append(", ");
    }
    sb.append("]");
    
    sb.append("userProviders: [");
    for (ExternalUsersProvider userProvider : getUsersProviders()) {
      sb.append(userProvider.getClass().getSimpleName()).append(", ");
    }
    sb.append("]");
    
    sb.append("groupProviders: [");
    for (ExternalGroupsProvider groupProvider : getGroupsProviders()) {
      sb.append(groupProvider.getClass().getSimpleName()).append(", ");
    }
    sb.append("]");
    
    sb.append("}");
    return sb.toString();
  }
}
