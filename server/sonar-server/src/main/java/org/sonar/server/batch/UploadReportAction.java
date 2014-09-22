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

package org.sonar.server.batch;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.component.AuthorizedComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.search.IndexClient;
import org.sonar.server.user.UserSession;

public class UploadReportAction implements RequestHandler {

  public static final String UPLOAD_REPORT_ACTION = "upload_report";

  static final String PARAM_PROJECT = "project";
  static final String PARAM_FIRST_ANALYSIS = "firstAnalysis";

  private final DbClient dbClient;
  private final IndexClient index;
  private final InternalPermissionService permissionService;

  public UploadReportAction(DbClient dbClient, IndexClient index, InternalPermissionService permissionService) {
    this.dbClient = dbClient;
    this.index = index;
    this.permissionService = permissionService;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(UPLOAD_REPORT_ACTION)
      .setDescription("Update analysis report")
      .setSince("5.0")
      .setPost(true)
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_PROJECT)
      .setRequired(true)
      .setDescription("Project key")
      .setExampleValue("org.codehaus.sonar:sonar");

    action
      .createParam(PARAM_FIRST_ANALYSIS)
      .setDescription("Is it the first analysis of this project ?")
      .setDefaultValue(false)
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession session = dbClient.openSession(false);
    try {
      String projectKey = request.mandatoryParam(PARAM_PROJECT);
      AuthorizedComponentDto project = dbClient.componentDao().getAuthorizedComponentByKey(projectKey, session);

      // Create permission on project
      boolean isFirstAnalysis = request.mandatoryParamAsBoolean(PARAM_FIRST_ANALYSIS);
      if (isFirstAnalysis) {
        permissionService.applyDefaultPermissionTemplate(session, project);
        session.commit();
      }

      UserSession.get().checkGlobalPermission(GlobalPermissions.SCAN_EXECUTION);

      // Index project's issues
      dbClient.issueDao().synchronizeAfter(session,
        index.get(IssueIndex.class).getLastSynchronization(),
        ImmutableMap.of("project", projectKey));
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
