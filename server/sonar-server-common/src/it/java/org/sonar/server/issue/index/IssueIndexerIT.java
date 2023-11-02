/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.assertj.core.api.Assertions;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.Indexers.EntityEvent;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.IndexPermissions;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.security.SecurityStandards.SQCategory;
import org.sonar.server.security.SecurityStandards.VulnerabilityProbability;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.server.es.Indexers.BranchEvent.DELETION;
import static org.sonar.server.es.Indexers.EntityEvent.PROJECT_KEY_UPDATE;
import static org.sonar.server.es.Indexers.EntityEvent.PROJECT_TAGS_UPDATE;
import static org.sonar.server.issue.IssueDocTesting.newDoc;
import static org.sonar.server.issue.index.IssueIndexDefinition.SUB_FIELD_SEVERITY;
import static org.sonar.server.issue.index.IssueIndexDefinition.SUB_FIELD_SOFTWARE_QUALITY;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_ISSUE;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.TYPE_AUTHORIZATION;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_POROUS_DEFENSES;

public class IssueIndexerIT {

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public LogTester logTester = new LogTester();

  private final IssueIndexer underTest = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()), null);

  @Test
  public void getIndexTypes_shouldReturnTypeIssue() {
    assertThat(underTest.getIndexTypes()).containsExactly(TYPE_ISSUE);
  }

  @Test
  public void getAuthorizationScope_shouldBeProject() {
    AuthorizationScope scope = underTest.getAuthorizationScope();
    assertThat(scope.getIndexType().getIndex()).isEqualTo(IssueIndexDefinition.DESCRIPTOR);
    assertThat(scope.getIndexType().getType()).isEqualTo(TYPE_AUTHORIZATION);

    Predicate<IndexPermissions> projectPredicate = scope.getEntityPredicate();
    IndexPermissions project = new IndexPermissions("P1", Qualifiers.PROJECT);
    IndexPermissions file = new IndexPermissions("F1", Qualifiers.FILE);
    assertThat(projectPredicate.test(project)).isTrue();
    assertThat(projectPredicate.test(file)).isFalse();
  }

  @Test
  public void indexAllIssues_shouldIndexAllIssues() {
    IssueDto issue1 = db.issues().insert();
    IssueDto issue2 = db.issues().insert();

    underTest.indexAllIssues();

    assertThatIndexHasOnly(issue1, issue2);
  }

  @Test
  public void indexAllIssues_shouldIndexAllIssueFields() {
    RuleDto rule = db.rules().insert();
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto dir = db.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java/foo"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, dir, "F1"));
    IssueDto issue = db.issues().insert(rule, project, file);

    underTest.indexAllIssues();

    IssueDoc doc = es.getDocuments(TYPE_ISSUE, IssueDoc.class).get(0);
    assertThat(doc.getId()).isEqualTo(issue.getKey());
    assertThat(doc.assigneeUuid()).isEqualTo(issue.getAssigneeUuid());
    assertThat(doc.authorLogin()).isEqualTo(issue.getAuthorLogin());
    assertThat(doc.componentUuid()).isEqualTo(file.uuid());
    assertThat(doc.projectUuid()).isEqualTo(projectData.projectUuid());
    assertThat(doc.branchUuid()).isEqualTo(project.uuid());
    assertThat(doc.isMainBranch()).isTrue();
    assertThat(doc.closeDate()).isEqualTo(issue.getIssueCloseDate());
    assertThat(doc.creationDate()).isEqualToIgnoringMillis(issue.getIssueCreationDate());
    assertThat(doc.directoryPath()).isEqualTo(dir.path());
    assertThat(doc.filePath()).isEqualTo(file.path());
    assertThat(doc.scope()).isEqualTo(IssueScope.MAIN);
    assertThat(doc.language()).isEqualTo(issue.getLanguage());
    assertThat(doc.line()).isEqualTo(issue.getLine());
    // functional date
    assertThat(doc.updateDate()).isEqualToIgnoringMillis(new Date(issue.getIssueUpdateTime()));
    assertThat(doc.getCwe()).containsExactlyInAnyOrder(SecurityStandards.UNKNOWN_STANDARD);
    assertThat(doc.getOwaspTop10()).isEmpty();
    assertThat(doc.getSansTop25()).isEmpty();
    assertThat(doc.getSonarSourceSecurityCategory()).isEqualTo(SQCategory.OTHERS);
    assertThat(doc.getVulnerabilityProbability()).isEqualTo(VulnerabilityProbability.LOW);
    assertThat(doc.impacts())
      .containsExactlyInAnyOrder(Map.of(
        SUB_FIELD_SOFTWARE_QUALITY, SoftwareQuality.MAINTAINABILITY.name(),
        SUB_FIELD_SEVERITY, Severity.HIGH.name()));
    assertThat(doc.simpleStatus()).isEqualTo(issue.getSimpleStatus().name());
  }

  @Test
  public void indexAllIssues_shouldIndexSecurityStandards() {
    RuleDto rule = db.rules().insert(r -> r.setSecurityStandards(new HashSet<>(Arrays.asList("cwe:123", "owaspTop10:a3", "cwe:863", "owaspAsvs-4.0:2.1.1"))));
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto dir = db.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java/foo"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, dir, "F1"));
    db.issues().insert(rule, project, file);

    underTest.indexAllIssues();

    IssueDoc doc = es.getDocuments(TYPE_ISSUE, IssueDoc.class).get(0);
    assertThat(doc.getCwe()).containsExactlyInAnyOrder("123", "863");
    assertThat(doc.getOwaspTop10()).containsExactlyInAnyOrder("a3");
    assertThat(doc.getOwaspAsvs40()).containsExactlyInAnyOrder("2.1.1");
    assertThat(doc.getSansTop25()).containsExactlyInAnyOrder(SANS_TOP_25_POROUS_DEFENSES);
  }

  @Test
  public void indexOnStartup_does_not_fail_on_errors_and_does_enable_recovery_mode() {
    es.lockWrites(TYPE_ISSUE);
    db.issues().insert();

    Set<IndexType> uninitializedIndexTypes = emptySet();
    assertThatThrownBy(() -> underTest.indexOnStartup(uninitializedIndexTypes))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("SYNCHRONOUS StartupIndexer must implement indexOnStartup");
    assertThatIndexHasSize(0);
    assertThatEsQueueTableHasSize(0);
    es.unlockWrites(TYPE_ISSUE);
  }

  @Test
  public void indexOnAnalysis_indexes_the_issues_of_project() {
    RuleDto rule = db.rules().insert();
    ComponentDto branchComponent = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branchComponent));
    IssueDto issue = db.issues().insert(rule, branchComponent, file);
    ComponentDto otherProject = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertComponent(newFileDto(otherProject));

    underTest.indexOnAnalysis(branchComponent.uuid());

    assertThatIndexHasOnly(issue);
  }

  @Test
  public void indexOnAnalysis_does_not_delete_orphan_docs() {
    RuleDto rule = db.rules().insert();
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()));
    IssueDto issue = db.issues().insert(rule, projectData.getMainBranchComponent(), file);

    // orphan in the project
    addIssueToIndex(projectData.projectUuid(), projectData.getMainBranchComponent().uuid(), "orphan");

    underTest.indexOnAnalysis(projectData.getMainBranchComponent().uuid());

    assertThat(es.getDocuments(TYPE_ISSUE))
      .extracting(SearchHit::getId)
      .containsExactlyInAnyOrder(issue.getKey(), "orphan");
  }

  /**
   * Indexing recovery is handled by Compute Engine, without using
   * the table es_queue
   */
  @Test
  public void indexOnAnalysis_does_not_fail_on_errors_and_does_not_enable_recovery_mode() {
    es.lockWrites(TYPE_ISSUE);
    IssueDto issue = db.issues().insert();

    String projectUuid = issue.getProjectUuid();
    assertThatThrownBy(() -> underTest.indexOnAnalysis(projectUuid))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Unrecoverable indexation failures: 1 errors among 1 requests. Check Elasticsearch logs for further details.");
    assertThatIndexHasSize(0);
    assertThatEsQueueTableHasSize(0);
    es.unlockWrites(TYPE_ISSUE);
  }

  @Test
  public void index_is_not_updated_when_updating_project_key() {
    // issue is inserted to verify that indexing of project is not triggered
    IssueDto issue = db.issues().insert();

    IndexingResult result = indexProject(issue.getProjectUuid(), PROJECT_KEY_UPDATE);
    assertThat(result.getTotal()).isZero();
    assertThatIndexHasSize(0);
  }

  @Test
  public void index_is_not_updated_when_updating_tags() {
    // issue is inserted to verify that indexing of project is not triggered
    IssueDto issue = db.issues().insert();

    IndexingResult result = indexProject(issue.getProjectUuid(), PROJECT_TAGS_UPDATE);
    assertThat(result.getTotal()).isZero();
    assertThatIndexHasSize(0);
  }

  @Test
  public void index_is_updated_when_deleting_branch() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch1 = db.components().insertProjectBranch(project);
    BranchDto branch2 = db.components().insertProjectBranch(project);

    addIssueToIndex(project.getUuid(), branch1.getUuid(), "I1");
    addIssueToIndex(project.getUuid(), branch1.getUuid(), "I2");
    addIssueToIndex(project.getUuid(), branch2.getUuid(), "I3");

    assertThatIndexHasSize(3);

    IndexingResult result = indexBranches(List.of(branch1.getUuid()), DELETION);

    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getSuccess()).isEqualTo(2);
    assertThatIndexHasOnly("I3");
  }

  @Test
  public void index_is_updated_when_deleting_project() {
    BranchDto branch = db.components().insertPrivateProject().getMainBranchDto();
    addIssueToIndex(branch.getProjectUuid(), branch.getUuid(), "I1");
    assertThatIndexHasSize(1);

    IndexingResult result = indexProject(branch.getProjectUuid(), EntityEvent.DELETION);

    assertThat(result.getTotal()).isOne();
    assertThat(result.getSuccess()).isOne();
    assertThatIndexHasSize(0);
  }

  @Test
  public void errors_during_project_deletion_are_recovered() {
    addIssueToIndex("P1", "B1", "I1");
    addIssueToIndex("P1", "B2", "I2");

    assertThatIndexHasSize(2);
    es.lockWrites(TYPE_ISSUE);

    IndexingResult result = indexProject("P1", EntityEvent.DELETION);
    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getFailures()).isEqualTo(2);

    // index is still read-only, fail to recover
    result = recover();
    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getFailures()).isEqualTo(2);
    assertThatIndexHasSize(2);

    es.unlockWrites(TYPE_ISSUE);

    result = recover();
    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getFailures()).isZero();
    assertThatIndexHasSize(0);
  }

  @Test
  public void commitAndIndexIssues_commits_db_transaction_and_adds_issues_to_index() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    // insert issues in db without committing
    IssueDto issue1 = IssueTesting.newIssue(rule, project, file);
    IssueDto issue2 = IssueTesting.newIssue(rule, project, file);
    db.getDbClient().issueDao().insert(db.getSession(), issue1, issue2);

    underTest.commitAndIndexIssues(db.getSession(), asList(issue1, issue2));

    // issues are persisted and indexed
    assertThatIndexHasOnly(issue1, issue2);
    assertThatDbHasOnly(issue1, issue2);
    assertThatEsQueueTableHasSize(0);
  }

  @Test
  public void commitAndIndexIssues_removes_issue_from_index_if_it_does_not_exist_in_db() {
    IssueDto issue1 = new IssueDto().setKee("I1").setProjectUuid("B1");
    addIssueToIndex("B1", issue1.getProjectUuid(), issue1.getKey());
    IssueDto issue2 = db.issues().insert();

    underTest.commitAndIndexIssues(db.getSession(), asList(issue1, issue2));

    // issue1 is removed from index, issue2 is persisted and indexed
    assertThatIndexHasOnly(issue2);
    assertThatDbHasOnly(issue2);
    assertThatEsQueueTableHasSize(0);
  }

  @Test
  public void indexing_errors_during_commitAndIndexIssues_are_recovered() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    // insert issues in db without committing
    IssueDto issue1 = IssueTesting.newIssue(rule, project, file);
    IssueDto issue2 = IssueTesting.newIssue(rule, project, file);
    db.getDbClient().issueDao().insert(db.getSession(), issue1, issue2);

    // index is read-only
    es.lockWrites(TYPE_ISSUE);

    underTest.commitAndIndexIssues(db.getSession(), asList(issue1, issue2));

    // issues are persisted but not indexed
    assertThatIndexHasSize(0);
    assertThatDbHasOnly(issue1, issue2);
    assertThatEsQueueTableHasSize(2);

    // re-enable write on index
    es.unlockWrites(TYPE_ISSUE);

    // emulate the recovery daemon
    IndexingResult result = recover();

    assertThatEsQueueTableHasSize(0);
    assertThatIndexHasOnly(issue1, issue2);
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getTotal()).isEqualTo(2L);
  }

  @Test
  public void recovery_does_not_fail_if_unsupported_docIdType() {
    EsQueueDto item = EsQueueDto.create(TYPE_ISSUE.format(), "I1", "unknown", "P1");
    db.getDbClient().esQueueDao().insert(db.getSession(), item);
    db.commit();

    recover();

    assertThat(logTester.logs(Level.ERROR))
      .filteredOn(l -> l.contains("Unsupported es_queue.doc_id_type for issues. Manual fix is required: "))
      .hasSize(1);
    assertThatEsQueueTableHasSize(1);
  }

  @Test
  public void indexing_recovers_multiple_errors_on_the_same_issue() {
    es.lockWrites(TYPE_ISSUE);
    IssueDto issue = db.issues().insert();

    // three changes on the same issue
    underTest.commitAndIndexIssues(db.getSession(), singletonList(issue));
    underTest.commitAndIndexIssues(db.getSession(), singletonList(issue));
    underTest.commitAndIndexIssues(db.getSession(), singletonList(issue));

    assertThatIndexHasSize(0);
    // three attempts of indexing are stored in es_queue recovery table
    assertThatEsQueueTableHasSize(3);

    es.unlockWrites(TYPE_ISSUE);
    recover();

    assertThatIndexHasOnly(issue);
    assertThatEsQueueTableHasSize(0);
  }

  @Test
  public void indexing_recovers_multiple_errors_on_the_same_project() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    db.issues().insert(rule, project, file);
    db.issues().insert(rule, project, file);

    es.lockWrites(TYPE_ISSUE);

    IndexingResult result = indexBranches(List.of(project.uuid()), DELETION);
    assertThat(result.getTotal()).isEqualTo(2L);
    assertThat(result.getFailures()).isEqualTo(2L);

    // index is still read-only, fail to recover
    result = recover();
    assertThat(result.getTotal()).isEqualTo(2L);
    assertThat(result.getFailures()).isEqualTo(2L);
    assertThatIndexHasSize(0);

    es.unlockWrites(TYPE_ISSUE);

    result = recover();
    assertThat(result.getTotal()).isEqualTo(2L);
    assertThat(result.getFailures()).isZero();
    assertThatIndexHasSize(2);
    assertThatEsQueueTableHasSize(0);
  }

  private IndexingResult indexBranches(List<String> branchUuids, Indexers.BranchEvent cause) {
    Collection<EsQueueDto> items = underTest.prepareForRecoveryOnBranchEvent(db.getSession(), branchUuids, cause);
    db.commit();
    return underTest.index(db.getSession(), items);
  }

  private IndexingResult indexProject(String projectUuid, EntityEvent cause) {
    Collection<EsQueueDto> items = underTest.prepareForRecoveryOnEntityEvent(db.getSession(), singletonList(projectUuid), cause);
    db.commit();
    return underTest.index(db.getSession(), items);
  }

  @Test
  public void deleteByKeys_shouldDeleteDocsByKeys() {
    addIssueToIndex("P1", "B1", "Issue1");
    addIssueToIndex("P1", "B1", "Issue2");
    addIssueToIndex("P1", "B1", "Issue3");
    addIssueToIndex("P2", "B2", "Issue4");

    assertThatIndexHasOnly("Issue1", "Issue2", "Issue3", "Issue4");

    underTest.deleteByKeys("P1", asList("Issue1", "Issue2"));

    assertThatIndexHasOnly("Issue3", "Issue4");
  }

  @Test
  public void deleteByKeys_shouldNotRecoverFromErrors() {
    addIssueToIndex("P1", "B1", "Issue1");
    es.lockWrites(TYPE_ISSUE);

    List<String> issues = List.of("Issue1");
    assertThatThrownBy(() -> underTest.deleteByKeys("P1", issues))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Unrecoverable indexation failures: 1 errors among 1 requests. Check Elasticsearch logs for further details.");
    assertThatIndexHasOnly("Issue1");
    assertThatEsQueueTableHasSize(0);
    es.unlockWrites(TYPE_ISSUE);
  }

  @Test
  public void deleteByKeys_whenEmptyList_shouldDoNothing() {
    addIssueToIndex("P1", "B1", "Issue1");
    addIssueToIndex("P1", "B1", "Issue2");
    addIssueToIndex("P1", "B1", "Issue3");

    underTest.deleteByKeys("B1", emptyList());

    assertThatIndexHasOnly("Issue1", "Issue2", "Issue3");
  }

  /**
   * This is a technical constraint, to ensure, that the indexers can be called in any order, during startup.
   */
  @Test
  public void parent_child_relationship_does_not_require_ordering_of_index_requests() {
    IssueDoc issueDoc = new IssueDoc();
    issueDoc.setKey("key");
    issueDoc.setProjectUuid("parent-does-not-exist");
    new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()), null)
      .index(singletonList(issueDoc).iterator());

    assertThat(es.countDocuments(TYPE_ISSUE)).isOne();
  }

  @Test
  public void index_issue_in_non_main_branch() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));
    BranchDto branchDto = db.getDbClient().branchDao().selectByUuid(db.getSession(), branch.uuid()).orElseThrow();
    ComponentDto dir = db.components().insertComponent(ComponentTesting.newDirectory(branch, "src/main/java/foo"));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, dir, "F1"));
    IssueDto issue = db.issues().insert(rule, branch, file);

    underTest.indexAllIssues();

    IssueDoc doc = es.getDocuments(TYPE_ISSUE, IssueDoc.class).get(0);
    assertThat(doc.getId()).isEqualTo(issue.getKey());
    assertThat(doc.componentUuid()).isEqualTo(file.uuid());
    assertThat(doc.projectUuid()).isEqualTo(branchDto.getProjectUuid());
    assertThat(doc.branchUuid()).isEqualTo(branch.uuid());
    assertThat(doc.isMainBranch()).isFalse();
    assertThat(doc.scope()).isEqualTo(IssueScope.MAIN);
  }

  @Test
  public void indexIssue_whenSwitchMainBranch_shouldIndexIsMainBranch() {
    RuleDto rule = db.rules().insert();
    ProjectData projectData = db.components().insertPrivateProject();
    BranchDto mainBranchDto = projectData.getMainBranchDto();
    ComponentDto mainBranchComponent = projectData.getMainBranchComponent();
    BranchDto newMainBranchDto = db.components().insertProjectBranch(projectData.getProjectDto(), b -> b.setKey("newMainBranch"));
    ComponentDto newMainBranchComponent = db.components().getComponentDto(newMainBranchDto);
    IssueDto issue1 = createIssue(rule, mainBranchComponent);
    IssueDto issue2 = createIssue(rule, newMainBranchComponent);
    underTest.indexAllIssues();
    assertThat(es.getDocuments(TYPE_ISSUE, IssueDoc.class)).extracting(IssueDoc::branchUuid, IssueDoc::isMainBranch)
      .containsExactlyInAnyOrder(tuple(issue1.getProjectUuid(), true), tuple(issue2.getProjectUuid(), false));

    db.getDbClient().branchDao().updateIsMain(db.getSession(), projectData.getMainBranchDto().getUuid(), false);
    db.getDbClient().branchDao().updateIsMain(db.getSession(), newMainBranchDto.getUuid(), true);
    indexBranches(List.of(mainBranchDto.getUuid(), newMainBranchDto.getUuid()), Indexers.BranchEvent.SWITCH_OF_MAIN_BRANCH);

    assertThat(es.getDocuments(TYPE_ISSUE, IssueDoc.class)).extracting(IssueDoc::branchUuid, IssueDoc::isMainBranch)
      .containsExactlyInAnyOrder(tuple(issue1.getProjectUuid(), false), tuple(issue2.getProjectUuid(), true));
  }

  private IssueDto createIssue(RuleDto rule, ComponentDto branch) {
    ComponentDto dir2 = db.components().insertComponent(ComponentTesting.newDirectory(branch, "src/main/java/foo"));
    ComponentDto file2 = db.components().insertComponent(newFileDto(branch, dir2));
    return db.issues().insert(rule, branch, file2);
  }

  @Test
  public void issue_on_test_file_has_test_scope() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto dir = db.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java/foo"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, dir, "F1").setQualifier("UTS"));
    IssueDto issue = db.issues().insert(rule, project, file);

    underTest.indexAllIssues();

    IssueDoc doc = es.getDocuments(TYPE_ISSUE, IssueDoc.class).get(0);
    assertThat(doc.getId()).isEqualTo(issue.getKey());
    assertThat(doc.componentUuid()).isEqualTo(file.uuid());
    assertThat(doc.scope()).isEqualTo(IssueScope.TEST);
  }

  @Test
  public void issue_on_directory_has_main_code_scope() {
    RuleDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto dir = db.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java/foo"));
    IssueDto issue = db.issues().insert(rule, project, dir);

    underTest.indexAllIssues();

    IssueDoc doc = es.getDocuments(TYPE_ISSUE, IssueDoc.class).get(0);
    assertThat(doc.getId()).isEqualTo(issue.getKey());
    assertThat(doc.componentUuid()).isEqualTo(dir.uuid());
    assertThat(doc.scope()).isEqualTo(IssueScope.MAIN);
  }

  @Test
  public void issue_on_project_has_main_code_scope() {
    RuleDto rule = db.rules().insert();
    ComponentDto mainBranchComponent = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto issue = db.issues().insert(rule, mainBranchComponent, mainBranchComponent);

    underTest.indexAllIssues();

    IssueDoc doc = es.getDocuments(TYPE_ISSUE, IssueDoc.class).get(0);
    assertThat(doc.getId()).isEqualTo(issue.getKey());
    assertThat(doc.componentUuid()).isEqualTo(mainBranchComponent.uuid());
    assertThat(doc.scope()).isEqualTo(IssueScope.MAIN);
  }

  @Test
  public void getType() {
    Assertions.assertThat(underTest.getType()).isEqualTo(StartupIndexer.Type.ASYNCHRONOUS);
  }

  @Test
  public void indexOnAnalysis_whenChangedComponents_shouldReindexOnlyChangedComponents() {
    RuleDto rule = db.rules().insert();
    ComponentDto mainBranchComponent = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto changedComponent1 = db.components().insertComponent(newFileDto(mainBranchComponent));
    ComponentDto unchangedComponent = db.components().insertComponent(newFileDto(mainBranchComponent));
    ComponentDto ChangedComponent2 = db.components().insertComponent(newFileDto(mainBranchComponent));
    IssueDto changedIssue1 = db.issues().insert(rule, mainBranchComponent, changedComponent1);
    IssueDto changedIssue2 = db.issues().insert(rule, mainBranchComponent, changedComponent1);
    IssueDto changedIssue3 = db.issues().insert(rule, mainBranchComponent, ChangedComponent2);
    db.issues().insert(rule, mainBranchComponent, unchangedComponent);
    db.issues().insert(rule, mainBranchComponent, unchangedComponent);

    underTest.indexOnAnalysis(mainBranchComponent.uuid(), Set.of(unchangedComponent.uuid()));

    assertThatIndexHasOnly(changedIssue1, changedIssue2, changedIssue3);
  }

  @Test
  public void indexOnAnalysis_whenEmptyUnchangedComponents_shouldReindexEverything() {
    RuleDto rule = db.rules().insert();
    ComponentDto mainBranchComponent = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto changedComponent = db.components().insertComponent(newFileDto(mainBranchComponent));
    IssueDto changedIssue1 = db.issues().insert(rule, mainBranchComponent, changedComponent);
    IssueDto changedIssue2 = db.issues().insert(rule, mainBranchComponent, changedComponent);

    underTest.indexOnAnalysis(mainBranchComponent.uuid(), Set.of());

    assertThatIndexHasOnly(changedIssue1, changedIssue2);
  }

  @Test
  public void indexOnAnalysis_whenChangedComponentWithoutIssue_shouldReindexNothing() {
    db.rules().insert();
    ComponentDto mainBranchComponent = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertComponent(newFileDto(mainBranchComponent));

    underTest.indexOnAnalysis(mainBranchComponent.uuid(), Set.of());

    assertThat(es.getDocuments(TYPE_ISSUE)).isEmpty();
  }

  private void addIssueToIndex(String projectUuid, String branchUuid, String issueKey) {
    es.putDocuments(TYPE_ISSUE,
      newDoc().setKey(issueKey).setProjectUuid(projectUuid).setBranchUuid(branchUuid));
  }

  private void assertThatIndexHasSize(long expectedSize) {
    assertThat(es.countDocuments(TYPE_ISSUE)).isEqualTo(expectedSize);
  }

  private void assertThatIndexHasOnly(IssueDto... expectedIssues) {
    assertThat(es.getDocuments(TYPE_ISSUE))
      .extracting(SearchHit::getId)
      .containsExactlyInAnyOrder(Arrays.stream(expectedIssues).map(IssueDto::getKey).toArray(String[]::new));
  }

  private void assertThatIndexHasOnly(String... expectedKeys) {
    List<IssueDoc> issues = es.getDocuments(TYPE_ISSUE, IssueDoc.class);
    assertThat(issues).extracting(IssueDoc::key).containsOnly(expectedKeys);
  }

  private void assertThatEsQueueTableHasSize(int expectedSize) {
    assertThat(db.countRowsOfTable("es_queue")).isEqualTo(expectedSize);
  }

  private void assertThatDbHasOnly(IssueDto... expectedIssues) {
    try (DbSession otherSession = db.getDbClient().openSession(false)) {
      List<String> keys = Arrays.stream(expectedIssues).map(IssueDto::getKey).toList();
      assertThat(db.getDbClient().issueDao().selectByKeys(otherSession, keys)).hasSize(expectedIssues.length);
    }
  }

  private IndexingResult recover() {
    Collection<EsQueueDto> items = db.getDbClient().esQueueDao().selectForRecovery(db.getSession(), System.currentTimeMillis() + 1_000L, 10);
    return underTest.index(db.getSession(), items);
  }
}
