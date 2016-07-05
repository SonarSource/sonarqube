/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.scan.report;

import java.io.Serializable;
import java.util.Comparator;

public class RuleReportComparator implements Comparator<RuleReport>, Serializable {
  @Override
  public int compare(RuleReport o1, RuleReport o2) {
    if (bothHaveNoNewIssue(o1, o2)) {
      return compareByRuleSeverityAndName(o1, o2);
    } else if (bothHaveNewIssues(o1, o2)) {
      if (sameSeverity(o1, o2) && !sameNewIssueCount(o1, o2)) {
        return compareNewIssueCount(o1, o2);
      } else {
        return compareByRuleSeverityAndName(o1, o2);
      }
    } else {
      return compareNewIssueCount(o1, o2);
    }
  }

  private static int compareByRuleSeverityAndName(RuleReport o1, RuleReport o2) {
    return o1.getReportRuleKey().compareTo(o2.getReportRuleKey());
  }

  private static boolean sameNewIssueCount(RuleReport o1, RuleReport o2) {
    return o2.getTotal().getNewIssuesCount() == o1.getTotal().getNewIssuesCount();
  }

  private static boolean sameSeverity(RuleReport o1, RuleReport o2) {
    return o1.getSeverity().equals(o2.getSeverity());
  }

  private static int compareNewIssueCount(RuleReport o1, RuleReport o2) {
    return o2.getTotal().getNewIssuesCount() - o1.getTotal().getNewIssuesCount();
  }

  private static boolean bothHaveNewIssues(RuleReport o1, RuleReport o2) {
    return o1.getTotal().getNewIssuesCount() > 0 && o2.getTotal().getNewIssuesCount() > 0;
  }

  private static boolean bothHaveNoNewIssue(RuleReport o1, RuleReport o2) {
    return o1.getTotal().getNewIssuesCount() == 0 && o2.getTotal().getNewIssuesCount() == 0;
  }
}
