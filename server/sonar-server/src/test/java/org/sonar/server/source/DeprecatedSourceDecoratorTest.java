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

package org.sonar.server.source;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.component.ComponentDto;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeprecatedSourceDecoratorTest {

  @Mock
  CodeColorizers codeColorizers;

  DeprecatedSourceDecorator sourceDecorator;

  @Before
  public void setUp() throws Exception {
    sourceDecorator = new DeprecatedSourceDecorator(codeColorizers);
  }

  @Test
  public void get_source_as_html() throws Exception {
    String source = "line 1\nline 2\nline 3\n";
    String htmlSource = "<span>line 1</span>\n<span>line 2</span>\n";

    when(codeColorizers.toHtml(source, "java")).thenReturn(htmlSource);

    List<String> result = sourceDecorator.getSourceAsHtml(new ComponentDto().setLanguage("java"), source);
    assertThat(result).containsExactly("<span>line 1</span>", "<span>line 2</span>", "");
  }

  @Test
  public void get_source_as_html_with_from_and_to_params() throws Exception {
    String source = "line 1\nline 2\nline 3\n";
    String htmlSource = "<span>line 1</span>\n<span>line 2</span>\n<span>line 3</span>\n";

    when(codeColorizers.toHtml(source, "java")).thenReturn(htmlSource);

    List<String> result = sourceDecorator.getSourceAsHtml(new ComponentDto().setLanguage("java"), source, 2, 3);
    assertThat(result).containsExactly("<span>line 2</span>", "<span>line 3</span>");
  }

  @Test
  public void get_source_as_html_with_from_param() throws Exception {
    String source = "line 1\nline 2\nline 3\n";
    String htmlSource = "<span>line 1</span>\n<span>line 2</span>\n<span>line 3</span>\n";

    when(codeColorizers.toHtml(source, "java")).thenReturn(htmlSource);

    List<String> result = sourceDecorator.getSourceAsHtml(new ComponentDto().setLanguage("java"), source, 2, null);
    assertThat(result).containsExactly("<span>line 2</span>", "<span>line 3</span>", "");
  }

  @Test
  public void get_source_as_html_with_to_param() throws Exception {
    String source = "line 1\nline 2\nline 3\n";
    String htmlSource = "<span>line 1</span>\n<span>line 2</span>\n<span>line 3</span>\n";

    when(codeColorizers.toHtml(source, "java")).thenReturn(htmlSource);

    List<String> result = sourceDecorator.getSourceAsHtml(new ComponentDto().setLanguage("java"), source, null, 3);
    assertThat(result).containsExactly("<span>line 1</span>", "<span>line 2</span>", "<span>line 3</span>");
  }
}
