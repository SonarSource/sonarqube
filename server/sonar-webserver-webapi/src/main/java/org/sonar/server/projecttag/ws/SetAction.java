/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.projecttag.ws;

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.projecttag.TagsWsSupport;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class SetAction implements ProjectTagsWsAction {

  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_TAGS = "tags";

  private final DbClient dbClient;
  private final TagsWsSupport tagsWsSupport;

  public SetAction(DbClient dbClient, TagsWsSupport tagsWsSupport) {
    this.dbClient = dbClient;
    this.tagsWsSupport = tagsWsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set")
      .setDescription("Set tags on a project.<br>" +
        "Requires the 'Administer' or 'Create Project' permissions on the specified project.")
      .setSince("6.4")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_TAGS)
      .setDescription("Comma-separated list of tags")
      .setRequired(true)
      .setExampleValue("finance, offshore");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    List<String> tags = request.mandatoryParamAsStrings(PARAM_TAGS);

    try (DbSession dbSession = dbClient.openSession(false)) {
      tagsWsSupport.updateProjectTags(dbSession, projectKey, tags);
    }

    response.noContent();
  }
}
