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
package org.sonar.core.issue.tracking;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.base.Strings;

import java.util.List;
import java.util.Set;

import org.sonar.core.hash.SourceLinesHashesComputer;

/**
 * Sequence of hash of lines for a given file
 */
public class LineHashSequence {

  /**
   * Hashes of lines. Line 1 is at index 0. No null elements.
   */
  private final List<String> hashes;
  private final SetMultimap<String, Integer> lineByHash;

  public LineHashSequence(List<String> hashes) {
    this.hashes = hashes;
    this.lineByHash = HashMultimap.create();
    
    int lineNo = 1;
    
    for (String hash : hashes) {
      lineByHash.put(hash, lineNo);
      lineNo++;
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
  public Set<Integer> getLinesForHash(String hash) {
    return lineByHash.get(hash);
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

  public static LineHashSequence createForLines(List<String> lines) {
    SourceLinesHashesComputer hashesComputer = new SourceLinesHashesComputer(lines.size());
    for (String line : lines) {
      hashesComputer.addLine(line);
    }
    return new LineHashSequence(hashesComputer.getLineHashes());
  }

}
