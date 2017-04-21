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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.scanner.protocol.input.ScannerInput;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.MediaTypes;

import static com.google.common.collect.Maps.newHashMap;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class IssuesAction implements BatchWsAction {

  private static final String PARAM_KEY = "key";

  private final DbClient dbClient;
  private final IssueIndex issueIndex;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public IssuesAction(DbClient dbClient, IssueIndex issueIndex, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.issueIndex = issueIndex;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("issues")
      .setDescription("Return open issues")
      .setResponseExample(getClass().getResource("issues-example.proto"))
      .setSince("5.1")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Project, module or file key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    response.stream().setMediaType(MediaTypes.PROTOBUF);

    try (DbSession session = dbClient.openSession(false)) {
      String componentKey = request.mandatoryParam(PARAM_KEY);
      ComponentDto component = componentFinder.getByKey(session, componentKey);
      userSession.checkComponentPermission(USER, component);

      Map<String, String> keysByUUid = keysByUUid(session, component);

      ScannerInput.ServerIssue.Builder issueBuilder = ScannerInput.ServerIssue.newBuilder();
      for (Iterator<IssueDoc> issueDocIterator = issueIndex.selectIssuesForBatch(component); issueDocIterator.hasNext();) {
        handleIssue(issueDocIterator.next(), issueBuilder, keysByUUid, response.stream().output());
      }
    }
  }

  private static void handleIssue(IssueDoc issue, ScannerInput.ServerIssue.Builder issueBuilder, Map<String, String> keysByUUid, OutputStream out) {
    issueBuilder.setKey(issue.key());
    issueBuilder.setModuleKey(keysByUUid.get(issue.moduleUuid()));
    setNullable(issue.filePath(), issueBuilder::setPath);
    issueBuilder.setRuleRepository(issue.ruleKey().repository());
    issueBuilder.setRuleKey(issue.ruleKey().rule());
    setNullable(issue.checksum(), issueBuilder::setChecksum);
    setNullable(issue.assignee(), issueBuilder::setAssigneeLogin);
    setNullable(issue.line(), issueBuilder::setLine);
    setNullable(issue.message(), issueBuilder::setMsg);
    issueBuilder.setSeverity(org.sonar.scanner.protocol.Constants.Severity.valueOf(issue.severity()));
    issueBuilder.setManualSeverity(issue.isManualSeverity());
    issueBuilder.setStatus(issue.status());
    setNullable(issue.resolution(), issueBuilder::setResolution);
    issueBuilder.setType(issue.type().name());
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
      ComponentDto module = dbClient.componentDao().selectOrFailByUuid(session, moduleUuid);
      keysByUUid.put(module.uuid(), module.key());
    }
    return keysByUUid;
  }
}
