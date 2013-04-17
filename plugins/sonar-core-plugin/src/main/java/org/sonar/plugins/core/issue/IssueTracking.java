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

package org.sonar.plugins.core.issue;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.*;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.scan.LastSnapshots;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueDto;
import org.sonar.plugins.core.timemachine.SourceChecksum;
import org.sonar.plugins.core.timemachine.ViolationTrackingBlocksRecognizer;
import org.sonar.plugins.core.timemachine.tracking.*;

import javax.annotation.Nullable;

import java.util.*;

public class IssueTracking implements BatchExtension {

  private static final Comparator<LinePair> LINE_PAIR_COMPARATOR = new Comparator<LinePair>() {
    public int compare(LinePair o1, LinePair o2) {
      return o2.weight - o1.weight;
    }
  };
  private final Project project;
  private final RuleFinder ruleFinder;
  private final LastSnapshots lastSnapshots;
  private final SonarIndex index;
  /**
   * Live collection of unmapped past issues.
   */
  private Set<IssueDto> unmappedLastIssues = Sets.newHashSet();
  /**
   * Map of old issues by new issues
   */
  private Map<DefaultIssue, IssueDto> referenceIssuesMap = Maps.newIdentityHashMap();

  public IssueTracking(Project project, RuleFinder ruleFinder, LastSnapshots lastSnapshots, SonarIndex index) {
    this.project = project;
    this.ruleFinder = ruleFinder;
    this.lastSnapshots = lastSnapshots;
    this.index = index;
  }

  public void track(Resource resource, Collection<IssueDto> referenceIssues, Collection<DefaultIssue> newIssues) {
    referenceIssuesMap.clear();

    String source = index.getSource(resource);
    setChecksumOnNewIssues(newIssues, source);

    // Map new issues with old ones
    mapIssues(newIssues, referenceIssues, source, resource);
  }

  private void setChecksumOnNewIssues(Collection<DefaultIssue> issues, String source) {
    List<String> checksums = SourceChecksum.lineChecksumsOfFile(source);
    for (DefaultIssue issue : issues) {
      issue.setChecksum(SourceChecksum.getChecksumForLine(checksums, issue.line()));
    }
  }

  @VisibleForTesting
  Map<DefaultIssue, IssueDto> mapIssues(Collection<DefaultIssue> newIssues, @Nullable List<IssueDto> lastIssues) {
    return mapIssues(newIssues, lastIssues, null, null);
  }

  @VisibleForTesting
  Map<DefaultIssue, IssueDto> mapIssues(Collection<DefaultIssue> newIssues, @Nullable Collection<IssueDto> lastIssues, @Nullable String source, @Nullable Resource resource) {
    boolean hasLastScan = false;
    Multimap<Integer, IssueDto> lastIssuesByRule = LinkedHashMultimap.create();

    if (lastIssues != null) {
      hasLastScan = true;
      mapLastIssues(newIssues, lastIssues, lastIssuesByRule);
    }

    // If each new issue matches an old one we can stop the matching mechanism
    if (referenceIssuesMap.size() != newIssues.size()) {
      if (source != null && resource != null && hasLastScan) {
        String referenceSource = lastSnapshots.getSource(resource);
        if (referenceSource != null) {
          mapNewissues(referenceSource, newIssues, lastIssuesByRule, source);
        }
      }
      mapIssuesOnSameRule(newIssues, lastIssuesByRule);
    }

    unmappedLastIssues.clear();
    return referenceIssuesMap;
  }

  private void mapLastIssues(Collection<DefaultIssue> newIssues, Collection<IssueDto> lastIssues, Multimap<Integer, IssueDto> lastIssuesByRule) {
    unmappedLastIssues.addAll(lastIssues);

    for (IssueDto lastIssue : lastIssues) {
      lastIssuesByRule.put(getRule(lastIssue), lastIssue);
    }

    // Match the key of the issue. (For manual issues)
    for (DefaultIssue newIssue : newIssues) {
      mapIssue(newIssue,
        findLastIssueWithSameKey(newIssue, lastIssuesByRule.get(getRule(newIssue))),
        lastIssuesByRule, referenceIssuesMap);
    }

    // Try first to match issues on same rule with same line and with same checksum (but not necessarily with same message)
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue)) {
        mapIssue(newIssue,
          findLastIssueWithSameLineAndChecksum(newIssue, lastIssuesByRule.get(getRule(newIssue))),
          lastIssuesByRule, referenceIssuesMap);
      }
    }
  }

  private void mapNewissues(String referenceSource, Collection<DefaultIssue> newIssues, Multimap<Integer, IssueDto> lastIssuesByRule, String source) {
    HashedSequence<StringText> hashedReference = HashedSequence.wrap(new StringText(referenceSource), StringTextComparator.IGNORE_WHITESPACE);
    HashedSequence<StringText> hashedSource = HashedSequence.wrap(new StringText(source), StringTextComparator.IGNORE_WHITESPACE);
    HashedSequenceComparator<StringText> hashedComparator = new HashedSequenceComparator<StringText>(StringTextComparator.IGNORE_WHITESPACE);

    ViolationTrackingBlocksRecognizer rec = new ViolationTrackingBlocksRecognizer(hashedReference, hashedSource, hashedComparator);

    Multimap<Integer, DefaultIssue> newIssuesByLines = newIssuesByLines(newIssues, rec);
    Multimap<Integer, IssueDto> lastIssuesByLines = lastIssuesByLines(unmappedLastIssues, rec);

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
        map(newIssuesByLines.get(hashOccurrence.lineB), lastIssuesByLines.get(hashOccurrence.lineA), lastIssuesByRule);
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
        map(newIssuesByLines.get(linePair.lineB), lastIssuesByLines.get(linePair.lineA), lastIssuesByRule);
      }
    }
  }

  private void mapIssuesOnSameRule(Collection<DefaultIssue> newIssues, Multimap<Integer, IssueDto> lastIssuesByRule) {
    // Try then to match issues on same rule with same message and with same checksum
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue)) {
        mapIssue(newIssue,
          findLastIssueWithSameChecksumAndMessage(newIssue, lastIssuesByRule.get(getRule(newIssue))),
          lastIssuesByRule, referenceIssuesMap);
      }
    }

    // Try then to match issues on same rule with same line and with same message
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue)) {
        mapIssue(newIssue,
          findLastIssueWithSameLineAndMessage(newIssue, lastIssuesByRule.get(getRule(newIssue))),
          lastIssuesByRule, referenceIssuesMap);
      }
    }

    // Last check: match issue if same rule and same checksum but different line and different message
    // See SONAR-2812
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue)) {
        mapIssue(newIssue,
          findLastIssueWithSameChecksum(newIssue, lastIssuesByRule.get(getRule(newIssue))),
          lastIssuesByRule, referenceIssuesMap);
      }
    }
  }

  @VisibleForTesting
  IssueDto getReferenceIssue(DefaultIssue issue) {
    return referenceIssuesMap.get(issue);
  }

  private Integer getRule(DefaultIssue issue) {
    return ruleFinder.findByKey(issue.ruleKey()).getId();
  }

  private Integer getRule(IssueDto issue) {
    return ruleFinder.findById(issue.getRuleId()).getId();
  }

  private void map(Collection<DefaultIssue> newIssues, Collection<IssueDto> lastIssues, Multimap<Integer, IssueDto> lastIssuesByRule) {
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue)) {
        for (IssueDto pastIssue : lastIssues) {
          if (isNotAlreadyMapped(pastIssue) && Objects.equal(getRule(newIssue), getRule(pastIssue))) {
            mapIssue(newIssue, pastIssue, lastIssuesByRule, referenceIssuesMap);
            break;
          }
        }
      }
    }
  }

  private Multimap<Integer, DefaultIssue> newIssuesByLines(Collection<DefaultIssue> newIssues, ViolationTrackingBlocksRecognizer rec) {
    Multimap<Integer, DefaultIssue> newIssuesByLines = LinkedHashMultimap.create();
    for (DefaultIssue newIssue : newIssues) {
      if (isNotAlreadyMapped(newIssue) && rec.isValidLineInSource(newIssue.line())) {
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

  private boolean isNotAlreadyMapped(IssueDto pastIssue) {
    return !unmappedLastIssues.contains(pastIssue);
  }

  private boolean isNotAlreadyMapped(DefaultIssue newIssue) {
    return !referenceIssuesMap.containsKey(newIssue);
  }

  private boolean isSameChecksum(DefaultIssue newIssue, IssueDto pastIssue) {
    return Objects.equal(pastIssue.getChecksum(), newIssue.getChecksum());
  }

  private boolean isSameLine(DefaultIssue newIssue, IssueDto pastIssue) {
    return Objects.equal(pastIssue.getLine(), newIssue.line());
  }

  private boolean isSameMessage(DefaultIssue newIssue, IssueDto pastIssue) {
    return Objects.equal(IssueDto.abbreviateMessage(newIssue.message()), pastIssue.getMessage());
  }

  private boolean isSameKey(DefaultIssue newIssue, IssueDto pastIssue) {
    return Objects.equal(newIssue.key(), pastIssue.getUuid());
  }

  private void mapIssue(DefaultIssue newIssue, IssueDto pastIssue, Multimap<Integer, IssueDto> lastIssuesByRule, Map<DefaultIssue, IssueDto> issueMap) {
    if (pastIssue != null) {
      newIssue.setKey(pastIssue.getUuid());
      if (pastIssue.isManualSeverity()) {
        newIssue.setSeverity(pastIssue.getSeverity());
      }

      newIssue.setCreatedAt(pastIssue.getCreatedAt());
      newIssue.setUpdatedAt(project.getAnalysisDate());
      newIssue.setNew(false);

      // TODO
//      newIssue.setPersonId(pastIssue.getPersonId());

      lastIssuesByRule.remove(getRule(newIssue), pastIssue);
      issueMap.put(newIssue, pastIssue);
      unmappedLastIssues.remove(pastIssue);
    } else {
      newIssue.setNew(true);
      newIssue.setCreatedAt(project.getAnalysisDate());
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  static class LinePair {
    int lineA;
    int lineB;
    int weight;

    public LinePair(int lineA, int lineB, int weight) {
      this.lineA = lineA;
      this.lineB = lineB;
      this.weight = weight;
    }
  }

  static class HashOccurrence {
    int lineA;
    int lineB;
    int countA;
    int countB;
  }


}
