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
package org.sonar.server.text;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RubyTextServiceTest {

  MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  RubyTextService text = new RubyTextService(macroInterpreter);

  @Test
  public void interpretMacros() {
    text.interpretMacros("text with macros");
    verify(macroInterpreter, times(1)).interpret("text with macros");
  }

  @Test
  public void markdownToHtml() {
    String html = text.markdownToHtml("some *markdown*");
    assertThat(html).isEqualTo("some <em>markdown</em>");
  }

  @Test
  public void should_escape_markdown_input() {
    String html = text.markdownToHtml("a > b");
    assertThat(html).isEqualTo("a &gt; b");
  }
}
