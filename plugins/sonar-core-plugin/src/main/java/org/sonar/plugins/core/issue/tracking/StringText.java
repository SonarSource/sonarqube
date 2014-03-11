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
package org.sonar.plugins.core.issue.tracking;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Text is a {@link Sequence} of lines.
 */
public class StringText implements Sequence {

  final String content;

  /**
   * Map of line number to starting position within {@link #content}.
   */
  final List<Integer> lines;

  public StringText(String str) {
    this.content = str;
    this.lines = lineMap(content, 0, content.length());
  }

  public int length() {
    return lines.size() - 2;
  }

  private static List<Integer> lineMap(String buf, int ptr, int end) {
    List<Integer> lines = Lists.newArrayList();
    lines.add(Integer.MIN_VALUE);
    for (; ptr < end; ptr = nextLF(buf, ptr)) {
      lines.add(ptr);
    }
    lines.add(end);
    return lines;
  }

  private static int nextLF(String b, int ptr) {
    return next(b, ptr, '\n');
  }

  private static int next(final String b, int ptr, final char chrA) {
    final int sz = b.length();
    while (ptr < sz) {
      if (b.charAt(ptr++) == chrA) {
        return ptr;
      }
    }
    return ptr;
  }

}
