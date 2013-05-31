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

package org.sonar.plugins.core.issue;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.scan.LastSnapshots;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.plugins.core.issue.tracking.*;

import javax.annotation.Nullable;

import java.util.*;

public class IssueTracking implements BatchExtension {

  private final LastSnapshots lastSnapshots;
  private final SonarIndex index;

  public IssueTracking(LastSnapshots lastSnapshots, SonarIndex index) {
    this.lastSnapshots = lastSnapshots;
    this.index = index;
  }

  public IssueTrackingResult track(Resource resource, Collection<IssueDto> dbIssues, Collection<DefaultIssue> newIssues) {
    IssueTrackingResult result = new IssueTrackingResult();

    String source = index.getSource(resource);
    setChecksumOnNewIssues(newIssues, source);

    // Map new issues with old ones
    mapIssues(newIssues, dbIssues, source, resource, result);
    return result;
  }

  private void setChecksumOnNewIssues(Collection<DefaultIssue> issues, String source) {
    List<String> checksums = SourceChecksum.lineChecksumsOfFile(source);
    for (DefaultIssue issue : issues) {
      issue.setChecksum(SourceChecksum.getChecksumForLine(checksums, issue.line()));
    }
  }

  @VisibleForTesting
  void mapIssues(Collection<DefaultIssue> newIssues, @Nullable Collection<IssueDto> lastIssues, @Nullable String source, @Nullable Resource resource, IssueTrackingResult result) {
    boolean hasLastScan = false;

    if (lastIssues != null) {
      hasLastScan = true;
      mapLastIssues(newIssues, lastIssues, result);
    }

    // If each new issue matches an old one we can stop the matching mechanism
    if (result.matched().size() != newIssues.size()) {
      if (source != null && resource != null && hasLastScan) {
        String referenceSource = lastSnapshots.getSource(resource);
        if (referenceSource != null) {
          mapNewissues(referenceSource, newIssues, source, result);
        }
      }
      mapIssuesOnSameRule(newIssues, result);
    }
  }

  private void mapLastIssues(Collection<DefaultIssue> newIssues, Collection<IssueDto> lastIssues, IssueTrackingResult result) {
    for (IssueDto lastIssue : lastIssues) {
      result.addUnmatched(lastIssue);
    }

    // Match the key of the issue. (For manual issues)
    for (DefaultIssue newIssue : newIssues) {
      mapIssue(newIssue, findLastIssueWithSameKey(newIssue, result.unmatchedForRule(newIssue.ruleKey())), result);
    }

    // Try first to match issues on same rule with same line and with same checksum (but not necessarily with same message)
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue, result)) {
        mapIssue(
          newIssue,
          findLastIssueWithSameLineAndChecksum(newIssue, result.unmatchedForRule(newIssue.ruleKey())),
          result);
      }
    }
  }

  private void mapNewissues(String referenceSource, Collection<DefaultIssue> newIssues, String source, IssueTrackingResult result) {
    HashedSequence<StringText> hashedReference = HashedSequence.wrap(new StringText(referenceSource), StringTextComparator.IGNORE_WHITESPACE);
    HashedSequence<StringText> hashedSource = HashedSequence.wrap(new StringText(source), StringTextComparator.IGNORE_WHITESPACE);
    HashedSequenceComparator<StringText> hashedComparator = new HashedSequenceComparator<StringText>(StringTextComparator.IGNORE_WHITESPACE);

    ViolationTrackingBlocksRecognizer rec = new ViolationTrackingBlocksRecognizer(hashedReference, hashedSource, hashedComparator);

    Multimap<Integer, DefaultIssue> newIssuesByLines = newIssuesByLines(newIssues, rec, result);
    Multimap<Integer, IssueDto> lastIssuesByLines = lastIssuesByLines(result.unmatched(), rec);

    RollingHashSequence<HashedSequence<StringText>> a = RollingHashSequence.wrap(hashedReference, hashedComparator, 5);
    RollingHashSequence<HashedSequence<StringText>> b = RollingHashSequence.wrap(hashedSource, hashedComparator, 5);
    RollingHashSequenceComparator<HashedSequence<StringText>> cmp = new RollingHashSequenceComparator<HashedSequence<StringText>>(hashedComparator);

    Map<Integer, HashOccurrence> map = Maps.newHashMap();

    for (Integer line : lastIssuesByLines.keySet()) {
      int hash = cmp.hash(a, line - 1);
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
      int hash = cmp.hash(b, line - 1);
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
          int weight = rec.computeLengthOfMaximalBlock(oldLine - 1, newLine - 1);
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
          findLastIssueWithSameChecksumAndMessage(newIssue, result.unmatchedForRule(newIssue.ruleKey())),
          result);
      }
    }

    // Try then to match issues on same rule with same line and with same message
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue, result)) {
        mapIssue(
          newIssue,
          findLastIssueWithSameLineAndMessage(newIssue, result.unmatchedForRule(newIssue.ruleKey())),
          result);
      }
    }

    // Last check: match issue if same rule and same checksum but different line and different message
    // See SONAR-2812
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue, result)) {
        mapIssue(
          newIssue,
          findLastIssueWithSameChecksum(newIssue, result.unmatchedForRule(newIssue.ruleKey())),
          result);
      }
    }
  }

  private void map(Collection<DefaultIssue> newIssues, Collection<IssueDto> lastIssues, IssueTrackingResult result) {
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue, result)) {
        for (IssueDto pastIssue : lastIssues) {
          if (isNotAlreadyMapped(pastIssue, result) && Objects.equal(newIssue.ruleKey(), RuleKey.of(pastIssue.getRuleRepo(), pastIssue.getRule()))) {
            mapIssue(newIssue, pastIssue, result);
            break;
          }
        }
      }
    }
  }

  private Multimap<Integer, DefaultIssue> newIssuesByLines(Collection<DefaultIssue> newIssues, ViolationTrackingBlocksRecognizer rec, IssueTrackingResult result) {
    Multimap<Integer, DefaultIssue> newIssuesByLines = LinkedHashMultimap.create();
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue, result) && rec.isValidLineInSource(newIssue.line())) {
        newIssuesByLines.put(newIssue.line(), newIssue);
      }
    }
    return newIssuesByLines;
  }

  private Multimap<Integer, IssueDto> lastIssuesByLines(Collection<IssueDto> lastIssues, ViolationTrackingBlocksRecognizer rec) {
    Multimap<Integer, IssueDto> lastIssuesByLines = LinkedHashMultimap.create();
    for (IssueDto pastIssue : lastIssues) {
      if (rec.isValidLineInReference(pastIssue.getLine())) {
        lastIssuesByLines.put(pastIssue.getLine(), pastIssue);
      }
    }
    return lastIssuesByLines;
  }

  private IssueDto findLastIssueWithSameChecksum(DefaultIssue newIssue, Collection<IssueDto> lastIssues) {
    for (IssueDto pastIssue : lastIssues) {
      if (isSameChecksum(newIssue, pastIssue)) {
        return pastIssue;
      }
    }
    return null;
  }

  private IssueDto findLastIssueWithSameLineAndMessage(DefaultIssue newIssue, Collection<IssueDto> lastIssues) {
    for (IssueDto pastIssue : lastIssues) {
      if (isSameLine(newIssue, pastIssue) && isSameMessage(newIssue, pastIssue)) {
        return pastIssue;
      }
    }
    return null;
  }

  private IssueDto findLastIssueWithSameChecksumAndMessage(DefaultIssue newIssue, Collection<IssueDto> lastIssues) {
    for (IssueDto pastIssue : lastIssues) {
      if (isSameChecksum(newIssue, pastIssue) && isSameMessage(newIssue, pastIssue)) {
        return pastIssue;
      }
    }
    return null;
  }

  private IssueDto findLastIssueWithSameLineAndChecksum(DefaultIssue newIssue, Collection<IssueDto> lastIssues) {
    for (IssueDto pastIssue : lastIssues) {
      if (isSameLine(newIssue, pastIssue) && isSameChecksum(newIssue, pastIssue)) {
        return pastIssue;
      }
    }
    return null;
  }

  private IssueDto findLastIssueWithSameKey(DefaultIssue newIssue, Collection<IssueDto> lastIssues) {
    for (IssueDto pastIssue : lastIssues) {
      if (isSameKey(newIssue, pastIssue)) {
        return pastIssue;
      }
    }
    return null;
  }

  private boolean isNotAlreadyMapped(IssueDto pastIssue, IssueTrackingResult result) {
    return result.unmatched().contains(pastIssue);
  }

  private boolean isNotAlreadyMapped(DefaultIssue newIssue, IssueTrackingResult result) {
    return !result.isMatched(newIssue);
  }

  private boolean isSameChecksum(DefaultIssue newIssue, IssueDto pastIssue) {
    return Objects.equal(pastIssue.getChecksum(), newIssue.checksum());
  }

  private boolean isSameLine(DefaultIssue newIssue, IssueDto pastIssue) {
    return Objects.equal(pastIssue.getLine(), newIssue.line());
  }

  private boolean isSameMessage(DefaultIssue newIssue, IssueDto pastIssue) {
    return Objects.equal(newIssue.message(), pastIssue.getMessage());
  }

  private boolean isSameKey(DefaultIssue newIssue, IssueDto pastIssue) {
    return Objects.equal(newIssue.key(), pastIssue.getKee());
  }

  private void mapIssue(DefaultIssue issue, @Nullable IssueDto ref, IssueTrackingResult result) {
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
