/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.user.UserDto;
import org.sonar.scanner.protocol.input.ScannerInput;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.MediaTypes;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class IssuesAction implements BatchWsAction {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_BRANCH = "branch";
  private static final Splitter MODULE_PATH_SPLITTER = Splitter.on('.').trimResults().omitEmptyStrings();

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public IssuesAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("issues")
      .setDescription("Return open issues")
      .setResponseExample(getClass().getResource("issues-example.proto"))
      .setSince("5.1")
      .setChangelog(new Change("7.6", String.format("The use of module keys in parameter '%s' is deprecated", PARAM_KEY)))
      .setInternal(true)
      .setDeprecatedSince("9.5")
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Project, module or file key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action
      .createParam(PARAM_BRANCH)
      .setSince("6.6")
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = loadComponent(dbSession, request);
      userSession.checkComponentPermission(USER, component);
      Map<String, String> keysByUUid = keysByUUid(dbSession, component);

      ScannerInput.ServerIssue.Builder responseBuilder = ScannerInput.ServerIssue.newBuilder();
      response.stream().setMediaType(MediaTypes.PROTOBUF);
      OutputStream output = response.stream().output();

      List<IssueDto> issueDtos = new ArrayList<>();
      switch (component.scope()) {
        case Scopes.PROJECT:
          issueDtos.addAll(dbClient.issueDao().selectNonClosedByModuleOrProjectExcludingExternalsAndSecurityHotspots(dbSession, component));
          break;
        case Scopes.FILE:
          issueDtos.addAll(dbClient.issueDao().selectNonClosedByComponentUuidExcludingExternalsAndSecurityHotspots(dbSession, component.uuid()));
          break;
        default:
          // only projects, modules and files are supported. Other types of components are not allowed.
          throw new IllegalArgumentException(format("Component of scope '%s' is not allowed", component.scope()));
      }

      List<String> usersUuids = issueDtos.stream()
        .filter(issue -> issue.getAssigneeUuid() != null)
        .map(IssueDto::getAssigneeUuid)
        .toList();

      Map<String, String> userLoginsByUserUuids = dbClient.userDao().selectByUuids(dbSession, usersUuids)
        .stream().collect(toMap(UserDto::getUuid, UserDto::getLogin));

      issueDtos.forEach(issue -> {
        issue.setAssigneeUuid(userLoginsByUserUuids.get(issue.getAssigneeUuid()));
        handleIssue(issue, responseBuilder, keysByUUid, output);
      });
    }
  }

  private static void handleIssue(IssueDto issue, ScannerInput.ServerIssue.Builder issueBuilder, Map<String, String> keysByUUid, OutputStream out) {
    issueBuilder.setKey(issue.getKey());
    String moduleUuid = extractModuleUuid(issue);
    issueBuilder.setModuleKey(keysByUUid.get(moduleUuid));
    ofNullable(issue.getFilePath()).ifPresent(issueBuilder::setPath);
    issueBuilder.setRuleRepository(issue.getRuleRepo());
    issueBuilder.setRuleKey(issue.getRule());
    ofNullable(issue.getChecksum()).ifPresent(issueBuilder::setChecksum);
    ofNullable(issue.getAssigneeUuid()).ifPresent(issueBuilder::setAssigneeLogin);
    ofNullable(issue.getLine()).ifPresent(issueBuilder::setLine);
    ofNullable(issue.getMessage()).ifPresent(issueBuilder::setMsg);
    issueBuilder.setSeverity(org.sonar.scanner.protocol.Constants.Severity.valueOf(issue.getSeverity()));
    issueBuilder.setManualSeverity(issue.isManualSeverity());
    issueBuilder.setStatus(issue.getStatus());
    ofNullable(issue.getResolution()).ifPresent(issueBuilder::setResolution);
    issueBuilder.setType(RuleType.valueOf(issue.getType()).name());
    issueBuilder.setCreationDate(issue.getIssueCreationTime());
    try {
      issueBuilder.build().writeDelimitedTo(out);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to serialize issue", e);
    }
    issueBuilder.clear();
  }

  private static String extractModuleUuid(IssueDto issue) {
    List<String> split = MODULE_PATH_SPLITTER.splitToList(issue.getModuleUuidPath());
    return split.get(split.size() - 1);
  }

  private Map<String, String> keysByUUid(DbSession session, ComponentDto component) {
    Map<String, String> keysByUUid = new HashMap<>();
    if (Scopes.PROJECT.equals(component.scope())) {
      List<ComponentDto> modulesTree = dbClient.componentDao().selectDescendantModules(session, component.uuid());
      for (ComponentDto componentDto : modulesTree) {
        keysByUUid.put(componentDto.uuid(), componentDto.getKey());
      }
    } else {
      String moduleUuid = component.moduleUuid();
      checkArgument(moduleUuid != null, "The component '%s' has no module uuid", component.uuid());
      ComponentDto module = dbClient.componentDao().selectOrFailByUuid(session, moduleUuid);
      keysByUUid.put(module.uuid(), module.getKey());
    }
    return keysByUUid;
  }

  private ComponentDto loadComponent(DbSession dbSession, Request request) {
    String componentKey = request.mandatoryParam(PARAM_KEY);
    String branch = request.param(PARAM_BRANCH);
    return componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, componentKey, branch, null);
  }
}
