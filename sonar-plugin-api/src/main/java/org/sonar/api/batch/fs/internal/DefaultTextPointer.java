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
package org.sonar.api.batch.fs.internal;

import org.sonar.api.batch.fs.TextPointer;

/**
 * @since 5.2
 */
public class DefaultTextPointer implements TextPointer {

  private final int line;
  private final int lineOffset;

  public DefaultTextPointer(int line, int lineOffset) {
    this.line = line;
    this.lineOffset = lineOffset;
  }

  @Override
  public int line() {
    return line;
  }

  @Override
  public int lineOffset() {
    return lineOffset;
  }

  @Override
  public String toString() {
    return "[line=" + line + ", lineOffset=" + lineOffset + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DefaultTextPointer)) {
      return false;
    }
    DefaultTextPointer other = (DefaultTextPointer) obj;
    return other.line == this.line && other.lineOffset == this.lineOffset;
  }

  @Override
  public int hashCode() {
    return 37 * this.line + lineOffset;
  }

  @Override
  public int compareTo(TextPointer o) {
    if (this.line == o.line()) {
      return Integer.compare(this.lineOffset, o.lineOffset());
    }
    return Integer.compare(this.line, o.line());
  }

}
