/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.db.user.GroupDto;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.NewIndex;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@ServerSide
@ComputeEngineSide
public class AuthorizationTypeSupport {

  public static final String TYPE_AUTHORIZATION = "authorization";
  public static final String FIELD_GROUP_IDS = "groupIds";
  public static final String FIELD_USER_IDS = "userIds";

  /**
   * When true, then anybody can access to the project. In that case
   * it's useless to store granted groups and users. The related
   * fields are empty.
   */
  public static final String FIELD_ALLOW_ANYONE = "allowAnyone";

  private final UserSession userSession;

  public AuthorizationTypeSupport(UserSession userSession) {
    this.userSession = userSession;
  }

  /**
   * @return the identifier of the ElasticSearch type (including it's index name), that corresponds to a certain document type
   */
  public static IndexType getAuthorizationIndexType(IndexType indexType) {
    requireNonNull(indexType);
    requireNonNull(indexType.getIndex());
    checkArgument(!AuthorizationTypeSupport.TYPE_AUTHORIZATION.equals(indexType.getType()), "Authorization types do not have authorization on their own.");
    return new IndexType(indexType.getIndex(), AuthorizationTypeSupport.TYPE_AUTHORIZATION);
  }

  /**
   * Creates a type that requires to verify that user has the read permission
   * when searching for documents.
   * It relies on a parent type named "authorization" that is automatically
   * populated by {@link org.sonar.server.permission.index.PermissionIndexer}.
   *
   * Both types {@code typeName} and "authorization" are created. Documents
   * must be created with _parent and _routing having the parent uuid as values.
   *
   * @see NewIndex.NewIndexType#requireProjectAuthorization()
   */
  public static NewIndex.NewIndexType enableProjectAuthorization(NewIndex.NewIndexType type) {
    type.setAttribute("_parent", ImmutableMap.of("type", TYPE_AUTHORIZATION));
    type.setAttribute("_routing", ImmutableMap.of("required", true));

    NewIndex.NewIndexType authType = type.getIndex().createType(TYPE_AUTHORIZATION);
    authType.setAttribute("_routing", ImmutableMap.of("required", true));
    authType.createLongField(FIELD_GROUP_IDS);
    authType.createLongField(FIELD_USER_IDS);
    authType.createBooleanField(FIELD_ALLOW_ANYONE);
    authType.setEnableSource(false);
    return type;
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
