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

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.persistence.MyBatis;

public class ResourceIndexerDao {

  public static final int MINIMUM_KEY_SIZE = 3;

  private final MyBatis mybatis;

  public ResourceIndexerDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  void index(ResourceDto resource, SqlSession session) {
    String name = resource.getName();
    if (StringUtils.isBlank(name)) {
      return;
    }
    String normalizedName = normalize(name);
    if (normalizedName.length() >= MINIMUM_KEY_SIZE) {
      ResourceIndexerMapper mapper = session.getMapper(ResourceIndexerMapper.class);

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

      ResourceIndexDto dto = new ResourceIndexDto()
        .setResourceId(resource.getId())
        .setProjectId(rootId)
        .setNameSize(name.length());

      for (int position = 0; position <= normalizedName.length() - MINIMUM_KEY_SIZE; position++) {
        dto.setPosition(position);
        dto.setKey(StringUtils.substring(normalizedName, position));
        mapper.insert(dto);
      }

      session.commit();
    }
  }

  public void index(String resourceName, int resourceId, int projectId) {
    SqlSession sqlSession = mybatis.openSession();
    try {
      index(new ResourceDto().setId(resourceId).setName(resourceName).setRootId(projectId), sqlSession);

    } finally {
      sqlSession.close();
    }
  }


  public void index(ResourceIndexerFilter filter) {
    final SqlSession sqlSession = mybatis.openSession(ExecutorType.BATCH);
    try {
      sqlSession.select("selectResourcesToIndex", filter, new ResultHandler() {
        public void handleResult(ResultContext context) {
          ResourceDto resource = (ResourceDto) context.getResultObject();
          index(resource, sqlSession);
        }
      });
    } finally {
      sqlSession.close();
    }
  }

  static String normalize(String input) {
    return StringUtils.lowerCase(input);
  }
}
