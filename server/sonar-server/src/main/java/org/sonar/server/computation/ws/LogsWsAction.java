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

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.user.UserSession;

public class LogsWsAction implements CeWsAction {

  public static final String ACTION = "logs";
  public static final String PARAM_TASK_UUID = "taskId";

  private final UserSession userSession;
  private final CeLogging ceLogging;

  public LogsWsAction(UserSession userSession, CeLogging ceLogging) {
    this.userSession = userSession;
    this.ceLogging = ceLogging;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .setDescription("Logs of a task. Returns HTTP code 404 if task does not " +
        "exist or if logs are not available. Requires system administration permission.")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_TASK_UUID)
      .setRequired(true)
      .setDescription("Id of task")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    userSession.checkGlobalPermission(UserRole.ADMIN);

    String taskUuid = wsRequest.mandatoryParam(PARAM_TASK_UUID);
    Optional<File> logFile = ceLogging.fileForTaskUuid(taskUuid);
    if (logFile.isPresent()) {
      writeFile(logFile.get(), wsResponse);
    } else {
      throw new NotFoundException();
    }
  }

  private static void writeFile(File file, Response wsResponse) {
    try {
      Response.Stream stream = wsResponse.stream();
      stream.setMediaType(MimeTypes.TXT);
      FileUtils.copyFile(file, stream.output());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to copy compute engine log file to HTTP response: " + file.getAbsolutePath(), e);
    }
  }
}
