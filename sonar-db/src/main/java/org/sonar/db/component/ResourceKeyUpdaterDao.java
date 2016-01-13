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

  public void updateKey(long projectId, String newKey) {
    DbSession session = mybatis.openSession(true);
    ResourceKeyUpdaterMapper mapper = session.getMapper(ResourceKeyUpdaterMapper.class);
    try {
      if (mapper.countResourceByKey(newKey) > 0) {
        throw new IllegalStateException("Impossible to update key: a resource with \"" + newKey + "\" key already exists.");
      }

      // must SELECT first everything
      ResourceDto project = mapper.selectProject(projectId);
      String projectOldKey = project.getKey();
      List<ResourceDto> resources = mapper.selectProjectResources(projectId);
      resources.add(project);

      // and then proceed with the batch UPDATE at once
      runBatchUpdateForAllResources(resources, projectOldKey, newKey, mapper);

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Map<String, String> checkModuleKeysBeforeRenaming(long projectId, String stringToReplace, String replacementString) {
    SqlSession session = mybatis.openSession(false);
    ResourceKeyUpdaterMapper mapper = session.getMapper(ResourceKeyUpdaterMapper.class);
    Map<String, String> result = Maps.newHashMap();
    try {
      Set<ResourceDto> modules = collectAllModules(projectId, stringToReplace, mapper);
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

  public void bulkUpdateKey(DbSession session, long projectId, String stringToReplace, String replacementString) {
    ResourceKeyUpdaterMapper mapper = session.getMapper(ResourceKeyUpdaterMapper.class);
    // must SELECT first everything
    Set<ResourceDto> modules = collectAllModules(projectId, stringToReplace, mapper);
    checkNewNameOfAllModules(modules, stringToReplace, replacementString, mapper);
    Map<ResourceDto, List<ResourceDto>> allResourcesByModuleMap = Maps.newHashMap();
    for (ResourceDto module : modules) {
      allResourcesByModuleMap.put(module, mapper.selectProjectResources(module.getId()));
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

  public void bulkUpdateKey(long projectId, String stringToReplace, String replacementString) {
    DbSession session = mybatis.openSession(true);
    try {
      bulkUpdateKey(session, projectId, stringToReplace, replacementString);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private static String computeNewKey(ResourceDto resource, String stringToReplace, String replacementString) {
    return resource.getKey().replaceAll(stringToReplace, replacementString);
  }

  private static void runBatchUpdateForAllResources(Collection<ResourceDto> resources, String oldKey, String newKey, ResourceKeyUpdaterMapper mapper) {
    for (ResourceDto resource : resources) {
      String resourceKey = resource.getKey();
      resource.setKey(newKey + resourceKey.substring(oldKey.length(), resourceKey.length()));
      String resourceDeprecatedKey = resource.getDeprecatedKey();
      if (StringUtils.isNotBlank(resourceDeprecatedKey)) {
        resource.setDeprecatedKey(newKey + resourceDeprecatedKey.substring(oldKey.length(), resourceDeprecatedKey.length()));
      }
      mapper.update(resource);
    }
  }

  private static Set<ResourceDto> collectAllModules(long projectId, String stringToReplace, ResourceKeyUpdaterMapper mapper) {
    ResourceDto project = mapper.selectProject(projectId);
    Set<ResourceDto> modules = Sets.newHashSet();
    if (project.getKey().contains(stringToReplace)) {
      modules.add(project);
    }
    for (ResourceDto submodule : mapper.selectDescendantProjects(projectId)) {
      modules.addAll(collectAllModules(submodule.getId(), stringToReplace, mapper));
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
