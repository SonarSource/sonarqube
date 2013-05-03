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

import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.ServerComponent;
import org.sonar.core.source.HtmlSourceDecorator;
import org.sonar.markdown.Markdown;

import java.util.List;

public class WebText implements ServerComponent {

  private final MacroInterpreter macroInterpreter;
  private final HtmlSourceDecorator sourceDecorator;

  public WebText(MacroInterpreter macroInterpreter, HtmlSourceDecorator sourceDecorator) {
    this.macroInterpreter = macroInterpreter;
    this.sourceDecorator = sourceDecorator;
  }

  public String interpretMacros(String text) {
    return macroInterpreter.interpret(text);
  }

  public String markdownToHtml(String markdown) {
    // TODO move HTML escaping to sonar-markdown
    return Markdown.convertToHtml(StringEscapeUtils.escapeHtml(markdown));
  }

  public List<String> highlightedSourceLines(long snapshotId) {
    return sourceDecorator.getDecoratedSourceAsHtml(snapshotId);
  }
}
