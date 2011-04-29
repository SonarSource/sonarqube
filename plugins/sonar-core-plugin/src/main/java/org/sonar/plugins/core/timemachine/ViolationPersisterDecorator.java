/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.*;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.SnapshotSource;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.batch.components.PastViolationsLoader;
import org.sonar.batch.index.ViolationPersister;

import java.util.*;

@DependsUpon({DecoratorBarriers.END_OF_VIOLATIONS_GENERATION, DecoratorBarriers.START_VIOLATION_TRACKING})
@DependedUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
public class ViolationPersisterDecorator implements Decorator {

  /**
   * Those chars would be ignored during generation of checksums.
   */
  private static final String SPACE_CHARS = "\t\n\r ";

  private PastViolationsLoader pastViolationsLoader;
  private ViolationPersister violationPersister;

  List<String> checksums;

  public ViolationPersisterDecorator(PastViolationsLoader pastViolationsLoader, ViolationPersister violationPersister) {
    this.pastViolationsLoader = pastViolationsLoader;
    this.violationPersister = violationPersister;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (context.getViolations().isEmpty()) {
      return;
    }
    // Load new violations
    List<Violation> newViolations = context.getViolations();

    // Load past violations
    List<RuleFailureModel> pastViolations = pastViolationsLoader.getPastViolations(resource);

    // Load current source code and calculate checksums for each line
    checksums = getChecksums(pastViolationsLoader.getSource(resource));

    // Map new violations with old ones
    Map<Violation, RuleFailureModel> violationMap = mapViolations(newViolations, pastViolations);

    for (Violation newViolation : newViolations) {
      String checksum = getChecksumForLine(checksums, newViolation.getLineId());
      violationPersister.saveViolation(context.getProject(), newViolation, violationMap.get(newViolation), checksum);
    }
    violationPersister.commit();
    // Clear cache
    checksums.clear();
  }

  Map<Violation, RuleFailureModel> mapViolations(List<Violation> newViolations, List<RuleFailureModel> pastViolations) {
    Map<Violation, RuleFailureModel> violationMap = new IdentityHashMap<Violation, RuleFailureModel>();

    Multimap<Integer, RuleFailureModel> pastViolationsByRule = LinkedHashMultimap.create();
    for (RuleFailureModel pastViolation : pastViolations) {
      pastViolationsByRule.put(pastViolation.getRuleId(), pastViolation);
    }

    // Try first an exact matching : same rule, same message, same line and same checkum
    for (Violation newViolation : newViolations) {
      mapViolation(newViolation,
          findPastViolationWithSameLineAndChecksumAndMessage(newViolation, pastViolationsByRule.get(newViolation.getRule().getId())),
          pastViolationsByRule, violationMap);
    }

    // If each new violation matches an old one we can stop the matching mechanism
    if (violationMap.size() != newViolations.size()) {

      // Try then to match violations on same rule with same message and with same checkum
      for (Violation newViolation : newViolations) {
        if (isNotAlreadyMapped(newViolation, violationMap)) {
          mapViolation(newViolation,
              findPastViolationWithSameChecksumAndMessage(newViolation, pastViolationsByRule.get(newViolation.getRule().getId())),
              pastViolationsByRule, violationMap);
        }
      }

      // Try then to match violations on same rule with same line and with same message
      for (Violation newViolation : newViolations) {
        if (isNotAlreadyMapped(newViolation, violationMap)) {
          mapViolation(newViolation,
              findPastViolationWithSameLineAndMessage(newViolation, pastViolationsByRule.get(newViolation.getRule().getId())),
              pastViolationsByRule, violationMap);
        }
      }
    }

    return violationMap;
  }

  private final boolean isNotAlreadyMapped(Violation newViolation, Map<Violation, RuleFailureModel> violationMap) {
    return violationMap.get(newViolation) == null;
  }

  private RuleFailureModel findPastViolationWithSameLineAndMessage(Violation newViolation, Collection<RuleFailureModel> pastViolations) {
    for (RuleFailureModel pastViolation : pastViolations) {
      if (isSameLine(newViolation, pastViolation) && isSameMessage(newViolation, pastViolation)) {
        return pastViolation;
      }
    }
    return null;
  }

  private RuleFailureModel findPastViolationWithSameChecksumAndMessage(Violation newViolation, Collection<RuleFailureModel> pastViolations) {
    for (RuleFailureModel pastViolation : pastViolations) {
      if (isSameChecksum(newViolation, pastViolation) && isSameMessage(newViolation, pastViolation)) {
        return pastViolation;
      }
    }
    return null;
  }

  private RuleFailureModel findPastViolationWithSameLineAndChecksumAndMessage(Violation newViolation,
      Collection<RuleFailureModel> pastViolations) {
    for (RuleFailureModel pastViolation : pastViolations) {
      if (isSameLine(newViolation, pastViolation) && isSameChecksum(newViolation, pastViolation)
          && isSameMessage(newViolation, pastViolation)) {
        return pastViolation;
      }
    }
    return null;
  }

  private boolean isSameChecksum(Violation newViolation, RuleFailureModel pastViolation) {
    return pastViolation.getChecksum()!=null && StringUtils.equals(pastViolation.getChecksum(), getChecksumForLine(checksums, newViolation.getLineId()));
  }

  private boolean isSameLine(Violation newViolation, RuleFailureModel pastViolation) {
    return pastViolation.getLine() == newViolation.getLineId(); //When lines are null, we also return true
  }

  private boolean isSameMessage(Violation newViolation, RuleFailureModel pastViolation) {
    return StringUtils.equals(RuleFailureModel.abbreviateMessage(newViolation.getMessage()), pastViolation.getMessage());
  }

  private void mapViolation(Violation newViolation, RuleFailureModel pastViolation,
      Multimap<Integer, RuleFailureModel> pastViolationsByRule, Map<Violation, RuleFailureModel> violationMap) {
    if (pastViolation != null) {
      pastViolationsByRule.remove(newViolation.getRule().getId(), pastViolation);
      violationMap.put(newViolation, pastViolation);
    }
  }

  /**
   * @return checksums, never null
   */
  private List<String> getChecksums(SnapshotSource source) {
    return source == null || source.getData() == null ? Collections.<String> emptyList() : getChecksums(source.getData());
  }

  /**
   * @param data can't be null
   */
  static List<String> getChecksums(String data) {
    String[] lines = data.split("\r?\n|\r", -1);
    List<String> result = Lists.newArrayList();
    for (String line : lines) {
      result.add(getChecksum(line));
    }
    return result;
  }

  static String getChecksum(String line) {
    String reducedLine = StringUtils.replaceChars(line, SPACE_CHARS, "");
    return DigestUtils.md5Hex(reducedLine);
  }

  /**
   * @return checksum or null if checksum not exists for line
   */
  private String getChecksumForLine(List<String> checksums, Integer line) {
    if (line == null || line < 1 || line > checksums.size()) {
      return null;
    }
    return checksums.get(line - 1);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
