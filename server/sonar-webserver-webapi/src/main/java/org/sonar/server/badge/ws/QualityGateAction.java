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
package org.sonar.server.badge.ws;

import com.google.common.io.Resources;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;

import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;

public class QualityGateAction extends AbstractProjectBadgesWsAction {

  private final DbClient dbClient;

  public QualityGateAction(DbClient dbClient, ProjectBadgesSupport support, SvgGenerator svgGenerator) {
    super(support, svgGenerator);
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("quality_gate")
      .setHandler(this)
      .setSince("7.1")
      .setDescription("Generate badge for project's quality gate as an SVG.<br/>" +
        "Requires 'Browse' permission on the specified project.")
      .setResponseExample(Resources.getResource(getClass(), "quality_gate-example.svg"));
    support.addProjectAndBranchParams(action);
  }

  @Override
  protected String getBadge(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      BranchDto branch = support.getBranch(dbSession, request);
      Level qualityGateStatus = getQualityGate(dbSession, branch);
      return svgGenerator.generateQualityGate(qualityGateStatus);
    }
  }

  private Level getQualityGate(DbSession dbSession, BranchDto branch) {
    return Level.valueOf(dbClient.measureDao().selectByComponentUuid(dbSession, branch.getUuid())
      .map(m -> m.getString(ALERT_STATUS_KEY))
      .orElseThrow(() -> new ProjectBadgesException("Quality gate has not been found")));
  }

}
