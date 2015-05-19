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

import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.GroupMapper;

import java.util.Date;
import java.util.List;

/**
 * @since 3.2
 */
public class GroupDao implements DaoComponent {

  private System2 system;

  public GroupDao(System2 system) {
    this.system = system;
  }

  public GroupDto selectByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  public GroupDto insert(DbSession session, GroupDto item) {
    Date createdAt = new Date(system.now());
    item.setCreatedAt(createdAt)
      .setUpdatedAt(createdAt);
    mapper(session).insert(item);
    return item;
  }

  public List<GroupDto> findByUserLogin(DbSession session, String login){
    return mapper(session).selectByUserLogin(login);
  }

  private GroupMapper mapper(DbSession session) {
    return session.getMapper(GroupMapper.class);
  }
}
