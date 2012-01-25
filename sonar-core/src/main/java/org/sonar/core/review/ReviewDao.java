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
package org.sonar.core.review;

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;

import java.util.List;

public class ReviewDao implements BatchComponent, ServerComponent {
  private final MyBatis mybatis;

  public ReviewDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public ReviewDto selectById(long id) {
    SqlSession session = mybatis.openSession();
    try {
      ReviewMapper mapper = session.getMapper(ReviewMapper.class);
      return mapper.selectById(id);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ReviewDto> selectByQuery(ReviewQuery query) {
    SqlSession session = mybatis.openSession();
    try {
      ReviewMapper mapper = session.getMapper(ReviewMapper.class);
      List<ReviewDto> result;
      if (query.needToPartitionQuery()) {
        result = Lists.newArrayList();
        for (ReviewQuery partitionedQuery : query.partition()) {
          result.addAll(mapper.selectByQuery(partitionedQuery));
        }
      } else {
        result = mapper.selectByQuery(query);
      }
      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Integer countByQuery(ReviewQuery query) {
    SqlSession session = mybatis.openSession();
    try {
      ReviewMapper mapper = session.getMapper(ReviewMapper.class);
      Integer result = 0;
      if (query.needToPartitionQuery()) {
        for (ReviewQuery partitionedQuery : query.partition()) {
          result += mapper.countByQuery(partitionedQuery);
        }
      } else {
        result = mapper.countByQuery(query);
      }
      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
