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

import org.sonar.channel.RegexChannel;

/**
 * Headings are triggered by equal signs at the beginning of a line. The depth of the heading is determined by the number
 * of equal signs (up to 6).
 *
 * E.g., the input:
 * <pre>
 * = Level 1
 * == Level 2
 * === Level 3
 * ==== Level 4
 * ===== Level 5
 * ====== Level 6
 * </pre>
 * will produce:
 * <pre>
 * {@literal<h1>}Level 1{@literal</h1>}
 * {@literal<h2>}Level 2{@literal</h2>}
 * {@literal<h3>}Level 3{@literal</h3>}
 * {@literal<h4>}Level 4{@literal</h4>}
 * {@literal<h5>}Level 5{@literal</h5>}
 * {@literal<h6>}Level 6{@literal</h6>}
 * </pre>
 * @since 4.4
 *
 */
public class HtmlHeadingChannel extends RegexChannel<MarkdownOutput> {

  private static final int MAX_HEADING_DEPTH = 6;

  public HtmlHeadingChannel() {
    super("\\s*=+\\s[^\r\n]*+[\r\n]*");
  }

  @Override
  protected void consume(CharSequence token, MarkdownOutput output) {
    int index = 0;
    int headingLevel = 0;
    while(index < token.length() && Character.isWhitespace(token.charAt(index))) {
      index ++;
    }
    while(index < token.length() && index <= MAX_HEADING_DEPTH && token.charAt(index) == '=') {
      index ++;
      headingLevel ++;
    }
    while(index < token.length() && Character.isWhitespace(token.charAt(index))) {
      index ++;
    }
    CharSequence headingText = token.subSequence(index, token.length());

    output.append("<h" + headingLevel + ">");
    output.append(headingText);
    output.append("</h" + headingLevel + ">");
  }
}
