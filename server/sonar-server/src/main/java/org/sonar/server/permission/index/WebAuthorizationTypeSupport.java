/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.permission.index;

import java.util.Optional;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.sonar.api.server.ServerSide;
import org.sonar.db.user.GroupDto;
import org.sonar.server.user.UserSession;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.FIELD_ALLOW_ANYONE;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.FIELD_GROUP_IDS;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.FIELD_USER_IDS;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.TYPE_AUTHORIZATION;

@ServerSide
public class WebAuthorizationTypeSupport {

  private final UserSession userSession;

  public WebAuthorizationTypeSupport(UserSession userSession) {
    this.userSession = userSession;
  }

  /**
   * Build a filter to restrict query to the documents on which
   * user has read access.
   */
  public QueryBuilder createQueryFilter() {
    if (userSession.isRoot()) {
      return QueryBuilders.matchAllQuery();
    }

    Integer userId = userSession.getUserId();
    BoolQueryBuilder filter = boolQuery();

    // anyone
    filter.should(QueryBuilders.termQuery(FIELD_ALLOW_ANYONE, true));

    // users
    Optional.ofNullable(userId)
      .map(Integer::longValue)
      .ifPresent(id -> filter.should(termQuery(FIELD_USER_IDS, id)));

    // groups
    userSession.getGroups()
      .stream()
      .map(GroupDto::getId)
      .forEach(groupId -> filter.should(termQuery(FIELD_GROUP_IDS, groupId)));

    return JoinQueryBuilders.hasParentQuery(
      TYPE_AUTHORIZATION,
      QueryBuilders.boolQuery().filter(filter),
      false);
  }
}
