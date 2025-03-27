/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.user.GroupDto;

import static java.util.Objects.requireNonNull;

public interface UserSession {

  /**
   * Login of the authenticated user. Returns {@code null}
   * if {@link #isLoggedIn()} is {@code false}.
   */
  @CheckForNull
  String getLogin();

  /**
   * Uuid of the authenticated user. Returns {@code null}
   * if {@link #isLoggedIn()} is {@code false}.
   */
  @CheckForNull
  String getUuid();

  /**
   * Name of the authenticated user. Returns {@code null}
   * if {@link #isLoggedIn()} is {@code false}.
   */
  @CheckForNull
  String getName();

  @CheckForNull
  Long getLastSonarlintConnectionDate();

  /**
   * The groups that the logged-in user is member of. An empty
   * collection is returned if {@link #isLoggedIn()} is {@code false}.
   */
  Collection<GroupDto> getGroups();

  boolean shouldResetPassword();

  /**
   * This enum supports by name only the few providers for which specific code exists.
   */
  enum IdentityProvider {
    SONARQUBE("sonarqube"), GITHUB("github"), BITBUCKETCLOUD("bitbucket"), OTHER("other");

    final String key;

    IdentityProvider(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

    public static IdentityProvider getFromKey(String key) {
      return Arrays.stream(IdentityProvider.values())
        .filter(i -> i.getKey().equals(key))
        .findAny()
        .orElse(OTHER);
    }
  }

  /**
   * @return empty if user is anonymous
   */
  Optional<IdentityProvider> getIdentityProvider();

  record ExternalIdentity(String id, String login) {
    public ExternalIdentity(String id, String login) {
      this.id = requireNonNull(id, "id can't be null");
      this.login = requireNonNull(login, "login can't be null");
    }

    @Override
    public String toString() {
      return "ExternalIdentity{" +
        "id='" + id + '\'' +
        ", login='" + login + '\'' +
        '}';
    }

  }

  /**
   * @return empty if {@link #getIdentityProvider()} returns empty or {@link IdentityProvider#SONARQUBE}
   */
  Optional<ExternalIdentity> getExternalIdentity();

  /**
   * Whether the user is logged-in or anonymous.
   */
  boolean isLoggedIn();

  /**
   * Ensures that user is logged in otherwise throws {@link org.sonar.server.exceptions.UnauthorizedException}.
   */
  UserSession checkLoggedIn();

  /**
   * Returns {@code true} if the permission is granted, otherwise {@code false}.
   */
  boolean hasPermission(GlobalPermission permission);

  /**
   * Ensures that {@link #hasPermission(GlobalPermission)} is {@code true},
   * otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkPermission(GlobalPermission permission);

  /**
   * Returns {@code true} if the permission is granted to user on the component,
   * otherwise {@code false}.
   * If the component does not exist, then returns {@code false}.
   *
   * @param component  non-null component.
   * @param permission project permission as defined by {@link org.sonar.server.permission.PermissionService}
   */
  boolean hasComponentPermission(ProjectPermission permission, ComponentDto component);

  boolean hasEntityPermission(ProjectPermission permission, EntityDto entity);

  boolean hasEntityPermission(ProjectPermission permission, String entityUuid);

  boolean hasChildProjectsPermission(ProjectPermission permission, ComponentDto component);

  boolean hasChildProjectsPermission(ProjectPermission permission, EntityDto application);

  boolean hasPortfolioChildProjectsPermission(ProjectPermission permission, ComponentDto component);

  /**
   * Using {@link #hasComponentPermission(ProjectPermission, ComponentDto)} is recommended
   * because it does not have to load project if the referenced component
   * is not a project.
   *
   * @deprecated use {@link #hasComponentPermission(ProjectPermission, ComponentDto)} instead
   */
  @Deprecated
  boolean hasComponentUuidPermission(ProjectPermission permission, String componentUuid);

  /**
   * Return the subset of specified components which the user has granted permission.
   * An empty list is returned if input is empty or if no components are allowed to be
   * accessed.
   * If the input is ordered, then the returned components are in the same order.
   * The duplicated components are returned duplicated too.
   */
  List<ComponentDto> keepAuthorizedComponents(ProjectPermission permission, Collection<ComponentDto> components);

  <T extends EntityDto> List<T> keepAuthorizedEntities(ProjectPermission permission, Collection<T> components);

  /**
   * Ensures that {@link #hasComponentPermission(ProjectPermission, ComponentDto)} is {@code true},
   * otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkComponentPermission(ProjectPermission projectPermission, ComponentDto component);

  /**
   * Ensures that {@link #hasEntityPermission(String, EntityDto)} is {@code true},
   * otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkEntityPermission(ProjectPermission projectPermission, EntityDto entity);

  /**
   * Ensures that {@link #hasEntityPermission(String, EntityDto)} is {@code true},
   * otherwise throws a {@link org.sonar.server.exceptions.ResourceForbiddenException}.
   * <p>
   * Differs from {@link #checkEntityPermission(ProjectPermission, EntityDto)} by throwing a different exception (ensuring no resource listing is possible).
   */
  UserSession checkEntityPermissionOrElseThrowResourceForbiddenException(ProjectPermission projectPermission, EntityDto entity);

  /**
   * Ensures that {@link #hasChildProjectsPermission(ProjectPermission, ComponentDto)} is {@code true}
   * otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkChildProjectsPermission(ProjectPermission projectPermission, ComponentDto project);

  /**
   * Ensures that {@link #hasChildProjectsPermission(ProjectPermission, EntityDto)} is {@code true}
   * otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkChildProjectsPermission(ProjectPermission projectPermission, EntityDto application);

  /**
   * Ensures that {@link #hasComponentUuidPermission(ProjectPermission, String)} is {@code true},
   * otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   *
   * @deprecated use {@link #checkComponentPermission(ProjectPermission, ComponentDto)} instead
   */
  @Deprecated
  UserSession checkComponentUuidPermission(ProjectPermission permission, String componentUuid);

  /**
   * Whether user can administrate system, for example for using cross-organizations services
   * like update center, system info or management of users.
   * Returns {@code true} if:
   * <ul>
   *   <li>user is administrator</li>
   * </ul>
   */
  boolean isSystemAdministrator();

  /**
   * Ensures that {@link #isSystemAdministrator()} is {@code true},
   * otherwise throws {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkIsSystemAdministrator();

  boolean isActive();

  boolean isAuthenticatedBrowserSession();
}
