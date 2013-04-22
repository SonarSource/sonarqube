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
package org.sonar.core.template;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;

public class LoadedTemplateDao implements BatchComponent, ServerComponent {

  private MyBatis mybatis;

  public LoadedTemplateDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public int countByTypeAndKey(String type, String key) {
    SqlSession session = mybatis.openSession();
    LoadedTemplateMapper mapper = session.getMapper(LoadedTemplateMapper.class);
    try {
      return mapper.countByTypeAndKey(type, key);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(LoadedTemplateDto loadedTemplateDto) {
    SqlSession session = mybatis.openSession();
    LoadedTemplateMapper mapper = session.getMapper(LoadedTemplateMapper.class);
    try {
      mapper.insert(loadedTemplateDto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
