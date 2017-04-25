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
package org.sonar.server.ui.ws;

import java.util.List;
import java.util.function.Consumer;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.page.Page;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.user.UserSession;

import static org.sonar.server.ws.KeyExamples.KEY_ORG_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

public class OrganizationAction implements NavigationWsAction {

  private static final String ACTION_NAME = "organization";
  private static final String PARAM_ORGANIZATION = "organization";

  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final UserSession userSession;
  private final PageRepository pageRepository;

  public OrganizationAction(DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider, UserSession userSession, PageRepository pageRepository) {
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.userSession = userSession;
    this.pageRepository = pageRepository;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction projectNavigation = context.createAction(ACTION_NAME)
      .setDescription("Get information concerning organization navigation for the current user")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("organization-example.json"))
      .setSince("6.3")
      .setChangelog(
        new Change("6.4", "The field 'projectVisibility' is added"));

    projectNavigation.createParam(PARAM_ORGANIZATION)
      .setRequired(true)
      .setDescription("the organization key")
      .setExampleValue(KEY_ORG_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String organizationKey = request.mandatoryParam(PARAM_ORGANIZATION);
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = checkFoundWithOptional(
        dbClient.organizationDao().selectByKey(dbSession, organizationKey),
        "No organization with key '%s'", organizationKey);
      boolean newProjectPrivate = dbClient.organizationDao().getNewProjectPrivate(dbSession, organization);

      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      writeOrganization(json, organization, newProjectPrivate);
      json.endObject()
        .close();
    }
  }

  private void writeOrganization(JsonWriter json, OrganizationDto organization, boolean newProjectPrivate) {
    json.name("organization")
      .beginObject()
      .prop("canAdmin", userSession.hasPermission(OrganizationPermission.ADMINISTER, organization))
      .prop("canProvisionProjects", userSession.hasPermission(OrganizationPermission.PROVISION_PROJECTS, organization))
      .prop("canDelete", organization.isGuarded() ? userSession.isSystemAdministrator() : userSession.hasPermission(OrganizationPermission.ADMINISTER, organization))
      .prop("isDefault", organization.getKey().equals(defaultOrganizationProvider.get().getKey()))
      .prop("projectVisibility", newProjectPrivate ? "private" : "public");
    List<Page> pages = pageRepository.getOrganizationPages(false);
    json.name("pages");
    writePages(json, pages);
    if (userSession.hasPermission(OrganizationPermission.ADMINISTER, organization)) {
      List<Page> adminPages = pageRepository.getOrganizationPages(true);
      json.name("adminPages");
      writePages(json, adminPages);
    }
    json.endObject();
  }

  private static void writePages(JsonWriter json, List<Page> pages) {
    json.beginArray();
    pages.forEach(writePage(json));
    json.endArray();
  }

  private static Consumer<Page> writePage(JsonWriter json) {
    return page -> json.beginObject()
      .prop("key", page.getKey())
      .prop("name", page.getName())
      .endObject();
  }
}
