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
package org.sonar.api.batch.fs.internal;

import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

/**
 * @since 5.2
 */
public class DefaultTextRange implements TextRange {

  private final TextPointer start;
  private final TextPointer end;

  public DefaultTextRange(TextPointer start, TextPointer end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public TextPointer start() {
    return start;
  }

  @Override
  public TextPointer end() {
    return end;
  }

  @Override
  public boolean overlap(TextRange another) {
    // [A,B] and [C,D]
    // B > C && D > A
    return this.end.compareTo(another.start()) > 0 && another.end().compareTo(this.start) > 0;
  }

  @Override
  public String toString() {
    return "Range[from " + start + " to " + end + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    DefaultTextRange other = (DefaultTextRange) obj;
    return start.equals(other.start) && end.equals(other.end);
  }

  @Override
  public int hashCode() {
    return start.hashCode() * 17 + end.hashCode();
  }

}
