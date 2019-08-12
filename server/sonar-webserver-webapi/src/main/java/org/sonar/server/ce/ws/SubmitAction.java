/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.ce.ws;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.ce.task.CeTask;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.server.ce.queue.ReportSubmitter;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.Ce;

import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.sonar.core.component.ComponentKeys.MAX_COMPONENT_KEY_LENGTH;
import static org.sonar.db.component.ComponentValidator.MAX_COMPONENT_NAME_LENGTH;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class SubmitAction implements CeWsAction {

  private static final String PARAM_ORGANIZATION_KEY = "organization";
  private static final String PARAM_PROJECT_KEY = "projectKey";
  private static final String PARAM_PROJECT_NAME = "projectName";
  private static final String PARAM_REPORT_DATA = "report";
  private static final String PARAM_ANALYSIS_CHARACTERISTIC = "characteristic";

  private final ReportSubmitter reportSubmitter;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public SubmitAction(ReportSubmitter reportSubmitter, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.reportSubmitter = reportSubmitter;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("submit")
      .setDescription("Submits a scanner report to the queue. Report is processed asynchronously. Requires analysis permission. " +
        "If the project does not exist, then the provisioning permission is also required.")
      .setPost(true)
      .setInternal(true)
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(getClass().getResource("submit-example.json"));

    action.createParam(PARAM_ORGANIZATION_KEY)
      .setDescription("Key of the organization the project belongs to")
      .setExampleValue("my-org")
      .setSince("6.3")
      .setInternal(true);

    action
      .createParam(PARAM_PROJECT_KEY)
      .setRequired(true)
      .setMaximumLength(MAX_COMPONENT_KEY_LENGTH)
      .setDescription("Key of project")
      .setExampleValue("my_project");

    action
      .createParam(PARAM_PROJECT_NAME)
      .setRequired(false)
      .setDescription("Optional name of the project, used only if the project does not exist yet. If name is longer than %d, it is abbreviated.", MAX_COMPONENT_NAME_LENGTH)
      .setExampleValue("My Project");

    action
      .createParam(PARAM_REPORT_DATA)
      .setRequired(true)
      .setDescription("Report file. Format is not an API, it changes among SonarQube versions.");

    action
      .createParam(PARAM_ANALYSIS_CHARACTERISTIC)
      .setRequired(false)
      .setDescription("Optional characteristic of the analysis. Can be repeated to define multiple characteristics.")
      .setExampleValue("branchType=long")
      .setSince("6.6");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    String organizationKey = wsRequest.getParam(PARAM_ORGANIZATION_KEY)
      .emptyAsNull()
      .or(defaultOrganizationProvider.get()::getKey);
    String projectKey = wsRequest.mandatoryParam(PARAM_PROJECT_KEY);
    String projectName = abbreviate(defaultIfBlank(wsRequest.param(PARAM_PROJECT_NAME), projectKey), MAX_COMPONENT_NAME_LENGTH);

    Map<String, String> characteristics = parseTaskCharacteristics(wsRequest);

    try (InputStream report = new BufferedInputStream(wsRequest.mandatoryParamAsPart(PARAM_REPORT_DATA).getInputStream())) {
      CeTask task = reportSubmitter.submit(organizationKey, projectKey, projectName, characteristics, report);
      Ce.SubmitResponse submitResponse = Ce.SubmitResponse.newBuilder()
        .setTaskId(task.getUuid())
        .setProjectId(task.getComponent().get().getUuid())
        .build();
      WsUtils.writeProtobuf(submitResponse, wsRequest, wsResponse);
    }
  }

  private static Map<String, String> parseTaskCharacteristics(Request wsRequest) {
    Map<String, String> characteristics = new LinkedHashMap<>();

    for (String param : wsRequest.multiParam(PARAM_ANALYSIS_CHARACTERISTIC)) {
      String[] pair = StringUtils.split(param, "=", 2);
      checkRequest(pair.length == 2, "Parameter '%s' must be a key-value pair with the format 'key=value'.", PARAM_ANALYSIS_CHARACTERISTIC);
      checkRequest(!characteristics.containsKey(pair[0]), "Key '%s' was provided twice with parameters '%s'", pair[0], PARAM_ANALYSIS_CHARACTERISTIC);
      if (CeTaskCharacteristicDto.SUPPORTED_KEYS.contains(pair[0])) {
        characteristics.put(pair[0], pair[1]);
      }
    }
    return characteristics;
  }

}
