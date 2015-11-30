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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.computation.log.LogFileRef;
import org.sonar.server.exceptions.NotFoundException;
import org.sonarqube.ws.MediaTypes;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;

public class LogsAction implements CeWsAction {

  public static final String ACTION = "logs";
  public static final String PARAM_TASK_UUID = "taskId";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final CeLogging ceLogging;

  public LogsAction(DbClient dbClient, UserSession userSession, CeLogging ceLogging) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.ceLogging = ceLogging;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .setDescription("Logs of a task. Format of response is plain text. HTTP code 404 is returned if the task does not " +
        "exist or if logs are not available. Requires system administration permission.")
      .setResponseExample(getClass().getResource("logs-example.log"))
      .setInternal(true)
      .setSince("5.2")
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
    LogFileRef ref = loadLogRef(taskUuid);
    Optional<File> logFile = ceLogging.getFile(ref);
    if (logFile.isPresent()) {
      writeFile(logFile.get(), wsResponse);
    } else {
      throw new NotFoundException(format("Logs of task %s not found", taskUuid));
    }
  }

  private LogFileRef loadLogRef(String taskUuid) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<CeQueueDto> queueDto = dbClient.ceQueueDao().selectByUuid(dbSession, taskUuid);
      if (queueDto.isPresent()) {
        return LogFileRef.from(queueDto.get());
      }
      Optional<CeActivityDto> activityDto = dbClient.ceActivityDao().selectByUuid(dbSession, taskUuid);
      if (activityDto.isPresent()) {
        return LogFileRef.from(activityDto.get());
      }
      throw new NotFoundException(format("Task %s not found", taskUuid));

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static void writeFile(File file, Response wsResponse) {
    try {
      Response.Stream stream = wsResponse.stream();
      stream.setMediaType(MediaTypes.TXT);
      FileUtils.copyFile(file, stream.output());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to copy compute engine log file to HTTP response: " + file.getAbsolutePath(), e);
    }
  }
}
