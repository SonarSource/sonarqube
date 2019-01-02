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
package org.sonar.server.source;

class OpeningHtmlTag {

  private final int startOffset;
  private final String cssClass;

  OpeningHtmlTag(int startOffset, String cssClass) {
    this.startOffset = startOffset;
    this.cssClass = cssClass;
  }

  int getStartOffset() {
    return startOffset;
  }

  String getCssClass() {
    return cssClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return compareTo((OpeningHtmlTag) o);
  }

  @Override
  public int hashCode() {
    int result = startOffset;
    result = 31 * result + (cssClass != null ? cssClass.hashCode() : 0);
    return result;
  }

  private boolean compareTo(OpeningHtmlTag otherTag) {
    if (startOffset != otherTag.startOffset) {
      return false;
    }
    return (cssClass != null) ? cssClass.equals(otherTag.cssClass) : (otherTag.cssClass == null);
  }
}
