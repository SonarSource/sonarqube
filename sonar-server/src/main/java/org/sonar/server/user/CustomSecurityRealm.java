
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
package org.sonar.server.user;

import java.util.List;

import org.sonar.api.security.Authenticator;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.SecurityRealm;

import com.google.common.collect.ImmutableList;


public class CustomSecurityRealm extends SecurityRealm {

  private final List<Authenticator> authenticators;
  private final List<ExternalUsersProvider> usersProviders;
  private final List<ExternalGroupsProvider> groupsProviders;

  public CustomSecurityRealm(List<Authenticator> authenticators,
      List<ExternalUsersProvider> usersProviders,
      List<ExternalGroupsProvider> groupsProviders) {
    this.authenticators = ImmutableList.copyOf(authenticators);
    this.usersProviders = ImmutableList.copyOf(usersProviders);
    this.groupsProviders = ImmutableList.copyOf(groupsProviders);
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public List<Authenticator> getAuthenticators() {
    return this.authenticators;
  }

  @Override
  public List<ExternalUsersProvider> getUsersProviders() {
    return this.usersProviders;
  }

  @Override
  public List<ExternalGroupsProvider> getGroupsProviders() {
    return this.groupsProviders;
  }

}
