/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EncoderBase;
import java.util.List;
import org.apache.commons.text.StringEscapeUtils;

import static ch.qos.logback.core.CoreConstants.COMMA_CHAR;
import static ch.qos.logback.core.CoreConstants.DOUBLE_QUOTE_CHAR;
import static ch.qos.logback.core.CoreConstants.UTF_8_CHARSET;

public class ScannerLogbackEncoder extends EncoderBase<ILoggingEvent> {

  private static final byte[] EMPTY_BYTES = new byte[0];
  private static final char OPEN_OBJ = '{';
  private static final char CLOSE_OBJ = '}';
  private static final char VALUE_SEPARATOR = COMMA_CHAR;
  private static final char QUOTE = DOUBLE_QUOTE_CHAR;
  private static final String QUOTE_COL = "\":";

  private final ThrowableProxyConverter tpc = new ThrowableProxyConverter();

  public ScannerLogbackEncoder() {
    tpc.setOptionList(List.of("full"));
    tpc.start();
  }

  @Override
  public byte[] headerBytes() {
    return EMPTY_BYTES;
  }

  @Override
  public byte[] encode(ILoggingEvent event) {
    StringBuilder sb = new StringBuilder();
    sb.append(OPEN_OBJ);
    var level = event.getLevel();
    if (level != null) {
      appenderMember(sb, "level", level.levelStr);
      sb.append(VALUE_SEPARATOR);
    }

    appenderMember(sb, "message", StringEscapeUtils.escapeJson(event.getFormattedMessage()));

    IThrowableProxy tp = event.getThrowableProxy();
    String stackTrace = null;
    if (tp != null) {
      sb.append(VALUE_SEPARATOR);
      stackTrace = tpc.convert(event);
      appenderMember(sb, "stacktrace", StringEscapeUtils.escapeJson(stackTrace));
    }

    sb.append(CLOSE_OBJ);
    sb.append(CoreConstants.JSON_LINE_SEPARATOR);
    return sb.toString().getBytes(UTF_8_CHARSET);
  }

  private static void appenderMember(StringBuilder sb, String key, String value) {
    sb.append(QUOTE).append(key).append(QUOTE_COL).append(QUOTE).append(value).append(QUOTE);
  }

  @Override
  public byte[] footerBytes() {
    return EMPTY_BYTES;
  }
}
