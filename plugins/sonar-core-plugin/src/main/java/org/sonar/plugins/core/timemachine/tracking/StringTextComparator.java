/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.timemachine.tracking;

/**
 * Equivalence function for {@link StringText}.
 */
public abstract class StringTextComparator implements SequenceComparator<StringText> {

  /**
   * Ignores all whitespace.
   */
  public static final StringTextComparator IGNORE_WHITESPACE = new StringTextComparator() {

    public boolean equals(StringText a, int ai, StringText b, int bi) {
      ai++;
      bi++;
      int as = a.lines.get(ai);
      int bs = b.lines.get(bi);
      int ae = a.lines.get(ai + 1);
      int be = b.lines.get(bi + 1);
      ae = trimTrailingWhitespace(a.content, as, ae);
      be = trimTrailingWhitespace(b.content, bs, be);
      while ((as < ae) && (bs < be)) {
        char ac = a.content.charAt(as);
        char bc = b.content.charAt(bs);
        while ((as < ae - 1) && (Character.isWhitespace(ac))) {
          as++;
          ac = a.content.charAt(as);
        }
        while ((bs < be - 1) && (Character.isWhitespace(bc))) {
          bs++;
          bc = b.content.charAt(bs);
        }
        if (ac != bc) {
          return false;
        }
        as++;
        bs++;
      }
      return (as == ae) && (bs == be);
    }

    @Override
    protected int hashRegion(String content, int start, int end) {
      int hash = 5381;
      for (; start < end; start++) {
        char c = content.charAt(start);
        if (!Character.isWhitespace(c)) {
          hash = ((hash << 5) + hash) + (c & 0xff);
        }
      }
      return hash;
    }

  };

  public int hash(StringText seq, int line) {
    final int begin = seq.lines.get(line + 1);
    final int end = seq.lines.get(line + 2);
    return hashRegion(seq.content, begin, end);
  }

  protected abstract int hashRegion(String content, int start, int end);

  public static int trimTrailingWhitespace(String content, int start, int end) {
    end--;
    while (start <= end && Character.isWhitespace(content.charAt(end))) {
      end--;
    }
    return end + 1;
  }

}
