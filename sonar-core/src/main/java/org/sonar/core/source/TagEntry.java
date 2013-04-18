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

package org.sonar.core.source;

public class TagEntry {

  private final int startOffset;
  private final String cssClass;

  public TagEntry(int startOffset, String cssClass) {
    this.startOffset = startOffset;
    this.cssClass = cssClass;
  }

  public int getStartOffset() {
    return startOffset;
  }

  public String getCssClass() {
    return cssClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TagEntry tagEntry = (TagEntry) o;

    if (startOffset != tagEntry.startOffset) return false;
    if (cssClass != null ? !cssClass.equals(tagEntry.cssClass) : tagEntry.cssClass != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = startOffset;
    result = 31 * result + (cssClass != null ? cssClass.hashCode() : 0);
    return result;
  }
}
