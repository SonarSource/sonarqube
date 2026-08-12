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
package org.sonar.server.v2.api.history.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.ServerUserSession;
import org.sonarsource.history.api.model.ProjectCollectionHistoryEntityType;
import org.sonarsource.history.model.ProjectBranch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;

class ProjectCollectionContextLoaderIT {

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private ComponentDto rootPortfolio;
  private ComponentDto childWithProject;
  private ComponentDto grandchildWithoutDirectProjects;
  private ComponentDto greatGrandchildWithProject;
  private ComponentDto siblingWithApplication;
  private ComponentDto emptyChild;
  private ComponentDto emptyGrandchild;
  private ProjectData rootProject;
  private ProjectData rootApplicationProject;
  private ProjectData childProject;
  private ProjectData deeplyNestedProject;
  private ProjectData nestedApplication;
  private ProjectData firstNestedApplicationProject;
  private ProjectData secondNestedApplicationProject;
  private ProjectData siblingApplicationProject;
  private ProjectData outsideProject;
  private ProjectData outsideApplicationProject;
  private ProjectCollectionContextLoader underTest;

  @BeforeEach
  void setUp() {
    rootPortfolio = db.components().insertPrivatePortfolio();
    childWithProject = insertSubportfolio(rootPortfolio);
    grandchildWithoutDirectProjects = insertSubportfolio(childWithProject);
    greatGrandchildWithProject = insertSubportfolio(grandchildWithoutDirectProjects);
    emptyGrandchild = insertSubportfolio(childWithProject);
    siblingWithApplication = insertSubportfolio(rootPortfolio);
    emptyChild = insertSubportfolio(rootPortfolio);

    rootProject = insertProject(rootPortfolio);
    rootApplicationProject = db.components().insertPrivateProject();
    ProjectData rootApplication = insertApplication(rootPortfolio, rootApplicationProject);
    childProject = insertProject(childWithProject);
    deeplyNestedProject = insertProject(greatGrandchildWithProject);
    firstNestedApplicationProject = db.components().insertPrivateProject();
    secondNestedApplicationProject = db.components().insertPrivateProject();
    nestedApplication = insertApplication(
      grandchildWithoutDirectProjects, firstNestedApplicationProject, secondNestedApplicationProject);
    siblingApplicationProject = db.components().insertPrivateProject();
    ProjectData siblingApplication = insertApplication(siblingWithApplication, siblingApplicationProject);
    ProjectData emptyApplication = insertApplication(emptyGrandchild);

    ComponentDto outsidePortfolio = db.components().insertPrivatePortfolio();
    outsideProject = insertProject(outsidePortfolio);
    outsideApplicationProject = db.components().insertPrivateProject();
    ProjectData outsideApplication = insertApplication(outsidePortfolio, outsideApplicationProject);

    UserDto user = db.users().insertUser();
    grantBrowsePermission(user, rootPortfolio, childWithProject, grandchildWithoutDirectProjects,
      greatGrandchildWithProject, siblingWithApplication, emptyGrandchild, emptyChild, outsidePortfolio,
      rootProject.getMainBranchComponent(), rootApplication.getMainBranchComponent(),
      rootApplicationProject.getMainBranchComponent(), childProject.getMainBranchComponent(),
      deeplyNestedProject.getMainBranchComponent(), nestedApplication.getMainBranchComponent(),
      firstNestedApplicationProject.getMainBranchComponent(), secondNestedApplicationProject.getMainBranchComponent(),
      siblingApplication.getMainBranchComponent(), siblingApplicationProject.getMainBranchComponent(),
      emptyApplication.getMainBranchComponent(), outsideProject.getMainBranchComponent(),
      outsideApplication.getMainBranchComponent(), outsideApplicationProject.getMainBranchComponent());
    underTest = new ProjectCollectionContextLoader(new ServerUserSession(db.getDbClient(), user, false), db.getDbClient());
  }

  @Test
  void loadRootPortfolioReturnsProjectsFromEveryDepthAsOneFlatList() {
    ProjectCollectionContext context = underTest.load(db.getSession(), rootPortfolio.uuid());

    assertThat(context.branches())
      .extracting(ProjectBranch::branchId)
      .containsExactlyInAnyOrder(rootProject.getMainBranchDto().getUuid(), rootApplicationProject.getMainBranchDto().getUuid(),
        childProject.getMainBranchDto().getUuid(),
        deeplyNestedProject.getMainBranchDto().getUuid(), firstNestedApplicationProject.getMainBranchDto().getUuid(),
        secondNestedApplicationProject.getMainBranchDto().getUuid(), siblingApplicationProject.getMainBranchDto().getUuid())
      .doesNotContain(outsideProject.getMainBranchDto().getUuid(), outsideApplicationProject.getMainBranchDto().getUuid());
  }

  @Test
  void loadApplicationReturnsOnlyProjectsBelongingToThatApplication() {
    ProjectCollectionContext applicationContext = underTest.load(
      db.getSession(), ProjectCollectionHistoryEntityType.APPLICATION, nestedApplication.getMainBranchDto().getUuid());

    assertThat(applicationContext.branches())
      .extracting(ProjectBranch::branchId)
      .containsExactlyInAnyOrder(firstNestedApplicationProject.getMainBranchDto().getUuid(),
        secondNestedApplicationProject.getMainBranchDto().getUuid())
      .doesNotContain(rootProject.getMainBranchDto().getUuid(), childProject.getMainBranchDto().getUuid(),
        rootApplicationProject.getMainBranchDto().getUuid(), deeplyNestedProject.getMainBranchDto().getUuid(),
        siblingApplicationProject.getMainBranchDto().getUuid(),
        outsideApplicationProject.getMainBranchDto().getUuid());
  }

  @Test
  void loadSelectedSubportfolioReturnsOnlyProjectsWithinItsNestedHierarchy() {
    ProjectCollectionContext childContext = underTest.load(db.getSession(), ProjectCollectionHistoryEntityType.PORTFOLIO, childWithProject.uuid());
    ProjectCollectionContext grandchildContext = underTest.load(db.getSession(), ProjectCollectionHistoryEntityType.PORTFOLIO, grandchildWithoutDirectProjects.uuid());

    assertThat(childContext.branches())
      .extracting(ProjectBranch::branchId)
      .containsExactlyInAnyOrder(childProject.getMainBranchDto().getUuid(), deeplyNestedProject.getMainBranchDto().getUuid(),
        firstNestedApplicationProject.getMainBranchDto().getUuid(), secondNestedApplicationProject.getMainBranchDto().getUuid())
      .doesNotContain(rootProject.getMainBranchDto().getUuid(), rootApplicationProject.getMainBranchDto().getUuid(),
        siblingApplicationProject.getMainBranchDto().getUuid(),
        outsideProject.getMainBranchDto().getUuid(), outsideApplicationProject.getMainBranchDto().getUuid());
    assertThat(grandchildContext.branches())
      .extracting(ProjectBranch::branchId)
      .containsExactlyInAnyOrder(deeplyNestedProject.getMainBranchDto().getUuid(),
        firstNestedApplicationProject.getMainBranchDto().getUuid(), secondNestedApplicationProject.getMainBranchDto().getUuid())
      .doesNotContain(childProject.getMainBranchDto().getUuid(), siblingApplicationProject.getMainBranchDto().getUuid());
  }

  @Test
  void loadSubportfolioWithoutProjectsReturnsAnEmptyFlatList() {
    ProjectCollectionContext emptyChildContext = underTest.load(db.getSession(), ProjectCollectionHistoryEntityType.PORTFOLIO, emptyChild.uuid());
    ProjectCollectionContext emptyGrandchildContext = underTest.load(db.getSession(), ProjectCollectionHistoryEntityType.PORTFOLIO, emptyGrandchild.uuid());

    assertThat(emptyChildContext.branches()).isEmpty();
    assertThat(emptyGrandchildContext.branches()).isEmpty();
  }

  private ComponentDto insertSubportfolio(ComponentDto parent) {
    ComponentDto child = db.components().insertSubportfolio(parent);
    db.components().addPortfolioReference(parent, child.uuid());
    return child;
  }

  private ProjectData insertProject(ComponentDto portfolio) {
    ProjectData project = db.components().insertPrivateProject();
    db.components().addPortfolioProject(portfolio, project.getProjectDto());
    db.components().insertComponent(newProjectCopy(project.getMainBranchComponent(), portfolio));
    return project;
  }

  private ProjectData insertApplication(ComponentDto portfolio, ProjectData... projects) {
    ProjectData application = db.components().insertPrivateApplication();
    db.components().addPortfolioReference(portfolio, application.projectUuid());
    ComponentDto applicationComponent = org.sonar.db.component.ComponentTesting.newSubPortfolio(portfolio)
      .setCopyComponentUuid(application.getMainBranchDto().getUuid());
    db.components().insertComponent(applicationComponent);
    for (ProjectData project : projects) {
      db.components().addApplicationProject(application, project);
      db.components().insertComponent(newProjectCopy(project.getMainBranchComponent(), applicationComponent));
    }
    return application;
  }

  private void grantBrowsePermission(UserDto user, ComponentDto... components) {
    for (ComponentDto component : components) {
      db.users().insertProjectPermissionOnUser(user, ProjectPermission.USER, component);
    }
  }
}
