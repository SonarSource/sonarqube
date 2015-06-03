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
package org.sonar.core.issue.tracking;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Sequence of hash of lines for a given file
 */
public class LineHashSequence {

  private static final int[] EMPTY_INTS = new int[0];

  /**
   * Hashes of lines. Line 1 is at index 0. No null elements.
   */
  private final List<String> hashes;
  private final Map<String, int[]> linesByHash;

  public LineHashSequence(List<String> hashes) {
    this.hashes = hashes;
    this.linesByHash = new HashMap<>(hashes.size());
    for (int line = 1; line <= hashes.size(); line++) {
      String hash = hashes.get(line - 1);
      int[] lines = linesByHash.get(hash);
      linesByHash.put(hash, appendLineTo(line, lines));
    }
  }

  /**
   * Number of lines
   */
  public int length() {
    return hashes.size();
  }

  /**
   * Checks if the line, starting with 1, is defined.
   */
  public boolean hasLine(int line) {
    return 0 < line && line <= hashes.size();
  }

  /**
   * The lines, starting with 1, that matches the given hash.
   */
  public int[] getLinesForHash(String hash) {
    int[] lines = linesByHash.get(hash);
    return lines == null ? EMPTY_INTS : lines;
  }

  /**
   * Hash of the given line, which starts with 1. Return empty string
   * is the line does not exist.
   */
  public String getHashForLine(int line) {
    if (line > 0 && line <= hashes.size()) {
      return Strings.nullToEmpty(hashes.get(line - 1));
    }
    return "";
  }

  List<String> getHashes() {
    return hashes;
  }

  private static int[] appendLineTo(int line, @Nullable int[] to) {
    int[] result;
    if (to == null) {
      result = new int[] {line};
    } else {
      result = new int[to.length + 1];
      System.arraycopy(to, 0, result, 0, to.length);
      result[result.length - 1] = line;
    }
    return result;
  }

  public static LineHashSequence createForLines(Iterable<String> lines) {
    List<String> hashes = new ArrayList<>();
    for (String line : lines) {
      hashes.add(hash(line));
    }
    return new LineHashSequence(hashes);
  }

  // FIXME duplicates ComputeFileSourceData
  private static String hash(String line) {
    String reducedLine = StringUtils.replaceChars(line, "\t ", "");
    if (reducedLine.isEmpty()) {
      return "";
    }
    return DigestUtils.md5Hex(reducedLine);
  }
}
