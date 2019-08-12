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
package org.sonar.server.rule.ws;

import com.google.common.io.Resources;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.rule.index.RuleIndex;

import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;

public class TagsAction implements RulesWsAction {

  private final RuleIndex ruleIndex;
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public TagsAction(RuleIndex ruleIndex, DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.ruleIndex = ruleIndex;
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller
      .createAction("tags")
      .setDescription("List rule tags")
      .setSince("4.4")
      .setHandler(this)
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
  public void handle(Request request, Response response) {
    OrganizationDto organization = getOrganization(request.param(PARAM_ORGANIZATION));
    String query = request.param(Param.TEXT_QUERY);
    int pageSize = request.mandatoryParamAsInt("ps");

    List<String> tags = ruleIndex.listTags(organization, query, pageSize == 0 ? Integer.MAX_VALUE : pageSize);

    writeResponse(response, tags);
  }

  private OrganizationDto getOrganization(@Nullable String organizationKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String organizationOrDefaultKey = Optional.ofNullable(organizationKey)
        .orElseGet(defaultOrganizationProvider.get()::getKey);
      return NotFoundException.checkFoundWithOptional(
        dbClient.organizationDao().selectByKey(dbSession, organizationOrDefaultKey),
        "No organization with key '%s'", organizationOrDefaultKey);
    }
  }

  private static void writeResponse(Response response, List<String> tags) {
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject().name("tags").beginArray();
      tags.forEach(json::value);
      json.endArray().endObject();
    }
  }
}
