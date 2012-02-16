/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.markdown;

import org.sonar.channel.RegexChannel;

/**
 * Markdown treats double backtick quote (``) as indicators of code. Text wrapped with two `` and that spans on multiple lines will be wrapped with 
 * an HTML {@literal <pre><code>} tag.
 * 
 * E.g., the input:
 * ``This code
 *   spans on 2 lines`` 
 * will produce:
 * {@literal<pre>}{@literal<code>}This code
 * spans on 2 lines{@literal</code>}{@literal</pre>}
 */
class HtmlMultilineCodeChannel extends RegexChannel<MarkdownOutput> {

  public HtmlMultilineCodeChannel() {
    super("``[\\s\\S]+?``");
  }

  @Override
  protected void consume(CharSequence token, MarkdownOutput output) {
    output.append("<pre><code>");
    output.append(token.subSequence(2, token.length() - 2));
    output.append("</code></pre>");
  }
}
