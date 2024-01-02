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
package org.sonar.application.process;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.config.AppSettings;
import org.sonar.process.Props;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.process.ProcessProperties.Property.LOG_JSON_OUTPUT;

/**
 * Reads process output and writes to logs
 */
public class StreamGobbler extends Thread {
  public static final String LOGGER_STARTUP = "startup";
  public static final String LOGGER_GOBBLER = "gobbler";

  private static final String LOGGER_STARTUP_FORMAT = String.format("[%s]", LOGGER_STARTUP);

  private final AppSettings appSettings;

  private final InputStream is;
  private final Logger logger;
  /*
  This logger forwards startup logs (thanks to re-using fileappender) from subprocesses to sonar.log when running SQ not from wrapper.
   */
  private final Logger startupLogger;

  StreamGobbler(InputStream is, AppSettings appSettings, String processKey) {
    this(is, processKey, appSettings, LoggerFactory.getLogger(LOGGER_GOBBLER), LoggerFactory.getLogger(LOGGER_STARTUP));
  }

  StreamGobbler(InputStream is, String processKey, AppSettings appSettings, Logger logger, Logger startupLogger) {
    super(String.format("Gobbler[%s]", processKey));
    this.is = is;
    this.logger = logger;
    this.appSettings = appSettings;
    this.startupLogger = startupLogger;
  }

  @Override
  public void run() {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.contains(LOGGER_STARTUP)) {
          logStartupLog(line);
        } else {
          logger.info(line);
        }
      }
    } catch (Exception ignored) {
      // ignore
    }
  }

  private void logStartupLog(String line) {
    if (isJsonLoggingEnabled()) {
      JsonElement jsonElement = JsonParser.parseString(line);
      if (!jsonElement.getAsJsonObject().get("logger").getAsString().equals(LOGGER_STARTUP)) {
        // Log contains "startup" string but only in the message content. We skip.
        return;
      }
      startupLogger.warn(jsonElement.getAsJsonObject().get("message").getAsString());
    } else if (line.contains(LOGGER_STARTUP_FORMAT)) {
      startupLogger.warn(line.substring(line.indexOf(LOGGER_STARTUP_FORMAT) + LOGGER_STARTUP_FORMAT.length() + 1));
    }
  }

  private boolean isJsonLoggingEnabled() {
    Props props = appSettings.getProps();
    return props.valueAsBoolean(LOG_JSON_OUTPUT.getKey(), Boolean.parseBoolean(LOG_JSON_OUTPUT.getDefaultValue()));
  }

  static void waitUntilFinish(@Nullable StreamGobbler gobbler) {
    if (gobbler != null) {
      try {
        gobbler.join();
      } catch (InterruptedException ignored) {
        // consider as finished, restore the interrupted flag
        Thread.currentThread().interrupt();
      }
    }
  }
}
