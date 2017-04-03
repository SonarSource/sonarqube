/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.usergroups.ws;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.user.UserSession;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_ORGANIZATION_KEY;

public class SearchAction implements UserGroupsWsAction {

  private static final String FIELD_ID = "id";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_MEMBERS_COUNT = "membersCount";
  private static final List<String> ALL_FIELDS = Arrays.asList(FIELD_NAME, FIELD_DESCRIPTION, FIELD_MEMBERS_COUNT);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupWsSupport groupWsSupport;

  public SearchAction(DbClient dbClient, UserSession userSession, GroupWsSupport groupWsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.groupWsSupport = groupWsSupport;
  }

  @Override
  public void define(NewController context) {
    WebService.NewAction action = context.createAction("search")
      .setDescription("Search for user groups.<br>" +
        "Requires the following permission: 'Administer System'.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("search-example.json"))
      .setSince("5.2")
      .addFieldsParam(ALL_FIELDS)
      .addPagingParams(100, MAX_LIMIT)
      .addSearchQuery("sonar-users", "names");

    action.createParam(PARAM_ORGANIZATION_KEY)
      .setDescription("Key of organization. If not set then groups are searched in default organization.")
      .setExampleValue("my-org")
      .setSince("6.2")
      .setInternal(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    int page = request.mandatoryParamAsInt(Param.PAGE);
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    SearchOptions options = new SearchOptions()
      .setPage(page, pageSize);

    String query = defaultIfBlank(request.param(Param.TEXT_QUERY), "");
    Set<String> fields = neededFields(request);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = groupWsSupport.findOrganizationByKey(dbSession, request.param(PARAM_ORGANIZATION_KEY));
      userSession.checkLoggedIn().checkPermission(ADMINISTER, organization);

      int limit = dbClient.groupDao().countByQuery(dbSession, organization.getUuid(), query);
      List<GroupDto> groups = dbClient.groupDao().selectByQuery(dbSession, organization.getUuid(), query, options.getOffset(), pageSize);
      List<Integer> groupIds = groups.stream().map(GroupDto::getId).collect(MoreCollectors.toList(groups.size()));
      Map<String, Integer> userCountByGroup = dbClient.groupMembershipDao().countUsersByGroups(dbSession, groupIds);

      JsonWriter json = response.newJsonWriter().beginObject();
      options.writeJson(json, limit);
      writeGroups(json, groups, userCountByGroup, fields);
      json.endObject().close();
    }
  }

  private static void writeGroups(JsonWriter json, List<GroupDto> groups, Map<String, Integer> userCountByGroup, Set<String> fields) {
    json.name("groups").beginArray();
    for (GroupDto group : groups) {
      writeGroup(json, group, userCountByGroup.get(group.getName()), fields);
    }
    json.endArray();
  }

  private static void writeGroup(JsonWriter json, GroupDto group, Integer memberCount, Set<String> fields) {
    json.beginObject()
      .prop(FIELD_ID, group.getId().toString())
      .prop(FIELD_NAME, fields.contains(FIELD_NAME) ? group.getName() : null)
      .prop(FIELD_DESCRIPTION, fields.contains(FIELD_DESCRIPTION) ? group.getDescription() : null)
      .prop(FIELD_MEMBERS_COUNT, fields.contains(FIELD_MEMBERS_COUNT) ? memberCount : null)
      .endObject();
  }

  private static Set<String> neededFields(Request request) {
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
