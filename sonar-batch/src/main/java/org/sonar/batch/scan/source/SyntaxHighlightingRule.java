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
package org.sonar.batch.scan.source;

/**
 * @since 3.6
 */
public class SyntaxHighlightingRule {

  private final int startPosition;
  private final int endPosition;
  private final String textType;

  private SyntaxHighlightingRule(int startPosition, int endPosition, String textType) {
    this.startPosition = startPosition;
    this.endPosition = endPosition;
    this.textType = textType;
  }

  public static SyntaxHighlightingRule create(int startPosition, int endPosition, String textType) {
    return new SyntaxHighlightingRule(startPosition, endPosition, textType);
  }

  public int getStartPosition() {
    return startPosition;
  }

  public int getEndPosition() {
    return endPosition;
  }

  public String getTextType() {
    return textType;
  }
}
