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
package org.sonar.db.component;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentKeyNewValue;

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
    checkExistentKey(mapper, newKey);

    // must SELECT first everything
    List<ResourceDto> resources = new LinkedList<>();

    // add all branch components
    dbSession.getMapper(BranchMapper.class).selectByProjectUuid(projectUuid)
      .forEach(branch -> {
        resources.addAll(mapper.selectBranchResources(branch.getUuid()));
        resources.add(mapper.selectComponentByUuid(branch.getUuid()));
      });

    // and then proceed with the batch UPDATE at once
    runBatchUpdateForAllResources(resources, projectOldKey, newKey, mapper, (resource, oldKey) -> {
    }, dbSession);
  }

  @VisibleForTesting
  static String computeNewKey(String key, String stringToReplace, String replacementString) {
    return key.replace(stringToReplace, replacementString);
  }

  private void runBatchUpdateForAllResources(Collection<ResourceDto> resources, String oldKey, String newKey, ComponentKeyUpdaterMapper mapper,
    @Nullable BiConsumer<ResourceDto, String> consumer, DbSession dbSession) {
    for (ResourceDto resource : resources) {
      String oldResourceKey = resource.getKey();
      String newResourceKey = newKey + oldResourceKey.substring(oldKey.length());
      resource.setKey(newResourceKey);
      String oldResourceDeprecatedKey = resource.getDeprecatedKey();
      if (StringUtils.isNotBlank(oldResourceDeprecatedKey)) {
        String newResourceDeprecatedKey = newKey + oldResourceDeprecatedKey.substring(oldKey.length());
        resource.setDeprecatedKey(newResourceDeprecatedKey);
      }
      mapper.updateComponent(resource);
      if (resource.getScope().equals(Scopes.PROJECT) && (resource.getQualifier().equals(Qualifiers.PROJECT) || resource.getQualifier().equals(Qualifiers.APP))) {
        auditPersister.componentKeyUpdate(dbSession, new ComponentKeyNewValue(resource.getUuid(), oldResourceKey, newResourceKey), resource.getQualifier());
        mapper.updateProject(oldResourceKey, newResourceKey);
      }

      if (consumer != null) {
        consumer.accept(resource, oldResourceKey);
      }
    }
  }

  public static void checkExistentKey(ComponentKeyUpdaterMapper mapper, String resourceKey) {
    if (mapper.countComponentsByKey(resourceKey) > 0) {
      throw new IllegalArgumentException("Impossible to update key: a component with key \"" + resourceKey + "\" already exists.");
    }
  }
}
