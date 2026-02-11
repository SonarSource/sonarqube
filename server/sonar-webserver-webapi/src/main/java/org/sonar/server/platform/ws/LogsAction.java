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
package org.sonar.server.platform.ws;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import org.apache.commons.io.FileUtils;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.process.ProcessId;
import org.sonar.server.log.DistributedServerLogging;
import org.sonar.server.log.ServerLogging;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.MediaTypes;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class LogsAction implements SystemWsAction {
  private static final String NAME = "name";
  private static final String ACCESS_LOG = "access";
  private static final String DEPRECATION_LOG = "deprecation";

  private final UserSession userSession;
  private final ServerLogging serverLogging;

  public LogsAction(UserSession userSession, ServerLogging serverLogging) {
    this.userSession = userSession;
    this.serverLogging = serverLogging;
  }

  @Override
  public void define(WebService.NewController controller) {
    var values = stream(ProcessId.values()).map(ProcessId::getKey).collect(toList());
    values.add(ACCESS_LOG);
    values.add(DEPRECATION_LOG);
    values.sort(String::compareTo);

    WebService.NewAction action = controller.createAction("logs")
      .setDescription("Get system logs in plain-text format. Requires system administration permission.")
      .setResponseExample(getClass().getResource("logs-example.log"))
      .setSince("5.2")
      .setChangelog(
        new Change("2025.2", format("Added support for Data Center Edition for all possible values of '%s' except 'es'.", NAME)),
        new Change("2025.2", "Removed deprecated 'process' property."),
        new Change("10.4", "Add support for deprecation logs in process property."),
        new Change("10.4", format("Deprecate property 'process' in favor of '%s'.", NAME)))
      .setHandler(this);

    action
      .createParam(NAME)
      .setPossibleValues(values)
      .setDefaultValue(ProcessId.APP.getKey())
      .setSince("6.2")
      .setDescription("Name of the logs to get");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    boolean nodeToNodeCall = serverLogging.isValidNodeToNodeCall(wsRequest.getHeaders());
    if (!nodeToNodeCall) {
      userSession.checkIsSystemAdministrator();
    }

    String logName = wsRequest.mandatoryParam(NAME);
    String filePrefix = getFilePrefix(logName);

    if (!nodeToNodeCall && serverLogging instanceof DistributedServerLogging) {
      buildAndSendLogsForDataCenterEdition(wsResponse, filePrefix, logName);
    } else {
      buildAndSendLogsForSingleNode(wsResponse, filePrefix);
    }
  }

  private void buildAndSendLogsForDataCenterEdition(Response wsResponse, String filePrefix, String logName) throws IOException {
    File zipfile = serverLogging.getDistributedLogs(filePrefix, logName);

    wsResponse.stream().setMediaType(MediaTypes.ZIP);
    FileUtils.copyFile(zipfile, wsResponse.stream().output());
  }

  private void buildAndSendLogsForSingleNode(Response wsResponse, String filePrefix) throws IOException {
    File file = serverLogging.getLogsForSingleNode(filePrefix);
    if (file == null) {
      wsResponse.stream().setStatus(HttpURLConnection.HTTP_NOT_FOUND);
    } else {
      wsResponse.stream().setMediaType(MediaTypes.TXT);
      FileUtils.copyFile(file, wsResponse.stream().output());
    }
  }

  private static String getFilePrefix(String logName) {
    return switch (logName) {
      case ACCESS_LOG -> ACCESS_LOG;
      case DEPRECATION_LOG -> DEPRECATION_LOG;
      default -> ProcessId.fromKey(logName).getLogFilenamePrefix();
    };
  }
}
