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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;

public class UploadReportAction implements RequestHandler {

  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_FIRST_ANALYSIS = "firstAnalysis";

  private final DbClient dbClient;

  public UploadReportAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("upload_report")
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
      // TODO
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
