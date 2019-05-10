/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue.workflow;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_IN_REVIEW;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.db.rule.RuleTesting.XOO_X1;

@RunWith(DataProviderRunner.class)
public class IssueWorkflowForSecurityHotspotsTest {

  private static final String[] ALL_STATUSES_LEADING_TO_CLOSED = new String[] {STATUS_TO_REVIEW, STATUS_IN_REVIEW, STATUS_RESOLVED};

  private static final String[] SUPPORTED_RESOLUTIONS_FOR_UNCLOSING = new String[] {RESOLUTION_FIXED, RESOLUTION_REMOVED};

  private IssueFieldsSetter updater = new IssueFieldsSetter();

  private IssueWorkflow underTest = new IssueWorkflow(new FunctionExecutor(updater), updater);

  @DataProvider
  public static Object[][] allStatusesLeadingToClosed() {
    return Arrays.stream(ALL_STATUSES_LEADING_TO_CLOSED)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  private static DefaultIssue newClosedIssue(String resolution) {
    return new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(RuleKey.of("js", "S001"))
      .setResolution(resolution)
      .setStatus(STATUS_CLOSED)
      .setNew(false)
      .setCloseDate(new Date(5_999_999L));
  }

  private static void setStatusPreviousToClosed(DefaultIssue issue, String previousStatus) {
    addStatusChange(issue, new Date(), previousStatus, STATUS_CLOSED);
  }

  private static void addStatusChange(DefaultIssue issue, Date date, String previousStatus, String newStatus) {
    issue.addChange(new FieldDiffs().setCreationDate(date).setDiff("status", previousStatus, newStatus));
  }

  @Test
  public void list_out_transitions_in_status_to_review() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue().setType(RuleType.SECURITY_HOTSPOT).setStatus(STATUS_TO_REVIEW);

    List<Transition> transitions = underTest.outTransitions(issue);

    assertThat(keys(transitions)).containsExactlyInAnyOrder("setinreview", "resolveasreviewed", "openasvulnerability");
  }

  @Test
  public void list_out_transitions_in_status_in_review() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue().setType(RuleType.SECURITY_HOTSPOT).setStatus(STATUS_IN_REVIEW);

    List<Transition> transitions = underTest.outTransitions(issue);

    assertThat(keys(transitions)).containsExactlyInAnyOrder("resolveasreviewed", "openasvulnerability", "resetastoreview");
  }

  @Test
  public void list_out_transitions_in_status_reviewed() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue().setType(RuleType.SECURITY_HOTSPOT).setStatus(STATUS_REVIEWED);

    List<Transition> transitions = underTest.outTransitions(issue);

    assertThat(keys(transitions)).containsExactlyInAnyOrder("openasvulnerability", "resetastoreview");
  }

  @Test
  public void list_out_vulnerability_transitions_in_status_open() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue().setType(RuleType.VULNERABILITY).setResolution(RESOLUTION_FIXED).setStatus(STATUS_OPEN).setIsFromHotspot(true);

    List<Transition> transitions = underTest.outTransitions(issue);

    assertThat(keys(transitions)).containsExactlyInAnyOrder("resolveasreviewed", "resetastoreview");
  }

  @Test
  public void set_as_in_review() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setIsFromHotspot(true)
      .setStatus(STATUS_TO_REVIEW);

    boolean result = underTest.doManualTransition(issue, DefaultTransitions.SET_AS_IN_REVIEW, IssueChangeContext.createUser(new Date(), "USER1"));

    assertThat(result).isTrue();
    assertThat(issue.getStatus()).isEqualTo(STATUS_IN_REVIEW);
    assertThat(issue.resolution()).isNull();
  }

  @Test
  public void resolve_as_reviewed_from_to_review() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setIsFromHotspot(true)
      .setStatus(STATUS_TO_REVIEW);

    boolean result = underTest.doManualTransition(issue, DefaultTransitions.RESOLVE_AS_REVIEWED, IssueChangeContext.createUser(new Date(), "USER1"));

    assertThat(result).isTrue();
    assertThat(issue.getStatus()).isEqualTo(STATUS_REVIEWED);
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
  }

  @Test
  public void resolve_as_reviewed_from_in_review() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setIsFromHotspot(true)
      .setStatus(STATUS_IN_REVIEW)
      .setResolution(null);

    boolean result = underTest.doManualTransition(issue, DefaultTransitions.RESOLVE_AS_REVIEWED, IssueChangeContext.createUser(new Date(), "USER1"));

    assertThat(result).isTrue();
    assertThat(issue.getStatus()).isEqualTo(STATUS_REVIEWED);
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
  }

  @Test
  public void open_as_vulnerability_from_in_review() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setIsFromHotspot(true)
      .setStatus(STATUS_IN_REVIEW)
      .setResolution(null);

    boolean result = underTest.doManualTransition(issue, DefaultTransitions.OPEN_AS_VULNERABILITY, IssueChangeContext.createUser(new Date(), "USER1"));

    assertThat(result).isTrue();
    assertThat(issue.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(issue.getStatus()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(issue.resolution()).isNull();
  }

  @Test
  public void open_as_vulnerability_from_to_review() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setIsFromHotspot(true)
      .setStatus(STATUS_TO_REVIEW)
      .setResolution(null);

    boolean result = underTest.doManualTransition(issue, DefaultTransitions.OPEN_AS_VULNERABILITY, IssueChangeContext.createUser(new Date(), "USER1"));

    assertThat(result).isTrue();
    assertThat(issue.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(issue.getStatus()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(issue.resolution()).isNull();
  }

  @Test
  public void open_as_vulnerability_from_reviewed() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setIsFromHotspot(true)
      .setResolution(RESOLUTION_FIXED)
      .setStatus(STATUS_REVIEWED);

    boolean result = underTest.doManualTransition(issue, DefaultTransitions.OPEN_AS_VULNERABILITY, IssueChangeContext.createUser(new Date(), "USER1"));

    assertThat(result).isTrue();
    assertThat(issue.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(issue.getStatus()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(issue.resolution()).isNull();
  }

  @Test
  public void reset_as_to_review_from_reviewed() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setIsFromHotspot(true)
      .setStatus(STATUS_REVIEWED)
      .setResolution(RESOLUTION_FIXED);

    boolean result = underTest.doManualTransition(issue, DefaultTransitions.RESET_AS_TO_REVIEW, IssueChangeContext.createUser(new Date(), "USER1"));
    assertThat(result).isTrue();
    assertThat(issue.type()).isEqualTo(RuleType.SECURITY_HOTSPOT);
    assertThat(issue.getStatus()).isEqualTo(STATUS_TO_REVIEW);
    assertThat(issue.resolution()).isNull();
  }

  @Test
  public void reset_as_to_review_from_in_review() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setIsFromHotspot(true)
      .setStatus(STATUS_IN_REVIEW)
      .setResolution(null);

    boolean result = underTest.doManualTransition(issue, DefaultTransitions.RESET_AS_TO_REVIEW, IssueChangeContext.createUser(new Date(), "USER1"));
    assertThat(result).isTrue();
    assertThat(issue.type()).isEqualTo(RuleType.SECURITY_HOTSPOT);
    assertThat(issue.getStatus()).isEqualTo(STATUS_TO_REVIEW);
    assertThat(issue.resolution()).isNull();
  }

  @Test
  public void reset_as_to_review_from_opened_as_vulnerability() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.VULNERABILITY)
      .setIsFromHotspot(true)
      .setStatus(STATUS_OPEN)
      .setResolution(null);

    boolean result = underTest.doManualTransition(issue, DefaultTransitions.RESET_AS_TO_REVIEW, IssueChangeContext.createUser(new Date(), "USER1"));
    assertThat(result).isTrue();
    assertThat(issue.type()).isEqualTo(RuleType.SECURITY_HOTSPOT);
    assertThat(issue.getStatus()).isEqualTo(STATUS_TO_REVIEW);
    assertThat(issue.resolution()).isNull();
  }

  @Test
  public void automatically_close_resolved_security_hotspots_in_status_to_review() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setResolution(null)
      .setStatus(STATUS_TO_REVIEW)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();

    underTest.doAutomaticTransition(issue, IssueChangeContext.createScan(now));

    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  public void automatically_close_resolved_security_hotspots_in_status_in_review() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setResolution(null)
      .setStatus(STATUS_IN_REVIEW)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();

    underTest.doAutomaticTransition(issue, IssueChangeContext.createScan(now));

    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  public void automatically_close_resolved_security_hotspots_in_status_reviewed() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setResolution(RESOLUTION_FIXED)
      .setStatus(STATUS_REVIEWED)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();

    underTest.doAutomaticTransition(issue, IssueChangeContext.createScan(now));

    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  public void automatically_close_hotspots_opened_as_vulnerability() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setType(RuleType.VULNERABILITY)
      .setResolution(null)
      .setStatus(STATUS_OPEN)
      .setIsFromHotspot(true)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();

    underTest.doAutomaticTransition(issue, IssueChangeContext.createScan(now));

    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  @UseDataProvider("allStatusesLeadingToClosed")
  public void do_not_automatically_reopen_closed_issues_of_security_hotspots(String previousStatus) {
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(resolution -> {
        DefaultIssue issue = newClosedIssue(resolution);
        setStatusPreviousToClosed(issue, previousStatus);
        issue.setType(RuleType.SECURITY_HOTSPOT);
        return issue;
      })
      .toArray(DefaultIssue[]::new);
    Date now = new Date();
    underTest.start();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, IssueChangeContext.createScan(now));

      assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
      assertThat(issue.updateDate()).isNull();
    });
  }

  @Test
  public void doAutomaticTransition_does_nothing_on_security_hotspots_in_to_review_status() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(XOO_X1)
      .setResolution(null)
      .setStatus(STATUS_TO_REVIEW);

    underTest.start();
    underTest.doAutomaticTransition(issue, IssueChangeContext.createScan(new Date()));

    assertThat(issue.status()).isEqualTo(STATUS_TO_REVIEW);
    assertThat(issue.resolution()).isNull();
  }

  @Test
  @UseDataProvider("allStatusesLeadingToClosed")
  public void do_not_automatically_reopen_closed_issues_of_manual_vulnerability(String previousStatus) {
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(resolution -> {
        DefaultIssue issue = newClosedIssue(resolution);
        setStatusPreviousToClosed(issue, previousStatus);
        issue.setIsFromHotspot(true);
        return issue;
      })
      .toArray(DefaultIssue[]::new);
    Date now = new Date();
    underTest.start();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, IssueChangeContext.createScan(now));

      assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
      assertThat(issue.updateDate()).isNull();
    });
  }

  @Test
  public void do_not_allow_to_doManualTransition_when_condition_fails() {
    underTest.start();
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      // Detect is only available on hotspot
      .setType(RuleType.VULNERABILITY)
      .setIsFromHotspot(false)
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setRuleKey(XOO_X1);

    assertThat(underTest.doManualTransition(issue, DefaultTransitions.RESET_AS_TO_REVIEW, IssueChangeContext.createScan(new Date()))).isFalse();
  }

  private Collection<String> keys(List<Transition> transitions) {
    return transitions.stream().map(Transition::key).collect(MoreCollectors.toList());
  }
}
