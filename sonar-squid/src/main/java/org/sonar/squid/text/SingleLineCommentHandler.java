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

package org.sonar.squid.text;

public class SingleLineCommentHandler extends LineContextHandler {

  private StringBuilder comment;

  private final String commentStartTag;
  private final String commentNotStartTag;

  public SingleLineCommentHandler(String commentStartTag) {
    this(commentStartTag, null);
  }

  public SingleLineCommentHandler(String commentStartTag, String commentNotStartTag) {
    this.commentStartTag = commentStartTag;
    this.commentNotStartTag = commentNotStartTag;
  }

  @Override
  boolean matchToEnd(Line line, StringBuilder pendingLine) {
    if (comment == null) {
      throw new IllegalStateException("Method doContextBegin(StringBuilder pendingLine) has not been called.");
    }
    comment.append(getLastCharacter(pendingLine));
    return false;
  }

  @Override
  boolean matchToBegin(Line line, StringBuilder pendingLine) {
    boolean doContextBegin = matchEndOfString(pendingLine, commentStartTag)
        && (commentNotStartTag == null || !matchEndOfString(pendingLine, commentNotStartTag));
    if (doContextBegin) {
      comment = new StringBuilder(commentStartTag);
    }
    return doContextBegin;
  }

  @Override
  boolean matchWithEndOfLine(Line line, StringBuilder pendingLine) {
    line.setComment(comment.toString());
    comment = null;
    return true;
  }
}
