/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentKeyNewValue;

import static org.sonar.db.component.ComponentDto.BRANCH_KEY_SEPARATOR;
import static org.sonar.db.component.ComponentDto.generateBranchKey;

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

  public void updateKey(DbSession dbSession, String projectUuid, String newKey) {
    ComponentKeyUpdaterMapper mapper = dbSession.getMapper(ComponentKeyUpdaterMapper.class);
    checkExistentKey(mapper, newKey);

    // must SELECT first everything
    ResourceDto project = mapper.selectProjectByUuid(projectUuid);
    String projectOldKey = project.getKey();
    List<ResourceDto> resources = mapper.selectProjectResources(projectUuid);
    resources.add(project);

    // add branch components
    dbSession.getMapper(BranchMapper.class).selectByProjectUuid(projectUuid).stream()
      .filter(branch -> !projectUuid.equals(branch.getUuid()))
      .forEach(branch -> {
        resources.addAll(mapper.selectProjectResources(branch.getUuid()));
        resources.add(mapper.selectProjectByUuid(branch.getUuid()));
      });

    // and then proceed with the batch UPDATE at once
    runBatchUpdateForAllResources(resources, projectOldKey, newKey, mapper, (resource, oldKey) -> {
    }, dbSession);
  }

  public void updateApplicationBranchKey(DbSession dbSession, String appBranchUuid, String appKey, String newBranchName) {
    ComponentKeyUpdaterMapper mapper = dbSession.getMapper(ComponentKeyUpdaterMapper.class);

    String newAppBranchKey = generateBranchKey(appKey, newBranchName);
    checkExistentKey(mapper, newAppBranchKey);

    ResourceDto appBranch = mapper.selectProjectByUuid(appBranchUuid);
    String appBranchOldKey = appBranch.getKey();
    appBranch.setKey(newAppBranchKey);
    mapper.updateComponent(appBranch);

    auditPersister.componentKeyBranchUpdate(dbSession, new ComponentKeyNewValue(appBranchUuid, appBranchOldKey, newAppBranchKey), Qualifiers.APP);

    String oldAppBranchFragment = appBranchOldKey.replace(BRANCH_KEY_SEPARATOR, "");
    String newAppBranchFragment = appKey + newBranchName;
    for (ResourceDto appBranchResource : mapper.selectProjectResources(appBranchUuid)) {
      String newKey = computeNewKey(appBranchResource.getKey(), oldAppBranchFragment, newAppBranchFragment);
      appBranchResource.setKey(newKey);
      mapper.updateComponent(appBranchResource);
    }
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

  public static final class RekeyedResource {
    private final ResourceDto resource;
    private final String oldKey;

    public RekeyedResource(ResourceDto resource, String oldKey) {
      this.resource = resource;
      this.oldKey = oldKey;
    }

    public ResourceDto getResource() {
      return resource;
    }

    public String getOldKey() {
      return oldKey;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RekeyedResource that = (RekeyedResource) o;
      return Objects.equals(resource.getUuid(), that.resource.getUuid());
    }

    @Override
    public int hashCode() {
      return resource.getUuid().hashCode();
    }
  }

  public static void checkExistentKey(ComponentKeyUpdaterMapper mapper, String resourceKey) {
    if (mapper.countResourceByKey(resourceKey) > 0) {
      throw new IllegalArgumentException("Impossible to update key: a component with key \"" + resourceKey + "\" already exists.");
    }
  }
}
