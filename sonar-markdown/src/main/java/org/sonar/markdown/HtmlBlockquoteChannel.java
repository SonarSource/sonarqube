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

import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;
import org.sonar.channel.RegexChannel;

/**
 * Markdown treats lines starting with a greater than sign (&gt;) as a block of quoted text.
 *
 * E.g, the input:
 * <pre>
 * &gt; Yesterday it worked
 * &gt; Today it is not working
 * &gt; Software is like that
 * </pre>
 * will produce:
 * {@literal<blockquote>}{@literal<p>}Yesterday it worked{@literal<br/>}
 * Today it is not working{@literal<br/>}
 * Software is like that{@literal</blockquote>}
 * @since 4.4
 */
class HtmlBlockquoteChannel extends Channel<MarkdownOutput> {

  private QuotedLineElementChannel quotedLineElement = new QuotedLineElementChannel();
  private EndOfLine endOfLine = new EndOfLine();
  private boolean pendingBlockConstruction;

  @Override
  public boolean consume(CodeReader code, MarkdownOutput output) {
    try {
      if (code.getColumnPosition() == 0 && quotedLineElement.consume(code, output)) {
        while (endOfLine.consume(code, output) && quotedLineElement.consume(code, output)) {
          // consume input
        }
        output.append("</blockquote>");
        return true;
      }
      return false;
    } finally {
      pendingBlockConstruction = false;
    }
  }

  private class QuotedLineElementChannel extends RegexChannel<MarkdownOutput> {

    private QuotedLineElementChannel() {
      super("&gt;\\s[^\r\n]*+");
    }

    @Override
    public void consume(CharSequence token, MarkdownOutput output) {
      if (!pendingBlockConstruction) {
        output.append("<blockquote>");
        pendingBlockConstruction = true;
      }
      output.append(token.subSequence(searchIndexOfFirstCharacter(token), token.length()));
      output.append("<br/>");
    }

    private int searchIndexOfFirstCharacter(CharSequence token) {
      int index = 0;
      while (index < token.length()) {
        if (token.charAt(index) == '&') {
          index += 4;
          while (index < token.length()) {
            index ++;
            if (token.charAt(index) != ' ') {
              return index;
            }
          }
        }
        index ++;
      }
      return token.length() - 1;
    }
  }

  private static final class EndOfLine extends RegexChannel<MarkdownOutput> {

    public EndOfLine() {
      super("(\r?\n)|(\r)");
    }

    @Override
    public void consume(CharSequence token, MarkdownOutput output) {
      output.append(token);
    }
  }
}
