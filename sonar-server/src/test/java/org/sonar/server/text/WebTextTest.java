/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.core.source.HtmlSourceDecorator;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class WebTextTest {

  MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  HtmlSourceDecorator sourceDecorator = mock(HtmlSourceDecorator.class);
  WebText text = new WebText(macroInterpreter, sourceDecorator);

  @Test
  public void interpretMacros() throws Exception {
    text.interpretMacros("text with macros");
    verify(macroInterpreter, times(1)).interpret("text with macros");
    verifyZeroInteractions(sourceDecorator);
  }

  @Test
  public void markdownToHtml() throws Exception {
    String html = text.markdownToHtml("some *markdown*");
    assertThat(html).isEqualTo("some <em>markdown</em>");
  }

  @Test
  public void should_escape_markdown_input() throws Exception {
    String html = text.markdownToHtml("a > b");
    assertThat(html).isEqualTo("a &gt; b");
  }

  @Test
  public void highlightedSourceLines() throws Exception {
    text.highlightedSourceLines(123L);
    verify(sourceDecorator, times(1)).getDecoratedSourceAsHtml(123L);
    verifyZeroInteractions(macroInterpreter);
  }
}
