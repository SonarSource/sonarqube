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
package org.sonar.server.issue.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueChangeWSSupport;
import org.sonar.server.issue.IssueChangeWSSupport.Load;
import org.sonar.server.issue.IssueFinder;
import org.sonarqube.ws.Issues.ChangelogWsResponse;

import static java.util.Collections.singleton;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_CHANGELOG;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;

public class ChangelogAction implements IssuesWsAction {
  private final DbClient dbClient;
  private final IssueFinder issueFinder;
  private final IssueChangeWSSupport issueChangeSupport;

  public ChangelogAction(DbClient dbClient, IssueFinder issueFinder, IssueChangeWSSupport issueChangeSupport) {
    this.dbClient = dbClient;
    this.issueFinder = issueFinder;
    this.issueChangeSupport = issueChangeSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_CHANGELOG)
      .setDescription("Display changelog of an issue.<br/>" +
                      "Requires the 'Browse' permission on the project of the specified issue.")
      .setSince("4.1")
      .setChangelog(
        new Change("10.4", "'issueStatus' key is added in the differences"),
        new Change("10.4", "'status' and 'resolution' keys are now deprecated in the differences"),
        new Change("9.7", "'externalUser' and 'webhookSource' information added to the answer"),
        new Change("6.3", "changes on effort is expressed with the raw value in minutes (instead of the duration previously)"))
      .setHandler(this)
      .setResponseExample(Resources.getResource(IssuesWs.class, "changelog-example.json"));
    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto issue = issueFinder.getByKey(dbSession, request.mandatoryParam(PARAM_ISSUE));

      ChangelogWsResponse build = handle(dbSession, issue);
      writeProtobuf(build, request, response);
    }
  }

  public ChangelogWsResponse handle(DbSession dbSession, IssueDto issue) {
    IssueChangeWSSupport.FormattingContext formattingContext = issueChangeSupport.newFormattingContext(dbSession, singleton(issue), Load.CHANGE_LOG);

    ChangelogWsResponse.Builder builder = ChangelogWsResponse.newBuilder();
    issueChangeSupport.formatChangelog(issue, formattingContext)
      .forEach(builder::addChangelog);
    return builder.build();
  }
}
