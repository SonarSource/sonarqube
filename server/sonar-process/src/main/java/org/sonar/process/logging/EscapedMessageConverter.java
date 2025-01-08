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
package org.sonar.process.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

/**
 * Escapes log message which contains CR LF sequence
 */
public class EscapedMessageConverter extends ClassicConverter {

  private static final Pattern CR_PATTERN = Pattern.compile("\r");
  private static final Pattern LF_PATTERN = Pattern.compile("\n");

  public String convert(ILoggingEvent event) {
    String formattedMessage = event.getFormattedMessage();
    if (formattedMessage != null) {
      String result = CR_PATTERN.matcher(formattedMessage).replaceAll("\\\\r");
      result = LF_PATTERN.matcher(result).replaceAll("\\\\n");
      return result;
    }
    return null;
  }

}
