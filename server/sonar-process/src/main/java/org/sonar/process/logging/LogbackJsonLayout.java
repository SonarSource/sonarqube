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
package org.sonar.process.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Formats logs in JSON.
 * <p>
 * Strongly inspired by https://github.com/qos-ch/logback/blob/master/logback-classic/src/main/java/ch/qos/logback/classic/html/DefaultThrowableRenderer.java
 */
public class LogbackJsonLayout extends LayoutBase<ILoggingEvent> {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    .withLocale(Locale.US)
    .withZone(ZoneId.systemDefault());
  private static final Pattern NEWLINE_REGEXP = Pattern.compile("\n");

  private final String processKey;

  public LogbackJsonLayout(String processKey) {
    this.processKey = requireNonNull(processKey);
  }

  String getProcessKey() {
    return processKey;
  }

  @Override
  public String doLayout(ILoggingEvent event) {
    StringWriter output = new StringWriter();
    try (JsonWriter json = new JsonWriter(output)) {
      json.beginObject();
      json.name("process").value(processKey);
      for (Map.Entry<String, String> entry : event.getMDCPropertyMap().entrySet()) {
        if (entry.getValue() != null) {
          json.name(entry.getKey()).value(entry.getValue());
        }
      }
      json
        .name("instant").value(event.getTimeStamp())
        .name("date").value(DATE_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())))
        .name("severity").value(event.getLevel().toString())
        .name("logger").value(event.getLoggerName())
        .name("message").value(NEWLINE_REGEXP.matcher(event.getFormattedMessage()).replaceAll("\r"));
      IThrowableProxy tp = event.getThrowableProxy();
      if (tp != null) {
        json.name("stacktrace").beginArray();
        int nbOfTabs = 0;
        while (tp != null) {
          printFirstLine(json, tp, nbOfTabs);
          render(json, tp, nbOfTabs);
          tp = tp.getCause();
          nbOfTabs++;
        }
        json.endArray();

      }
      json.endObject();
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalStateException("BUG - fail to create JSON", e);
    }
    output.write(System.lineSeparator());
    return output.toString();
  }

  private static void render(JsonWriter output, IThrowableProxy tp, int nbOfTabs) throws IOException {
    String tabs = StringUtils.repeat("\t", nbOfTabs);
    int commonFrames = tp.getCommonFrames();
    StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();

    for (int i = 0; i < stepArray.length - commonFrames; i++) {
      StackTraceElementProxy step = stepArray[i];
      output.value(tabs + step.toString());
    }

    if (commonFrames > 0) {
      output.value(tabs + "... " + commonFrames + " common frames omitted");
    }

    for (IThrowableProxy suppressed : tp.getSuppressed()) {
      output.value(format("%sSuppressed: %s: %s", tabs, suppressed.getClassName(), suppressed.getMessage()));
      render(output, suppressed, nbOfTabs + 1);
    }
  }

  private static void printFirstLine(JsonWriter output, IThrowableProxy tp, int nbOfTabs) throws IOException {
    String tabs = StringUtils.repeat("\t", nbOfTabs);
    int commonFrames = tp.getCommonFrames();
    if (commonFrames > 0) {
      output.value(tabs + CoreConstants.CAUSED_BY);
    }
    output.value(tabs + tp.getClassName() + ": " + tp.getMessage());
  }
}
