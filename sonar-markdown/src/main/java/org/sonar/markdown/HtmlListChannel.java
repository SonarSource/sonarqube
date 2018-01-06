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
 * Lists come in two flavors:
 * <ul>
 *  <li>Unordered lists, triggered by lines that start with a <code>*</code></li>
 *  <li>Ordered lists (added in 4.4), triggered by lines that start with a digit followed by a <code>.</code></li>
 * </ul>

 * E.g., the input:
 * <pre>
 * * One
 * * Two
 * * Three
 * </pre>
 * will produce:
 * {@literal<ul>}{@literal<li>}One{@literal</li>}
 * {@literal<li>}Two{@literal</li>}
 * {@literal<li>}Three{@literal</li>}{@literal</ul>}
 *
 * Whereas the input:
 * <pre>
 * 1. One
 * 1. Two
 * 1. Three
 * </pre>
 * will produce:
 * {@literal<ol>}{@literal<li>}One{@literal</li>}
 * {@literal<li>}Two{@literal</li>}
 * {@literal<li>}Three{@literal</li>}{@literal</ol>}
 *
 * @since 2.10.1
 */
class HtmlListChannel extends Channel<MarkdownOutput> {

  private OrderedListElementChannel orderedListElement = new OrderedListElementChannel();
  private UnorderedListElementChannel unorderedListElement = new UnorderedListElementChannel();
  private EndOfLine endOfLine = new EndOfLine();
  private boolean pendingListConstruction;

  @Override
  public boolean consume(CodeReader code, MarkdownOutput output) {
    try {
      ListElementChannel currentChannel = null;
      if (code.getColumnPosition() == 0) {
        if (orderedListElement.consume(code, output)) {
          currentChannel = orderedListElement;
        } else if (unorderedListElement.consume(code, output)) {
          currentChannel = unorderedListElement;
        }
        if (currentChannel != null) {
          while (endOfLine.consume(code, output) && currentChannel.consume(code, output)) {
            // consume input
          }
          output.append("</" + currentChannel.listElement + ">");
          return true;
        }
      }
      return false;
    } finally {
      pendingListConstruction = false;
    }
  }

  private class OrderedListElementChannel extends ListElementChannel {
    public OrderedListElementChannel() {
      super("\\d\\.", "ol");
    }
  }

  private class UnorderedListElementChannel extends ListElementChannel {
    public UnorderedListElementChannel() {
      super("\\*", "ul");
    }
  }

  private abstract class ListElementChannel extends RegexChannel<MarkdownOutput> {

    private String listElement;

    protected ListElementChannel(String markerRegexp, String listElement) {
      super("\\s*+" + markerRegexp + "\\s[^\r\n]*+");
      this.listElement = listElement;
    }

    @Override
    protected void consume(CharSequence token, MarkdownOutput output) {
      if (!pendingListConstruction) {
        output.append("<" + listElement + ">");
        pendingListConstruction = true;
      }
      output.append("<li>");
      output.append(token.subSequence(searchIndexOfFirstCharacter(token), token.length()));
      output.append("</li>");
    }

    private int searchIndexOfFirstCharacter(CharSequence token) {
      for (int index = 0; index < token.length(); index++) {
        if (token.charAt(index) == '*'
          || Character.isDigit(token.charAt(index))) {
          if (token.charAt(index + 1) == '.') {
            index ++;
          }
          while (++ index < token.length()) {
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
