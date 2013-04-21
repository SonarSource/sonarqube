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

package org.sonar.squid.text;

public class MultiLinesCommentHandler extends LineContextHandler {

  private StringBuilder currentLineComment;
  private boolean isFirstLineOfComment = false;

  private boolean isJavadoc = false;
  private boolean isLicenseHeader = false;

  private boolean commentStarted = false;

  private static final String START_COMMENT_TAG = "/*";
  private static final String START_JAVADOC_TAG = "/**";
  private static final String START_GWT_NATIVE_CODE_TAG = "/*-{";
  private static final String END_COMMENT_TAG = "*/";

  @Override
  boolean matchToEnd(Line line, StringBuilder pendingLine) {
    if ( !commentStarted) {
      throw new IllegalStateException("Method doContextBegin(StringBuilder pendingLine) has not been called first (line = '" + pendingLine
          + "').");
    }
    currentLineComment.append(getLastCharacter(pendingLine));
    if (isJavaDoc()) {
      isJavadoc = true;
    }
    if (isGwtNativeCode()) {
      initProperties();
      return true;
    }
    boolean match = matchEndOfString(pendingLine, END_COMMENT_TAG);
    if (match && !(isFirstLineOfComment && pendingLine.indexOf(START_COMMENT_TAG) + 1 == pendingLine.indexOf(END_COMMENT_TAG))) {
      endOfCommentLine(line);
      initProperties();
      return true;
    }
    return false;
  }

  private boolean isGwtNativeCode() {
    return isFirstLineOfComment && currentLineComment.length() == START_GWT_NATIVE_CODE_TAG.length()
        && currentLineComment.toString().equals(START_GWT_NATIVE_CODE_TAG);
  }

  private boolean isJavaDoc() {
    return isFirstLineOfComment && currentLineComment.length() == START_JAVADOC_TAG.length()
        && currentLineComment.toString().equals(START_JAVADOC_TAG);
  }

  @Override
  boolean matchToBegin(Line line, StringBuilder pendingLine) {
    boolean match = matchEndOfString(pendingLine, START_COMMENT_TAG);
    if (match) {
      isFirstLineOfComment = true;
      commentStarted = true;
      currentLineComment = new StringBuilder(START_COMMENT_TAG);
      isLicenseHeader = (line.getLineIndex() == 1);
    }
    return match;
  }

  @Override
  boolean matchWithEndOfLine(Line line, StringBuilder pendingLine) {
    endOfCommentLine(line);
    return false;
  }

  private void endOfCommentLine(Line line) {
    line.setComment(currentLineComment.toString(), isJavadoc, isLicenseHeader);
    currentLineComment = new StringBuilder();
    isFirstLineOfComment = false;
  }

  private void initProperties() {
    commentStarted = false;
    isJavadoc = false;
    isLicenseHeader = false;
    currentLineComment = new StringBuilder();
    isFirstLineOfComment = false;
  }
}
