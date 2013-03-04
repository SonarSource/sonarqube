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
package org.sonar.plugins.core.timemachine;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.violations.ViolationQuery;
import org.sonar.batch.scan.LastSnapshots;
import org.sonar.plugins.core.timemachine.tracking.HashedSequence;
import org.sonar.plugins.core.timemachine.tracking.HashedSequenceComparator;
import org.sonar.plugins.core.timemachine.tracking.RollingHashSequence;
import org.sonar.plugins.core.timemachine.tracking.RollingHashSequenceComparator;
import org.sonar.plugins.core.timemachine.tracking.StringText;
import org.sonar.plugins.core.timemachine.tracking.StringTextComparator;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@DependsUpon({DecoratorBarriers.END_OF_VIOLATIONS_GENERATION, DecoratorBarriers.START_VIOLATION_TRACKING})
@DependedUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
public class ViolationTrackingDecorator implements Decorator {
  private LastSnapshots lastSnapshots;
  private Map<Violation, RuleFailureModel> referenceViolationsMap = Maps.newIdentityHashMap();
  private SonarIndex index;
  private Project project;

  /**
   * Live collection of unmapped past violations.
   */
  private Set<RuleFailureModel> unmappedLastViolations = Sets.newHashSet();

  public ViolationTrackingDecorator(Project project, LastSnapshots lastSnapshots, SonarIndex index) {
    this.lastSnapshots = lastSnapshots;
    this.index = index;
    this.project = project;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    referenceViolationsMap.clear();

    ViolationQuery violationQuery = ViolationQuery.create().forResource(resource).setSwitchMode(ViolationQuery.SwitchMode.BOTH);
    if (context.getViolations(violationQuery).isEmpty()) {
      return;
    }

    String source = index.getSource(resource);

    // Load new violations
    List<Violation> newViolations = prepareNewViolations(context, source);

    // Load the violations of the last available analysis
    List<RuleFailureModel> referenceViolations = lastSnapshots.getViolations(resource);

    // Map new violations with old ones
    mapViolations(newViolations, referenceViolations, source, resource);
  }

  private List<Violation> prepareNewViolations(DecoratorContext context, String source) {
    List<Violation> result = Lists.newArrayList();
    List<String> checksums = SourceChecksum.lineChecksumsOfFile(source);
    for (Violation violation : context.getViolations()) {
      violation.setChecksum(SourceChecksum.getChecksumForLine(checksums, violation.getLineId()));
      result.add(violation);
    }
    return result;
  }

  public RuleFailureModel getReferenceViolation(Violation violation) {
    return referenceViolationsMap.get(violation);
  }

  @VisibleForTesting
  Map<Violation, RuleFailureModel> mapViolations(List<Violation> newViolations, @Nullable List<RuleFailureModel> lastViolations) {
    return mapViolations(newViolations, lastViolations, null, null);
  }

  @VisibleForTesting
  Map<Violation, RuleFailureModel> mapViolations(List<Violation> newViolations, @Nullable List<RuleFailureModel> lastViolations,
                                                 @Nullable String source, @Nullable Resource resource) {
    boolean hasLastScan = false;
    Multimap<Integer, RuleFailureModel> lastViolationsByRule = LinkedHashMultimap.create();
    
    if (lastViolations != null) {
      hasLastScan = true;
      unmappedLastViolations.addAll(lastViolations);

      for (RuleFailureModel lastViolation : lastViolations) {
        lastViolationsByRule.put(lastViolation.getRuleId(), lastViolation);
      }

      // Match the permanent id of the violation. This id is for example set explicitly when injecting manual violations
      for (Violation newViolation : newViolations) {
        mapViolation(newViolation,
          findLastViolationWithSamePermanentId(newViolation, lastViolationsByRule.get(newViolation.getRule().getId())),
          lastViolationsByRule, referenceViolationsMap);
      }

      // Try first to match violations on same rule with same line and with same checksum (but not necessarily with same message)
      for (Violation newViolation : newViolations) {
        if (isNotAlreadyMapped(newViolation)) {
          mapViolation(newViolation,
            findLastViolationWithSameLineAndChecksum(newViolation, lastViolationsByRule.get(newViolation.getRule().getId())),
            lastViolationsByRule, referenceViolationsMap);
        }
      }
    }

    // If each new violation matches an old one we can stop the matching mechanism
    if (referenceViolationsMap.size() != newViolations.size()) {
      if (source != null && resource != null && hasLastScan) {
        String referenceSource = lastSnapshots.getSource(resource);
        if (referenceSource != null) {
          HashedSequence<StringText> hashedReference = HashedSequence.wrap(new StringText(referenceSource), StringTextComparator.IGNORE_WHITESPACE);
          HashedSequence<StringText> hashedSource = HashedSequence.wrap(new StringText(source), StringTextComparator.IGNORE_WHITESPACE);
          HashedSequenceComparator<StringText> hashedComparator = new HashedSequenceComparator<StringText>(StringTextComparator.IGNORE_WHITESPACE);

          ViolationTrackingBlocksRecognizer rec = new ViolationTrackingBlocksRecognizer(hashedReference, hashedSource, hashedComparator);

          Multimap<Integer, Violation> newViolationsByLines = newViolationsByLines(newViolations, rec);
          Multimap<Integer, RuleFailureModel> lastViolationsByLines = lastViolationsByLines(unmappedLastViolations, rec);

          RollingHashSequence<HashedSequence<StringText>> a = RollingHashSequence.wrap(hashedReference, hashedComparator, 5);
          RollingHashSequence<HashedSequence<StringText>> b = RollingHashSequence.wrap(hashedSource, hashedComparator, 5);
          RollingHashSequenceComparator<HashedSequence<StringText>> cmp = new RollingHashSequenceComparator<HashedSequence<StringText>>(hashedComparator);

          Map<Integer, HashOccurrence> map = Maps.newHashMap();

          for (Integer line : lastViolationsByLines.keySet()) {
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

          for (Integer line : newViolationsByLines.keySet()) {
            int hash = cmp.hash(b, line - 1);
            HashOccurrence hashOccurrence = map.get(hash);
            if (hashOccurrence != null) {
              hashOccurrence.lineB = line;
              hashOccurrence.countB++;
            }
          }

          for (HashOccurrence hashOccurrence : map.values()) {
            if (hashOccurrence.countA == 1 && hashOccurrence.countB == 1) {
              // Guaranteed that lineA has been moved to lineB, so we can map all violations on lineA to all violations on lineB
              map(newViolationsByLines.get(hashOccurrence.lineB), lastViolationsByLines.get(hashOccurrence.lineA), lastViolationsByRule);
              lastViolationsByLines.removeAll(hashOccurrence.lineA);
              newViolationsByLines.removeAll(hashOccurrence.lineB);
            }
          }

          // Check if remaining number of lines exceeds threshold
          if (lastViolationsByLines.keySet().size() * newViolationsByLines.keySet().size() < 250000) {
            List<LinePair> possibleLinePairs = Lists.newArrayList();
            for (Integer oldLine : lastViolationsByLines.keySet()) {
              for (Integer newLine : newViolationsByLines.keySet()) {
                int weight = rec.computeLengthOfMaximalBlock(oldLine - 1, newLine - 1);
                possibleLinePairs.add(new LinePair(oldLine, newLine, weight));
              }
            }
            Collections.sort(possibleLinePairs, LINE_PAIR_COMPARATOR);
            for (LinePair linePair : possibleLinePairs) {
              // High probability that lineA has been moved to lineB, so we can map all violations on lineA to all violations on lineB
              map(newViolationsByLines.get(linePair.lineB), lastViolationsByLines.get(linePair.lineA), lastViolationsByRule);
            }
          }
        }
      }

      // Try then to match violations on same rule with same message and with same checksum
      for (Violation newViolation : newViolations) {
        if (isNotAlreadyMapped(newViolation)) {
          mapViolation(newViolation,
            findLastViolationWithSameChecksumAndMessage(newViolation, lastViolationsByRule.get(newViolation.getRule().getId())),
            lastViolationsByRule, referenceViolationsMap);
        }
      }

      // Try then to match violations on same rule with same line and with same message
      for (Violation newViolation : newViolations) {
        if (isNotAlreadyMapped(newViolation)) {
          mapViolation(newViolation,
            findLastViolationWithSameLineAndMessage(newViolation, lastViolationsByRule.get(newViolation.getRule().getId())),
            lastViolationsByRule, referenceViolationsMap);
        }
      }

      // Last check: match violation if same rule and same checksum but different line and different message
      // See SONAR-2812
      for (Violation newViolation : newViolations) {
        if (isNotAlreadyMapped(newViolation)) {
          mapViolation(newViolation,
            findLastViolationWithSameChecksum(newViolation, lastViolationsByRule.get(newViolation.getRule().getId())),
            lastViolationsByRule, referenceViolationsMap);
        }
      }
    }

    unmappedLastViolations.clear();
    return referenceViolationsMap;
  }

  private void map(Collection<Violation> newViolations, Collection<RuleFailureModel> lastViolations, Multimap<Integer, RuleFailureModel> lastViolationsByRule) {
    for (Violation newViolation : newViolations) {
      if (isNotAlreadyMapped(newViolation)) {
        for (RuleFailureModel pastViolation : lastViolations) {
          if (isNotAlreadyMapped(pastViolation) && Objects.equal(newViolation.getRule().getId(), pastViolation.getRuleId())) {
            mapViolation(newViolation, pastViolation, lastViolationsByRule, referenceViolationsMap);
            break;
          }
        }
      }
    }
  }

  private Multimap<Integer, Violation> newViolationsByLines(Collection<Violation> newViolations, ViolationTrackingBlocksRecognizer rec) {
    Multimap<Integer, Violation> newViolationsByLines = LinkedHashMultimap.create();
    for (Violation newViolation : newViolations) {
      if (isNotAlreadyMapped(newViolation)) {
        if (rec.isValidLineInSource(newViolation.getLineId())) {
          newViolationsByLines.put(newViolation.getLineId(), newViolation);
        }
      }
    }
    return newViolationsByLines;
  }

  private Multimap<Integer, RuleFailureModel> lastViolationsByLines(Collection<RuleFailureModel> lastViolations, ViolationTrackingBlocksRecognizer rec) {
    Multimap<Integer, RuleFailureModel> lastViolationsByLines = LinkedHashMultimap.create();
    for (RuleFailureModel pastViolation : lastViolations) {
      if (rec.isValidLineInReference(pastViolation.getLine())) {
        lastViolationsByLines.put(pastViolation.getLine(), pastViolation);
      }
    }
    return lastViolationsByLines;
  }

  private static final Comparator<LinePair> LINE_PAIR_COMPARATOR = new Comparator<LinePair>() {
    public int compare(LinePair o1, LinePair o2) {
      return o2.weight - o1.weight;
    }
  };

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

  private boolean isNotAlreadyMapped(RuleFailureModel pastViolation) {
    return unmappedLastViolations.contains(pastViolation);
  }

  private boolean isNotAlreadyMapped(Violation newViolation) {
    return !referenceViolationsMap.containsKey(newViolation);
  }

  private RuleFailureModel findLastViolationWithSameChecksum(Violation newViolation, Collection<RuleFailureModel> lastViolations) {
    for (RuleFailureModel pastViolation : lastViolations) {
      if (isSameChecksum(newViolation, pastViolation)) {
        return pastViolation;
      }
    }
    return null;
  }

  private RuleFailureModel findLastViolationWithSameLineAndMessage(Violation newViolation, Collection<RuleFailureModel> lastViolations) {
    for (RuleFailureModel pastViolation : lastViolations) {
      if (isSameLine(newViolation, pastViolation) && isSameMessage(newViolation, pastViolation)) {
        return pastViolation;
      }
    }
    return null;
  }

  private RuleFailureModel findLastViolationWithSameChecksumAndMessage(Violation newViolation, Collection<RuleFailureModel> lastViolations) {
    for (RuleFailureModel pastViolation : lastViolations) {
      if (isSameChecksum(newViolation, pastViolation) && isSameMessage(newViolation, pastViolation)) {
        return pastViolation;
      }
    }
    return null;
  }

  private RuleFailureModel findLastViolationWithSameLineAndChecksum(Violation newViolation, Collection<RuleFailureModel> lastViolations) {
    for (RuleFailureModel pastViolation : lastViolations) {
      if (isSameLine(newViolation, pastViolation) && isSameChecksum(newViolation, pastViolation)) {
        return pastViolation;
      }
    }
    return null;
  }

  private RuleFailureModel findLastViolationWithSamePermanentId(Violation newViolation, Collection<RuleFailureModel> lastViolations) {
    for (RuleFailureModel pastViolation : lastViolations) {
      if (isSamePermanentId(newViolation, pastViolation)) {
        return pastViolation;
      }
    }
    return null;
  }

  private boolean isSameChecksum(Violation newViolation, RuleFailureModel pastViolation) {
    return StringUtils.equals(pastViolation.getChecksum(), newViolation.getChecksum());
  }

  private boolean isSameLine(Violation newViolation, RuleFailureModel pastViolation) {
    return ObjectUtils.equals(pastViolation.getLine(), newViolation.getLineId());
  }

  private boolean isSameMessage(Violation newViolation, RuleFailureModel pastViolation) {
    return StringUtils.equals(RuleFailureModel.abbreviateMessage(newViolation.getMessage()), pastViolation.getMessage());
  }

  private boolean isSamePermanentId(Violation newViolation, RuleFailureModel pastViolation) {
    return newViolation.getPermanentId() != null && newViolation.getPermanentId().equals(pastViolation.getPermanentId());
  }

  private void mapViolation(Violation newViolation, RuleFailureModel pastViolation,
                            Multimap<Integer, RuleFailureModel> lastViolationsByRule, Map<Violation, RuleFailureModel> violationMap) {
    if (pastViolation != null) {
      newViolation.setCreatedAt(pastViolation.getCreatedAt());
      newViolation.setPermanentId(pastViolation.getPermanentId());
      newViolation.setSwitchedOff(pastViolation.isSwitchedOff());
      newViolation.setPersonId(pastViolation.getPersonId());
      newViolation.setNew(false);
      lastViolationsByRule.remove(newViolation.getRule().getId(), pastViolation);
      violationMap.put(newViolation, pastViolation);
      unmappedLastViolations.remove(pastViolation);
    } else {
      newViolation.setNew(true);
      newViolation.setCreatedAt(project.getAnalysisDate());
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
