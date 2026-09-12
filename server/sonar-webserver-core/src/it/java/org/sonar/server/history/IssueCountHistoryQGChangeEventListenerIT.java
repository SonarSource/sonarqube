/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.history;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.IssueProducer;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DBSessionsImpl;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListenersImpl;
import org.sonarsource.history.server.db.HistoryDbClient;
import org.sonarsource.history.server.db.HistoryMyBatisConfExtension;
import org.sonarsource.history.server.db.mapper.IssueTtrHistoryMapperFragments;
import org.sonarsource.history.server.db.repository.IssueCountDimensionsRepository;
import org.sonarsource.history.server.db.repository.IssueCountHistoryRepository;
import org.sonarsource.history.server.db.repository.IssueTtrHistoryRepository;
import org.sonarsource.history.server.service.IssueCountHistoryRecordingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.issue.impact.Severity.HIGH;

public class IssueCountHistoryQGChangeEventListenerIT {

  private static final Instant NOW = Instant.parse("2026-09-10T00:30:00Z");
  private static final long TODAY = Instant.parse("2026-09-10T00:00:00Z").toEpochMilli();

  @Rule
  public final DbTester db = DbTester.createWithConfExtension(System2.INSTANCE, new HistoryMyBatisConfExtension(IssueTtrHistoryMapperFragments.class));

  private final System2 system2 = mock(System2.class);
  private final HistoryDbClient historyDbClient = new HistoryDbClient(
    db.getDbClient().getMyBatis(), new DBSessionsImpl(db.getDbClient().getMyBatis()),
    List.of(new IssueCountDimensionsRepository(), new IssueCountHistoryRepository(), new IssueTtrHistoryRepository()));
  private final IssueCountHistoryQGChangeEventListener underTest = new IssueCountHistoryQGChangeEventListener(
    db.getDbClient(), new IssueCountHistoryRecordingService(historyDbClient), system2, Runnable::run);
  private final QGChangeEventListenersImpl dispatcher = new QGChangeEventListenersImpl(Set.of(underTest));

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(NOW.toEpochMilli());
  }

  @Test
  public void scanner_false_positive_and_reopening_update_history_without_analysis() {
    assertFalsePositiveAndReopening(IssueProducer.SCANNER);
  }

  @Test
  public void hunter_false_positive_and_reopening_update_history_without_analysis() {
    assertFalsePositiveAndReopening(IssueProducer.HUNTER_AGENT);
  }

  @Test
  public void refreshes_entire_non_main_branch_and_preserves_other_branches() {
    var project = db.components().insertPrivateProject();
    ComponentDto main = project.getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(main, b -> b.setKey("feature"));
    IssueDto mainIssue = insertIssue(main, IssueProducer.SCANNER, RuleType.VULNERABILITY);
    IssueDto changed = insertIssue(branch, IssueProducer.HUNTER_AGENT, RuleType.VULNERABILITY);
    insertIssue(branch, IssueProducer.SCANNER, RuleType.VULNERABILITY);
    broadcast(mainIssue);
    broadcast(changed);

    updateAndBroadcast(changed, "RESOLVED", "FALSE-POSITIVE");

    assertCount(main.branchUuid(), "OPEN", 1);
    assertCount(branch.branchUuid(), "OPEN", 1);
    assertCount(branch.branchUuid(), "FALSE_POSITIVE", 1);
  }

  @Test
  public void closing_last_issue_records_zero_for_previous_dimension() {
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto issue = insertIssue(branch, IssueProducer.SCANNER, RuleType.VULNERABILITY);
    broadcast(issue);

    updateAndBroadcast(issue, "CLOSED", "FIXED");

    assertCount(branch.branchUuid(), "OPEN", 0);
    assertThat(db.countRowsOfTable("issue_count_history")).isOne();
  }

  @Test
  public void next_day_change_preserves_previous_days_snapshot() {
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto issue = insertIssue(branch, IssueProducer.SCANNER, RuleType.VULNERABILITY);
    broadcast(issue);
    when(system2.now()).thenReturn(NOW.plusSeconds(86400).toEpochMilli());

    updateAndBroadcast(issue, "RESOLVED", "WONTFIX");

    assertCount(branch.branchUuid(), "OPEN", 1);
    assertCount(branch.branchUuid(), "OPEN", 0, TODAY + 86400000L);
    assertCount(branch.branchUuid(), "ACCEPTED", 1, TODAY + 86400000L);
  }

  @Test
  public void ignores_pull_requests() {
    ComponentDto main = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto pullRequest = db.components().insertProjectBranch(main, b -> b.setBranchType(BranchType.PULL_REQUEST).setKey("123"));

    broadcast(insertIssue(pullRequest, IssueProducer.HUNTER_AGENT, RuleType.VULNERABILITY));

    assertThat(db.countRowsOfTable("issue_count_history")).isZero();
  }

  @Test
  public void excludes_hotspots_from_snapshot() {
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto issue = insertIssue(branch, IssueProducer.SCANNER, RuleType.VULNERABILITY);
    insertIssue(branch, IssueProducer.SCANNER, RuleType.SECURITY_HOTSPOT);

    broadcast(issue);

    assertCount(branch.branchUuid(), "OPEN", 1);
    assertThat(db.countRowsOfTable("issue_count_history")).isOne();
  }

  @Test
  public void ignores_notifications_without_changed_issues() {
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto issue = insertIssue(branch, IssueProducer.SCANNER, RuleType.VULNERABILITY);

    dispatcher.broadcastOnAnyChange(List.of(eventFor(issue)), false);

    assertThat(db.countRowsOfTable("issue_count_history")).isZero();
  }

  private void assertFalsePositiveAndReopening(IssueProducer producer) {
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto issue = insertIssue(branch, producer, RuleType.VULNERABILITY);
    broadcast(issue);
    assertCount(branch.branchUuid(), "OPEN", 1);

    updateAndBroadcast(issue, "RESOLVED", "FALSE-POSITIVE");
    assertCount(branch.branchUuid(), "OPEN", 0);
    assertCount(branch.branchUuid(), "FALSE_POSITIVE", 1);
    broadcast(issue);
    assertCount(branch.branchUuid(), "OPEN", 0);
    assertCount(branch.branchUuid(), "FALSE_POSITIVE", 1);
    assertThat(db.countRowsOfTable("issue_count_history")).isEqualTo(2);

    updateAndBroadcast(issue, "OPEN", null);
    assertCount(branch.branchUuid(), "OPEN", 1);
    assertCount(branch.branchUuid(), "FALSE_POSITIVE", 0);
    assertThat(db.countRowsOfTable("issue_count_history")).isEqualTo(2);
  }

  private IssueDto insertIssue(ComponentDto branch, IssueProducer producer, RuleType type) {
    RuleDto rule = db.rules().insert(r -> r.setType(type));
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    IssueDto issue = db.issues().insert(rule, branch, file,
      i -> i.setType(type).setIssueProducer(producer).setStatus("OPEN").setResolution(null).setSeverity("CRITICAL")
        .addImpact(new ImpactDto().setSoftwareQuality(SECURITY).setSeverity(HIGH)));
    db.commit();
    return issue;
  }

  private void updateAndBroadcast(IssueDto issue, String status, @Nullable String resolution) {
    issue.setStatus(status).setResolution(resolution);
    db.getDbClient().issueDao().update(db.getSession(), issue);
    db.commit();
    broadcast(issue);
  }

  private void broadcast(IssueDto issue) {
    dispatcher.broadcastOnIssueChange(List.of(issue.toDefaultIssue()), List.of(eventFor(issue)), false);
  }

  private QGChangeEvent eventFor(IssueDto issue) {
    BranchDto branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), issue.getProjectUuid()).orElseThrow();
    QGChangeEvent event = mock(QGChangeEvent.class);
    when(event.getBranch()).thenReturn(branch);
    return event;
  }

  private void assertCount(String branchUuid, String status, int expected) {
    assertCount(branchUuid, status, expected, TODAY);
  }

  private void assertCount(String branchUuid, String status, int expected, long recordedAt) {
    var rows = db.select("select h.issue_count from issue_count_history h "
      + "join issue_count_dimensions d on d.id = h.dimension_id "
      + "where h.entity_id = '" + branchUuid + "' and h.entity_type = 'PROJECT_BRANCH' "
      + "and d.security_rating = 4 and d.issue_status = '" + status + "' and h.recorded_at_epoch = " + recordedAt);
    assertThat(rows).isNotEmpty();
    assertThat(rows.stream().mapToInt(row -> ((Number) row.get("issue_count")).intValue()).sum()).isEqualTo(expected);
  }
}
