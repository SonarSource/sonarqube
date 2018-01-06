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
package org.sonar.server.issue.ws;

import com.google.common.io.Resources;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.rule.index.RuleIndex;
import org.sonarqube.ws.Issues;

import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;

/**
 * List issue tags matching a given query.
 * @since 5.1
 */
public class TagsAction implements IssuesWsAction {

  private final IssueIndex issueIndex;
  private final RuleIndex ruleIndex;
  private final DbClient dbClient;

  public TagsAction(IssueIndex issueIndex, RuleIndex ruleIndex, DbClient dbClient) {
    this.issueIndex = issueIndex;
    this.ruleIndex = ruleIndex;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("tags")
      .setHandler(this)
      .setSince("5.1")
      .setDescription("List tags matching a given query")
      .setResponseExample(Resources.getResource(getClass(), "tags-example.json"));
    action.createSearchQuery("misra", "tags");
    action.createPageSize(10, 100);
    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(false)
      .setInternal(true)
      .setExampleValue("my-org")
      .setSince("6.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String query = request.param(Param.TEXT_QUERY);
    int pageSize = request.mandatoryParamAsInt("ps");
    OrganizationDto organization = getOrganization(request.param(PARAM_ORGANIZATION));
    List<String> tags = listTags(organization, query, pageSize == 0 ? Integer.MAX_VALUE : pageSize);

    Issues.TagsResponse.Builder tagsResponseBuilder = Issues.TagsResponse.newBuilder();
    tags.forEach(tagsResponseBuilder::addTags);
    writeProtobuf(tagsResponseBuilder.build(), request, response);
  }

  private List<String> listTags(@Nullable OrganizationDto organization, @Nullable String textQuery, int pageSize) {
    Collection<String> issueTags = issueIndex.listTags(organization, textQuery, pageSize);
    Collection<String> ruleTags = ruleIndex.listTags(organization, textQuery, pageSize);

    SortedSet<String> result = new TreeSet<>();
    result.addAll(issueTags);
    result.addAll(ruleTags);
    List<String> resultAsList = new ArrayList<>(result);
    return resultAsList.size() > pageSize && pageSize > 0 ? resultAsList.subList(0, pageSize) : resultAsList;
  }

  @CheckForNull
  private OrganizationDto getOrganization(@Nullable String organizationKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return organizationKey == null ? null
        : checkFoundWithOptional(dbClient.organizationDao().selectByKey(dbSession, organizationKey), "No organization with key '%s'", organizationKey);
    }
  }

}
