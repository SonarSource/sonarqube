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
package org.sonar.ce.task.projectanalysis.step;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.rule.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.MutableTreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.issue.ProtoIssueCache;
import org.sonar.ce.task.projectanalysis.pushevent.PushEventFactory;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbTester;
import org.sonar.db.pushevent.PushEventDto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistPushEventsStepIT {

  private final TestSystem2 system2 = new TestSystem2().setNow(1L);

  @Rule
  public DbTester db = DbTester.create(system2);

  public final PushEventFactory pushEventFactory = mock(PushEventFactory.class);
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public MutableTreeRootHolderRule treeRootHolder = new MutableTreeRootHolderRule();
  private ProtoIssueCache protoIssueCache;
  private PersistPushEventsStep underTest;

  @Before
  public void before() throws IOException {
    protoIssueCache = new ProtoIssueCache(temp.newFile(), System2.INSTANCE);
    buildComponentTree();
    underTest = new PersistPushEventsStep(db.getDbClient(), protoIssueCache, pushEventFactory, treeRootHolder);
  }

  @Test
  public void description() {
    assertThat(underTest.getDescription()).isEqualTo("Publishing taint vulnerabilities and security hotspots events");
  }

  @Test
  public void do_nothing_if_no_issues() {
    underTest.execute(mock(ComputationStep.Context.class));

    assertThat(db.countSql(db.getSession(), "SELECT count(uuid) FROM push_events")).isZero();
  }

  @Test
  public void resilient_to_failure() {
    protoIssueCache.newAppender().append(
      createIssue("key1").setType(RuleType.VULNERABILITY))
      .close();

    when(pushEventFactory.raiseEventOnIssue(any(), any())).thenThrow(new RuntimeException("I have a bad feelings about this"));

    assertThatCode(() -> underTest.execute(mock(ComputationStep.Context.class)))
      .doesNotThrowAnyException();

    assertThat(db.countSql(db.getSession(), "SELECT count(uuid) FROM push_events")).isZero();
  }

  @Test
  public void skip_persist_if_no_push_events() {
    protoIssueCache.newAppender().append(
      createIssue("key1").setType(RuleType.VULNERABILITY))
      .close();

    underTest.execute(mock(ComputationStep.Context.class));

    assertThat(db.countSql(db.getSession(), "SELECT count(uuid) FROM push_events")).isZero();
  }

  @Test
  public void do_nothing_if_issue_does_not_have_component() {
    protoIssueCache.newAppender().append(
      createIssue("key1").setType(RuleType.VULNERABILITY)
        .setComponentUuid(null))
      .close();

    underTest.execute(mock(ComputationStep.Context.class));

    assertThat(db.countSql(db.getSession(), "SELECT count(uuid) FROM push_events")).isZero();
  }

  @Test
  public void store_push_events() {
    protoIssueCache.newAppender()
      .append(createIssue("key1").setType(RuleType.VULNERABILITY)
        .setComponentUuid("cu1")
        .setComponentKey("ck1"))
      .append(createIssue("key2").setType(RuleType.VULNERABILITY)
        .setComponentUuid("cu2")
        .setComponentKey("ck2"))
      .close();

    when(pushEventFactory.raiseEventOnIssue(eq("uuid_1"), any(DefaultIssue.class))).thenReturn(
      Optional.of(createPushEvent()),
      Optional.of(createPushEvent()));

    underTest.execute(mock(ComputationStep.Context.class));

    assertThat(db.countSql(db.getSession(), "SELECT count(uuid) FROM push_events")).isEqualTo(2);
  }

  @Test
  public void store_push_events_for_branch() {
    var project = db.components().insertPrivateProject().getProjectDto();
    db.components().insertProjectBranch(project, b -> b.setUuid("uuid_1"));

    protoIssueCache.newAppender()
      .append(createIssue("key1").setType(RuleType.VULNERABILITY)
        .setComponentUuid("cu1")
        .setComponentKey("ck1"))
      .append(createIssue("key2").setType(RuleType.VULNERABILITY)
        .setComponentUuid("cu2")
        .setComponentKey("ck2"))
      .close();

    when(pushEventFactory.raiseEventOnIssue(eq(project.getUuid()), any(DefaultIssue.class))).thenReturn(
      Optional.of(createPushEvent()),
      Optional.of(createPushEvent()));

    underTest.execute(mock(ComputationStep.Context.class));

    assertThat(db.countSql(db.getSession(), "SELECT count(uuid) FROM push_events")).isEqualTo(2);
  }

  @Test
  public void store_push_events_in_batches() {
    var appender = protoIssueCache.newAppender();

    IntStream.range(1, 252)
      .forEach(value -> {
        var defaultIssue = createIssue("key-" + value).setType(RuleType.VULNERABILITY)
          .setComponentUuid("cu" + value)
          .setComponentKey("ck" + value);
        appender.append(defaultIssue);
        when(pushEventFactory.raiseEventOnIssue(anyString(), eq(defaultIssue))).thenReturn(Optional.of(createPushEvent()));
      });

    appender.close();

    underTest.execute(mock(ComputationStep.Context.class));

    assertThat(db.countSql(db.getSession(), "SELECT count(uuid) FROM push_events")).isEqualTo(251);
  }

  private DefaultIssue createIssue(String key) {
    return new DefaultIssue()
      .setKey(key)
      .setProjectKey("p")
      .setStatus("OPEN")
      .setProjectUuid("project-uuid")
      .setComponentKey("c")
      .setRuleKey(RuleKey.of("r", "r"))
      .setCreationDate(new Date());
  }

  private PushEventDto createPushEvent() {
    return new PushEventDto().setProjectUuid("project-uuid").setName("event").setPayload("test".getBytes(UTF_8));
  }

  private void buildComponentTree() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1)
      .setUuid("uuid_1")
      .addChildren(ReportComponent.builder(Component.Type.FILE, 2)
        .setUuid("issue-component-uuid")
        .build())
      .addChildren(ReportComponent.builder(Component.Type.FILE, 3)
        .setUuid("location-component-uuid")
        .build())
      .build());
  }

}
