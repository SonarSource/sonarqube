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
package org.sonar.server.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.process.ProcessProperties;
import org.sonar.process.logging.LogbackHelper;

import static org.sonar.api.utils.log.LoggerLevel.TRACE;
import static org.sonar.process.ProcessProperties.Property.LOG_LEVEL_UNIFIED;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;

@ServerSide
@ComputeEngineSide
public class ServerLogging implements Startable {
  static final List<String> UNIFIED_PREFIXES = List.of("com.sonarsource", "org.sonarsource");
  /**
   * Allowed values of {@link ProcessProperties.Property#LOG_LEVEL_UNIFIED}. Unlike the root level, this only
   * affects the {@link #UNIFIED_PREFIXES} loggers, so the full range of Logback levels is allowed, including
   * {@link Level#WARN} and {@link Level#OFF}.
   */
  private static final List<String> ALLOWED_UNIFIED_PREFIX_LEVELS = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF");
  /** Used for Hazelcast's distributed queries in cluster mode */
  private static ServerLogging instance;

  private final LogbackHelper helper;
  private final Configuration config;
  private final ServerProcessLogging serverProcessLogging;
  private final Database database;

  @Inject
  public ServerLogging(Configuration config, ServerProcessLogging serverProcessLogging, Database database) {
    this(new LogbackHelper(), config, serverProcessLogging, database);
  }

  @VisibleForTesting
  ServerLogging(LogbackHelper helper, Configuration config, ServerProcessLogging serverProcessLogging, Database database) {
    this.helper = helper;
    this.config = config;
    this.serverProcessLogging = serverProcessLogging;
    this.database = database;
  }

  /**
   * The level applied to the {@link #UNIFIED_PREFIXES} loggers: {@code unifiedPrefixLevelOverride} if set (see
   * {@link ProcessProperties.Property#LOG_LEVEL_UNIFIED}), otherwise the default of capping to
   * {@link Level#WARN} when the root level is {@link Level#INFO}, or following the root level otherwise.
   */
  static Level toUnifiedLevel(Level rootLevel, @CheckForNull Level unifiedPrefixLevelOverride) {
    if (unifiedPrefixLevelOverride != null) {
      return unifiedPrefixLevelOverride;
    }
    return Level.INFO.equals(rootLevel) ? Level.WARN : rootLevel;
  }

  /**
   * Resolves the value of {@link ProcessProperties.Property#LOG_LEVEL_UNIFIED}, if any. An unsupported value
   * is logged as a warning and ignored rather than failing startup.
   */
  @CheckForNull
  static Level resolveUnifiedPrefixLevelOverride(@CheckForNull String rawValue) {
    if (rawValue == null) {
      return null;
    }
    String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
    if (!ALLOWED_UNIFIED_PREFIX_LEVELS.contains(normalized)) {
      LoggerFactory.getLogger(ServerLogging.class).warn("Ignoring property {}: log level {} is not a supported value (allowed levels are {})",
        LOG_LEVEL_UNIFIED.getKey(), rawValue, ALLOWED_UNIFIED_PREFIX_LEVELS);
      return null;
    }
    return Level.toLevel(normalized);
  }

  @Override
  public void start() {
    instance = this;
  }

  @Override
  public void stop() {
    instance = null;
  }

  public static void changeLevelFromHazelcastDistributedQuery(LoggerLevel level) {
    instance.changeLevel(level);
  }

  public static int getWebAPIPortFromHazelcastQuery() {
    Optional<String> port = instance.config.get(ProcessProperties.Property.WEB_PORT.getKey());
    return port.map(Integer::parseInt).orElse(9000);
  }

  public static String getWebAPIAddressFromHazelcastQuery() {
    return instance.config.get(ProcessProperties.Property.WEB_HOST.getKey())
      .orElseGet(() -> instance.config.get(ProcessProperties.Property.CLUSTER_NODE_HOST.getKey())
        .orElseThrow(() -> new IllegalStateException("No web host found in configuration")));
  }

  public static String getWebAPIContextFromHazelcastQuery() {
    final String context = instance.config.get(ProcessProperties.Property.WEB_CONTEXT.getKey()).orElse("");
    // Strip trailing slash to avoid double slashes in URL construction (e.g., /sonarqube//api)
    return context.endsWith("/") ? context.substring(0, context.length() - 1) : context;
  }

  public void changeLevel(LoggerLevel level) {
    Level logbackLevel = Level.toLevel(level.name());
    database.enableSqlLogging(level == TRACE);
    helper.changeRoot(serverProcessLogging.getLogLevelConfig(), logbackLevel);
    LoggerContext ctx = helper.getRootContext();
    Level unifiedPrefixLevelOverride = resolveUnifiedPrefixLevelOverride(config.get(LOG_LEVEL_UNIFIED.getKey()).orElse(null));
    UNIFIED_PREFIXES.forEach(prefix -> ctx.getLogger(prefix).setLevel(toUnifiedLevel(logbackLevel, unifiedPrefixLevelOverride)));
    LoggerFactory.getLogger(ServerLogging.class).info("Level of logs changed to {}", level);
  }

  public LoggerLevel getRootLoggerLevel() {
    return Loggers.get(Logger.ROOT_LOGGER_NAME).getLevel();
  }

  /**
   * The directory that contains log files. May not exist.
   */
  public File getLogsDir() {
    return new File(config.get(PATH_LOGS.getKey()).get());
  }

  public Optional<Path> getLogFilePath(String filePrefix, File logsDir) throws IOException {
    try (Stream<Path> stream = Files.list(Paths.get(logsDir.getPath()))) {
      return stream
        .filter(hasMatchingLogFiles(filePrefix))
        .max(Comparator.comparing(Path::toString));
    }
  }

  public Predicate<Path> hasMatchingLogFiles(String filePrefix) {
    return p -> {
      String stringPath = p.getFileName().toString();
      return stringPath.startsWith(filePrefix) && stringPath.endsWith(".log");
    };
  }

  public File getLogsForSingleNode(String filePrefix) throws IOException {
    File logsDir = getLogsDir();
    Optional<Path> path = getLogFilePath(filePrefix, logsDir);

    if (path.isEmpty()) {
      return null;
    }

    File file = new File(logsDir, path.get().getFileName().toString());

    // filenames are defined in the enum LogProcess. Still to prevent any vulnerability,
    // path is double-checked to prevent returning any file present on the file system.
    if (file.exists() && file.getParentFile().equals(logsDir)) {
      return file;
    } else {
      return null;
    }
  }

  public File getDistributedLogs(String filePrefix, String logName) {
    throw new UnsupportedOperationException("This method should not be called on a standalone instance of SonarQube");
  }

  public boolean isValidNodeToNodeCall(Map<String, String> headers) {
    return false;
  }
}
