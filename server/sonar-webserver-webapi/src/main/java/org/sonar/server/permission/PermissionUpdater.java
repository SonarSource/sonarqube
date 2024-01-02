/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.es.Indexers;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.utils.Preconditions.checkState;
import static org.sonar.server.es.Indexers.EntityEvent.PERMISSION_CHANGE;

public class PermissionUpdater<T extends PermissionChange> {

  private final Indexers indexers;

  private final Map<Class<?>, GranteeTypeSpecificPermissionUpdater<T>> specificPermissionClassToHandler;

  public PermissionUpdater(Indexers indexers, Set<GranteeTypeSpecificPermissionUpdater<T>> permissionChangers) {
    this.indexers = indexers;
    specificPermissionClassToHandler = permissionChangers.stream()
      .collect(toMap(GranteeTypeSpecificPermissionUpdater::getHandledClass, Function.identity()));
  }

  public void apply(DbSession dbSession, Collection<T> changes) {
    checkState(changes.stream().map(PermissionChange::getProjectUuid).distinct().count() <= 1,
      "Only one project per changes is supported");

    List<String> projectOrViewUuids = new ArrayList<>();
    Map<Optional<String>, List<T>> granteeUuidToPermissionChanges = changes.stream().collect(groupingBy(change -> Optional.ofNullable(change.getUuidOfGrantee())));
    granteeUuidToPermissionChanges.values().forEach(permissionChanges -> applyForSingleGrantee(dbSession, projectOrViewUuids, permissionChanges));

    indexers.commitAndIndexOnEntityEvent(dbSession, projectOrViewUuids, PERMISSION_CHANGE);
  }

  private void applyForSingleGrantee(DbSession dbSession, List<String> projectOrViewUuids, List<T> permissionChanges) {
    T anyPermissionChange = permissionChanges.iterator().next();
    EntityDto entity = anyPermissionChange.getEntity();
    String entityUuid = Optional.ofNullable(entity).map(EntityDto::getUuid).orElse(null);
    GranteeTypeSpecificPermissionUpdater<T> granteeTypeSpecificPermissionUpdater = getSpecificProjectUpdater(anyPermissionChange);
    Set<String> existingPermissions = granteeTypeSpecificPermissionUpdater.loadExistingEntityPermissions(dbSession, anyPermissionChange.getUuidOfGrantee(), entityUuid);
    for (T permissionChange : permissionChanges) {
      if (granteeTypeSpecificPermissionUpdater.apply(dbSession, existingPermissions, permissionChange) && permissionChange.getProjectUuid() != null) {
        projectOrViewUuids.add(permissionChange.getProjectUuid());
      }
    }
  }

  private GranteeTypeSpecificPermissionUpdater<T> getSpecificProjectUpdater(T anyPermissionChange) {
    return specificPermissionClassToHandler.get(anyPermissionChange.getClass());
  }

}
