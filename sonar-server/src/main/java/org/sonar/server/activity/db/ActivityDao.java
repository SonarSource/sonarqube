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
package org.sonar.server.activity.db;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.utils.System2;
import org.sonar.core.activity.db.ActivityDto;
import org.sonar.core.activity.db.ActivityKey;
import org.sonar.core.activity.db.ActivityMapper;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;
import org.sonar.server.search.IndexDefinition;

import java.util.List;

/**
 * @since 4.4
 */
public class ActivityDao extends BaseDao<ActivityMapper, ActivityDto, ActivityKey> {

  public ActivityDao() {
    this(System2.INSTANCE);
  }

  @VisibleForTesting
  public ActivityDao(System2 system) {
    super(IndexDefinition.LOG, ActivityMapper.class, system);
  }

  @Override
  protected ActivityDto doGetNullableByKey(DbSession session, ActivityKey key) {
    return mapper(session).selectByKey(key);
  }

  @Override
  protected ActivityDto doInsert(DbSession session, ActivityDto item) {
    mapper(session).insert(item);
    return item;
  }

  @Override
  protected ActivityDto doUpdate(DbSession session, ActivityDto item) {
   throw new IllegalStateException("Cannot update Log!");
  }

  @Override
  protected void doDeleteByKey(DbSession session, ActivityKey key) {
    throw new IllegalStateException("Cannot delete Log!");
  }

  public List<ActivityDto> findAll(DbSession session) {
    return mapper(session).selectAll();
  }

  @Override
  public void synchronizeAfter(DbSession session, long timestamp) {
    throw new IllegalStateException("Log Index does not synchronize!");
  }
}
