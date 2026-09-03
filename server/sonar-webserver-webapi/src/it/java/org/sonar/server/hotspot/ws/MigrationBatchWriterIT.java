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
package org.sonar.server.hotspot.ws;

import java.util.Date;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.rule.RuleType;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class MigrationBatchWriterIT {

  private final System2 system2 = new TestSystem2().setNow(1_500_000_000_000L);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final DbClient dbClient = db.getDbClient();
  private final SequenceUuidFactory uuidFactory = new SequenceUuidFactory();
  private final IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private final IssueIndexer issueIndexer = mock(IssueIndexer.class);
  private final WebIssueStorage issueStorage = new WebIssueStorage(system2, dbClient,
    new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class)), issueIndexer, uuidFactory);
  private final TestIssueChangePostProcessor postProcessor = new TestIssueChangePostProcessor();

  private final MigrationBatchWriter underTest = new MigrationBatchWriter(dbClient, issueStorage, postProcessor,
    issueIndexer, uuidFactory, system2);

  @Test
  public void write_shouldPersistFieldChangesAndChangelog() {
    when(issueIndexer.enqueueForIndexing(any(), any())).thenReturn(List.of());
    Fixture f = newChangedHotspot();

    underTest.write(List.of(f.issue));

    IssueDto reloaded = dbClient.issueDao().selectOrFailByKey(db.getSession(), f.original.getKey());
    assertThat(reloaded.getType()).isEqualTo(RuleType.VULNERABILITY.getDbConstant());
    List<FieldDiffs> changelog = dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), f.original.getKey());
    assertThat(changelog).hasSize(1);
    assertThat(changelog.get(0).diffs()).containsKey("type");
  }

  @Test
  public void write_shouldRecomputeMeasuresForTouchedComponents() {
    when(issueIndexer.enqueueForIndexing(any(), any())).thenReturn(List.of());
    Fixture f = newChangedHotspot();

    underTest.write(List.of(f.issue));

    assertThat(postProcessor.wasCalled()).isTrue();
    assertThat(postProcessor.calledComponents()).extracting(ComponentDto::uuid).contains(f.file.uuid());
  }

  @Test
  public void write_shouldEnqueueForIndexingThenIndexAfterCommit() {
    List<EsQueueDto> esItems = List.of(EsQueueDto.create("issues", "some-issue-key"));
    when(issueIndexer.enqueueForIndexing(any(), any())).thenReturn(esItems);
    Fixture f = newChangedHotspot();

    underTest.write(List.of(f.issue));

    // Enqueue (in the transaction) must happen before the (post-commit) ES write of the same items.
    var inOrder = inOrder(issueIndexer);
    inOrder.verify(issueIndexer).enqueueForIndexing(any(), any());
    inOrder.verify(issueIndexer).index(any(), eq(esItems));
  }

  private Fixture newChangedHotspot() {
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    IssueDto original = db.issues().insert(vulnerabilityRule, branch, file, i -> i.setType(RuleType.SECURITY_HOTSPOT).setTags(List.of()));

    DefaultIssue issue = dbClient.issueDao().selectOrFailByKey(db.getSession(), original.getKey()).toDefaultIssue();
    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(system2.now()), "admin-uuid").build();
    issueFieldsSetter.setType(issue, RuleType.VULNERABILITY, context);
    return new Fixture(original, issue, file);
  }

  private record Fixture(IssueDto original, DefaultIssue issue, ComponentDto file) {
  }
}