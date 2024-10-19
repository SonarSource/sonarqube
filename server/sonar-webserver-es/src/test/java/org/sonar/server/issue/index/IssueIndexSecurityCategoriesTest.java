/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.db.component.ComponentDto;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.issue.IssueDocTesting.newDocForProject;

class IssueIndexSecurityCategoriesTest extends IssueIndexTestCommon {

  @Test
  void searchSinglePciDss32Category() {
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDocForProject("openvul1", project).setPciDss32(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setPciDss32(asList("3.3.2", "1.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR)
    );

    assertThatSearchReturnsOnly(queryPciDss32("1"), "openvul1", "openvul2");
    assertThatSearchReturnsOnly(queryPciDss32("1.2.0"), "openvul1");
    assertThatSearchReturnsEmpty(queryPciDss32("1.2"));
  }

  @Test
  void searchMultiplePciDss32Categories() {
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDocForProject("openvul1", project).setPciDss32(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setPciDss32(asList("3.3.2", "2.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("openvul3", project).setPciDss32(asList("4.1", "5.4")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR)
    );

    assertThatSearchReturnsOnly(queryPciDss32("1", "4"), "openvul1", "openvul3");
    assertThatSearchReturnsOnly(queryPciDss32("1.2.0", "5.4"), "openvul1", "openvul3");
    assertThatSearchReturnsEmpty(queryPciDss32("6", "7", "8", "9", "10", "11", "12"));
  }

  @Test
  void searchSinglePciDss40Category() {
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDocForProject("openvul1", project).setPciDss40(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setPciDss40(asList("3.3.2", "1.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR)
    );

    assertThatSearchReturnsOnly(queryPciDss40("1"), "openvul1", "openvul2");
    assertThatSearchReturnsOnly(queryPciDss40("1.2.0"), "openvul1");
    assertThatSearchReturnsEmpty(queryPciDss40("1.2"));
  }

  @Test
  void searchMultiplePciDss40Categories() {
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDocForProject("openvul1", project).setPciDss40(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setPciDss40(asList("3.3.2", "2.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("openvul3", project).setPciDss40(asList("4.1", "5.4")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR)
    );

    assertThatSearchReturnsOnly(queryPciDss40("1", "4"), "openvul1", "openvul3");
    assertThatSearchReturnsOnly(queryPciDss40("1.2.0", "5.4"), "openvul1", "openvul3");
    assertThatSearchReturnsEmpty(queryPciDss40("6", "7", "8", "9", "10", "11", "12"));
  }

  @Test
  void searchMixedPciDssCategories() {
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDocForProject("openvul1", project).setPciDss40(asList("1.2.0", "3.4.5")).setPciDss32(List.of("2.1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setPciDss40(asList("3.3.2", "2.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("openvul3", project).setPciDss32(asList("4.1", "5.4")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR)
    );

    assertThatSearchReturnsOnly(queryPciDss40("1", "4"), "openvul1");
    assertThatSearchReturnsOnly(queryPciDss40("1.2.0", "5.4"), "openvul1");
    assertThatSearchReturnsEmpty(queryPciDss40("6", "7", "8", "9", "10", "11", "12"));

    assertThatSearchReturnsOnly(queryPciDss32("3", "2.1"), "openvul1");
    assertThatSearchReturnsOnly(queryPciDss32("1", "2"), "openvul1");
    assertThatSearchReturnsOnly(queryPciDss32("4", "3"), "openvul3");
    assertThatSearchReturnsEmpty(queryPciDss32("1", "3", "6", "7", "8", "9", "10", "11", "12"));

  }

  private IssueQuery.Builder queryPciDss32(String... values) {
    return IssueQuery.builder()
      .pciDss32(stream(values).collect(toList()))
      .types(List.of("CODE_SMELL", "BUG", "VULNERABILITY"));
  }

  private IssueQuery.Builder queryPciDss40(String... values) {
    return IssueQuery.builder()
      .pciDss40(stream(values).collect(toList()))
      .types(List.of("CODE_SMELL", "BUG", "VULNERABILITY"));
  }
}
