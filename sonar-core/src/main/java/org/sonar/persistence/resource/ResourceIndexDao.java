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

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.sonar.persistence.MyBatis;

import java.util.Collections;
import java.util.List;

public class ResourceIndexDao {

  public static final int MINIMUM_SEARCH_SIZE = 3;
  public static final int MINIMUM_KEY_SIZE = 3;

  private final MyBatis mybatis;

  public ResourceIndexDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<ResourceIndexDto> search(String input) {
    if (StringUtils.isBlank(input) || input.length() < MINIMUM_SEARCH_SIZE) {
      return Collections.emptyList();
    }
    SqlSession sqlSession = mybatis.openSession();
    try {
      ResourceIndexMapper mapper = sqlSession.getMapper(ResourceIndexMapper.class);
      return mapper.selectLikeKey(normalize(input) + "%");
    } finally {
      sqlSession.close();
    }
  }

  public void index(String resourceName, int resourceId, int projectId) {
    if (StringUtils.isBlank(resourceName)) {
      return;
    }
    String normalizedName = normalize(resourceName);
    if (normalizedName.length() >= MINIMUM_KEY_SIZE) {
      SqlSession sqlSession = mybatis.openSession(ExecutorType.BATCH);
      try {
        ResourceIndexMapper mapper = sqlSession.getMapper(ResourceIndexMapper.class);
        ResourceIndexDto dto = new ResourceIndexDto().setResourceId(resourceId).setProjectId(projectId);

        for (int position = 0; position <= normalizedName.length() - MINIMUM_KEY_SIZE; position++) {
          dto.setPosition(position);
          dto.setKey(StringUtils.substring(normalizedName, position));
          mapper.insert(dto);
        }

        sqlSession.commit();

      } finally {
        sqlSession.close();
      }
    }
  }

  static String normalize(String input) {
    String result = StringUtils.trim(input);
    result = StringUtils.lowerCase(result);
    return result;
  }
}
