/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.markdown;

import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;
import org.sonar.channel.RegexChannel;

class HtmlListChannel extends Channel<MarkdownOutput> {

  private ListElementChannel listElement = new ListElementChannel();
  private EndOfLine endOfLine = new EndOfLine();
  private boolean pendingListConstruction;

  @Override
  public boolean consume(CodeReader code, MarkdownOutput output) {
    try {
      if (code.getColumnPosition() == 0 && listElement.consume(code, output)) {
        while (endOfLine.consume(code, output) && listElement.consume(code, output)) {
          // consume input
        }
        output.append("</ul>");
        return true;
      }
      return false;
    } finally {
      pendingListConstruction = false;
    }
  }

  private class ListElementChannel extends RegexChannel<MarkdownOutput> {

    public ListElementChannel() {
      super("\\s*+\\*\\s[^\r\n]*+");
    }

    @Override
    protected void consume(CharSequence token, MarkdownOutput output) {
      if (!pendingListConstruction) {
        output.append("<ul>");
        pendingListConstruction = true;
      }
      output.append("<li>");
      output.append(token.subSequence(searchIndexOfFirstCharacter(token), token.length()));
      output.append("</li>");
    }

    private int searchIndexOfFirstCharacter(CharSequence token) {
      for (int index = 0; index < token.length(); index++) {
        if (token.charAt(index) == '*') {
          for (index++; index < token.length(); index++) {
            if (token.charAt(index) != ' ') {
              return index;
            }
          }
        }
      }
      return token.length() - 1;
    }
  }

  private static final class EndOfLine extends RegexChannel<MarkdownOutput> {

    public EndOfLine() {
      super("(\r?\n)|(\r)");
    }

    @Override
    protected void consume(CharSequence token, MarkdownOutput output) {
      output.append(token);
    }
  }
}
