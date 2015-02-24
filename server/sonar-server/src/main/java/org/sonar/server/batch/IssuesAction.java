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

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.batch.protocol.input.BatchInput;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.BatchIssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.user.UserSession;

import java.io.IOException;
import java.io.OutputStream;

public class IssuesAction implements RequestHandler {

  private static final String PARAM_KEY = "key";

  private final DbClient dbClient;

  public IssuesAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("issues")
      .setDescription("Return open issues")
      .setSince("5.1")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Project or module key")
      .setExampleValue("org.codehaus.sonar:sonar");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession.get().checkGlobalPermission(GlobalPermissions.PREVIEW_EXECUTION);
    final String moduleKey = request.mandatoryParam(PARAM_KEY);

    response.stream().setMediaType(MimeTypes.PROTOBUF);
    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto moduleOrProject = dbClient.componentDao().getByKey(session, moduleKey);
      UserSession.get().checkComponentPermission(UserRole.USER, moduleKey);

      BatchIssueResultHandler batchIssueResultHandler = new BatchIssueResultHandler(response.stream().output());
      if (moduleOrProject.isRootProject()) {
        dbClient.issueDao().selectNonClosedIssuesByProjectUuid(session, moduleOrProject.uuid(), batchIssueResultHandler);
      } else {
        dbClient.issueDao().selectNonClosedIssuesByModuleUuid(session, moduleOrProject.uuid(), batchIssueResultHandler);
      }

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private static class BatchIssueResultHandler implements ResultHandler {
    private OutputStream out;
    BatchInput.ServerIssue.Builder issueBuilder = BatchInput.ServerIssue.newBuilder();

    public BatchIssueResultHandler(OutputStream out) {
      this.out = out;
    }

    @Override
    public void handleResult(ResultContext rc) {
      BatchIssueDto batchIssueDto = (BatchIssueDto) rc.getResultObject();
      issueBuilder.setKey(batchIssueDto.getKey());
      issueBuilder.setComponentKey(batchIssueDto.getComponentKey());
      issueBuilder.setRuleRepository(batchIssueDto.getRuleRepo());
      issueBuilder.setRuleKey(batchIssueDto.getRuleKey());
      issueBuilder.setChecksum(batchIssueDto.getChecksum());
      String assigneeLogin = batchIssueDto.getAssigneeLogin();
      if (assigneeLogin != null) {
        issueBuilder.setAssigneeLogin(assigneeLogin);
      }
      Integer line = batchIssueDto.getLine();
      if (line != null) {
        issueBuilder.setLine(line.intValue());
      }
      String message = batchIssueDto.getMessage();
      if (message != null) {
        issueBuilder.setMsg(message);
      }
      issueBuilder.setSeverity(org.sonar.batch.protocol.Constants.Severity.valueOf(batchIssueDto.getSeverity()));
      issueBuilder.setManualSeverity(batchIssueDto.isManualSeverity());
      issueBuilder.setStatus(batchIssueDto.getStatus());
      String resolution = batchIssueDto.getResolution();
      if (resolution != null) {
        issueBuilder.setResolution(resolution);
      }
      issueBuilder.setCreationDate(batchIssueDto.getCreationTime());
      try {
        issueBuilder.build().writeDelimitedTo(out);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to serialize issue", e);
      }
      issueBuilder.clear();
    }
  }
}
