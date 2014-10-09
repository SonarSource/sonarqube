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

package org.sonar.server.computation.ws;

import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.server.computation.ComputationService;

import java.util.List;

public class IsAnalysisReportQueueEmptyAction implements RequestHandler {
  private final ComputationService service;

  public IsAnalysisReportQueueEmptyAction(ComputationService service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    controller
      .createAction("is_queue_empty")
      .setDescription("Check is the analysis report queue is empty")
      .setInternal(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    List<AnalysisReportDto> reports = service.findAllUnfinishedAnalysisReports();
    boolean isQueueEmpty = reports.isEmpty();

    IOUtils.write(String.valueOf(isQueueEmpty), response.stream().output());
  }
}
