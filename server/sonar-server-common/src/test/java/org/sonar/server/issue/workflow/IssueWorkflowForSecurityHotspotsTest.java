/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TaintChecker;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.issue.DefaultTransitions.RESET_AS_TO_REVIEW;
import static org.sonar.api.issue.DefaultTransitions.RESOLVE_AS_ACKNOWLEDGED;
import static org.sonar.api.issue.DefaultTransitions.RESOLVE_AS_REVIEWED;
import static org.sonar.api.issue.DefaultTransitions.RESOLVE_AS_SAFE;
import static org.sonar.api.issue.Issue.RESOLUTION_ACKNOWLEDGED;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByScanBuilder;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.db.rule.RuleTesting.XOO_X1;
import static org.sonar.server.issue.workflow.IssueWorkflowTest.emptyIfNull;

@RunWith(DataProviderRunner.class)
public class IssueWorkflowForSecurityHotspotsTest {
  private static final IssueChangeContext SOME_CHANGE_CONTEXT = issueChangeContextByUserBuilder(new Date(), "USER1").build();
  private static final List<String> RESOLUTION_TYPES = List.of(RESOLUTION_FIXED, RESOLUTION_SAFE, RESOLUTION_ACKNOWLEDGED);

  private final IssueFieldsSetter updater = new IssueFieldsSetter();
  private final IssueWorkflow underTest = new IssueWorkflow(new FunctionExecutor(updater), updater, mock(TaintChecker.class));

  @Test
  @UseDataProvider("anyResolutionIncludingNone")
  public void to_review_hotspot_with_any_resolution_can_be_resolved_as_safe_or_fixed(@Nullable String resolution) {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_TO_REVIEW, resolution);

    List<Transition> transitions = underTest.outTransitions(hotspot);

    assertThat(keys(transitions)).containsExactlyInAnyOrder(RESOLVE_AS_REVIEWED, RESOLVE_AS_SAFE, RESOLVE_AS_ACKNOWLEDGED);
  }

  @DataProvider
  public static Object[][] anyResolutionIncludingNone() {
    return Stream.of(
      Issue.RESOLUTIONS.stream(),
      Issue.SECURITY_HOTSPOT_RESOLUTIONS.stream(),
      Stream.of(secure().nextAlphabetic(12), null))
      .flatMap(t -> t)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  public void reviewed_as_fixed_hotspot_can_be_resolved_as_safe_or_put_back_to_review() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED);

    List<Transition> transitions = underTest.outTransitions(hotspot);

    assertThat(keys(transitions)).containsExactlyInAnyOrder(RESOLVE_AS_SAFE, RESET_AS_TO_REVIEW, RESOLVE_AS_ACKNOWLEDGED);
  }

  @Test
  public void reviewed_as_safe_hotspot_can_be_resolved_as_fixed_or_put_back_to_review() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_REVIEWED, RESOLUTION_SAFE);

    List<Transition> transitions = underTest.outTransitions(hotspot);

    assertThat(keys(transitions)).containsExactlyInAnyOrder(RESOLVE_AS_REVIEWED, RESET_AS_TO_REVIEW, RESOLVE_AS_ACKNOWLEDGED);
  }

  @Test
  @UseDataProvider("anyResolutionButSafeOrFixed")
  public void reviewed_with_any_resolution_but_safe_or_fixed_can_not_be_changed(String resolution) {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_REVIEWED, resolution);

    List<Transition> transitions = underTest.outTransitions(hotspot);

    assertThat(transitions).isEmpty();
  }

  @DataProvider
  public static Object[][] anyResolutionButSafeOrFixed() {
    return Stream.of(
      Issue.RESOLUTIONS.stream(),
      Issue.SECURITY_HOTSPOT_RESOLUTIONS.stream(),
      Stream.of(secure().nextAlphabetic(12)))
      .flatMap(t -> t)
      .filter(t -> !RESOLUTION_TYPES.contains(t))
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  public void doManualTransition_to_review_hostpot_is_resolved_as_fixed() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_TO_REVIEW, null);

    boolean result = underTest.doManualTransition(hotspot, RESOLVE_AS_REVIEWED, SOME_CHANGE_CONTEXT);

    assertThat(result).isTrue();
    assertThat(hotspot.getStatus()).isEqualTo(STATUS_REVIEWED);
    assertThat(hotspot.resolution()).isEqualTo(RESOLUTION_FIXED);
  }

  @Test
  public void doManualTransition_reviewed_as_safe_hostpot_is_resolved_as_fixed() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_REVIEWED, RESOLUTION_SAFE);

    boolean result = underTest.doManualTransition(hotspot, RESOLVE_AS_REVIEWED, SOME_CHANGE_CONTEXT);

    assertThat(result).isTrue();
    assertThat(hotspot.getStatus()).isEqualTo(STATUS_REVIEWED);
    assertThat(hotspot.resolution()).isEqualTo(RESOLUTION_FIXED);
  }

  @Test
  public void doManualTransition_to_review_hostpot_is_resolved_as_safe() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_TO_REVIEW, null);

    boolean result = underTest.doManualTransition(hotspot, RESOLVE_AS_SAFE, SOME_CHANGE_CONTEXT);

    assertThat(result).isTrue();
    assertThat(hotspot.getStatus()).isEqualTo(STATUS_REVIEWED);
    assertThat(hotspot.resolution()).isEqualTo(RESOLUTION_SAFE);
  }

  @Test
  public void doManualTransition_reviewed_as_fixed_hostpot_is_resolved_as_safe() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED);

    boolean result = underTest.doManualTransition(hotspot, RESOLVE_AS_SAFE, SOME_CHANGE_CONTEXT);

    assertThat(result).isTrue();
    assertThat(hotspot.getStatus()).isEqualTo(STATUS_REVIEWED);
    assertThat(hotspot.resolution()).isEqualTo(RESOLUTION_SAFE);
  }

  @Test
  public void doManualTransition_reviewed_as_fixed_hostpot_is_put_back_to_review() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED);

    boolean result = underTest.doManualTransition(hotspot, RESET_AS_TO_REVIEW, SOME_CHANGE_CONTEXT);

    assertThat(result).isTrue();
    assertThat(hotspot.getStatus()).isEqualTo(STATUS_TO_REVIEW);
    assertThat(hotspot.resolution()).isNull();
  }

  @Test
  public void doManualTransition_reviewed_as_safe_hostpot_is_put_back_to_review() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_REVIEWED, RESOLUTION_SAFE);

    boolean result = underTest.doManualTransition(hotspot, RESET_AS_TO_REVIEW, SOME_CHANGE_CONTEXT);

    assertThat(result).isTrue();
    assertThat(hotspot.getStatus()).isEqualTo(STATUS_TO_REVIEW);
    assertThat(hotspot.resolution()).isNull();
  }

  @Test
  public void reset_as_to_review_from_reviewed() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED);

    boolean result = underTest.doManualTransition(hotspot, RESET_AS_TO_REVIEW, SOME_CHANGE_CONTEXT);
    assertThat(result).isTrue();
    assertThat(hotspot.type()).isEqualTo(SECURITY_HOTSPOT);
    assertThat(hotspot.getStatus()).isEqualTo(STATUS_TO_REVIEW);
    assertThat(hotspot.resolution()).isNull();
  }

  @Test
  public void automatically_close_resolved_security_hotspots_in_status_to_review() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_TO_REVIEW, null)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();

    underTest.doAutomaticTransition(hotspot, issueChangeContextByScanBuilder(now).build());

    assertThat(hotspot.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(hotspot.status()).isEqualTo(STATUS_CLOSED);
    assertThat(hotspot.closeDate()).isNotNull();
    assertThat(hotspot.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  @UseDataProvider("safeOrFixedResolutions")
  public void automatically_close_hotspot_resolved_as_fixed_or_safe(String resolution) {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_REVIEWED, resolution)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();

    underTest.doAutomaticTransition(hotspot, issueChangeContextByScanBuilder(now).build());

    assertThat(hotspot.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(hotspot.status()).isEqualTo(STATUS_CLOSED);
    assertThat(hotspot.closeDate()).isNotNull();
    assertThat(hotspot.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @DataProvider
  public static Object[][] safeOrFixedResolutions() {
    return new Object[][] {
      {RESOLUTION_SAFE},
      {RESOLUTION_FIXED}
    };
  }

  @Test
  public void automatically_reopen_closed_security_hotspots() {
    DefaultIssue hotspot1 = newClosedHotspot(RESOLUTION_REMOVED);
    setStatusPreviousToClosed(hotspot1, STATUS_REVIEWED, RESOLUTION_SAFE, RESOLUTION_REMOVED);

    DefaultIssue hotspot2 = newClosedHotspot(RESOLUTION_FIXED);
    setStatusPreviousToClosed(hotspot2, STATUS_TO_REVIEW, null, RESOLUTION_FIXED);

    Date now = new Date();
    underTest.start();

    underTest.doAutomaticTransition(hotspot1,  issueChangeContextByScanBuilder(now).build());
    underTest.doAutomaticTransition(hotspot2,  issueChangeContextByScanBuilder(now).build());

    assertThat(hotspot1.updateDate()).isNotNull();
    assertThat(hotspot1.status()).isEqualTo(STATUS_REVIEWED);
    assertThat(hotspot1.resolution()).isEqualTo(RESOLUTION_SAFE);

    assertThat(hotspot2.updateDate()).isNotNull();
    assertThat(hotspot2.status()).isEqualTo(STATUS_TO_REVIEW);
    assertThat(hotspot2.resolution()).isNull();
  }

  @Test
  public void doAutomaticTransition_does_nothing_on_security_hotspots_in_to_review_status() {
    DefaultIssue hotspot = newHotspot(STATUS_TO_REVIEW, null)
      .setKey("ABCDE")
      .setRuleKey(XOO_X1);

    underTest.start();
    underTest.doAutomaticTransition(hotspot, issueChangeContextByScanBuilder(new Date()).build());

    assertThat(hotspot.status()).isEqualTo(STATUS_TO_REVIEW);
    assertThat(hotspot.resolution()).isNull();
  }

  private Collection<String> keys(List<Transition> transitions) {
    return transitions.stream().map(Transition::key).toList();
  }

  private static void setStatusPreviousToClosed(DefaultIssue hotspot, String previousStatus, @Nullable String previousResolution, @Nullable String newResolution) {
    addStatusChange(hotspot, new Date(), previousStatus, STATUS_CLOSED, previousResolution, newResolution);
  }

  private static void addStatusChange(DefaultIssue issue, Date date, String previousStatus, String newStatus, @Nullable String previousResolution, @Nullable String newResolution) {
    issue.addChange(new FieldDiffs()
      .setCreationDate(date)
      .setDiff("status", previousStatus, newStatus)
      .setDiff("resolution", emptyIfNull(previousResolution), emptyIfNull(newResolution)));
  }

  private static DefaultIssue newClosedHotspot(String resolution) {
    return newHotspot(STATUS_CLOSED, resolution)
      .setKey("ABCDE")
      .setRuleKey(RuleKey.of("js", "S001"))
      .setNew(false)
      .setCloseDate(new Date(5_999_999L));
  }

  private static DefaultIssue newHotspot(String status, @Nullable String resolution) {
    return new DefaultIssue()
      .setType(SECURITY_HOTSPOT)
      .setStatus(status)
      .setResolution(resolution);
  }
}
