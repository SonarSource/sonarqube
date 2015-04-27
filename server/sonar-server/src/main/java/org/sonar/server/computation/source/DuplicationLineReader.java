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

package org.sonar.server.computation.source;

import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.source.db.FileSourceDb;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class DuplicationLineReader implements LineReader {

  private final List<BatchReport.Duplication> duplications;
  private final Map<BatchReport.Range, Integer> duplicationIdsByRange;

  public DuplicationLineReader(List<BatchReport.Duplication> duplications) {
    this.duplications = newArrayList(duplications);
    // Sort duplication to have deterministic results and avoid false variation that would lead to an unnecessary update of the source files
    // data
    Collections.sort(this.duplications, new DuplicationComparator());

    this.duplicationIdsByRange = createDuplicationIdsByRange(this.duplications);
  }

  @Override
  public void read(FileSourceDb.Line.Builder lineBuilder) {
    int line = lineBuilder.getLine();
    List<BatchReport.Range> blocks = findDuplicationBlockMatchingLine(line);
    for (BatchReport.Range block : blocks) {
      lineBuilder.addDuplication(duplicationIdsByRange.get(block));
    }
  }

  private List<BatchReport.Range> findDuplicationBlockMatchingLine(int line) {
    List<BatchReport.Range> blocks = newArrayList();
    for (BatchReport.Duplication duplication : duplications) {
      if (matchLine(duplication.getOriginPosition(), line)) {
        blocks.add(duplication.getOriginPosition());
      }
      for (BatchReport.Duplicate duplicate : duplication.getDuplicateList()) {
        if (isDuplicationOnSameFile(duplicate) && matchLine(duplicate.getRange(), line)) {
          blocks.add(duplicate.getRange());
        }
      }
    }
    return blocks;
  }

  private static boolean isDuplicationOnSameFile(BatchReport.Duplicate duplicate) {
    return !duplicate.hasOtherFileKey() && !duplicate.hasOtherFileRef();
  }

  private static boolean matchLine(BatchReport.Range range, int line) {
    return range.getStartLine() <= line && line <= range.getEndLine();
  }

  private static int length(BatchReport.Range range) {
    return (range.getEndLine() - range.getStartLine()) + 1;
  }

  private Map<BatchReport.Range, Integer> createDuplicationIdsByRange(List<BatchReport.Duplication> duplications) {
    Map<BatchReport.Range, Integer> map = newHashMap();
    int blockId = 1;
    for (BatchReport.Duplication duplication : this.duplications) {
      map.put(duplication.getOriginPosition(), blockId);
      blockId++;
      for (BatchReport.Duplicate duplicate : duplication.getDuplicateList()) {
        if (isDuplicationOnSameFile(duplicate)) {
          map.put(duplicate.getRange(), blockId);
          blockId++;
        }
      }
    }
    return map;
  }

  private static class DuplicationComparator implements Comparator<BatchReport.Duplication>, Serializable {
    @Override
    public int compare(BatchReport.Duplication d1, BatchReport.Duplication d2) {
      if (d1.getOriginPosition().getStartLine() == d2.getOriginPosition().getStartLine()) {
        return Integer.compare(length(d1.getOriginPosition()), length(d2.getOriginPosition()));
      } else {
        return Integer.compare(d1.getOriginPosition().getStartLine(), d2.getOriginPosition().getStartLine());
      }
    }
  }

}
