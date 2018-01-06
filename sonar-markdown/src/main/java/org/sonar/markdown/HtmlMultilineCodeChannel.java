/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.markdown;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonar.channel.RegexChannel;

/**
 * Markdown treats double backtick quote (``) as indicators of code. Text wrapped with two `` and that spans on multiple lines will be wrapped with 
 * an HTML {@literal <pre><code>} tag.
 * 
 * E.g., the input:
 * <pre>
 * ``
 * This code
 * spans on 2 lines
 * ``
 * </pre> 
 * will produce:
 * {@literal<pre>}{@literal<code>}This code
 * spans on 2 lines{@literal</code>}{@literal</pre>}
 *
 * @since 2.14
 */
class HtmlMultilineCodeChannel extends RegexChannel<MarkdownOutput> {

  private static final String NEWLINE = "(?:\\n\\r|\\r|\\n)";
  private static final String LANGUAGE = "([a-zA-Z][a-zA-Z0-9_]*+)?";
  private static final String DETECTION_REGEXP = "``" + LANGUAGE + NEWLINE + "([\\s\\S]+?)" + NEWLINE + "``";

  private final Matcher regexpMatcher;

  public HtmlMultilineCodeChannel() {
    super(DETECTION_REGEXP);
    regexpMatcher = Pattern.compile(DETECTION_REGEXP).matcher("");
  }

  @Override
  protected void consume(CharSequence token, MarkdownOutput output) {
    regexpMatcher.reset(token);
    regexpMatcher.matches();
    output.append("<pre");
    String language = regexpMatcher.group(1);
    if (language != null) {
      output.append(" lang=\"");
      output.append(language);
      output.append("\"");
    }
    output.append("><code>");
    output.append(regexpMatcher.group(2));
    output.append("</code></pre>");
  }
}
