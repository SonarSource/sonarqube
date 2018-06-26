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
package org.sonar.server.user;

import com.google.common.collect.HashMultimap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;

public class LightUserSessionRule extends BaseUserSession implements TestRule {
  private HashMultimap<String, String> projectUuidByPermission = HashMultimap.create();
  private Set<String> projectPermissionsCheckedByUuid = new HashSet<>();
  private Map<String, String> projectUuidByComponentUuid = newHashMap();
  private boolean root = false;
  private String login;
  private Integer userId;
  private String uuid;
  private String name;
  private Set<GroupDto> groups = new HashSet<>();

  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        reset();
        try {
          base.evaluate();
        } finally {
          reset();
        }
      }
    };
  }

  private void reset() {
    this.root = false;
    this.login = null;
    this.userId = null;
    this.userId = null;
    this.name = null;
    this.groups.clear();
  }

  public LightUserSessionRule setRoot() {
    this.root = true;
    return this;
  }

  public LightUserSessionRule anonymous() {
    reset();
    return this;
  }

  public LightUserSessionRule logIn() {
    return logIn(randomAlphabetic(6));
  }

  public LightUserSessionRule logIn(String login) {
    reset();
    this.login = login;
    return this;
  }

  public LightUserSessionRule logIn(UserDto userDto) {
    reset();
    logIn(userDto.getLogin());
    setUserId(userDto.getId());
    return this;
  }

  public LightUserSessionRule setUserId(Integer userId) {
    this.userId = userId;
    return this;
  }

  public LightUserSessionRule setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public LightUserSessionRule setName(String name) {
    this.name = name;
    return this;
  }

  public LightUserSessionRule setGroups(GroupDto... groups) {
    this.groups.clear();
    this.groups.addAll(Arrays.asList(groups));
    return this;
  }

  /**
   * Use this method to register public root component and non root components the UserSession must be aware of.
   * (ie. this method can be used to emulate the content of the DB)
   */
  public LightUserSessionRule registerComponents(ComponentDto... components) {
    Arrays.stream(components)
      .forEach(component -> {
        if (component.projectUuid().equals(component.uuid()) && !component.isPrivate()) {
          this.projectUuidByPermission.put(UserRole.USER, component.uuid());
          this.projectUuidByPermission.put(UserRole.CODEVIEWER, component.uuid());
          this.projectPermissionsCheckedByUuid.add(UserRole.USER);
          this.projectPermissionsCheckedByUuid.add(UserRole.CODEVIEWER);
        }
        this.projectUuidByComponentUuid.put(component.uuid(), component.projectUuid());
      });
    return this;
  }

  @Override
  protected boolean hasPermissionImpl(OrganizationPermission permission, String organizationUuid) {
    throw new UnsupportedOperationException("hasPermissionImpl not implemented");
  }

  @Override
  protected Optional<String> componentUuidToProjectUuid(String componentUuid) {
    return Optional.ofNullable(projectUuidByComponentUuid.get(componentUuid));
  }

  @Override
  protected boolean hasProjectUuidPermission(String permission, String projectUuid) {
    return projectPermissionsCheckedByUuid.contains(permission) && projectUuidByPermission.get(permission).contains(projectUuid);
  }

  @CheckForNull
  @Override
  public String getLogin() {
    return login;
  }

  @CheckForNull
  @Override
  public String getUuid() {
    return uuid;
  }

  @CheckForNull
  @Override
  public String getName() {
    return name;
  }

  @CheckForNull
  @Override
  public Integer getUserId() {
    return userId;
  }

  @Override
  public Collection<GroupDto> getGroups() {
    return groups;
  }

  @Override
  public boolean isLoggedIn() {
    return login != null;
  }

  @Override
  public boolean isRoot() {
    return root;
  }

  @Override
  public UserSession checkIsRoot() {
    throw new UnsupportedOperationException("checkIsRoot not implemented");
  }

  @Override
  public UserSession checkLoggedIn() {
    throw new UnsupportedOperationException("checkLoggedIn not implemented");
  }

  @Override
  public UserSession checkPermission(OrganizationPermission permission, OrganizationDto organization) {
    throw new UnsupportedOperationException("checkPermission not implemented");
  }

  @Override
  public UserSession checkPermission(OrganizationPermission permission, String organizationUuid) {
    throw new UnsupportedOperationException("checkPermission not implemented");
  }

  @Override
  public UserSession checkComponentPermission(String projectPermission, ComponentDto component) {
    throw new UnsupportedOperationException("checkComponentPermission not implemented");
  }

  @Override
  public UserSession checkComponentUuidPermission(String permission, String componentUuid) {
    throw new UnsupportedOperationException("checkComponentUuidPermission not implemented");
  }

  @Override
  public boolean isSystemAdministrator() {
    throw new UnsupportedOperationException("isSystemAdministrator not implemented");
  }

  @Override
  public UserSession checkIsSystemAdministrator() {
    throw new UnsupportedOperationException("checkIsSystemAdministrator not implemented");
  }
}
