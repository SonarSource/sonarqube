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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.protocol.output.BatchReport;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@BatchSide
public class IssueTracking {

  private SourceHashHolder sourceHashHolder;

  /**
   * @param sourceHashHolder Null when working on resource that is not a file (directory/project)
   */
  public IssueTrackingResult track(@Nullable SourceHashHolder sourceHashHolder, Collection<ServerIssue> previousIssues, Collection<BatchReport.Issue> rawIssues) {
    this.sourceHashHolder = sourceHashHolder;
    IssueTrackingResult result = new IssueTrackingResult();

    // Map new issues with old ones
    mapIssues(rawIssues, previousIssues, sourceHashHolder, result);
    return result;
  }

  private String checksum(BatchReport.Issue rawIssue) {
    if (sourceHashHolder != null && rawIssue.hasLine()) {
      FileHashes hashedSource = sourceHashHolder.getHashedSource();
      // Extra verification if some plugin managed to create issue on a wrong line
      Preconditions.checkState(rawIssue.getLine() <= hashedSource.length(), "Invalid line number for issue %s. File has only %s line(s)", rawIssue, hashedSource.length());
      return hashedSource.getHash(rawIssue.getLine());
    }
    return null;
  }

  @VisibleForTesting
  void mapIssues(Collection<BatchReport.Issue> rawIssues, @Nullable Collection<ServerIssue> previousIssues, @Nullable SourceHashHolder sourceHashHolder,
    IssueTrackingResult result) {
    boolean hasLastScan = false;

    if (previousIssues != null) {
      hasLastScan = true;
      mapLastIssues(rawIssues, previousIssues, result);
    }

    // If each new issue matches an old one we can stop the matching mechanism
    if (result.matched().size() != rawIssues.size()) {
      if (sourceHashHolder != null && hasLastScan) {
        FileHashes hashedReference = sourceHashHolder.getHashedReference();
        if (hashedReference != null) {
          mapNewissues(hashedReference, sourceHashHolder.getHashedSource(), rawIssues, result);
        }
      }
      mapIssuesOnSameRule(rawIssues, result);
    }
  }

  private void mapLastIssues(Collection<BatchReport.Issue> rawIssues, Collection<ServerIssue> previousIssues, IssueTrackingResult result) {
    for (ServerIssue lastIssue : previousIssues) {
      result.addUnmatched(lastIssue);
    }

    // Try first to match issues on same rule with same line and with same checksum (but not necessarily with same message)
    for (BatchReport.Issue rawIssue : rawIssues) {
      if (isNotAlreadyMapped(rawIssue, result)) {
        mapIssue(
          rawIssue,
          findLastIssueWithSameLineAndChecksum(rawIssue, result),
          result);
      }
    }
  }

  private void mapNewissues(FileHashes hashedReference, FileHashes hashedSource, Collection<BatchReport.Issue> rawIssues, IssueTrackingResult result) {

    IssueTrackingBlocksRecognizer rec = new IssueTrackingBlocksRecognizer(hashedReference, hashedSource);

    RollingFileHashes a = RollingFileHashes.create(hashedReference, 5);
    RollingFileHashes b = RollingFileHashes.create(hashedSource, 5);

    Multimap<Integer, BatchReport.Issue> rawIssuesByLines = rawIssuesByLines(rawIssues, rec, result);
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

    for (Integer line : rawIssuesByLines.keySet()) {
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
        map(rawIssuesByLines.get(hashOccurrence.lineB), lastIssuesByLines.get(hashOccurrence.lineA), result);
        lastIssuesByLines.removeAll(hashOccurrence.lineA);
        rawIssuesByLines.removeAll(hashOccurrence.lineB);
      }
    }

    // Check if remaining number of lines exceeds threshold
    if (lastIssuesByLines.keySet().size() * rawIssuesByLines.keySet().size() < 250000) {
      List<LinePair> possibleLinePairs = Lists.newArrayList();
      for (Integer oldLine : lastIssuesByLines.keySet()) {
        for (Integer newLine : rawIssuesByLines.keySet()) {
          int weight = rec.computeLengthOfMaximalBlock(oldLine, newLine);
          possibleLinePairs.add(new LinePair(oldLine, newLine, weight));
        }
      }
      Collections.sort(possibleLinePairs, LINE_PAIR_COMPARATOR);
      for (LinePair linePair : possibleLinePairs) {
        // High probability that lineA has been moved to lineB, so we can map all Issues on lineA to all Issues on lineB
        map(rawIssuesByLines.get(linePair.lineB), lastIssuesByLines.get(linePair.lineA), result);
      }
    }
  }

  private void mapIssuesOnSameRule(Collection<BatchReport.Issue> rawIssues, IssueTrackingResult result) {
    // Try then to match issues on same rule with same message and with same checksum
    for (BatchReport.Issue rawIssue : rawIssues) {
      if (isNotAlreadyMapped(rawIssue, result)) {
        mapIssue(
          rawIssue,
          findLastIssueWithSameChecksumAndMessage(rawIssue, result.unmatchedByKeyForRule(ruleKey(rawIssue)).values()),
          result);
      }
    }

    // Try then to match issues on same rule with same line and with same message
    for (BatchReport.Issue rawIssue : rawIssues) {
      if (isNotAlreadyMapped(rawIssue, result)) {
        mapIssue(
          rawIssue,
          findLastIssueWithSameLineAndMessage(rawIssue, result.unmatchedByKeyForRule(ruleKey(rawIssue)).values()),
          result);
      }
    }

    // Last check: match issue if same rule and same checksum but different line and different message
    // See SONAR-2812
    for (BatchReport.Issue rawIssue : rawIssues) {
      if (isNotAlreadyMapped(rawIssue, result)) {
        mapIssue(
          rawIssue,
          findLastIssueWithSameChecksum(rawIssue, result.unmatchedByKeyForRule(ruleKey(rawIssue)).values()),
          result);
      }
    }
  }

  private static RuleKey ruleKey(BatchReport.Issue rawIssue) {
    return RuleKey.of(rawIssue.getRuleRepository(), rawIssue.getRuleKey());
  }

  private void map(Collection<BatchReport.Issue> rawIssues, Collection<ServerIssue> previousIssues, IssueTrackingResult result) {
    for (BatchReport.Issue rawIssue : rawIssues) {
      if (isNotAlreadyMapped(rawIssue, result)) {
        for (ServerIssue previousIssue : previousIssues) {
          if (isNotAlreadyMapped(previousIssue, result) && Objects.equal(ruleKey(rawIssue), previousIssue.ruleKey())) {
            mapIssue(rawIssue, previousIssue, result);
            break;
          }
        }
      }
    }
  }

  private Multimap<Integer, BatchReport.Issue> rawIssuesByLines(Collection<BatchReport.Issue> rawIssues, IssueTrackingBlocksRecognizer rec, IssueTrackingResult result) {
    Multimap<Integer, BatchReport.Issue> rawIssuesByLines = LinkedHashMultimap.create();
    for (BatchReport.Issue rawIssue : rawIssues) {
      if (isNotAlreadyMapped(rawIssue, result) && rawIssue.hasLine() && rec.isValidLineInSource(rawIssue.getLine())) {
        rawIssuesByLines.put(rawIssue.getLine(), rawIssue);
      }
    }
    return rawIssuesByLines;
  }

  private static Multimap<Integer, ServerIssue> lastIssuesByLines(Collection<ServerIssue> previousIssues, IssueTrackingBlocksRecognizer rec) {
    Multimap<Integer, ServerIssue> previousIssuesByLines = LinkedHashMultimap.create();
    for (ServerIssue previousIssue : previousIssues) {
      if (rec.isValidLineInReference(previousIssue.line())) {
        previousIssuesByLines.put(previousIssue.line(), previousIssue);
      }
    }
    return previousIssuesByLines;
  }

  private ServerIssue findLastIssueWithSameChecksum(BatchReport.Issue rawIssue, Collection<ServerIssue> previousIssues) {
    for (ServerIssue previousIssue : previousIssues) {
      if (isSameChecksum(rawIssue, previousIssue)) {
        return previousIssue;
      }
    }
    return null;
  }

  private ServerIssue findLastIssueWithSameLineAndMessage(BatchReport.Issue rawIssue, Collection<ServerIssue> previousIssues) {
    for (ServerIssue previousIssue : previousIssues) {
      if (isSameLine(rawIssue, previousIssue) && isSameMessage(rawIssue, previousIssue)) {
        return previousIssue;
      }
    }
    return null;
  }

  private ServerIssue findLastIssueWithSameChecksumAndMessage(BatchReport.Issue rawIssue, Collection<ServerIssue> previousIssues) {
    for (ServerIssue previousIssue : previousIssues) {
      if (isSameChecksum(rawIssue, previousIssue) && isSameMessage(rawIssue, previousIssue)) {
        return previousIssue;
      }
    }
    return null;
  }

  private ServerIssue findLastIssueWithSameLineAndChecksum(BatchReport.Issue rawIssue, IssueTrackingResult result) {
    Collection<ServerIssue> sameRuleAndSameLineAndSameChecksum = result.unmatchedForRuleAndForLineAndForChecksum(ruleKey(rawIssue), line(rawIssue), checksum(rawIssue));
    if (!sameRuleAndSameLineAndSameChecksum.isEmpty()) {
      return sameRuleAndSameLineAndSameChecksum.iterator().next();
    }
    return null;
  }

  @CheckForNull
  private static Integer line(BatchReport.Issue rawIssue) {
    return rawIssue.hasLine() ? rawIssue.getLine() : null;
  }

  private static boolean isNotAlreadyMapped(ServerIssue previousIssue, IssueTrackingResult result) {
    return result.unmatched().contains(previousIssue);
  }

  private static boolean isNotAlreadyMapped(BatchReport.Issue rawIssue, IssueTrackingResult result) {
    return !result.isMatched(rawIssue);
  }

  private boolean isSameChecksum(BatchReport.Issue rawIssue, ServerIssue previousIssue) {
    return Objects.equal(previousIssue.checksum(), checksum(rawIssue));
  }

  private boolean isSameLine(BatchReport.Issue rawIssue, ServerIssue previousIssue) {
    return Objects.equal(previousIssue.line(), line(rawIssue));
  }

  private boolean isSameMessage(BatchReport.Issue rawIssue, ServerIssue previousIssue) {
    return Objects.equal(message(rawIssue), previousIssue.message());
  }

  @CheckForNull
  private static String message(BatchReport.Issue rawIssue) {
    return rawIssue.hasMsg() ? rawIssue.getMsg() : null;
  }

  private static void mapIssue(BatchReport.Issue rawIssue, @Nullable ServerIssue ref, IssueTrackingResult result) {
    if (ref != null) {
      result.setMatch(rawIssue, ref);
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
