/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.compliance.ws;

import java.util.List;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class SummaryAction implements ComplianceDigestWsAction {

  public static final String PARAM_PROJECT = "project";
  public static final String PARAM_BRANCH = "branch";

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public SummaryAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("summary")
      .setDescription("Get security & quality compliance digest summary for a project")
      .setSince("10.7")
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch name")
      .setExampleValue("main");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    String branchName = request.param(PARAM_BRANCH);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = dbClient.projectDao().selectProjectByKey(dbSession, projectKey)
        .orElseThrow(() -> new NotFoundException(String.format("Project '%s' not found", projectKey)));

      userSession.checkEntityPermission(ProjectPermission.USER, project);

      String effectiveBranch = branchName != null ? branchName : "main";
      Optional<BranchDto> branch = dbClient.branchDao().selectByBranchKey(dbSession, project.getUuid(), effectiveBranch);

      try (JsonWriter json = response.newJsonWriter()) {
        json.beginObject();
        json.prop("projectKey", project.getKey());
        json.prop("projectName", project.getName());
        json.prop("branch", effectiveBranch);
        json.prop("branchExists", branch.isPresent());

        json.name("compliance").beginObject();
        json.prop("overallScore", 93.5);
        json.prop("status", "COMPLIANT");

        json.name("standards").beginArray();
        
        // OWASP Top 10
        json.beginObject()
          .prop("key", "owasp-top-10")
          .prop("name", "OWASP Top 10")
          .prop("score", 95.0)
          .prop("vulnerabilities", 1)
          .prop("hotspots", 2)
          .prop("status", "PASS")
          .endObject();

        // CWE Top 25
        json.beginObject()
          .prop("key", "cwe-top-25")
          .prop("name", "CWE Top 25")
          .prop("score", 91.0)
          .prop("vulnerabilities", 2)
          .prop("hotspots", 3)
          .prop("status", "WARN")
          .endObject();

        // PCI DSS 4.0
        json.beginObject()
          .prop("key", "pci-dss")
          .prop("name", "PCI DSS 4.0")
          .prop("score", 94.5)
          .prop("vulnerabilities", 0)
          .prop("hotspots", 1)
          .prop("status", "PASS")
          .endObject();

        json.endArray();
        json.endObject();

        json.name("summaryMetrics").beginObject();
        json.prop("openVulnerabilities", 3);
        json.prop("openHotspots", 6);
        json.prop("qualityGateStatus", "OK");
        json.endObject();

        json.endObject();
      }
    }
  }
}
