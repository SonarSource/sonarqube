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
package org.sonar.server.platform.ws;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.process.ProcessId;
import org.sonar.server.log.ServerLogging;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.MediaTypes;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class LogsAction implements SystemWsAction {
  /**
   * @deprecated since 10.4, use {@link #NAME} instead.
   */
  @Deprecated(since = "10.4", forRemoval = true)
  private static final String PROCESS_PROPERTY = "process";
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
        new Change("10.4", "Add support for deprecation logs in process property."),
        new Change("10.4", format("Deprecate property '%s' in favor of '%s'.", PROCESS_PROPERTY, NAME)))
      .setHandler(this);

    action
      .createParam(NAME)
      .setDeprecatedKey(PROCESS_PROPERTY, "10.4")
      .setPossibleValues(values)
      .setDefaultValue(ProcessId.APP.getKey())
      .setSince("6.2")
      .setDescription("Name of the logs to get");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    userSession.checkIsSystemAdministrator();

    String logName = wsRequest.mandatoryParam(NAME);
    String filePrefix = getFilePrefix(logName);

    File logsDir = serverLogging.getLogsDir();

    Optional<Path> path = getLogFilePath(filePrefix, logsDir);

    if (path.isEmpty()) {
      wsResponse.stream().setStatus(HttpURLConnection.HTTP_NOT_FOUND);
      return;
    }

    File file = new File(logsDir, path.get().getFileName().toString());

    // filenames are defined in the enum LogProcess. Still to prevent any vulnerability,
    // path is double-checked to prevent returning any file present on the file system.
    if (file.exists() && file.getParentFile().equals(logsDir)) {
      wsResponse.stream().setMediaType(MediaTypes.TXT);
      FileUtils.copyFile(file, wsResponse.stream().output());
    } else {
      wsResponse.stream().setStatus(HttpURLConnection.HTTP_NOT_FOUND);
    }

  }

  private static String getFilePrefix(String logName) {
    return switch (logName) {
      case ACCESS_LOG -> ACCESS_LOG;
      case DEPRECATION_LOG -> DEPRECATION_LOG;
      default -> ProcessId.fromKey(logName).getLogFilenamePrefix();
    };
  }

  private static Optional<Path> getLogFilePath(String filePrefix, File logsDir) throws IOException {
    try (Stream<Path> stream = Files.list(Paths.get(logsDir.getPath()))) {
      return stream
        .filter(hasMatchingLogFiles(filePrefix))
        .max(Comparator.comparing(Path::toString));
    }
  }

  private static Predicate<Path> hasMatchingLogFiles(String filePrefix) {
    return p -> {
      String stringPath = p.getFileName().toString();
      return stringPath.startsWith(filePrefix) && stringPath.endsWith(".log");
    };
  }
}
