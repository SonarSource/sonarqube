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

import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

/**
 * Post-run verification/status view for the Hotspots-to-Issues migration. Reports the only
 * admin-actionable signal: how many Security Hotspots remain to migrate (re-run until it reaches zero). ES indexing
 * and Portfolio/Application refresh converge automatically via the recovery indexer and the Compute Engine, so they
 * are not surfaced here.
 */
public class MigrationStatusAction implements HotspotsWsAction {

  private static final String PARAM_PROJECT = "project";

  private final UserSession userSession;
  private final DbClient dbClient;

  public MigrationStatusAction(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("migration_status")
      .setHandler(this)
      .setInternal(true)
      .setSince("2026.4")
      .setDescription("""
        Report how many Security Hotspots remain to be migrated to Issues (optionally scoped to a project). \
        The migration is complete for the scope once this reaches zero. Requires 'Administer System' permission.""");

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key. If not provided, the status is reported for the whole instance.")
      .setRequired(false);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    String projectKey = request.param(PARAM_PROJECT);

    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<String> scopeProjectUuids = resolveScope(dbSession, projectKey);
      int remainingHotspots = dbClient.issueDao().countHotspotsForMigration(dbSession, scopeProjectUuids);

      try (JsonWriter json = response.newJsonWriter()) {
        json.beginObject()
          .prop("remainingHotspots", remainingHotspots)
          .prop("complete", remainingHotspots == 0)
          .endObject();
      }
    }
  }

  @Nullable
  private Set<String> resolveScope(DbSession dbSession, @Nullable String projectKey) {
    if (projectKey == null) {
      return null;
    }
    ProjectDto project = dbClient.projectDao().selectProjectByKey(dbSession, projectKey)
      .orElseThrow(() -> new NotFoundException("Project not found: " + projectKey));
    return Set.of(project.getUuid());
  }
}
