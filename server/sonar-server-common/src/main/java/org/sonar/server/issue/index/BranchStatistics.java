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
package org.sonar.server.issue.index;

import java.util.Map;
import org.sonar.api.rules.RuleType;

import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;

public class BranchStatistics {

  private final String branchUuid;
  private final long bugs;
  private final long vulnerabilities;
  private final long codeSmells;

  public BranchStatistics(String branchUuid, Map<String, Long> issueCountByType) {
    this.branchUuid = branchUuid;
    this.bugs = getNonNullValue(issueCountByType, BUG);
    this.vulnerabilities = getNonNullValue(issueCountByType, VULNERABILITY);
    this.codeSmells = getNonNullValue(issueCountByType, CODE_SMELL);
  }

  public String getBranchUuid() {
    return branchUuid;
  }

  public long getBugs() {
    return bugs;
  }

  public long getVulnerabilities() {
    return vulnerabilities;
  }

  public long getCodeSmells() {
    return codeSmells;
  }

  private static long getNonNullValue(Map<String, Long> issueCountByType, RuleType type) {
    Long value = issueCountByType.get(type.name());
    return value == null ? 0L : value;
  }
}
