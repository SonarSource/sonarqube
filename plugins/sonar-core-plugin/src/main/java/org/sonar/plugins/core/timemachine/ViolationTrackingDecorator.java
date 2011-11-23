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
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.*;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.violations.ViolationQuery;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@DependsUpon({DecoratorBarriers.END_OF_VIOLATIONS_GENERATION, DecoratorBarriers.START_VIOLATION_TRACKING})
@DependedUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
public class ViolationTrackingDecorator implements Decorator {
  private ReferenceAnalysis referenceAnalysis;
  private Map<Violation, RuleFailureModel> referenceViolationsMap = Maps.newIdentityHashMap();
  private SonarIndex index;
  private Project project;

  public ViolationTrackingDecorator(Project project, ReferenceAnalysis referenceAnalysis, SonarIndex index) {
    this.referenceAnalysis = referenceAnalysis;
    this.index = index;
    this.project = project;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    referenceViolationsMap.clear();

    ViolationQuery violationQuery = ViolationQuery.create().forResource(resource).setSwitchMode(ViolationQuery.SwitchMode.BOTH);
    if (!context.getViolations(violationQuery).isEmpty()) {
      // Load new violations
      List<Violation> newViolations = prepareNewViolations(context);

      // Load reference violations
      List<RuleFailureModel> referenceViolations = referenceAnalysis.getViolations(resource);

      // Map new violations with old ones
      mapViolations(newViolations, referenceViolations);
    }
  }

  private List<Violation> prepareNewViolations(DecoratorContext context) {
    List<Violation> result = Lists.newArrayList();
    List<String> checksums = SourceChecksum.lineChecksumsOfFile(index.getSource(context.getResource()));
    for (Violation violation : context.getViolations()) {
      violation.setChecksum(SourceChecksum.getChecksumForLine(checksums, violation.getLineId()));
      result.add(violation);
    }
    return result;
  }

  public RuleFailureModel getReferenceViolation(Violation violation) {
    return referenceViolationsMap.get(violation);
  }

  Map<Violation, RuleFailureModel> mapViolations(List<Violation> newViolations, List<RuleFailureModel> pastViolations) {
    Multimap<Integer, RuleFailureModel> pastViolationsByRule = LinkedHashMultimap.create();
    for (RuleFailureModel pastViolation : pastViolations) {
      pastViolationsByRule.put(pastViolation.getRuleId(), pastViolation);
    }

    // Try first to match violations on same rule with same line and with same checkum (but not necessarily with same message)
    for (Violation newViolation : newViolations) {
      mapViolation(newViolation,
          findPastViolationWithSameLineAndChecksum(newViolation, pastViolationsByRule.get(newViolation.getRule().getId())),
          pastViolationsByRule, referenceViolationsMap);
    }

    // If each new violation matches an old one we can stop the matching mechanism
    if (referenceViolationsMap.size() != newViolations.size()) {
      // Try then to match violations on same rule with same message and with same checkum
      for (Violation newViolation : newViolations) {
        if (isNotAlreadyMapped(newViolation, referenceViolationsMap)) {
          mapViolation(newViolation,
              findPastViolationWithSameChecksumAndMessage(newViolation, pastViolationsByRule.get(newViolation.getRule().getId())),
              pastViolationsByRule, referenceViolationsMap);
        }
      }

      // Try then to match violations on same rule with same line and with same message
      for (Violation newViolation : newViolations) {
        if (isNotAlreadyMapped(newViolation, referenceViolationsMap)) {
          mapViolation(newViolation,
              findPastViolationWithSameLineAndMessage(newViolation, pastViolationsByRule.get(newViolation.getRule().getId())),
              pastViolationsByRule, referenceViolationsMap);
        }
      }

      // Last check: match violation if same rule and same checksum but different line and different message
      // See https://jira.codehaus.org/browse/SONAR-2812
      for (Violation newViolation : newViolations) {
        if (isNotAlreadyMapped(newViolation, referenceViolationsMap)) {
          mapViolation(newViolation,
              findPastViolationWithSameChecksum(newViolation, pastViolationsByRule.get(newViolation.getRule().getId())),
              pastViolationsByRule, referenceViolationsMap);
        }
      }
    }
    return referenceViolationsMap;
  }

  private boolean isNotAlreadyMapped(Violation newViolation, Map<Violation, RuleFailureModel> violationMap) {
    return !violationMap.containsKey(newViolation);
  }

  private RuleFailureModel findPastViolationWithSameChecksum(Violation newViolation, Collection<RuleFailureModel> pastViolations) {
    for (RuleFailureModel pastViolation : pastViolations) {
      if (isSameChecksum(newViolation, pastViolation)) {
        return pastViolation;
      }
    }
    return null;
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

  private RuleFailureModel findPastViolationWithSameLineAndChecksum(Violation newViolation, Collection<RuleFailureModel> pastViolations) {
    for (RuleFailureModel pastViolation : pastViolations) {
      if (isSameLine(newViolation, pastViolation) && isSameChecksum(newViolation, pastViolation)) {
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

  private void mapViolation(Violation newViolation, RuleFailureModel pastViolation,
                            Multimap<Integer, RuleFailureModel> pastViolationsByRule, Map<Violation, RuleFailureModel> violationMap) {
    if (pastViolation != null) {
      newViolation.setCreatedAt(pastViolation.getCreatedAt());
      newViolation.setSwitchedOff(pastViolation.isSwitchedOff());
      newViolation.setNew(false);
      pastViolationsByRule.remove(newViolation.getRule().getId(), pastViolation);
      violationMap.put(newViolation, pastViolation);
      
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
