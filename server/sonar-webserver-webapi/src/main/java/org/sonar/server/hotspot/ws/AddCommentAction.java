/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.db.permission.ProjectPermission;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.ws.IssueUpdater;

public class AddCommentAction implements HotspotsWsAction {
  private static final String PARAM_HOTSPOT_KEY = "hotspot";
  private static final String PARAM_COMMENT = "comment";
  private static final Integer MAXIMUM_COMMENT_LENGTH = 1000;

  private final DbClient dbClient;
  private final HotspotWsSupport hotspotWsSupport;
  private final IssueFieldsSetter issueFieldsSetter;
  private final IssueUpdater issueUpdater;

  public AddCommentAction(DbClient dbClient, HotspotWsSupport hotspotWsSupport,
    IssueFieldsSetter issueFieldsSetter, IssueUpdater issueUpdater) {
    this.dbClient = dbClient;
    this.hotspotWsSupport = hotspotWsSupport;
    this.issueFieldsSetter = issueFieldsSetter;
    this.issueUpdater = issueUpdater;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("add_comment")
      .setHandler(this)
      .setPost(true)
      .setDescription("Add a comment to a Security Hotpot.<br/>" +
        "Requires authentication and the following permission: 'Browse' on the project of the specified Security Hotspot.")
      .setSince("8.1")
      .setInternal(true);

    action.createParam(PARAM_HOTSPOT_KEY)
      .setDescription("Key of the Security Hotspot")
      .setExampleValue(Uuids.UUID_EXAMPLE_03)
      .setRequired(true);
    action.createParam(PARAM_COMMENT)
      .setDescription("Comment text.")
      .setMaximumLength(MAXIMUM_COMMENT_LENGTH)
      .setExampleValue("This is safe because user input is validated by the calling code")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    hotspotWsSupport.checkLoggedIn();

    String hotspotKey = request.mandatoryParam(PARAM_HOTSPOT_KEY);
    String comment = request.mandatoryParam(PARAM_COMMENT);
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto hotspot = hotspotWsSupport.loadHotspot(dbSession, hotspotKey);
      hotspotWsSupport.loadAndCheckBranch(dbSession, hotspot, ProjectPermission.USER);

      DefaultIssue defaultIssue = hotspot.toDefaultIssue();
      IssueChangeContext context = hotspotWsSupport.newIssueChangeContextWithoutMeasureRefresh();
      issueFieldsSetter.addComment(defaultIssue, comment, context);
      issueUpdater.saveIssueAndPreloadSearchResponseData(dbSession, hotspot, defaultIssue, context);
      response.noContent();
    }
  }
}
