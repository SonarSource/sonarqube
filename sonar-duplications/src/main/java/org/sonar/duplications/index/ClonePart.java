/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.duplications.index;

public class ClonePart {

  private final String resourceId;
  private final int startUnit;
  private final int startLine;
  private final int endLine;

  /**
   * Cache for hash code.
   */
  private int hash;

  public ClonePart(String resourceId, int startUnit, int startLine, int endLine) {
    this.resourceId = resourceId;
    this.startUnit = startUnit;
    this.startLine = startLine;
    this.endLine = endLine;
  }

  public String getResourceId() {
    return resourceId;
  }

  public int getUnitStart() {
    return startUnit;
  }

  public int getStartLine() {
    return startLine;
  }

  public int getEndLine() {
    return endLine;
  }

  public int getLines() {
    return endLine - startLine + 1;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClonePart) {
      ClonePart another = (ClonePart) obj;
      return another.resourceId.equals(resourceId)
        && another.startLine == startLine
        && another.endLine == endLine
        && another.startUnit == startUnit;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      h = resourceId.hashCode();
      h = 31 * h + startLine;
      h = 31 * h + endLine;
      h = 31 * h + startUnit;
      hash = h;
    }
    return h;
  }

  @Override
  public String toString() {
    return "'" + resourceId + "':[" + startUnit + "|" + startLine + "-" + endLine + "]";
  }

}
