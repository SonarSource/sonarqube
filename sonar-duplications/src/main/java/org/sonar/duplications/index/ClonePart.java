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
package org.sonar.duplications.index;

public class ClonePart {

  private final String resourceId;
  private final int unitStart;
  private final int lineStart;
  private final int lineEnd;

  /**
   * Cache for hash code.
   */
  private int hash;

  public ClonePart(String resourceId, int unitStart, int lineStart, int lineEnd) {
    this.resourceId = resourceId;
    this.unitStart = unitStart;
    this.lineStart = lineStart;
    this.lineEnd = lineEnd;
  }

  public String getResourceId() {
    return resourceId;
  }

  public int getUnitStart() {
    return unitStart;
  }

  public int getLineStart() {
    return lineStart;
  }

  public int getLineEnd() {
    return lineEnd;
  }

  public int getLines() {
    return lineEnd - lineStart + 1;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClonePart) {
      ClonePart another = (ClonePart) obj;
      return another.resourceId.equals(resourceId)
          && another.lineStart == lineStart
          && another.lineEnd == lineEnd
          && another.unitStart == unitStart;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      h = resourceId.hashCode();
      h = 31 * h + lineStart;
      h = 31 * h + lineEnd;
      h = 31 * h + unitStart;
      hash = h;
    }
    return h;
  }

  @Override
  public String toString() {
    return "'" + resourceId + "':[" + unitStart + "|" + lineStart + "-" + lineEnd + "]";
  }

}
