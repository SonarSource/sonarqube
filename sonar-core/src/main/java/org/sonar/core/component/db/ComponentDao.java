/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.component.db;

import org.apache.ibatis.session.SqlSession;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.ComponentQuery;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;

/**
 * @since 4.3
 */
public class ComponentDao {

  private final MyBatis myBatis;

  public ComponentDao(MyBatis myBatis) {
    this.myBatis = myBatis;
  }

  public Collection<ComponentDto> selectComponent(ComponentQuery query) {
    SqlSession session = myBatis.openSession();
    try {
      return select(query, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<ComponentDto> select(ComponentQuery query, SqlSession session) {
    return getMapper(session).selectComponents(query);
  }

  private ComponentMapper getMapper(SqlSession session) {
    return session.getMapper(ComponentMapper.class);
  }
}
