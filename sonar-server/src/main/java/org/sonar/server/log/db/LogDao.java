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
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;
import org.sonar.server.log.LogDto;
import org.sonar.server.search.IndexDefinition;

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
    return null;
  }

  @Override
  protected LogDto doInsert(DbSession session, LogDto item) {
    return null;
  }

  @Override
  protected LogDto doUpdate(DbSession session, LogDto item) {
    return null;
  }

  @Override
  protected void doDeleteByKey(DbSession session, LogKey key) {

  }

  @Override
  public void synchronizeAfter(DbSession session, long timestamp) {
    throw new IllegalStateException("Log Index does not synchronize!");
  }
}
