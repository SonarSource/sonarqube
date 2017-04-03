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
package org.sonar.server.issue.ws;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.rule.index.RuleIndex;

import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;

/**
 * List issue tags matching a given query.
 * @since 5.1
 */
public class TagsAction implements IssuesWsAction {

  private final IssueIndex issueIndex;
  private final RuleIndex ruleIndex;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public TagsAction(IssueIndex issueIndex, RuleIndex ruleIndex, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.issueIndex = issueIndex;
    this.ruleIndex = ruleIndex;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("tags")
      .setHandler(this)
      .setSince("5.1")
      .setDescription("List tags matching a given query")
      .setResponseExample(Resources.getResource(getClass(), "tags-example.json"));
    action.createParam(Param.TEXT_QUERY)
      .setDescription("A pattern to match tags against")
      .setExampleValue("misra");
    action.createParam(PAGE_SIZE)
      .setDescription("The size of the list to return")
      .setExampleValue("25")
      .setDefaultValue("10");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String query = request.param(Param.TEXT_QUERY);
    int pageSize = request.mandatoryParamAsInt("ps");
    List<String> tags = listTags(query, pageSize == 0 ? Integer.MAX_VALUE : pageSize);
    writeTags(response, tags);
  }

  private List<String> listTags(@Nullable String textQuery, int pageSize) {
    Collection<String> issueTags = issueIndex.listTags(defaultOrganizationProvider.get().getUuid(), textQuery, pageSize);
    Collection<String> ruleTags = ruleIndex.listTags(defaultOrganizationProvider.get().getUuid(), textQuery, pageSize);

    SortedSet<String> result = Sets.newTreeSet();
    result.addAll(issueTags);
    result.addAll(ruleTags);
    List<String> resultAsList = Lists.newArrayList(result);
    return resultAsList.size() > pageSize && pageSize > 0 ? resultAsList.subList(0, pageSize) : resultAsList;
  }

  private static void writeTags(Response response, List<String> tags) {
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject().name("tags").beginArray();
      tags.forEach(json::value);
      json.endArray().endObject();
    }
  }

}
