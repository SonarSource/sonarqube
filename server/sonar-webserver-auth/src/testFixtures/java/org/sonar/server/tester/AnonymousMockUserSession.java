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
package org.sonar.server.tester;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;

public class AnonymousMockUserSession extends AbstractMockUserSession<AnonymousMockUserSession> {

  public AnonymousMockUserSession() {
    super(AnonymousMockUserSession.class);
  }

  @Override
  public boolean isRoot() {
    return false;
  }

  @Override
  public String getLogin() {
    return null;
  }

  @Override public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Integer getUserId() {
    return null;
  }

  @Override
  public boolean isLoggedIn() {
    return false;
  }

  @Override
  public Collection<GroupDto> getGroups() {
    return Collections.emptyList();
  }

  @Override
  public Optional<IdentityProvider> getIdentityProvider() {
    return Optional.empty();
  }

  @Override
  public Optional<ExternalIdentity> getExternalIdentity() {
    return Optional.empty();
  }

  @Override
  public boolean hasMembershipImpl(OrganizationDto organizationDto) {
    return false;
  }
}
