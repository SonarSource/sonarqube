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

package org.sonar.batch.issue.tracking;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.sonar.api.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.issue.internal.DefaultIssue;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@BatchSide
public class IssueTracking {

  /**
   * @param sourceHashHolder Null when working on resource that is not a file (directory/project)
   */
  public IssueTrackingResult track(@Nullable SourceHashHolder sourceHashHolder, Collection<ServerIssue> previousIssues, Collection<DefaultIssue> newIssues) {
    IssueTrackingResult result = new IssueTrackingResult();

    if (sourceHashHolder != null) {
      setChecksumOnNewIssues(newIssues, sourceHashHolder);
    }

    // Map new issues with old ones
    mapIssues(newIssues, previousIssues, sourceHashHolder, result);
    return result;
  }

  private void setChecksumOnNewIssues(Collection<DefaultIssue> issues, SourceHashHolder sourceHashHolder) {
    if (issues.isEmpty()) {
      return;
    }
    FileHashes hashedSource = sourceHashHolder.getHashedSource();
    for (DefaultIssue issue : issues) {
      Integer line = issue.line();
      if (line != null) {
        // Extra verification if some plugin managed to create issue on a wrong line
        Preconditions.checkState(line <= hashedSource.length(), "Invalid line number for issue %s. File has only %s line(s)", issue, hashedSource.length());
        issue.setChecksum(hashedSource.getHash(line));
      }
    }
  }

  @VisibleForTesting
  void mapIssues(Collection<DefaultIssue> newIssues, @Nullable Collection<ServerIssue> previousIssues, @Nullable SourceHashHolder sourceHashHolder, IssueTrackingResult result) {
    boolean hasLastScan = false;

    if (previousIssues != null) {
      hasLastScan = true;
      mapLastIssues(newIssues, previousIssues, result);
    }

    // If each new issue matches an old one we can stop the matching mechanism
    if (result.matched().size() != newIssues.size()) {
      if (sourceHashHolder != null && hasLastScan) {
        FileHashes hashedReference = sourceHashHolder.getHashedReference();
        if (hashedReference != null) {
          mapNewissues(hashedReference, sourceHashHolder.getHashedSource(), newIssues, result);
        }
      }
      mapIssuesOnSameRule(newIssues, result);
    }
  }

  private void mapLastIssues(Collection<DefaultIssue> newIssues, Collection<ServerIssue> previousIssues, IssueTrackingResult result) {
    for (ServerIssue lastIssue : previousIssues) {
      result.addUnmatched(lastIssue);
    }

    // Match the key of the issue. (For manual issues)
    for (DefaultIssue newIssue : newIssues) {
      mapIssue(newIssue, result.unmatchedByKeyForRule(newIssue.ruleKey()).get(newIssue.key()), result);
    }

    // Try first to match issues on same rule with same line and with same checksum (but not necessarily with same message)
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue, result)) {
        mapIssue(
          newIssue,
          findLastIssueWithSameLineAndChecksum(newIssue, result),
          result);
      }
    }
  }

  private void mapNewissues(FileHashes hashedReference, FileHashes hashedSource, Collection<DefaultIssue> newIssues, IssueTrackingResult result) {

    IssueTrackingBlocksRecognizer rec = new IssueTrackingBlocksRecognizer(hashedReference, hashedSource);

    RollingFileHashes a = RollingFileHashes.create(hashedReference, 5);
    RollingFileHashes b = RollingFileHashes.create(hashedSource, 5);

    Multimap<Integer, DefaultIssue> newIssuesByLines = newIssuesByLines(newIssues, rec, result);
    Multimap<Integer, ServerIssue> lastIssuesByLines = lastIssuesByLines(result.unmatched(), rec);

    Map<Integer, HashOccurrence> map = Maps.newHashMap();

    for (Integer line : lastIssuesByLines.keySet()) {
      int hash = a.getHash(line);
      HashOccurrence hashOccurrence = map.get(hash);
      if (hashOccurrence == null) {
        // first occurrence in A
        hashOccurrence = new HashOccurrence();
        hashOccurrence.lineA = line;
        hashOccurrence.countA = 1;
        map.put(hash, hashOccurrence);
      } else {
        hashOccurrence.countA++;
      }
    }

    for (Integer line : newIssuesByLines.keySet()) {
      int hash = b.getHash(line);
      HashOccurrence hashOccurrence = map.get(hash);
      if (hashOccurrence != null) {
        hashOccurrence.lineB = line;
        hashOccurrence.countB++;
      }
    }

    for (HashOccurrence hashOccurrence : map.values()) {
      if (hashOccurrence.countA == 1 && hashOccurrence.countB == 1) {
        // Guaranteed that lineA has been moved to lineB, so we can map all issues on lineA to all issues on lineB
        map(newIssuesByLines.get(hashOccurrence.lineB), lastIssuesByLines.get(hashOccurrence.lineA), result);
        lastIssuesByLines.removeAll(hashOccurrence.lineA);
        newIssuesByLines.removeAll(hashOccurrence.lineB);
      }
    }

    // Check if remaining number of lines exceeds threshold
    if (lastIssuesByLines.keySet().size() * newIssuesByLines.keySet().size() < 250000) {
      List<LinePair> possibleLinePairs = Lists.newArrayList();
      for (Integer oldLine : lastIssuesByLines.keySet()) {
        for (Integer newLine : newIssuesByLines.keySet()) {
          int weight = rec.computeLengthOfMaximalBlock(oldLine, newLine);
          possibleLinePairs.add(new LinePair(oldLine, newLine, weight));
        }
      }
      Collections.sort(possibleLinePairs, LINE_PAIR_COMPARATOR);
      for (LinePair linePair : possibleLinePairs) {
        // High probability that lineA has been moved to lineB, so we can map all Issues on lineA to all Issues on lineB
        map(newIssuesByLines.get(linePair.lineB), lastIssuesByLines.get(linePair.lineA), result);
      }
    }
  }

  private void mapIssuesOnSameRule(Collection<DefaultIssue> newIssues, IssueTrackingResult result) {
    // Try then to match issues on same rule with same message and with same checksum
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue, result)) {
        mapIssue(
          newIssue,
          findLastIssueWithSameChecksumAndMessage(newIssue, result.unmatchedByKeyForRule(newIssue.ruleKey()).values()),
          result);
      }
    }

    // Try then to match issues on same rule with same line and with same message
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue, result)) {
        mapIssue(
          newIssue,
          findLastIssueWithSameLineAndMessage(newIssue, result.unmatchedByKeyForRule(newIssue.ruleKey()).values()),
          result);
      }
    }

    // Last check: match issue if same rule and same checksum but different line and different message
    // See SONAR-2812
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue, result)) {
        mapIssue(
          newIssue,
          findLastIssueWithSameChecksum(newIssue, result.unmatchedByKeyForRule(newIssue.ruleKey()).values()),
          result);
      }
    }
  }

  private void map(Collection<DefaultIssue> newIssues, Collection<ServerIssue> previousIssues, IssueTrackingResult result) {
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue, result)) {
        for (ServerIssue previousIssue : previousIssues) {
          if (isNotAlreadyMapped(previousIssue, result) && Objects.equal(newIssue.ruleKey(), previousIssue.ruleKey())) {
            mapIssue(newIssue, previousIssue, result);
            break;
          }
        }
      }
    }
  }

  private Multimap<Integer, DefaultIssue> newIssuesByLines(Collection<DefaultIssue> newIssues, IssueTrackingBlocksRecognizer rec, IssueTrackingResult result) {
    Multimap<Integer, DefaultIssue> newIssuesByLines = LinkedHashMultimap.create();
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue, result) && rec.isValidLineInSource(newIssue.line())) {
        newIssuesByLines.put(newIssue.line(), newIssue);
      }
    }
    return newIssuesByLines;
  }

  private Multimap<Integer, ServerIssue> lastIssuesByLines(Collection<ServerIssue> previousIssues, IssueTrackingBlocksRecognizer rec) {
    Multimap<Integer, ServerIssue> previousIssuesByLines = LinkedHashMultimap.create();
    for (ServerIssue previousIssue : previousIssues) {
      if (rec.isValidLineInReference(previousIssue.line())) {
        previousIssuesByLines.put(previousIssue.line(), previousIssue);
      }
    }
    return previousIssuesByLines;
  }

  private ServerIssue findLastIssueWithSameChecksum(DefaultIssue newIssue, Collection<ServerIssue> previousIssues) {
    for (ServerIssue previousIssue : previousIssues) {
      if (isSameChecksum(newIssue, previousIssue)) {
        return previousIssue;
      }
    }
    return null;
  }

  private ServerIssue findLastIssueWithSameLineAndMessage(DefaultIssue newIssue, Collection<ServerIssue> previousIssues) {
    for (ServerIssue previousIssue : previousIssues) {
      if (isSameLine(newIssue, previousIssue) && isSameMessage(newIssue, previousIssue)) {
        return previousIssue;
      }
    }
    return null;
  }

  private ServerIssue findLastIssueWithSameChecksumAndMessage(DefaultIssue newIssue, Collection<ServerIssue> previousIssues) {
    for (ServerIssue previousIssue : previousIssues) {
      if (isSameChecksum(newIssue, previousIssue) && isSameMessage(newIssue, previousIssue)) {
        return previousIssue;
      }
    }
    return null;
  }

  private ServerIssue findLastIssueWithSameLineAndChecksum(DefaultIssue newIssue, IssueTrackingResult result) {
    Collection<ServerIssue> sameRuleAndSameLineAndSameChecksum = result.unmatchedForRuleAndForLineAndForChecksum(newIssue.ruleKey(), newIssue.line(), newIssue.checksum());
    if (!sameRuleAndSameLineAndSameChecksum.isEmpty()) {
      return sameRuleAndSameLineAndSameChecksum.iterator().next();
    }
    return null;
  }

  private boolean isNotAlreadyMapped(ServerIssue previousIssue, IssueTrackingResult result) {
    return result.unmatched().contains(previousIssue);
  }

  private boolean isNotAlreadyMapped(DefaultIssue newIssue, IssueTrackingResult result) {
    return !result.isMatched(newIssue);
  }

  private boolean isSameChecksum(DefaultIssue newIssue, ServerIssue previousIssue) {
    return Objects.equal(previousIssue.checksum(), newIssue.checksum());
  }

  private boolean isSameLine(DefaultIssue newIssue, ServerIssue previousIssue) {
    return Objects.equal(previousIssue.line(), newIssue.line());
  }

  private boolean isSameMessage(DefaultIssue newIssue, ServerIssue previousIssue) {
    return Objects.equal(newIssue.message(), previousIssue.message());
  }

  private void mapIssue(DefaultIssue issue, @Nullable ServerIssue ref, IssueTrackingResult result) {
    if (ref != null) {
      result.setMatch(issue, ref);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  private static class LinePair {
    int lineA;
    int lineB;
    int weight;

    public LinePair(int lineA, int lineB, int weight) {
      this.lineA = lineA;
      this.lineB = lineB;
      this.weight = weight;
    }
  }

  private static class HashOccurrence {
    int lineA;
    int lineB;
    int countA;
    int countB;
  }

  private static final Comparator<LinePair> LINE_PAIR_COMPARATOR = new Comparator<LinePair>() {
    @Override
    public int compare(LinePair o1, LinePair o2) {
      int weightDiff = o2.weight - o1.weight;
      if (weightDiff != 0) {
        return weightDiff;
      } else {
        return Math.abs(o1.lineA - o1.lineB) - Math.abs(o2.lineA - o2.lineB);
      }
    }
  };

}
