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

import org.apache.commons.lang.StringUtils;
import org.sonar.squid.measures.Measurable;
import org.sonar.squid.measures.Metric;

class Line implements Measurable<Metric> {

  private static final String NOSONAR_TAG = "NOSONAR";

  private final int lineIndex;
  private int blankLine = 0;
  private int line = 1;
  private int lineOfCode = 0;
  private int commentLine = 0;
  private int headerCommentLine = 0;
  private int commentBlankLine = 0;
  private int commentedOutCodeLine = 0;
  private String comment = null;
  private StringBuilder stringLine;
  private boolean isBlank;
  private boolean isThereJavadoc;
  private boolean isThereLicenseHeaderComment;

  Line() {
    this.lineIndex = 0;
  }

  Line(String stringLine) {
    this();
    setString(new StringBuilder(stringLine));
  }

  Line(int lineIndex, StringBuilder stringLine) {
    this(lineIndex);
    setString(stringLine);
  }

  Line(int lineIndex) {
    this.lineIndex = lineIndex;
  }

  final void setString(StringBuilder stringLine) {
    this.stringLine = stringLine;
    isBlank = isBlankLine();
  }

  private boolean isBlankLine() {
    for (int i = 0; i < stringLine.length(); i++) {
      if ( !Character.isWhitespace(stringLine.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public double getDouble(Metric metric) {
    return getInt(metric);
  }

  /**
   * {@inheritDoc}
   */
  public int getInt(Metric metric) {
    switch (metric) {
      case BLANK_LINES:
        return blankLine;
      case LINES:
        return line;
      case LINES_OF_CODE:
        return lineOfCode;
      case COMMENT_LINES:
        return commentLine;
      case COMMENTED_OUT_CODE_LINES:
        return commentedOutCodeLine;
      case COMMENT_BLANK_LINES:
        return commentBlankLine;
      case HEADER_COMMENT_LINES:
        return headerCommentLine;
      default:
        throw new IllegalStateException("Metric " + metric.name() + " is not available on Line object.");
    }
  }

  /**
   * {@inheritDoc}
   */
  public void setMeasure(Metric metric, double measure) {
    setMeasure(metric, (int) measure);
  }

  /**
   * {@inheritDoc}
   */
  public void setMeasure(Metric metric, int measure) {
    switch (metric) {
      case BLANK_LINES:
        blankLine = measure;
        break;
      case LINES_OF_CODE:
        lineOfCode = measure;
        break;
      case COMMENT_LINES:
        commentLine = measure;
        break;
      case COMMENTED_OUT_CODE_LINES:
        commentedOutCodeLine = measure;
        break;
      case COMMENT_BLANK_LINES:
        commentBlankLine = measure;
        break;
      case HEADER_COMMENT_LINES:
        headerCommentLine = measure;
        break;
      case LINES:
        throw new IllegalStateException("Metric LINES always equals 1 on a Line and you are not permitted to change this value.");
      default:
        throw new IllegalStateException("Metric " + metric.name() + " is not suitable for Line object.");
    }
  }

  void setComment(String comment) {
    this.comment = comment;
  }

  void setComment(String comment, boolean isJavadoc) {
    setComment(comment);
    this.isThereJavadoc = isJavadoc;
  }

  void setComment(String comment, boolean isJavadoc, boolean isLicenseHeader) {
    setComment(comment, isJavadoc);
    this.isThereLicenseHeaderComment = isLicenseHeader;
  }

  String getString() {
    return stringLine.toString();
  }

  boolean isBlank() {
    return !isThereComment() && isBlank;
  }

  boolean isThereCode() {
    if ( !isBlank() && !isThereComment()) {
      return true;
    }
    if (isThereComment() && isThereCodeBeforeOrAfterComment()) {
      return true;
    }
    return false;
  }

  private boolean isThereCodeBeforeOrAfterComment() {
    if ( !isThereComment()) {
      throw new IllegalStateException("You can't call this method when there isn't any comment");
    }
    boolean isThereCodeBeforeComment = false;
    boolean isThereCodeAfterComment = false;
    int commentStartIndex = stringLine.indexOf(comment);
    int commentEndIndex = commentStartIndex + comment.length() - 1;
    if (commentStartIndex > 0) {
      isThereCodeBeforeComment = !StringUtils.isBlank(stringLine.substring(0, commentStartIndex - 1));
    }
    if (commentEndIndex > 0 && commentEndIndex != stringLine.length() - 1) {
      isThereCodeAfterComment = !StringUtils.isBlank(stringLine.substring(commentEndIndex + 1));
    }
    return isThereCodeBeforeComment || isThereCodeAfterComment;
  }

  boolean isThereComment() {
    return comment != null;
  }

  String getComment() {
    return comment;
  }

  boolean isThereBlankComment() {
    if (isThereComment()) {
      for (int i = 0; i < comment.length(); i++) {
        char character = comment.charAt(i);
        if ( !Character.isWhitespace(character) && character != '*' && character != '/') {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  boolean isThereJavadoc() {
    return isThereJavadoc;
  }

  boolean isThereLicenseHeaderComment() {
    return isThereLicenseHeaderComment;
  }

  boolean isThereNoSonarTag() {
    return isThereComment() && comment.contains(NOSONAR_TAG);
  }

  int getLineIndex() {
    return lineIndex;
  }

  void deleteLineContent() {
    comment = null;
    stringLine = null;
  }
}
