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
package org.sonar.server.component.persistence;

import com.google.common.annotations.VisibleForTesting;
import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.System2;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.db.ComponentMapper;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;

/**
 * @since 4.3
 */
public class ComponentDao extends BaseDao<ComponentMapper, ComponentDto, String> implements ServerComponent, DaoComponent {

  public ComponentDao() {
    this(System2.INSTANCE);
  }

  @VisibleForTesting
  public ComponentDao(System2 system) {
    super(ComponentMapper.class, system);
  }

  public ComponentDto getById(Long id, SqlSession session) {
    return getMapper(session).selectById(id);
  }

  public boolean existsById(Long id, SqlSession session) {
    return getMapper(session).countById(id) > 0;
  }

  private ComponentMapper getMapper(SqlSession session) {
    return session.getMapper(ComponentMapper.class);
  }

  @Override
  protected ComponentDto doGetByKey(String key, DbSession session) {
    return getMapper(session).selectByKey(key);
  }

  @Override
  protected ComponentDto doInsert(ComponentDto item, DbSession session) {
    throw new IllegalStateException("Not implemented yet");
  }

  @Override
  protected ComponentDto doUpdate(ComponentDto item, DbSession session) {
    throw new IllegalStateException("Not implemented yet");
  }

  @Override
  protected void doDeleteByKey(String key, DbSession session) {
    throw new IllegalStateException("Not implemented yet");
  }

  @Override
  public Iterable<String> keysOfRowsUpdatedAfter(long timestamp, DbSession session) {
    throw new IllegalStateException("Not implemented yet");
  }
}
