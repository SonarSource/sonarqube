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
package org.sonar.application.process;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.config.AppSettings;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.process.ProcessProperties.Property.LOG_JSON_OUTPUT;
import static org.sonar.process.ProcessProperties.Property.WEB_ACCESSLOGS_ENABLE;
import static org.sonar.process.ProcessProperties.Property.WEB_ACCESSLOGS_TARGET;

/**
 * Reads process output and writes to logs
 */
public class StreamGobbler extends Thread {
  public static final String LOGGER_STARTUP = "startup";
  public static final String LOGGER_GOBBLER = "gobbler";

  private static final String LOGGER_STARTUP_FORMAT = String.format("[%s]", LOGGER_STARTUP);

  /**
   * Everything a startup log line holds before its {@code [startup]} logger field: the date and time of
   * {@link org.sonar.process.logging.AbstractLogHelper#PREFIX_LOG_FORMAT}, then the level, an optional cluster node
   * name and the process key, ending with the (empty) thread id field.
   */
  private static final Pattern STARTUP_LINE_PREFIX = Pattern.compile("[\\d.]{10} [\\d:]{8} .*\\[\\]");

  private final AppSettings appSettings;

  private final InputStream is;
  private final Logger logger;
  /*
  This logger forwards startup logs (thanks to re-using fileappender) from subprocesses to sonar.log when running SQ not from wrapper.
   */
  private final Logger startupLogger;
  /**
   * Whether a line that is not a startup log must be forwarded rather than dropped. Only true for the Web process
   * once its access logs share this stream, so that no existing installation sees its sonar.log change.
   */
  private final boolean relayForeignLines;

  StreamGobbler(InputStream is, AppSettings appSettings, String processKey) {
    this(is, processKey, appSettings, LoggerFactory.getLogger(LOGGER_GOBBLER), LoggerFactory.getLogger(LOGGER_STARTUP));
  }

  StreamGobbler(InputStream is, String processKey, AppSettings appSettings, Logger logger, Logger startupLogger) {
    super(String.format("Gobbler[%s]", processKey));
    this.is = is;
    this.logger = logger;
    this.appSettings = appSettings;
    this.startupLogger = startupLogger;
    this.relayForeignLines = carriesAccessLogs(appSettings, processKey);
  }

  /**
   * The Web process writes its access logs to its own stdout when they are enabled and {@code sonar.web.accessLogs.target}
   * is set to {@code console} or {@code both}. Only then does this stream hold lines that are not core logs. Both
   * conditions mirror {@code TomcatAccessLog}, which attaches no appender at all once access logs are disabled.
   */
  private static boolean carriesAccessLogs(AppSettings appSettings, String processKey) {
    if (!ProcessId.WEB_SERVER.getKey().equals(processKey)) {
      return false;
    }
    Props props = appSettings.getProps();
    if (!props.valueAsBoolean(WEB_ACCESSLOGS_ENABLE.getKey(), true)) {
      return false;
    }
    String target = props.value(WEB_ACCESSLOGS_TARGET.getKey(), WEB_ACCESSLOGS_TARGET.getDefaultValue());
    return target != null && !"file".equalsIgnoreCase(target.trim());
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
      logJsonStartupLog(line);
    } else {
      logPlainTextStartupLog(line);
    }
  }

  private void logPlainTextStartupLog(String line) {
    int markerIndex = line.indexOf(LOGGER_STARTUP_FORMAT);
    // the stricter check only applies once access logs share this stream: for every other process the historical
    // "contains the marker" rule is kept, so that no existing installation sees its sonar.log change.
    if (markerIndex < 0 || (relayForeignLines && !isStartupLine(line, markerIndex))) {
      relayForeignLine(line);
      return;
    }
    // the marker may be the very end of the line, in which case there is no message after it. Keeping the historical
    // "skip exactly one character" semantics, but without running past the end of the line: the resulting exception
    // would be swallowed by run() and would kill this thread, silencing the sub process for good.
    int messageStart = markerIndex + LOGGER_STARTUP_FORMAT.length() + 1;
    String message = messageStart > line.length() ? "" : line.substring(messageStart);
    startupLogger.warn(message);
  }

  private void logJsonStartupLog(String line) {
    String message;
    try {
      JsonObject jsonObject = JsonParser.parseString(line).getAsJsonObject();
      if (!LOGGER_STARTUP.equals(asString(jsonObject.get("logger")))) {
        // the line is a core log whose message merely contains "startup"
        relayForeignLine(line);
        return;
      }
      message = asString(jsonObject.get("message"));
    } catch (RuntimeException e) {
      // not the JSON object we expect, e.g. a plain text access log line. Never rethrow: run() would swallow it and
      // this thread would die, permanently stopping the relay of everything the sub process logs afterwards.
      relayForeignLine(line);
      return;
    }
    if (message == null) {
      relayForeignLine(line);
    } else {
      startupLogger.warn(message);
    }
  }

  /**
   * A line that is not a startup log. Historically such a line was dropped, and it still is, so that the content of
   * sonar.log does not change for existing installations. The exception is the Web process once its access logs are
   * pushed to its stdout: those lines are not core logs at all and must reach the App's console as they are.
   */
  private void relayForeignLine(String line) {
    if (relayForeignLines) {
      logger.info(line);
    }
  }

  /**
   * Tells a genuine startup log apart from a line that merely contains the marker. Only applied when this gobbler
   * also carries access logs, whose content is attacker-controlled: without it, a request whose User-Agent contains
   * the marker would be diverted out of the access log stream and its tail emitted as a forged startup warning.
   * <p>
   * A startup log is written by the App-formatted console logger of {@code ServerProcessLogging}, so the marker is
   * the logger field of a line that starts with the log prefix, never a fragment in the middle of one.
   */
  private static boolean isStartupLine(String line, int markerIndex) {
    return STARTUP_LINE_PREFIX.matcher(line).region(0, markerIndex).matches();
  }

  @CheckForNull
  private static String asString(@Nullable JsonElement element) {
    return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
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
