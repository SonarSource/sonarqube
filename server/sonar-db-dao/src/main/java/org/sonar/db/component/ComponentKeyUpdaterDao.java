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
package org.sonar.db.component;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentKeyNewValue;

import static com.google.common.base.Preconditions.checkState;

/**
 * Class used to rename the key of a project and its resources.
 *
 * @since 3.2
 */
public class ComponentKeyUpdaterDao implements Dao {
  private final AuditPersister auditPersister;

  public ComponentKeyUpdaterDao(AuditPersister auditPersister) {
    this.auditPersister = auditPersister;
  }

  public void updateKey(DbSession dbSession, String projectUuid, String projectOldKey, String newKey) {
    ComponentKeyUpdaterMapper mapper = dbSession.getMapper(ComponentKeyUpdaterMapper.class);
    // Reject the new key only if it is already used by a DIFFERENT project. A key still held by this
    // same project (e.g. after a previous rename left projects.kee and components.kee out of sync) must
    // be allowed, so the project can be realigned to it.
    checkExistentKey(mapper, newKey, projectUuid);

    // must SELECT first everything
    List<ResourceDto> resources = new LinkedList<>();

    // add all branch components
    dbSession.getMapper(BranchMapper.class).selectByProjectUuid(projectUuid)
      .forEach(branch -> {
        resources.addAll(mapper.selectBranchResources(branch.getUuid()));
        resources.add(mapper.selectComponentByUuid(branch.getUuid()));
      });

    // and then proceed with the batch UPDATE at once
    runBatchUpdateForAllResources(resources, projectOldKey, newKey, projectUuid, mapper, (resource, oldKey) -> {
    }, dbSession);
  }

  @VisibleForTesting
  static String computeNewKey(String key, String stringToReplace, String replacementString) {
    return key.replace(stringToReplace, replacementString);
  }

  private void runBatchUpdateForAllResources(Collection<ResourceDto> resources, String oldKey, String newKey, String projectUuid,
    ComponentKeyUpdaterMapper mapper, @Nullable BiConsumer<ResourceDto, String> consumer, DbSession dbSession) {
    // Decide once whether oldKey is really the current prefix of this project's components. When projects.kee has
    // drifted from components.kee, the caller resolves the project by the stale projects.kee and passes it as
    // oldKey; it will not prefix the components. In that case we must NOT attempt prefix-substitution on them
    // (it would corrupt keys or crash on substring) - we only realign the projects row by UUID below.
    boolean renameComponents = resources.stream().anyMatch(resource -> resource.getKey().startsWith(oldKey));
    if (!renameComponents) {
      // Realign-only path: projects.kee is being moved onto a key the components already hold. Verify that
      // assumption, otherwise setting projects.kee = newKey below would silently re-introduce drift between
      // projects.kee and components.kee instead of fixing it.
      checkState(resources.stream().anyMatch(resource -> resource.getKey().equals(newKey)),
        "Key update aborted: no component holds key [%s]; renaming would leave projects.kee out of sync", newKey);
    }

    for (ResourceDto resource : resources) {
      String oldResourceKey = resource.getKey();
      // Substitute the prefix only on resources that actually start with oldKey. renameComponents is decided
      // globally (anyMatch), so under mixed/partial drift some resources may not share the prefix; substring on
      // those would throw StringIndexOutOfBoundsException or corrupt the key, so leave them untouched.
      if (renameComponents && oldResourceKey.startsWith(oldKey)) {
        renameComponent(resource, oldKey, newKey, mapper, dbSession);
      }
      if (consumer != null) {
        consumer.accept(resource, oldResourceKey);
      }
    }

    // Update the single projects row by UUID rather than by its (possibly out-of-sync) key string. projects.uuid
    // is the entity uuid passed in, distinct from the main component uuid, so it cannot be matched from the loop's
    // component resources. Keying by UUID guarantees the projects row moves with its components and realigns any
    // pre-existing drift between projects.kee and components.kee.
    int updatedProjects = mapper.updateProject(projectUuid, newKey);
    checkState(updatedProjects == 1,
      "Key update aborted: projects row [%s] update affected %s row(s), expected 1", projectUuid, updatedProjects);

    if (!renameComponents) {
      auditRealignedProject(resources, oldKey, newKey, dbSession);
    }
  }

  /**
   * Renames a single component by replacing its {@code oldKey} prefix with {@code newKey} (and the same on its
   * deprecated key when present), persists it, and emits an audit event for project/application roots.
   */
  private void renameComponent(ResourceDto resource, String oldKey, String newKey, ComponentKeyUpdaterMapper mapper, DbSession dbSession) {
    String oldResourceKey = resource.getKey();
    String newResourceKey = newKey + oldResourceKey.substring(oldKey.length());
    resource.setKey(newResourceKey);
    String oldResourceDeprecatedKey = resource.getDeprecatedKey();
    if (StringUtils.isNotBlank(oldResourceDeprecatedKey) && oldResourceDeprecatedKey.startsWith(oldKey)) {
      resource.setDeprecatedKey(newKey + oldResourceDeprecatedKey.substring(oldKey.length()));
    }
    int updatedComponents = mapper.updateComponent(resource);
    checkState(updatedComponents == 1,
      "Key update aborted: component [%s] update affected %s row(s), expected 1", resource.getUuid(), updatedComponents);
    if (isProjectOrApp(resource)) {
      auditPersister.componentKeyUpdate(dbSession, new ComponentKeyNewValue(resource.getUuid(), oldResourceKey, newResourceKey), resource.getQualifier());
    }
  }

  /**
   * Emits the key-change audit event on the realign-only path. The loop renamed nothing (components already hold
   * newKey), yet projects.kee moved from the stale key passed as {@code oldKey} to {@code newKey}, so record it
   * against the project/application root just as the rename path does.
   */
  private void auditRealignedProject(Collection<ResourceDto> resources, String oldKey, String newKey, DbSession dbSession) {
    resources.stream()
      .filter(ComponentKeyUpdaterDao::isProjectOrApp)
      .filter(resource -> resource.getKey().equals(newKey))
      .findFirst()
      .ifPresent(root -> auditPersister.componentKeyUpdate(dbSession,
        new ComponentKeyNewValue(root.getUuid(), oldKey, newKey), root.getQualifier()));
  }

  private static boolean isProjectOrApp(ResourceDto resource) {
    return resource.getScope().equals(ComponentScopes.PROJECT)
      && (resource.getQualifier().equals(ComponentQualifiers.PROJECT) || resource.getQualifier().equals(ComponentQualifiers.APP));
  }

  /**
   * Fails if {@code resourceKey} is already used by a component that does NOT belong to
   * {@code allowedProjectUuid}. When {@code allowedProjectUuid} is {@code null} the check is global
   * (any existing component with the key is a conflict).
   */
  public static void checkExistentKey(ComponentKeyUpdaterMapper mapper, String resourceKey, @Nullable String allowedProjectUuid) {
    int conflictingComponents = allowedProjectUuid == null
      ? mapper.countComponentsByKey(resourceKey)
      : mapper.countComponentsByKeyOutsideProject(resourceKey, allowedProjectUuid);
    if (conflictingComponents > 0) {
      throw new IllegalArgumentException("Impossible to update key: a component with key \"" + resourceKey + "\" already exists.");
    }
  }
}
