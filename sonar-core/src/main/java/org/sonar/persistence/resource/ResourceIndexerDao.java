/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence.resource;

import com.google.common.collect.Lists;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.persistence.DatabaseUtils;
import org.sonar.persistence.MyBatis;

import java.util.List;

public class ResourceIndexerDao {

  public static final int MINIMUM_KEY_SIZE = 3;

  private final MyBatis mybatis;

  public ResourceIndexerDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public ResourceIndexerDao index(String resourceName, int resourceId, int rootProjectId) {
    SqlSession sqlSession = mybatis.openSession();
    try {
      index(new ResourceDto().setId(resourceId).setName(resourceName).setRootId(rootProjectId), sqlSession, true);

    } finally {
      sqlSession.close();
    }
    return this;
  }


  public ResourceIndexerDao index(ResourceIndexerFilter filter) {
    final SqlSession sqlSession = mybatis.openSession(ExecutorType.BATCH);
    try {
      sqlSession.select("selectResourcesToIndex", filter, new ResultHandler() {
        public void handleResult(ResultContext context) {
          ResourceDto resource = (ResourceDto) context.getResultObject();

          // The column PROJECTS.ROOT_ID references the module but not the root project in a multi-modules project.
          boolean correctRootProjectId = false;

          index(resource, sqlSession, correctRootProjectId);
        }
      });
    } finally {
      sqlSession.close();
    }
    return this;
  }

  public ResourceIndexerDao delete(List<Integer> resourceIds) {
    final SqlSession sqlSession = mybatis.openSession();
    try {
      ResourceIndexerMapper mapper = sqlSession.getMapper(ResourceIndexerMapper.class);
      List<List<Integer>> partitionsOfResourceIds = Lists.partition(resourceIds, DatabaseUtils.MAX_IN_ELEMENTS);
      for (List<Integer> partitionOfResourceIds : partitionsOfResourceIds) {
        if (!partitionOfResourceIds.isEmpty()) {
          mapper.deleteByResourceIds(partitionOfResourceIds);
        }
      }
      sqlSession.commit();

    } finally {
      sqlSession.close();
    }
    return this;
  }

  void index(ResourceDto resource, SqlSession session, boolean correctProjectRootId) {
    String name = resource.getName();
    if (StringUtils.isBlank(name) || resource.getId() == null) {
      return;
    }

    String key = toKey(name);
    if (key.length() >= MINIMUM_KEY_SIZE) {
      ResourceIndexerMapper mapper = session.getMapper(ResourceIndexerMapper.class);
      boolean toBeIndexed = sanitizeIndex(resource, key, mapper);
      if (toBeIndexed) {

        ResourceIndexDto dto = new ResourceIndexDto()
          .setResourceId(resource.getId())
          .setRootProjectId(loadRootProjectId(resource, mapper, correctProjectRootId))
          .setNameSize(name.length());

        for (int position = 0; position <= key.length() - MINIMUM_KEY_SIZE; position++) {
          dto.setPosition(position);
          dto.setKey(StringUtils.substring(key, position));
          mapper.insert(dto);
        }

        session.commit();
      }
    }
  }

  private Integer loadRootProjectId(ResourceDto resource, ResourceIndexerMapper mapper, boolean correctProjectRootId) {
    if (correctProjectRootId) {
      return resource.getRootId();
    }
    Integer rootId;
    if (resource.getRootId() != null) {
      ResourceDto root = mapper.selectRootId(resource.getRootId());
      if (root != null) {
        rootId = (Integer) ObjectUtils.defaultIfNull(root.getRootId(), root.getId());
      } else {
        rootId = resource.getRootId();
      }
    } else {
      rootId = resource.getId();
    }
    return rootId;
  }

  /**
   * Return true if the resource must be indexed, false if the resource is already indexed.
   * If the resource is indexed with a different key, then this index is dropped and the
   * resource must be indexed again.
   */
  private boolean sanitizeIndex(ResourceDto resource, String key, ResourceIndexerMapper mapper) {
    ResourceIndexDto masterIndex = mapper.selectMasterIndexByResourceId(resource.getId());
    if (masterIndex != null && !StringUtils.equals(key, masterIndex.getKey())) {
      // resource has been renamed -> drop existing indexes
      mapper.deleteByResourceId(resource.getId());
      masterIndex = null;
    }
    return masterIndex == null;
  }

  static String toKey(String input) {
    return StringUtils.lowerCase(input);
  }
}
