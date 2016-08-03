/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

/**
 * Class used to rename the key of a project and its resources.
 *
 * @since 3.2
 */
public class ResourceKeyUpdaterDao implements Dao {
  private MyBatis mybatis;

  public ResourceKeyUpdaterDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void updateKey(String projectUuid, String newKey) {
    DbSession session = mybatis.openSession(true);
    ResourceKeyUpdaterMapper mapper = session.getMapper(ResourceKeyUpdaterMapper.class);
    try {
      if (mapper.countResourceByKey(newKey) > 0) {
        throw new IllegalArgumentException("Impossible to update key: a component with key \"" + newKey + "\" already exists.");
      }

      // must SELECT first everything
      ResourceDto project = mapper.selectProject(projectUuid);
      String projectOldKey = project.getKey();
      List<ResourceDto> resources = mapper.selectProjectResources(projectUuid);
      resources.add(project);

      // and then proceed with the batch UPDATE at once
      runBatchUpdateForAllResources(resources, projectOldKey, newKey, mapper);

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Map<String, String> checkModuleKeysBeforeRenaming(String projectUuid, String stringToReplace, String replacementString) {
    SqlSession session = mybatis.openSession(false);
    ResourceKeyUpdaterMapper mapper = session.getMapper(ResourceKeyUpdaterMapper.class);
    Map<String, String> result = Maps.newHashMap();
    try {
      Set<ResourceDto> modules = collectAllModules(projectUuid, stringToReplace, mapper);
      for (ResourceDto module : modules) {
        String newKey = computeNewKey(module, stringToReplace, replacementString);
        if (mapper.countResourceByKey(newKey) > 0) {
          result.put(module.getKey(), "#duplicate_key#");
        } else {
          result.put(module.getKey(), newKey);
        }
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
    return result;
  }

  public void bulkUpdateKey(DbSession session, String projectUuid, String stringToReplace, String replacementString) {
    ResourceKeyUpdaterMapper mapper = session.getMapper(ResourceKeyUpdaterMapper.class);
    // must SELECT first everything
    Set<ResourceDto> modules = collectAllModules(projectUuid, stringToReplace, mapper);
    checkNewNameOfAllModules(modules, stringToReplace, replacementString, mapper);
    Map<ResourceDto, List<ResourceDto>> allResourcesByModuleMap = Maps.newHashMap();
    for (ResourceDto module : modules) {
      allResourcesByModuleMap.put(module, mapper.selectProjectResources(module.getUuid()));
    }

    // and then proceed with the batch UPDATE at once
    for (ResourceDto module : modules) {
      String oldModuleKey = module.getKey();
      String newModuleKey = computeNewKey(module, stringToReplace, replacementString);
      Collection<ResourceDto> resources = Lists.newArrayList(module);
      resources.addAll(allResourcesByModuleMap.get(module));
      runBatchUpdateForAllResources(resources, oldModuleKey, newModuleKey, mapper);
    }
  }

  private static String computeNewKey(ResourceDto resource, String stringToReplace, String replacementString) {
    return resource.getKey().replaceAll(stringToReplace, replacementString);
  }

  private static void runBatchUpdateForAllResources(Collection<ResourceDto> resources, String oldKey, String newKey, ResourceKeyUpdaterMapper mapper) {
    for (ResourceDto resource : resources) {
      String oldResourceKey = resource.getKey();
      String newResourceKey = newKey + oldResourceKey.substring(oldKey.length(), oldResourceKey.length());
      resource.setKey(newResourceKey);
      String oldResourceDeprecatedKey = resource.getDeprecatedKey();
      if (StringUtils.isNotBlank(oldResourceDeprecatedKey)) {
        String newResourceDeprecatedKey = newKey + oldResourceDeprecatedKey.substring(oldKey.length(), oldResourceDeprecatedKey.length());
        resource.setDeprecatedKey(newResourceDeprecatedKey);
      }
      mapper.update(resource);
    }
  }

  private static Set<ResourceDto> collectAllModules(String projectUuid, String stringToReplace, ResourceKeyUpdaterMapper mapper) {
    ResourceDto project = mapper.selectProject(projectUuid);
    Set<ResourceDto> modules = Sets.newHashSet();
    if (project.getKey().contains(stringToReplace)) {
      modules.add(project);
    }
    for (ResourceDto submodule : mapper.selectDescendantProjects(projectUuid)) {
      modules.addAll(collectAllModules(submodule.getUuid(), stringToReplace, mapper));
    }
    return modules;
  }

  private static void checkNewNameOfAllModules(Set<ResourceDto> modules, String stringToReplace, String replacementString, ResourceKeyUpdaterMapper mapper) {
    for (ResourceDto module : modules) {
      String newName = computeNewKey(module, stringToReplace, replacementString);
      if (mapper.countResourceByKey(newName) > 0) {
        throw new IllegalStateException("Impossible to update key: a resource with \"" + newName + "\" key already exists.");
      }
    }
  }

}
