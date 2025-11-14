/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;
import java.util.Map;
import java.util.function.Supplier;

public class PatternLayoutEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {

  @Override
  public void start() {
    PatternLayout patternLayout = new PatternLayout();
    patternLayout.getDefaultConverterSupplierMap().putAll(getEscapedMessageConverterConfig());
    patternLayout.setContext(context);
    patternLayout.setPattern(getPattern());
    patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
    patternLayout.start();
    this.layout = patternLayout;
    super.start();
  }

  private static Map<String, Supplier<DynamicConverter>> getEscapedMessageConverterConfig() {
    return Map.of(
      "m", EscapedMessageConverter::new,
      "msg", EscapedMessageConverter::new,
      "message", EscapedMessageConverter::new);
  }

}
