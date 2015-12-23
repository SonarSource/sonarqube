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
package org.sonar.server.usergroups.ws;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.es.SearchOptions;

import static org.sonar.server.es.SearchOptions.MAX_LIMIT;

public class SearchAction implements UserGroupsWsAction {

  private static final String FIELD_ID = "id";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_MEMBERS_COUNT = "membersCount";
  private static final List<String> ALL_FIELDS = Arrays.asList(FIELD_NAME, FIELD_DESCRIPTION, FIELD_MEMBERS_COUNT);

  private DbClient dbClient;

  public SearchAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(NewController context) {
    context.createAction("search")
      .setDescription("Search for user groups")
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-search.json"))
      .setSince("5.2")
      .addFieldsParam(ALL_FIELDS)
      .addPagingParams(100, MAX_LIMIT)
      .addSearchQuery("sonar-users", "names");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    int page = request.mandatoryParamAsInt(Param.PAGE);
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    SearchOptions options = new SearchOptions()
      .setPage(page, pageSize);

    String query = StringUtils.defaultIfBlank(request.param(Param.TEXT_QUERY), "");
    Set<String> fields = neededFields(request);

    DbSession dbSession = dbClient.openSession(false);
    try {
      int limit = dbClient.groupDao().countByQuery(dbSession, query);
      List<GroupDto> groups = dbClient.groupDao().selectByQuery(dbSession, query, options.getOffset(), pageSize);
      Collection<Long> groupIds = Collections2.transform(groups, new Function<GroupDto, Long>() {
        @Override
        public Long apply(@Nonnull GroupDto input) {
          return input.getId();
        }
      });
      Map<String, Integer> userCountByGroup = dbClient.groupMembershipDao().countUsersByGroups(dbSession, groupIds);

      JsonWriter json = response.newJsonWriter().beginObject();
      options.writeJson(json, limit);
      writeGroups(json, groups, userCountByGroup, fields);
      json.endObject().close();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void writeGroups(JsonWriter json, List<GroupDto> groups, Map<String, Integer> userCountByGroup, Set<String> fields) {
    json.name("groups").beginArray();
    for (GroupDto group : groups) {
      writeGroup(json, group, userCountByGroup.get(group.getName()), fields);
    }
    json.endArray();
  }

  private void writeGroup(JsonWriter json, GroupDto group, Integer memberCount, Set<String> fields) {
    json.beginObject()
      .prop(FIELD_ID, group.getId().toString())
      .prop(FIELD_NAME, fields.contains(FIELD_NAME) ? group.getName() : null)
      .prop(FIELD_DESCRIPTION, fields.contains(FIELD_DESCRIPTION) ? group.getDescription() : null)
      .prop(FIELD_MEMBERS_COUNT, fields.contains(FIELD_MEMBERS_COUNT) ? memberCount : null)
      .endObject();
  }

  private Set<String> neededFields(Request request) {
    Set<String> fields = Sets.newHashSet();
    List<String> fieldsFromRequest = request.paramAsStrings(Param.FIELDS);
    if (fieldsFromRequest == null || fieldsFromRequest.isEmpty()) {
      fields.addAll(ALL_FIELDS);
    } else {
      fields.addAll(fieldsFromRequest);
    }
    return fields;
  }
}
