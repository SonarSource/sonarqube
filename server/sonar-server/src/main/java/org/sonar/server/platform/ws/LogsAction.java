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
package org.sonar.server.platform.ws;

import java.io.File;
import java.net.HttpURLConnection;
import org.apache.commons.io.FileUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.process.ProcessId;
import org.sonar.server.log.ServerLogging;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.MediaTypes;

import static java.util.Arrays.stream;

public class LogsAction implements SystemWsAction {

  private static final String PROCESS_PROPERTY = "process";

  private final UserSession userSession;
  private final ServerLogging serverLogging;

  public LogsAction(UserSession userSession, ServerLogging serverLogging) {
    this.userSession = userSession;
    this.serverLogging = serverLogging;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("logs")
      .setDescription("Get system logs in plain-text format. Requires system administration permission.")
      .setResponseExample(getClass().getResource("logs-example.log"))
      .setSince("5.2")
      .setHandler(this);

    action
      .createParam(PROCESS_PROPERTY)
      .setPossibleValues(stream(ProcessId.values())
        .map(ProcessId::getKey)
        .sorted()
        .collect(MoreCollectors.toList(ProcessId.values().length)))
      .setDefaultValue(ProcessId.APP.getKey())
      .setSince("6.2")
      .setDescription("Process to get logs from");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    userSession.checkIsSystemAdministrator();

    String processKey = wsRequest.mandatoryParam(PROCESS_PROPERTY);
    ProcessId processId = ProcessId.fromKey(processKey);

    File logsDir = serverLogging.getLogsDir();
    File file = new File(logsDir, processId.getLogFilenamePrefix() + ".log");
    // filenames are defined in the enum LogProcess. Still to prevent any vulnerability,
    // path is double-checked to prevent returning any file present on the file system.
    if (file.exists() && file.getParentFile().equals(logsDir)) {
      wsResponse.stream().setMediaType(MediaTypes.TXT);
      FileUtils.copyFile(file, wsResponse.stream().output());
    } else {
      wsResponse.stream().setStatus(HttpURLConnection.HTTP_NOT_FOUND);
    }
  }
}
