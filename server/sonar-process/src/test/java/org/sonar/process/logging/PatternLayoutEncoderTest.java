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
package org.sonar.process.logging;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class PatternLayoutEncoderTest {

  PatternLayoutEncoder underTest = new PatternLayoutEncoder();

  @Before
  public void before() {
    underTest.start();
  }

  @Test
  public void start_should_initialize_escaped_message_converter() {
    assertThat(underTest.getLayout())
      .isInstanceOf(ch.qos.logback.classic.PatternLayout.class);

    assertThat(((ch.qos.logback.classic.PatternLayout) underTest.getLayout()).getDefaultConverterMap())
      .contains(
        entry("m", EscapedMessageConverter.class.getName()),
        entry("msg", EscapedMessageConverter.class.getName()),
        entry("message", EscapedMessageConverter.class.getName()));
  }

}
