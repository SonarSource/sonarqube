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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.db.component.ComponentDto;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.doReturn;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.issue.IssueDocTesting.newDocForProject;

class IssueIndexSecurityCategoriesTest extends IssueIndexTestCommon {

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void searchSinglePciDss32Category(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDocForProject("openvul1", project).setPciDss32(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setPciDss32(asList("3.3.2", "1.5")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR)
    );

    assertThatSearchReturnsOnly(queryPciDss32("1"), "openvul1", "openvul2");
    assertThatSearchReturnsOnly(queryPciDss32("1.2.0"), "openvul1");
    assertThatSearchReturnsEmpty(queryPciDss32("1.2"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void searchMultiplePciDss32Categories(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDocForProject("openvul1", project).setPciDss32(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setPciDss32(asList("3.3.2", "2.5")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("openvul3", project).setPciDss32(asList("4.1", "5.4")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY,
        HIGH)).setStatus(Issue.STATUS_REOPENED).setSeverity(Severity.MINOR)
    );

    assertThatSearchReturnsOnly(queryPciDss32("1", "4"), "openvul1", "openvul3");
    assertThatSearchReturnsOnly(queryPciDss32("1.2.0", "5.4"), "openvul1", "openvul3");
    assertThatSearchReturnsEmpty(queryPciDss32("6", "7", "8", "9", "10", "11", "12"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void searchSinglePciDss40Category(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDocForProject("openvul1", project).setPciDss40(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setPciDss40(asList("3.3.2", "1.5")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR)
    );

    assertThatSearchReturnsOnly(queryPciDss40("1"), "openvul1", "openvul2");
    assertThatSearchReturnsOnly(queryPciDss40("1.2.0"), "openvul1");
    assertThatSearchReturnsEmpty(queryPciDss40("1.2"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void searchMultiplePciDss40Categories(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDocForProject("openvul1", project).setPciDss40(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setPciDss40(asList("3.3.2", "2.5")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("openvul3", project).setPciDss40(asList("4.1", "5.4")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY,
        HIGH)).setStatus(Issue.STATUS_REOPENED).setSeverity(Severity.MINOR)
    );

    assertThatSearchReturnsOnly(queryPciDss40("1", "4"), "openvul1", "openvul3");
    assertThatSearchReturnsOnly(queryPciDss40("1.2.0", "5.4"), "openvul1", "openvul3");
    assertThatSearchReturnsEmpty(queryPciDss40("6", "7", "8", "9", "10", "11", "12"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void searchMixedPciDssCategories(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDocForProject("openvul1", project).setPciDss40(asList("1.2.0", "3.4.5")).setPciDss32(List.of("2.1")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setPciDss40(asList("3.3.2", "2.5")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("openvul3", project).setPciDss32(asList("4.1", "5.4")).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY,
          HIGH)).setStatus(Issue.STATUS_REOPENED)
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
