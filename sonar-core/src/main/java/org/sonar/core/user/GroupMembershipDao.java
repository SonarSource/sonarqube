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

package org.sonar.core.user;

import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.MyBatis;

import java.util.List;

public class GroupMembershipDao {

  public GroupMembershipDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  private final MyBatis mybatis;

  public List<GroupMembershipDto> selectGroups(GroupMembershipQuery query) {
    SqlSession session = mybatis.openSession();
    try {
      GroupMembershipMapper mapper = session.getMapper(GroupMembershipMapper.class);
      return mapper.selectGroups(query);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
