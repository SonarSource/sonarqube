/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.hotspot.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.hotspot.ws.HotspotsToIssuesMigrator.MigrationResult;
import org.sonar.server.user.UserSession;

public class MigrateToIssuesAction implements HotspotsWsAction {

  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_DRY_RUN = "dryRun";

  private final UserSession userSession;
  private final HotspotsToIssuesMigrator migrator;

  public MigrateToIssuesAction(UserSession userSession, HotspotsToIssuesMigrator migrator) {
    this.userSession = userSession;
    this.migrator = migrator;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("migrate_to_issues")
      .setHandler(this)
      .setPost(true)
      .setInternal(true)
      .setSince("2026.4")
      .setDescription("Migrate Security Hotspots to Issues. Requires 'Administer System' permission.");

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key. If not provided, all projects are migrated.")
      .setRequired(false);

    action.createParam(PARAM_DRY_RUN)
      .setDescription("If true, only count hotspots to migrate without performing any writes.")
      .setDefaultValue("false")
      .setBooleanPossibleValues()
      .setRequired(false);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    String projectKey = request.param(PARAM_PROJECT);
    boolean dryRun = request.mandatoryParamAsBoolean(PARAM_DRY_RUN);

    MigrationResult result = migrator.migrate(projectKey, dryRun);

    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject();
      json.prop(PARAM_DRY_RUN, result.dryRun());
      json.name("projects");
      json.beginArray();
      for (HotspotsToIssuesMigrator.ProjectMigrationResult project : result.projects()) {
        json.beginObject();
        json.prop("projectKey", project.projectKey());
        json.prop("migrated", project.migrated());
        json.prop("skipped", project.skipped());
        json.endObject();
      }
      json.endArray();
      json.endObject();
    }
  }
}
