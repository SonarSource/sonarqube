/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.issue;

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rules.Violation;
import org.sonar.batch.ViolationFilters;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueFiltersTest {

  DeprecatedViolations deprecatedViolations = mock(DeprecatedViolations.class);
  ViolationFilters deprecatedFilters = mock(ViolationFilters.class);

  @Test
  public void accept_when_filter_chain_is_empty() throws Exception {
    org.sonar.api.issue.IssueFilter ok = mock(org.sonar.api.issue.IssueFilter.class);
    when(ok.accept(any(Issue.class))).thenReturn(true);

    org.sonar.api.issue.IssueFilter ko = mock(org.sonar.api.issue.IssueFilter.class);
    when(ko.accept(any(Issue.class))).thenReturn(false);

    when(deprecatedFilters.isEmpty()).thenReturn(true);
    IssueFilters filters = new IssueFilters(deprecatedFilters, deprecatedViolations, new org.sonar.api.issue.IssueFilter[]{ok, ko});
    assertThat(filters.accept(new DefaultIssue(), null)).isFalse();

    filters = new IssueFilters(deprecatedFilters, deprecatedViolations, new org.sonar.api.issue.IssueFilter[]{ok});
    assertThat(filters.accept(new DefaultIssue(), null)).isTrue();

    filters = new IssueFilters(deprecatedFilters, deprecatedViolations, new org.sonar.api.issue.IssueFilter[]{ko});
    assertThat(filters.accept(new DefaultIssue(), null)).isFalse();
  }

  @Test
  public void should_always_accept_if_no_filters() {
    when(deprecatedFilters.isEmpty()).thenReturn(true);
    IssueFilters filters = new IssueFilters(deprecatedFilters, deprecatedViolations);
    assertThat(filters.accept(new DefaultIssue(), null)).isTrue();
  }

  @Test
  public void should_check_deprecated_violation_filters() throws Exception {
    when(deprecatedFilters.isEmpty()).thenReturn(false);
    when(deprecatedFilters.isIgnored(any(Violation.class))).thenReturn(true);
    IssueFilters filters = new IssueFilters(deprecatedFilters, deprecatedViolations, new org.sonar.api.issue.IssueFilter[0]);
    assertThat(filters.accept(new DefaultIssue(), null)).isFalse();

  }
}
