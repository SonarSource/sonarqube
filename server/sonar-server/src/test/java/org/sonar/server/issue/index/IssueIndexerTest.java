/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexer;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.server.issue.IssueDocTesting.newDoc;

public class IssueIndexerTest {

  private static final String A_PROJECT_UUID = "P1";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings()));
  @Rule
  public DbTester dbTester = DbTester.create(system2);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private IssueIndexer underTest = new IssueIndexer(esTester.client(), new IssueIteratorFactory(dbTester.getDbClient()));

  @Test
  public void index_on_startup() {
    IssueIndexer indexer = spy(underTest);
    doNothing().when(indexer).indexOnStartup(null);
    indexer.indexOnStartup(null);
    verify(indexer).indexOnStartup(null);
  }

  @Test
  public void index_nothing() {
    underTest.index(Collections.emptyIterator());

    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE)).isEqualTo(0L);
  }

  @Test
  public void indexOnStartup_loads_and_indexes_all_issues() {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(org);
    ComponentDto dir = dbTester.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java/foo"));
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project, dir, "F1"));
    RuleDto rule = dbTester.rules().insertRule();
    IssueDto issue = dbTester.issues().insertIssue(IssueTesting.newDto(rule, file, project));

    underTest.indexOnStartup(null);

    List<IssueDoc> docs = esTester.getDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE, IssueDoc.class);
    assertThat(docs).hasSize(1);
    verifyDoc(docs.get(0), org, project, file, rule, issue);
  }

  @Test
  public void index_loads_and_indexes_issues_with_specified_keys() {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(org);
    ComponentDto dir = dbTester.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java/foo"));
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project, dir, "F1"));
    RuleDto rule = dbTester.rules().insertRule();
    IssueDto issue1 = dbTester.issues().insertIssue(IssueTesting.newDto(rule, file, project));
    IssueDto issue2 = dbTester.issues().insertIssue(IssueTesting.newDto(rule, file, project));

    underTest.index(asList(issue1.getKey()));

    List<IssueDoc> docs = esTester.getDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE, IssueDoc.class);
    assertThat(docs).hasSize(1);
    verifyDoc(docs.get(0), org, project, file, rule, issue1);
  }

  @Test
  public void index_throws_NoSuchElementException_if_the_specified_key_does_not_exist() {
    try {
      underTest.index(asList("does_not_exist"));
      fail();
    } catch (NoSuchElementException e) {
      assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE)).isEqualTo(0);
    }
  }

  @Test
  public void indexProject_loads_and_indexes_issues_with_specified_project_uuid() {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto project1 = dbTester.components().insertPrivateProject(org);
    ComponentDto file1 = dbTester.components().insertComponent(ComponentTesting.newFileDto(project1));
    ComponentDto project2 = dbTester.components().insertPrivateProject(org);
    ComponentDto file2 = dbTester.components().insertComponent(ComponentTesting.newFileDto(project2));
    RuleDto rule = dbTester.rules().insertRule();
    IssueDto issue1 = dbTester.issues().insertIssue(IssueTesting.newDto(rule, file1, project1));
    IssueDto issue2 = dbTester.issues().insertIssue(IssueTesting.newDto(rule, file2, project2));

    underTest.indexProject(project1.projectUuid(), ProjectIndexer.Cause.NEW_ANALYSIS);

    List<IssueDoc> docs = esTester.getDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE, IssueDoc.class);
    assertThat(docs).hasSize(1);
    verifyDoc(docs.get(0), org, project1, file1, rule, issue1);
  }

  @Test
  public void indexProject_does_nothing_when_project_is_being_created() {
    verifyThatProjectIsNotIndexed(ProjectIndexer.Cause.PROJECT_CREATION);
  }

  @Test
  public void indexProject_does_nothing_when_project_is_being_renamed() {
    verifyThatProjectIsNotIndexed(ProjectIndexer.Cause.PROJECT_KEY_UPDATE);
  }

  private void verifyThatProjectIsNotIndexed(ProjectIndexer.Cause cause) {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(org);
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDto rule = dbTester.rules().insertRule();
    IssueDto issue = dbTester.issues().insertIssue(IssueTesting.newDto(rule, file, project));

    underTest.indexProject(project.projectUuid(), cause);

    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE)).isEqualTo(0);
  }

  private static void verifyDoc(IssueDoc doc, OrganizationDto org, ComponentDto project, ComponentDto file, RuleDto rule, IssueDto issue) {
    assertThat(doc.key()).isEqualTo(issue.getKey());
    assertThat(doc.projectUuid()).isEqualTo(project.uuid());
    assertThat(doc.componentUuid()).isEqualTo(file.uuid());
    assertThat(doc.moduleUuid()).isEqualTo(project.uuid());
    assertThat(doc.modulePath()).isEqualTo(file.moduleUuidPath());
    assertThat(doc.directoryPath()).isEqualTo(StringUtils.substringBeforeLast(file.path(), "/"));
    assertThat(doc.severity()).isEqualTo(issue.getSeverity());
    assertThat(doc.ruleKey()).isEqualTo(rule.getKey());
    assertThat(doc.organizationUuid()).isEqualTo(org.getUuid());
    // functional date
    assertThat(doc.updateDate().getTime()).isEqualTo(issue.getIssueUpdateTime());
    // technical date
    assertThat(doc.getTechnicalUpdateDate().getTime()).isEqualTo(issue.getUpdatedAt());
  }

  @Test
  public void deleteProject_deletes_issues_of_a_specific_project() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    underTest.indexOnStartup(null);

    assertThat(esTester.countDocuments("issues", "issue")).isEqualTo(1);

    underTest.deleteProject("THE_PROJECT");

    assertThat(esTester.countDocuments("issues", "issue")).isZero();
  }

  @Test
  public void deleteByKeys_deletes_docs_by_keys() throws Exception {
    addIssue("P1", "Issue1");
    addIssue("P1", "Issue2");
    addIssue("P1", "Issue3");
    addIssue("P2", "Issue4");

    verifyIssueKeys("Issue1", "Issue2", "Issue3", "Issue4");

    underTest.deleteByKeys("P1", asList("Issue1", "Issue2"));

    verifyIssueKeys("Issue3", "Issue4");
  }

  @Test
  public void deleteByKeys_deletes_more_than_one_thousand_issues_by_keys() throws Exception {
    int numberOfIssues = 1010;
    List<String> keys = new ArrayList<>(numberOfIssues);
    IssueDoc[] issueDocs = new IssueDoc[numberOfIssues];
    for (int i = 0; i < numberOfIssues; i++) {
      String key = "Issue" + i;
      issueDocs[i] = newDoc().setKey(key).setProjectUuid(A_PROJECT_UUID);
      keys.add(key);
    }
    esTester.putDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE, issueDocs);

    assertThat(esTester.countDocuments("issues", "issue")).isEqualTo(numberOfIssues);
    underTest.deleteByKeys(A_PROJECT_UUID, keys);
    assertThat(esTester.countDocuments("issues", "issue")).isZero();
  }

  @Test
  public void nothing_to_do_when_delete_issues_on_empty_list() throws Exception {
    addIssue("P1", "Issue1");
    addIssue("P1", "Issue2");
    addIssue("P1", "Issue3");

    verifyIssueKeys("Issue1", "Issue2", "Issue3");

    underTest.deleteByKeys("P1", Collections.emptyList());

    verifyIssueKeys("Issue1", "Issue2", "Issue3");
  }

  /**
   * This is a technical constraint, to ensure, that the indexers can be called in any order, during startup.
   */
  @Test
  public void index_issue_without_parent_should_work() {
    IssueDoc issueDoc = new IssueDoc();
    issueDoc.setKey("key");
    issueDoc.setTechnicalUpdateDate(new Date());
    issueDoc.setProjectUuid("non-exitsing-parent");
    new IssueIndexer(esTester.client(), new IssueIteratorFactory(dbTester.getDbClient()))
      .index(asList(issueDoc).iterator());

    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE)).isEqualTo(1L);
  }

  private void addIssue(String projectUuid, String issueKey) throws Exception {
    esTester.putDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE,
      newDoc().setKey(issueKey).setProjectUuid(projectUuid));
  }

  private void verifyIssueKeys(String... expectedKeys) {
    List<IssueDoc> issues = esTester.getDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE, IssueDoc.class);
    assertThat(issues).extracting(IssueDoc::key).containsOnly(expectedKeys);
  }
}
