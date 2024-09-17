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
package org.sonar.server.component;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.Indexers.BranchEvent;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.server.es.Indexers.EntityEvent.DELETION;

public class ComponentCleanerServiceIT {

  private final System2 system2 = System2.INSTANCE;

  @Rule
  public final DbTester db = DbTester.create(system2);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final Indexers indexers = mock(Indexers.class);
  private final ComponentCleanerService underTest = new ComponentCleanerService(dbClient, indexers);

  @Test
  public void delete_project_from_db_and_index() {
    DbData data1 = insertProjectData();
    DbData data2 = insertProjectData();

    underTest.deleteEntity(dbSession, data1.project);

    assertNotExists(data1);
    assertExists(data2);
    verify(indexers).commitAndIndexEntities(any(), eq(List.of(data1.project)), eq(DELETION));
    verifyNoMoreInteractions(indexers);
  }

  @Test
  public void delete_list_of_projects_from_db_and_index() {
    DbData data1 = insertProjectData();
    DbData data2 = insertProjectData();
    DbData data3 = insertProjectData();

    underTest.delete(dbSession, asList(data1.project, data2.project));
    dbSession.commit();

    assertNotExists(data1);
    assertNotExists(data2);
    assertExists(data3);
    verify(indexers).commitAndIndexEntities(any(), eq(List.of(data1.project)), eq(DELETION));
    verify(indexers).commitAndIndexEntities(any(), eq(List.of(data2.project)), eq(DELETION));

    verifyNoMoreInteractions(indexers);
  }

  @Test
  public void delete_project_from_db() {
    ProjectData projectData1 = db.components().insertPublicProject();
    ComponentDto componentDto1 = projectData1.getMainBranchComponent();
    ProjectData projectData2 = db.components().insertPublicProject();
    ComponentDto componentDto2 = projectData2.getMainBranchComponent();

    underTest.deleteEntity(dbSession, projectData1.getProjectDto());
    dbSession.commit();

    assertNotExists(componentDto1);
    assertExists(componentDto2);
  }

  @Test
  public void fail_with_IAE_if_deleting_subview() {
    PortfolioDto portfolio = new PortfolioDto().setParentUuid("parent").setRootUuid("root").setUuid("uuid");
    assertThatThrownBy(() -> underTest.deleteEntity(dbSession, portfolio))
      .withFailMessage("Only projects can be deleted")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void delete_application_from_db_and_index() {
    DbData data1 = insertProjectData();
    DbData data2 = insertProjectData();
    DbData data3 = insertProjectData();
    ProjectData app1 = insertApplication(data2.project);
    ProjectData app2 = insertApplication(data3.project);

    underTest.deleteEntity(dbSession, app1.getProjectDto());
    dbSession.commit();

    assertProjectOrAppExists(app1.getProjectDto(), false);
    assertProjectOrAppExists(app2.getProjectDto(), true);
    assertExists(data1);
    assertExists(data2);
    assertExists(data3);
    verify(indexers).commitAndIndexEntities(any(), eq(List.of(app1.getProjectDto())), eq(DELETION));
    verifyNoMoreInteractions(indexers);
  }

  @Test
  public void delete_WhenDeletingPortfolio_ShouldDeleteComponents() {
    PortfolioDto portfolioDto1 = db.components().insertPrivatePortfolioDto();
    PortfolioDto portfolioDto2 = db.components().insertPrivatePortfolioDto();
    underTest.deleteEntity(dbSession, portfolioDto1);

    assertThat(dbClient.componentDao().selectByUuid(dbSession, portfolioDto1.getUuid())).isEmpty();
    assertThat(dbClient.componentDao().selectByUuid(dbSession, portfolioDto2.getUuid())).isPresent();

    verify(indexers).commitAndIndexEntities(any(), eq(List.of(portfolioDto1)), eq(DELETION));
    verifyNoMoreInteractions(indexers);
  }

  private ProjectData insertApplication(ProjectDto project) {
    ProjectData app = db.components().insertPublicApplication();
    db.components().addApplicationProject(app.getProjectDto(), project);
    return app;
  }

  @Test
  public void deleteBranch_whenDeletingNonMainBranch_shouldDeleteComponentsAndProjects() {
    DbData data1 = insertProjectData();
    DbData data2 = insertProjectData();
    DbData data3 = insertProjectData();

    BranchDto otherBranch = db.components().insertProjectBranch(data1.project);

    underTest.deleteBranch(dbSession, otherBranch);

    assertExists(data1);
    assertExists(data2);
    assertExists(data3);

    assertThat(dbClient.componentDao().selectByUuid(dbSession, otherBranch.getUuid())).isEmpty();
    assertThat(dbClient.branchDao().selectByUuid(dbSession, otherBranch.getUuid())).isEmpty();
    verify(indexers).commitAndIndexBranches(any(), eq(List.of(otherBranch)), eq(BranchEvent.DELETION));
    verifyNoMoreInteractions(indexers);
  }

  @Test
  public void deleteBranch_whenDeletingBiggestBranch_shouldUpdateProjectNcloc() {
    MetricDto metricNcloc = db.measures().insertMetric(m -> m.setKey("ncloc").setValueType(INT.toString()));
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    BranchDto biggestBranch = insertBranchWithNcloc(project, metricNcloc, 200d);
    insertBranchWithNcloc(project, metricNcloc, 10d);
    dbClient.projectDao().updateNcloc(dbSession, project.getUuid(), 200);

    assertThat(dbClient.projectDao().getNclocSum(db.getSession())).isEqualTo(200L);
    underTest.deleteBranch(dbSession, biggestBranch);
    assertThat(dbClient.projectDao().getNclocSum(db.getSession())).isEqualTo(10L);
  }

  private BranchDto insertBranchWithNcloc(ProjectDto project, MetricDto metricNcloc, double value) {
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));
    db.measures().insertMeasure(branch, m -> m.addValue(metricNcloc.getKey(), value));
    return branch;
  }

  @Test
  public void deleteBranch_whenMainBranch_shouldThrowIllegalArgumentException() {
    DbData data1 = insertProjectData();
    DbData data2 = insertProjectData();
    DbData data3 = insertProjectData();

    assertThatThrownBy(() -> underTest.deleteBranch(dbSession, data1.mainBranch)).isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Only non-main branches can be deleted");

    assertExists(data1);
    assertExists(data2);
    assertExists(data3);
    verifyNoMoreInteractions(indexers);
  }

  @Test
  public void delete_webhooks_from_projects() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    WebhookDto webhook1 = db.webhooks().insertWebhook(project1);
    db.webhookDelivery().insert(webhook1);
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    WebhookDto webhook2 = db.webhooks().insertWebhook(project2);
    db.webhookDelivery().insert(webhook2);
    ProjectDto projectNotToBeDeleted = db.components().insertPrivateProject().getProjectDto();
    WebhookDto webhook3 = db.webhooks().insertWebhook(projectNotToBeDeleted);
    db.webhookDelivery().insert(webhook3);

    ProjectDto projectDto1 = dbClient.projectDao().selectByUuid(dbSession, project1.getUuid()).get();
    ProjectDto projectDto2 = dbClient.projectDao().selectByUuid(dbSession, project2.getUuid()).get();

    underTest.delete(dbSession, asList(projectDto1, projectDto2));

    assertThat(db.countRowsOfTable(db.getSession(), "webhooks")).isOne();
    assertThat(db.countRowsOfTable(db.getSession(), "webhook_deliveries")).isOne();
  }

  private DbData insertProjectData() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();

    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, mainBranch, mainBranch);
    SnapshotDto analysis = db.components().insertSnapshot(mainBranch);
    return new DbData(projectData.getProjectDto(), projectData.getMainBranchDto(), analysis, issue);
  }

  private void assertNotExists(DbData data) {
    assertDataInDb(data, false);
  }

  private void assertExists(DbData data) {
    assertDataInDb(data, true);
  }

  private void assertDataInDb(DbData data, boolean exists) {
    assertProjectOrAppExists(data.project, exists);
    assertThat(dbClient.snapshotDao().selectByUuid(dbSession, data.snapshot.getUuid()).isPresent()).isEqualTo(exists);
    assertThat(dbClient.issueDao().selectByKey(dbSession, data.issue.getKey()).isPresent()).isEqualTo(exists);
  }

  private void assertProjectOrAppExists(ProjectDto appOrProject, boolean exists) {
    assertThat(dbClient.projectDao().selectByUuid(dbSession, appOrProject.getUuid()).isPresent()).isEqualTo(exists);
    assertThat(dbClient.branchDao().selectByProject(dbSession, appOrProject).isEmpty()).isEqualTo(!exists);
  }

  private void assertNotExists(ComponentDto componentDto) {
    assertComponentExists(componentDto, false);
  }

  private void assertExists(ComponentDto componentDto) {
    assertComponentExists(componentDto, true);
  }

  private void assertComponentExists(ComponentDto componentDto, boolean exists) {
    assertThat(dbClient.componentDao().selectByUuid(dbSession, componentDto.uuid()).isPresent()).isEqualTo(exists);
  }

  private static class DbData {
    final ProjectDto project;
    final BranchDto mainBranch;
    final SnapshotDto snapshot;
    final IssueDto issue;

    DbData(ProjectDto project, BranchDto mainBranch, SnapshotDto snapshot, IssueDto issue) {
      this.project = project;
      this.mainBranch = mainBranch;
      this.snapshot = snapshot;
      this.issue = issue;
    }
  }
}
