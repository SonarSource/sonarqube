/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.resource;

import com.google.common.collect.Sets;
import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Class used to rename the key of a project and its resources. 
 * 
 * @since 3.2
 */
public class ResourceKeyUpdaterDao {
  private MyBatis mybatis;

  public ResourceKeyUpdaterDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void updateKey(long projectId, String newKey) {
    SqlSession session = mybatis.openBatchSession();
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

  public void bulkUpdateKey(long projectId, String oldPrefix, String newPrefix) {
    SqlSession session = mybatis.openBatchSession();
    ResourceKeyUpdaterMapper mapper = session.getMapper(ResourceKeyUpdaterMapper.class);
    try {
      // must SELECT first everything
      Set<ResourceDto> modules = collectAllModules(projectId, oldPrefix, mapper);
      checkNewNameOfAllModules(modules, oldPrefix, newPrefix, mapper);
      Set<ResourceDto> allResources = Sets.newHashSet(modules);
      for (ResourceDto module : modules) {
        allResources.addAll(mapper.selectProjectResources(module.getId()));
      }

      // and then proceed with the batch UPDATE at once
      runBatchUpdateForAllResources(allResources, oldPrefix, newPrefix, mapper);

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private String computeNewKey(ResourceDto resource, String oldPrefix, String newPrefix) {
    String resourceKey = resource.getKey();
    return newPrefix + resourceKey.substring(oldPrefix.length(), resourceKey.length());
  }

  private void runBatchUpdateForAllResources(Collection<ResourceDto> resources, String oldPrefix, String newPrefix, ResourceKeyUpdaterMapper mapper) {
    for (ResourceDto resource : resources) {
      resource.setKey(computeNewKey(resource, oldPrefix, newPrefix));
      mapper.update(resource);
    }
  }

  private Set<ResourceDto> collectAllModules(long projectId, String oldPrefix, ResourceKeyUpdaterMapper mapper) {
    ResourceDto project = mapper.selectProject(projectId);
    Set<ResourceDto> modules = Sets.newHashSet();
    if (project.getKey().startsWith(oldPrefix)) {
      modules.add(project);
    }
    for (ResourceDto submodule : mapper.selectDescendantProjects(projectId)) {
      modules.addAll(collectAllModules(submodule.getId(), oldPrefix, mapper));
    }
    return modules;
  }

  private void checkNewNameOfAllModules(Set<ResourceDto> modules, String oldPrefix, String newPrefix, ResourceKeyUpdaterMapper mapper) {
    for (ResourceDto module : modules) {
      String newName = computeNewKey(module, oldPrefix, newPrefix);
      if (mapper.countResourceByKey(newName) > 0) {
        throw new IllegalStateException("Impossible to update key: a resource with \"" + newName + "\" key already exists.");
      }
    }
  }

}
