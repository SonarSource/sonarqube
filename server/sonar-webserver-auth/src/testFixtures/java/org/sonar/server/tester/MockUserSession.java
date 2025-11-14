/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.jetbrains.annotations.Nullable;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.AbstractUserSession;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.user.UserSession.IdentityProvider.SONARQUBE;

public class MockUserSession extends AbstractMockUserSession<MockUserSession> {
  private final String login;
  private String uuid;
  private String name;
  private List<GroupDto> groups = new ArrayList<>();
  private UserSession.IdentityProvider identityProvider;
  private UserSession.ExternalIdentity externalIdentity;
  private Long lastSonarlintConnectionDate;

  private boolean isAuthenticatedBrowserSession = false;

  public MockUserSession(String login) {
    super(MockUserSession.class);
    checkArgument(!login.isEmpty());
    this.login = login;
    setUuid(login + "uuid");
    setName(login + " name");
    setInternalIdentity();
  }

  public MockUserSession(UserDto userDto) {
    super(MockUserSession.class);
    checkArgument(!userDto.getLogin().isEmpty());
    this.login = userDto.getLogin();
    setUuid(userDto.getUuid());
    setName(userDto.getName());
    AbstractUserSession.Identity identity = AbstractUserSession.computeIdentity(userDto);
    this.identityProvider = identity.getIdentityProvider();
    this.externalIdentity = identity.getExternalIdentity();
  }

  @CheckForNull
  @Override
  public Long getLastSonarlintConnectionDate() {
    return lastSonarlintConnectionDate;
  }

  public MockUserSession setLastSonarlintConnectionDate(@Nullable Long lastSonarlintConnectionDate) {
    this.lastSonarlintConnectionDate = lastSonarlintConnectionDate;
    return this;
  }

  @Override
  public boolean isLoggedIn() {
    return true;
  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public boolean isAuthenticatedBrowserSession() {
    return isAuthenticatedBrowserSession;
  }

  @Override
  public String getLogin() {
    return this.login;
  }

  @Override
  public String getUuid() {
    return this.uuid;
  }

  public MockUserSession setUuid(String uuid) {
    this.uuid = requireNonNull(uuid);
    return this;
  }

  @Override
  public String getName() {
    return this.name;
  }

  public MockUserSession setName(String s) {
    this.name = requireNonNull(s);
    return this;
  }

  @Override
  public Collection<GroupDto> getGroups() {
    return groups;
  }

  public MockUserSession setGroups(GroupDto... groups) {
    this.groups = asList(groups);
    return this;
  }

  @Override
  public Optional<UserSession.IdentityProvider> getIdentityProvider() {
    return Optional.ofNullable(identityProvider);
  }

  public void setExternalIdentity(UserSession.IdentityProvider identityProvider, UserSession.ExternalIdentity externalIdentity) {
    checkArgument(identityProvider != SONARQUBE);
    this.identityProvider = identityProvider;
    this.externalIdentity = requireNonNull(externalIdentity);
  }

  public void setInternalIdentity() {
    this.identityProvider = SONARQUBE;
    this.externalIdentity = null;
  }

  @Override
  public Optional<UserSession.ExternalIdentity> getExternalIdentity() {
    return Optional.ofNullable(externalIdentity);
  }

  @Override
  public void flagAsBrowserSession() {
    isAuthenticatedBrowserSession = true;
  }
}
