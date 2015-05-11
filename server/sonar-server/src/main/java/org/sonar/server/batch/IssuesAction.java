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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.batch.protocol.input.BatchInput;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Maps.newHashMap;

public class IssuesAction implements BatchAction {

  private static final String PARAM_KEY = "key";

  private final DbClient dbClient;
  private final IssueIndex issueIndex;
  private final UserSession userSession;

  public IssuesAction(DbClient dbClient, IssueIndex issueIndex, UserSession userSession) {
    this.dbClient = dbClient;
    this.issueIndex = issueIndex;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("issues")
      .setDescription("Return open issues")
      .setSince("5.1")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Project, module or file key")
      .setExampleValue("org.codehaus.sonar:sonar");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkGlobalPermission(GlobalPermissions.PREVIEW_EXECUTION);
    final String moduleKey = request.mandatoryParam(PARAM_KEY);

    response.stream().setMediaType(MimeTypes.PROTOBUF);
    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto component = dbClient.componentDao().getByKey(session, moduleKey);
      Map<String, String> keysByUUid = keysByUUid(session, component);

      BatchInput.ServerIssue.Builder issueBuilder = BatchInput.ServerIssue.newBuilder();
      for (Iterator<IssueDoc> issueDocIterator = issueIndex.selectIssuesForBatch(component); issueDocIterator.hasNext();) {
        handleIssue(issueDocIterator.next(), issueBuilder, keysByUUid, response.stream().output());
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void handleIssue(IssueDoc issue, BatchInput.ServerIssue.Builder issueBuilder, Map<String, String> keysByUUid, OutputStream out) {
    issueBuilder.setKey(issue.key());
    issueBuilder.setModuleKey(keysByUUid.get(issue.moduleUuid()));
    String path = issue.filePath();
    if (path != null) {
      issueBuilder.setPath(path);
    }
    issueBuilder.setRuleRepository(issue.ruleKey().repository());
    issueBuilder.setRuleKey(issue.ruleKey().rule());
    String checksum = issue.checksum();
    if (checksum != null) {
      issueBuilder.setChecksum(checksum);
    }
    String assigneeLogin = issue.assignee();
    if (assigneeLogin != null) {
      issueBuilder.setAssigneeLogin(assigneeLogin);
    }
    Integer line = issue.line();
    if (line != null) {
      issueBuilder.setLine(line);
    }
    String message = issue.message();
    if (message != null) {
      issueBuilder.setMsg(message);
    }
    issueBuilder.setSeverity(org.sonar.batch.protocol.Constants.Severity.valueOf(issue.severity()));
    issueBuilder.setManualSeverity(issue.isManualSeverity());
    issueBuilder.setStatus(issue.status());
    String resolution = issue.resolution();
    if (resolution != null) {
      issueBuilder.setResolution(resolution);
    }
    issueBuilder.setCreationDate(issue.creationDate().getTime());
    try {
      issueBuilder.build().writeDelimitedTo(out);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to serialize issue", e);
    }
    issueBuilder.clear();
  }

  private Map<String, String> keysByUUid(DbSession session, ComponentDto component) {
    Map<String, String> keysByUUid = newHashMap();
    if (Scopes.PROJECT.equals(component.scope())) {
      List<ComponentDto> modulesTree = dbClient.componentDao().selectDescendantModules(session, component.uuid());
      for (ComponentDto componentDto : modulesTree) {
        keysByUUid.put(componentDto.uuid(), componentDto.key());
      }
    } else {
      String moduleUuid = component.moduleUuid();
      if (moduleUuid == null) {
        throw new IllegalArgumentException(String.format("The component '%s' has no module uuid", component.uuid()));
      }
      ComponentDto module = dbClient.componentDao().getByUuid(session, moduleUuid);
      keysByUUid.put(module.uuid(), module.key());
    }
    return keysByUUid;
  }
}
