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
import org.sonar.markdown.Markdown;
import org.sonar.server.source.HtmlSourceDecorator;

import java.util.List;

/**
 * @since 3.6
 */
public class RubyTextService implements ServerComponent {

  private final MacroInterpreter macroInterpreter;
  private final HtmlSourceDecorator sourceDecorator;

  public RubyTextService(MacroInterpreter macroInterpreter, HtmlSourceDecorator sourceDecorator) {
    this.macroInterpreter = macroInterpreter;
    this.sourceDecorator = sourceDecorator;
  }

  // TODO add ruby example
  public String interpretMacros(String text) {
    return macroInterpreter.interpret(text);
  }

  // TODO add ruby example
  public String markdownToHtml(String markdown) {
    // TODO move HTML escaping to sonar-markdown
    return Markdown.convertToHtml(StringEscapeUtils.escapeHtml(markdown));
  }

  // TODO add ruby example
  public List<String> highlightedSourceLines(long snapshotId) {
    return sourceDecorator.getDecoratedSourceAsHtml(snapshotId);
  }
}
