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
package org.sonar.server.qualitygate;

import com.google.common.base.Function;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.ProjectQgateAssociation;
import org.sonar.db.qualitygate.ProjectQgateAssociationQuery;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualitygate.QgateProjectFinder.Association;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.FluentIterable.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.qualitygate.ProjectQgateAssociationQuery.IN;
import static org.sonar.db.qualitygate.ProjectQgateAssociationQuery.OUT;
import static org.sonar.db.qualitygate.ProjectQgateAssociationQuery.builder;
import static org.sonar.server.qualitygate.QualityGates.SONAR_QUALITYGATE_PROPERTY;

public class QgateProjectFinderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);
  private QualityGateDto qGate;
  private QgateProjectFinder underTest = new QgateProjectFinder(dbClient, userSession);

  @Before
  public void setUp() throws Exception {
    qGate = new QualityGateDto().setName("Default Quality Gate");
    dbClient.qualityGateDao().insert(dbSession, qGate);
    dbTester.commit();
  }

  @Test
  public void return_empty_association() throws Exception {
    Association result = underTest.find(
      builder()
        .gateId(Long.toString(qGate.getId()))
        .build());

    assertThat(result.projects()).isEmpty();
  }

  @Test
  public void return_all_projects() throws Exception {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto associatedProject = insertProject(ComponentTesting.newPublicProjectDto(org));
    ComponentDto unassociatedProject = insertProject(ComponentTesting.newPublicProjectDto(org));
    associateProjectToQualitGate(associatedProject.getId());

    Association result = underTest.find(
      builder()
        .gateId(Long.toString(qGate.getId()))
        .build());

    Map<Long, ProjectQgateAssociation> projectsById = projectsById(result.projects());
    assertThat(projectsById).hasSize(2);

    verifyProject(projectsById.get(associatedProject.getId()), true, associatedProject.name());
    verifyProject(projectsById.get(unassociatedProject.getId()), false, unassociatedProject.name());
  }

  @Test
  public void return_only_associated_project() throws Exception {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto associatedProject = insertProject(ComponentTesting.newPublicProjectDto(org));
    insertProject(ComponentTesting.newPublicProjectDto(org));
    associateProjectToQualitGate(associatedProject.getId());

    Association result = underTest.find(
      builder()
        .membership(IN)
        .gateId(Long.toString(qGate.getId()))
        .build());

    Map<Long, ProjectQgateAssociation> projectsById = projectsById(result.projects());
    assertThat(projectsById).hasSize(1);
    verifyProject(projectsById.get(associatedProject.getId()), true, associatedProject.name());
  }

  @Test
  public void return_only_unassociated_project() throws Exception {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto associatedProject = insertProject(ComponentTesting.newPublicProjectDto(org));
    ComponentDto unassociatedProject = insertProject(ComponentTesting.newPublicProjectDto(org));
    associateProjectToQualitGate(associatedProject.getId());

    Association result = underTest.find(
      builder()
        .membership(OUT)
        .gateId(Long.toString(qGate.getId()))
        .build());

    Map<Long, ProjectQgateAssociation> projectsById = projectsById(result.projects());
    assertThat(projectsById).hasSize(1);
    verifyProject(projectsById.get(unassociatedProject.getId()), false, unassociatedProject.name());
  }

  @Test
  public void return_only_authorized_projects() throws Exception {
    UserDto user = dbTester.users().insertUser("a_login");
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project1 = componentDbTester.insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto));
    componentDbTester.insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto));

    // User can only see project 1
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.USER, project1);

    userSession.logIn(user.getLogin()).setUserId(user.getId());
    Association result = underTest.find(
      builder()
        .gateId(Long.toString(qGate.getId()))
        .build());

    verifyProjects(result, project1.getId());
  }

  @Test
  public void do_not_verify_permissions_if_user_is_root() throws Exception {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto project = componentDbTester.insertPrivateProject(org);
    ProjectQgateAssociationQuery query = builder()
      .gateId(Long.toString(qGate.getId()))
      .build();

    userSession.logIn().setNonRoot();
    verifyProjects(underTest.find(query));

    userSession.logIn().setRoot();
    verifyProjects(underTest.find(query), project.getId());
  }

  @Test
  public void test_paging() throws Exception {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto project1 = insertProject(ComponentTesting.newPublicProjectDto(org).setName("Project 1"));
    ComponentDto project2 = insertProject(ComponentTesting.newPublicProjectDto(org).setName("Project 2"));
    ComponentDto project3 = insertProject(ComponentTesting.newPublicProjectDto(org).setName("Project 3"));
    associateProjectToQualitGate(project1.getId());

    // Return partial result on first page
    verifyPaging(underTest.find(
      builder().gateId(Long.toString(qGate.getId()))
        .pageIndex(1)
        .pageSize(1)
        .build()),
      true, project1.getId());

    // Return partial result on second page
    verifyPaging(underTest.find(
      builder().gateId(Long.toString(qGate.getId()))
        .pageIndex(2)
        .pageSize(1)
        .build()),
      true, project2.getId());

    // Return partial result on first page
    verifyPaging(underTest.find(
      builder().gateId(Long.toString(qGate.getId()))
        .pageIndex(1)
        .pageSize(2)
        .build()),
      true, project1.getId(), project2.getId());

    // Return all result on first page
    verifyPaging(underTest.find(
      builder().gateId(Long.toString(qGate.getId()))
        .pageIndex(1)
        .pageSize(3)
        .build()),
      false, project1.getId(), project2.getId(), project3.getId());

    // Return no result as page index is off limit
    verifyPaging(underTest.find(
      builder().gateId(Long.toString(qGate.getId()))
        .pageIndex(3)
        .pageSize(3)
        .build()),
      false);
  }

  @Test
  public void fail_on_unknown_quality_gate() throws Exception {
    expectedException.expect(NotFoundException.class);
    underTest.find(builder().gateId("123").build());
  }

  private void verifyProject(ProjectQgateAssociation project, boolean expectedMembership, String expectedName) {
    assertThat(project.isMember()).isEqualTo(expectedMembership);
    assertThat(project.name()).isEqualTo(expectedName);
  }

  private void verifyProjects(Association association, Long... expectedProjectIds) {
    assertThat(association.projects()).extracting("id").containsOnly(expectedProjectIds);
  }

  private void verifyPaging(Association association, boolean expectedHasMoreResults, Long... expectedProjectIds) {
    assertThat(association.hasMoreResults()).isEqualTo(expectedHasMoreResults);
    assertThat(association.projects()).extracting("id").containsOnly(expectedProjectIds);
  }

  private void associateProjectToQualitGate(long projectId) {
    dbClient.propertiesDao().saveProperty(
      new PropertyDto().setKey(SONAR_QUALITYGATE_PROPERTY)
        .setResourceId(projectId)
        .setValue(Long.toString(qGate.getId())));
    dbTester.commit();
  }

  private ComponentDto insertProject(ComponentDto project) {
    dbTester.components().insertComponent(project);
    return project;
  }

  private static Map<Long, ProjectQgateAssociation> projectsById(List<ProjectQgateAssociation> projects) {
    return from(projects).uniqueIndex(ProjectToId.INSTANCE);
  }

  private enum ProjectToId implements Function<ProjectQgateAssociation, Long> {
    INSTANCE;

    @Override
    public Long apply(@Nonnull ProjectQgateAssociation input) {
      return input.id();
    }
  }
}
