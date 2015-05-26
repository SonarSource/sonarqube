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

package org.sonar.core.user;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

public class GroupMembershipDao implements DaoComponent {

  private final MyBatis mybatis;

  public GroupMembershipDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  // TODO Remove this method and associated client code when the UI is migrated to Backbone
  public List<GroupMembershipDto> selectGroups(GroupMembershipQuery query, Long userId, int offset, int limit) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectGroups(session, query, userId, offset, limit);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<GroupMembershipDto> selectGroups(SqlSession session, GroupMembershipQuery query, Long userId, int offset, int limit) {
    Map<String, Object> params = ImmutableMap.of("query", query, "userId", userId);
    return mapper(session).selectGroups(params, new RowBounds(offset, limit));
  }

  public int countGroups(SqlSession session, GroupMembershipQuery query, Long userId) {
    Map<String, Object> params = ImmutableMap.of("query", query, "userId", userId);
    return mapper(session).countGroups(params);
  }

  public Multimap<String, String> selectGroupsByLogins(final DbSession session, Collection<String> logins) {
    final Multimap<String, String> result = ArrayListMultimap.create();
    DaoUtils.executeLargeInputs(logins, new Function<List<String>, List<LoginGroup>>() {
      @Override
      public List<LoginGroup> apply(@Nonnull List<String> input) {
        List<LoginGroup> groupMemberships = mapper(session).selectGroupsByLogins(input);
        for (LoginGroup membership : groupMemberships) {
          result.put(membership.login(), membership.groupName());
        }
        return groupMemberships;
      }
    });

    return result;
  }

  @VisibleForTesting
  List<GroupMembershipDto> selectGroups(GroupMembershipQuery query, Long userId) {
    return selectGroups(query, userId, 0, Integer.MAX_VALUE);
  }

  private GroupMembershipMapper mapper(SqlSession session) {
    return session.getMapper(GroupMembershipMapper.class);
  }

}
