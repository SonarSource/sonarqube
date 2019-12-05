/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.DefaultTransitions.RESET_AS_TO_REVIEW;
import static org.sonar.api.issue.DefaultTransitions.RESOLVE_AS_REVIEWED;
import static org.sonar.api.issue.DefaultTransitions.RESOLVE_AS_SAFE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.db.rule.RuleTesting.XOO_X1;

@RunWith(DataProviderRunner.class)
public class IssueWorkflowForSecurityHotspotsTest {

  private static final IssueChangeContext SOME_CHANGE_CONTEXT = IssueChangeContext.createUser(new Date(), "USER1");

  private IssueFieldsSetter updater = new IssueFieldsSetter();

  private IssueWorkflow underTest = new IssueWorkflow(new FunctionExecutor(updater), updater);

  @Test
  @UseDataProvider("anyResolutionIncludingNone")
  public void to_review_hotspot_with_any_resolution_can_be_resolved_as_safe_or_fixed(@Nullable String resolution) {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_TO_REVIEW, resolution);

    List<Transition> transitions = underTest.outTransitions(hotspot);

    assertThat(keys(transitions)).containsExactlyInAnyOrder(RESOLVE_AS_REVIEWED, RESOLVE_AS_SAFE);
  }

  @DataProvider
  public static Object[][] anyResolutionIncludingNone() {
    return Stream.of(
      Issue.RESOLUTIONS.stream(),
      Issue.SECURITY_HOTSPOT_RESOLUTIONS.stream(),
      Stream.of(randomAlphabetic(12), null))
      .flatMap(t -> t)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  public void reviewed_as_fixed_hotspot_can_be_resolved_as_safe_or_put_back_to_review() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_REVIEWED, RESOLUTION_FIXED);

    List<Transition> transitions = underTest.outTransitions(hotspot);

    assertThat(keys(transitions)).containsExactlyInAnyOrder(RESOLVE_AS_SAFE, RESET_AS_TO_REVIEW);
  }

  @Test
  public void reviewed_as_safe_hotspot_can_be_resolved_as_fixed_or_put_back_to_review() {
    underTest.start();
    DefaultIssue hotspot = newHotspot(STATUS_REVIEWED, RESOLUTION_SAFE);

    List<Transition> transitions = underTest.outTransitions(hotspot);

    assertThat(keys(transitions)).containsExactlyInAnyOrder(RESOLVE_AS_REVIEWED, RESET_AS_TO_REVIEW);
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
      Stream.of(randomAlphabetic(12)))
      .flatMap(t -> t)
      .filter(t -> !RESOLUTION_FIXED.equals(t))
      .filter(t -> !RESOLUTION_SAFE.equals(t))
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

    underTest.doAutomaticTransition(hotspot, IssueChangeContext.createScan(now));

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

    underTest.doAutomaticTransition(hotspot, IssueChangeContext.createScan(now));

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
  @UseDataProvider("allStatusesLeadingToClosed")
  public void do_not_automatically_reopen_closed_issues_of_security_hotspots(String previousStatus) {
    DefaultIssue[] hotspots = Stream.of(RESOLUTION_FIXED, RESOLUTION_REMOVED)
      .map(resolution -> {
        DefaultIssue issue = newClosedHotspot(resolution);
        setStatusPreviousToClosed(issue, previousStatus);
        return issue;
      })
      .toArray(DefaultIssue[]::new);
    Date now = new Date();
    underTest.start();

    Arrays.stream(hotspots).forEach(hotspot -> {
      underTest.doAutomaticTransition(hotspot, IssueChangeContext.createScan(now));

      assertThat(hotspot.status()).isEqualTo(STATUS_CLOSED);
      assertThat(hotspot.updateDate()).isNull();
    });
  }

  @DataProvider
  public static Object[][] allStatusesLeadingToClosed() {
    return Stream.of(STATUS_TO_REVIEW, STATUS_REVIEWED)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  public void doAutomaticTransition_does_nothing_on_security_hotspots_in_to_review_status() {
    DefaultIssue hotspot = newHotspot(STATUS_TO_REVIEW, null)
      .setKey("ABCDE")
      .setRuleKey(XOO_X1);

    underTest.start();
    underTest.doAutomaticTransition(hotspot, IssueChangeContext.createScan(new Date()));

    assertThat(hotspot.status()).isEqualTo(STATUS_TO_REVIEW);
    assertThat(hotspot.resolution()).isNull();
  }

  private Collection<String> keys(List<Transition> transitions) {
    return transitions.stream().map(Transition::key).collect(MoreCollectors.toList());
  }

  private static void setStatusPreviousToClosed(DefaultIssue hotspot, String previousStatus) {
    addStatusChange(hotspot, new Date(), previousStatus, STATUS_CLOSED);
  }

  private static void addStatusChange(DefaultIssue issue, Date date, String previousStatus, String newStatus) {
    issue.addChange(new FieldDiffs().setCreationDate(date).setDiff("status", previousStatus, newStatus));
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
