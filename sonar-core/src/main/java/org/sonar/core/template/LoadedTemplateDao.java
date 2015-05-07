/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.core.template;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

@BatchSide
@ServerSide
public class LoadedTemplateDao implements DaoComponent {

  private MyBatis mybatis;

  public LoadedTemplateDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public int countByTypeAndKey(String type, String key) {
    SqlSession session = mybatis.openSession(false);
    try {
      return countByTypeAndKey(type, key, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countByTypeAndKey(String type, String key, SqlSession session) {
    return session.getMapper(LoadedTemplateMapper.class).countByTypeAndKey(type, key);
  }

  public void insert(LoadedTemplateDto loadedTemplateDto) {
    SqlSession session = mybatis.openSession(false);
    try {
      insert(loadedTemplateDto, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(LoadedTemplateDto loadedTemplateDto, SqlSession session) {
    session.getMapper(LoadedTemplateMapper.class).insert(loadedTemplateDto);
  }

  public void delete(DbSession session, String type, String key) {
    session.getMapper(LoadedTemplateMapper.class).delete(type, key);
  }
}
