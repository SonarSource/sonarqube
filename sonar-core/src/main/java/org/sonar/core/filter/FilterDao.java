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
package org.sonar.core.filter;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;

/**
 * @since 3.1
 */
public class FilterDao implements BatchComponent, ServerComponent {
  private MyBatis mybatis;

  public FilterDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public FilterDto findFilter(String name) {
    SqlSession session = mybatis.openSession();
    try {
      FilterMapper mapper = session.getMapper(FilterMapper.class);
      return mapper.findFilter(name);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(FilterDto filterDto) {
    SqlSession session = mybatis.openSession();
    FilterMapper mapper = session.getMapper(FilterMapper.class);
    try {
      mapper.insert(filterDto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
