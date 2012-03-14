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
package org.sonar.diff;

/**
 * Equivalence function for {@link Text}.
 */
public abstract class TextComparator extends SequenceComparator<Text> {

  public static final TextComparator DEFAULT = new TextComparator() {
    @Override
    public boolean equals(Text a, int ai, Text b, int bi) {
      ai++;
      bi++;
      int as = a.lines.get(ai);
      int bs = b.lines.get(bi);
      int ae = a.lines.get(ai + 1);
      int be = b.lines.get(bi + 1);
      if (ae - as != be - bs) {
        return false;
      }
      while (as < ae) {
        if (a.content[as++] != b.content[bs++]) {
          return false;
        }
      }
      return true;
    }

    @Override
    protected int hashRegion(final byte[] raw, int start, final int end) {
      int hash = 5381;
      for (; start < end; start++) {
        hash = ((hash << 5) + hash) + (raw[start] & 0xff);
      }
      return hash;
    }
  };

  /**
   * Ignores all whitespace.
   */
  public static final TextComparator IGNORE_WHITESPACE = new TextComparator() {
    @Override
    public boolean equals(Text a, int ai, Text b, int bi) {
      ai++;
      bi++;
      int as = a.lines.get(ai);
      int bs = b.lines.get(bi);
      int ae = a.lines.get(ai + 1);
      int be = b.lines.get(bi + 1);
      ae = trimTrailingWhitespace(a.content, as, ae);
      be = trimTrailingWhitespace(b.content, bs, be);
      while ((as < ae) && (bs < be)) {
        byte ac = a.content[as];
        byte bc = b.content[bs];
        while ((as < ae - 1) && (isWhitespace(ac))) {
          as++;
          ac = a.content[as];
        }
        while ((bs < be - 1) && (isWhitespace(bc))) {
          bs++;
          bc = b.content[bs];
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
    protected int hashRegion(byte[] raw, int start, int end) {
      int hash = 5381;
      for (; start < end; start++) {
        byte c = raw[start];
        if (!isWhitespace(c)) {
          hash = ((hash << 5) + hash) + (c & 0xff);
        }
      }
      return hash;
    }
  };

  @Override
  public int hash(Text seq, int line) {
    final int begin = seq.lines.get(line + 1);
    final int end = seq.lines.get(line + 2);
    return hashRegion(seq.content, begin, end);
  }

  protected abstract int hashRegion(final byte[] raw, int start, final int end);

  private static final boolean[] WHITESPACE = new boolean[256];

  static {
    WHITESPACE['\r'] = true;
    WHITESPACE['\n'] = true;
    WHITESPACE['\t'] = true;
    WHITESPACE[' '] = true;
  }

  public static boolean isWhitespace(byte c) {
    return WHITESPACE[c & 0xff];
  }

  public static int trimTrailingWhitespace(byte[] raw, int start, int end) {
    end--;
    while (start <= end && isWhitespace(raw[end])) {
      end--;
    }
    return end + 1;
  }

  public static int trimLeadingWhitespace(byte[] raw, int start, int end) {
    while (start < end && isWhitespace(raw[start])) {
      start++;
    }
    return start;
  }

}
