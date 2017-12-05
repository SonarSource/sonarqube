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
package org.sonar.server.measure.live;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.db.issue.IssueGroupDto;
import org.sonar.db.rule.SeverityUtil;

class IssueCounter {

  private final Map<RuleType, HighestSeverity> highestSeverityOfUnresolved = new EnumMap<>(RuleType.class);
  private final Map<RuleType, Effort> effortOfUnresolved = new EnumMap<>(RuleType.class);
  private final Map<String, Count> unresolvedBySeverity = new HashMap<>();
  private final Map<RuleType, Count> unresolvedByType = new EnumMap<>(RuleType.class);
  private final Map<String, Count> byResolution = new HashMap<>();
  private final Map<String, Count> byStatus = new HashMap<>();
  private final Count unresolved = new Count();

  IssueCounter(Collection<IssueGroupDto> groups) {
    for (IssueGroupDto group : groups) {
      RuleType ruleType = RuleType.valueOf(group.getRuleType());
      if (group.getResolution() == null) {
        highestSeverityOfUnresolved
          .computeIfAbsent(ruleType, k -> new HighestSeverity())
          .add(group);
        effortOfUnresolved
          .computeIfAbsent(ruleType, k -> new Effort())
          .add(group);
        unresolvedBySeverity
          .computeIfAbsent(group.getSeverity(), k -> new Count())
          .add(group);
        unresolvedByType
          .computeIfAbsent(ruleType, k -> new Count())
          .add(group);
        unresolved.add(group);
      } else {
        byResolution
          .computeIfAbsent(group.getResolution(), k -> new Count())
          .add(group);
      }
      if (group.getStatus() != null) {
        byStatus
          .computeIfAbsent(group.getStatus(), k -> new Count())
          .add(group);
      }
    }
  }

  public Optional<String> getHighestSeverityOfUnresolved(RuleType ruleType, boolean onlyInLeak) {
    return Optional.ofNullable(highestSeverityOfUnresolved.get(ruleType))
      .map(hs -> hs.severity(onlyInLeak));
  }

  public double sumEffortOfUnresolved(RuleType type, boolean onlyInLeak) {
    Effort effort = effortOfUnresolved.get(type);
    if (effort == null) {
      return 0.0;
    }
    return onlyInLeak ? effort.leak : effort.absolute;
  }

  public long countUnresolvedBySeverity(String severity, boolean onlyInLeak) {
    return value(unresolvedBySeverity.get(severity), onlyInLeak);
  }

  public long countByResolution(String resolution, boolean onlyInLeak) {
    return value(byResolution.get(resolution), onlyInLeak);
  }

  public long countUnresolvedByType(RuleType type, boolean onlyInLeak) {
    return value(unresolvedByType.get(type), onlyInLeak);
  }

  public long countByStatus(String status, boolean onlyInLeak) {
    return value(byStatus.get(status), onlyInLeak);
  }

  public long countUnresolved(boolean onlyInLeak) {
    return value(unresolved, onlyInLeak);
  }

  private static long value(@Nullable Count count, boolean onlyInLeak) {
    if (count == null) {
      return 0;
    }
    return onlyInLeak ? count.leak : count.absolute;
  }

  private static class Count {
    private long absolute = 0L;
    private long leak = 0L;

    void add(IssueGroupDto group) {
      absolute += group.getCount();
      if (group.isInLeak()) {
        leak += group.getCount();
      }
    }
  }

  private static class Effort {
    private double absolute = 0.0;
    private double leak = 0.0;

    void add(IssueGroupDto group) {
      absolute += group.getEffort();
      if (group.isInLeak()) {
        leak += group.getEffort();
      }
    }
  }

  private static class HighestSeverity {
    private int absolute = SeverityUtil.getOrdinalFromSeverity(Severity.INFO);
    private int leak = SeverityUtil.getOrdinalFromSeverity(Severity.INFO);

    void add(IssueGroupDto group) {
      int severity = SeverityUtil.getOrdinalFromSeverity(group.getSeverity());
      absolute = Math.max(severity, absolute);
      if (group.isInLeak()) {
        leak = Math.max(severity, leak);
      }
    }

    String severity(boolean inLeak) {
      return SeverityUtil.getSeverityFromOrdinal(inLeak ? leak : absolute);
    }
  }
}
