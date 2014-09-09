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

package org.sonar.server.user.db;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.GroupMapper;
import org.sonar.server.db.BaseDao;

import java.util.List;

/**
 * @since 3.2
 */
public class GroupDao extends BaseDao<GroupMapper, GroupDto, String> {

  public GroupDao() {
    this(System2.INSTANCE);
  }

  @VisibleForTesting
  public GroupDao(System2 system) {
    super(GroupMapper.class, system);
  }

  @Override
  protected GroupDto doGetNullableByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  @Override
  protected GroupDto doInsert(DbSession session, GroupDto item) {
    mapper(session).insert(item);
    return item;
  }

  public List<GroupDto> findByUserLogin(DbSession session, String login){
    return mapper(session).selectByUserLogin(login);
  }

}
