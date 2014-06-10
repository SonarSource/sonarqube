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
package org.sonar.server.log.db;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.utils.System2;
import org.sonar.core.log.db.LogDto;
import org.sonar.core.log.db.LogKey;
import org.sonar.core.log.db.LogMapper;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;
import org.sonar.server.search.IndexDefinition;

import java.util.List;

/**
 * @since 4.4
 */
public class LogDao extends BaseDao<LogMapper, LogDto, LogKey> {

  public LogDao() {
    this(System2.INSTANCE);
  }

  @VisibleForTesting
  public LogDao(System2 system) {
    super(IndexDefinition.LOG, LogMapper.class, system);
  }

  @Override
  protected LogDto doGetNullableByKey(DbSession session, LogKey key) {
    return mapper(session).selectByKey(key);
  }

  @Override
  protected LogDto doInsert(DbSession session, LogDto item) {
    mapper(session).insert(item);
    return item;
  }

  @Override
  protected LogDto doUpdate(DbSession session, LogDto item) {
   throw new IllegalStateException("Cannot update Log!");
  }

  @Override
  protected void doDeleteByKey(DbSession session, LogKey key) {
    throw new IllegalStateException("Cannot delete Log!");
  }

  public List<LogDto> findAll(DbSession session) {
    return mapper(session).selectAll();
  }

  @Override
  public void synchronizeAfter(DbSession session, long timestamp) {
    throw new IllegalStateException("Log Index does not synchronize!");
  }
}
