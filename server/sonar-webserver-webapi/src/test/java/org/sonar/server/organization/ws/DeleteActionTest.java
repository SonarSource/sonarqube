/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.organization.ws;

import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.BillingValidations;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileFactoryImpl;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.user.index.UserQuery;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ORGANIZATION;

@RunWith(DataProviderRunner.class)
public class DeleteActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(PROJECT, VIEW, APP).setAllQualifiers(PROJECT, VIEW, APP);
  private ComponentCleanerService spiedComponentCleanerService = spy(new ComponentCleanerService(db.getDbClient(), resourceTypes, mock(ProjectIndexers.class)));
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone().setEnabled(true);
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private QProfileFactory qProfileFactory = new QProfileFactoryImpl(dbClient, mock(UuidFactory.class), System2.INSTANCE, mock(ActiveRuleIndexer.class));
  private UserIndex userIndex = new UserIndex(es.client(), System2.INSTANCE);
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private ProjectLifeCycleListeners projectLifeCycleListeners = mock(ProjectLifeCycleListeners.class);
  private BillingValidationsProxy billingValidationsProxy = mock(BillingValidationsProxy.class);
  private OrganizationDeleter organizationDeleter = new OrganizationDeleter(dbClient, spiedComponentCleanerService, userIndexer, qProfileFactory, projectLifeCycleListeners, billingValidationsProxy);
  private WsActionTester wsTester = new WsActionTester(new DeleteAction(userSession, dbClient, defaultOrganizationProvider, organizationFlags, organizationDeleter));

  @Test
  public void definition() {
    WebService.Action action = wsTester.getDef();
    assertThat(action.key()).isEqualTo("delete");
    assertThat(action.isPost()).isTrue();
    assertThat(action.description()).isEqualTo("Delete an organization.<br/>" +
      "Require 'Administer System' permission on the specified organization. Organization support must be enabled.");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("6.2");
    assertThat(action.handler()).isNotNull();
    assertThat(action.params()).hasSize(1);
    assertThat(action.responseExample()).isNull();

    assertThat(action.param("organization"))
      .matches(param -> param.isRequired())
      .matches(param -> "foo-company".equals(param.exampleValue()))
      .matches(param -> "Organization key".equals(param.description()));
  }

  @Test
  public void organization_deletion_also_ensure_that_webhooks_of_this_organization_if_they_exist_are_cleared() {
    OrganizationDto organization = db.organizations().insert();
    db.webhooks().insertWebhook(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    WebhookDto projectWebhook = db.webhooks().insertWebhook(project);
    db.webhookDelivery().insert(projectWebhook);
    userSession.logIn().addPermission(ADMINISTER, organization);

    wsTester.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertThat(db.countRowsOfTable(db.getSession(), "webhooks")).isZero();
    assertThat(db.countRowsOfTable(db.getSession(), "webhook_deliveries")).isZero();
  }

  @Test
  public void organization_deletion_also_ensure_that_homepage_on_this_organization_if_it_exists_is_cleared() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = dbClient.userDao().insert(dbSession, newUserDto().setHomepageType("ORGANIZATION").setHomepageParameter(organization.getUuid()));
    dbSession.commit();

    userSession.logIn().addPermission(ADMINISTER, organization);

    wsTester.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    UserDto userReloaded = dbClient.userDao().selectUserById(dbSession, user.getId());
    assertThat(userReloaded.getHomepageType()).isNull();
    assertThat(userReloaded.getHomepageParameter()).isNull();
  }

  @Test
  public void organization_deletion_also_ensure_that_homepage_on_project_belonging_to_this_organization_if_it_exists_is_cleared() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    UserDto user = dbClient.userDao().insert(dbSession,
      newUserDto().setHomepageType("PROJECT").setHomepageParameter(project.uuid()));
    dbSession.commit();

    userSession.logIn().addPermission(ADMINISTER, organization);

    wsTester.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    UserDto userReloaded = dbClient.userDao().selectUserById(dbSession, user.getId());
    assertThat(userReloaded.getHomepageType()).isNull();
    assertThat(userReloaded.getHomepageParameter()).isNull();
    verify(projectLifeCycleListeners).onProjectsDeleted(ImmutableSet.of(Project.from(project)));
  }

  @Test
  public void fail_with_IllegalStateException_if_organization_support_is_disabled() {
    organizationFlags.setEnabled(false);
    userSession.logIn();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Organization support is disabled");

    try {
      wsTester.newRequest().execute();
    } finally {
      verifyZeroInteractions(projectLifeCycleListeners);
    }
  }

  @Test
  public void fail_with_UnauthorizedException_if_user_is_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    try {
      wsTester.newRequest()
        .execute();
    } finally {
      verifyNoMoreInteractions(projectLifeCycleListeners);
    }
  }

  @Test
  public void fail_with_IAE_if_key_param_is_missing() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'organization' parameter is missing");

    try {
      wsTester.newRequest().execute();
    } finally {
      verifyZeroInteractions(projectLifeCycleListeners);
    }
  }

  @Test
  public void fail_with_IAE_if_key_is_the_one_of_default_organization() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Default Organization can't be deleted");

    try {
      sendRequest(db.getDefaultOrganization());
    } finally {
      verifyZeroInteractions(projectLifeCycleListeners);
    }
  }

  @Test
  public void fail_with_NotFoundException_if_organization_with_specified_key_does_not_exist() {
    logInAsSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Organization with key 'foo' not found");

    try {
      sendRequest("foo");
    } finally {
      verifyZeroInteractions(projectLifeCycleListeners);
    }
  }

  @Test
  public void fail_with_ForbiddenException_when_user_is_not_administrator_of_specified_organization() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    try {
      sendRequest(organization);
    } finally {
      verifyZeroInteractions(projectLifeCycleListeners);
    }
  }

  @Test
  public void fail_with_ForbiddenException_when_user_is_system_administrator() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn().setSystemAdministrator();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    try {
      sendRequest(organization);
    } finally {
      verifyZeroInteractions(projectLifeCycleListeners);
    }
  }

  @Test
  public void fail_with_ForbiddenException_when_user_is_administrator_of_other_organization() {
    OrganizationDto organization = db.organizations().insert();
    logInAsAdministrator(db.getDefaultOrganization());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    try {
      sendRequest(organization);
    } finally {
      verifyZeroInteractions(projectLifeCycleListeners);
    }
  }

  @Test
  public void delete_specified_organization_if_exists_and_user_is_administrator_of_it() {
    OrganizationDto organization = db.organizations().insert();
    logInAsAdministrator(organization);

    sendRequest(organization);

    verifyOrganizationDoesNotExist(organization);
    verify(projectLifeCycleListeners).onProjectsDeleted(emptySet());
  }

  @Test
  public void delete_specified_organization_if_exists_and_user_is_organization_administrator() {
    OrganizationDto organization = db.organizations().insert();
    logInAsAdministrator(organization);

    sendRequest(organization);

    verifyOrganizationDoesNotExist(organization);
    verify(projectLifeCycleListeners).onProjectsDeleted(emptySet());
  }

  @Test
  @UseDataProvider("OneOrMoreIterations")
  public void delete_components_of_specified_organization(int numberOfIterations) {
    OrganizationDto organization = db.organizations().insert();
    Set<ComponentDto> projects = IntStream.range(0, numberOfIterations).mapToObj(i -> {
      ComponentDto project = db.components().insertPrivateProject(organization);
      ComponentDto module = db.components().insertComponent(ComponentTesting.newModuleDto(project));
      ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(module, "a/b" + i));
      ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(module, directory));
      ComponentDto branch = db.components().insertProjectBranch(project);
      return project;
    }).collect(toSet());

    logInAsAdministrator(organization);

    sendRequest(organization);

    verifyOrganizationDoesNotExist(organization);
    assertThat(db.countRowsOfTable(db.getSession(), "projects")).isZero();
    verify(projectLifeCycleListeners).onProjectsDeleted(projects.stream().map(Project::from).collect(toSet()));
  }

  @DataProvider
  public static Object[][] OneOrMoreIterations() {
    return new Object[][] {
      {1},
      {1 + new Random().nextInt(10)},
    };
  }

  @Test
  public void delete_branches() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    ComponentDto branch = db.components().insertProjectBranch(project);
    logInAsAdministrator(organization);

    sendRequest(organization);

    verifyOrganizationDoesNotExist(organization);
    assertThat(db.countRowsOfTable(db.getSession(), "projects")).isZero();
    assertThat(db.countRowsOfTable(db.getSession(), "project_branches")).isZero();
    verify(projectLifeCycleListeners).onProjectsDeleted(ImmutableSet.of(Project.from(project)));
  }

  @Test
  public void delete_permissions_templates_and_permissions_and_groups_of_specified_organization() {
    OrganizationDto org = db.organizations().insert();
    OrganizationDto otherOrg = db.organizations().insert();

    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup(org);
    GroupDto group2 = db.users().insertGroup(org);
    GroupDto otherGroup1 = db.users().insertGroup(otherOrg);
    GroupDto otherGroup2 = db.users().insertGroup(otherOrg);

    ComponentDto projectDto = db.components().insertPublicProject(org);
    ComponentDto otherProjectDto = db.components().insertPublicProject(otherOrg);

    db.users().insertPermissionOnAnyone(org, "u1");
    db.users().insertPermissionOnAnyone(otherOrg, "not deleted u1");
    db.users().insertPermissionOnUser(org, user1, "u2");
    db.users().insertPermissionOnUser(otherOrg, user1, "not deleted u2");
    db.users().insertPermissionOnGroup(group1, "u3");
    db.users().insertPermissionOnGroup(otherGroup1, "not deleted u3");
    db.users().insertProjectPermissionOnAnyone("u4", projectDto);
    db.users().insertProjectPermissionOnAnyone("not deleted u4", otherProjectDto);
    db.users().insertProjectPermissionOnGroup(group1, "u5", projectDto);
    db.users().insertProjectPermissionOnGroup(otherGroup1, "not deleted u5", otherProjectDto);
    db.users().insertProjectPermissionOnUser(user1, "u6", projectDto);
    db.users().insertProjectPermissionOnUser(user1, "not deleted u6", otherProjectDto);

    PermissionTemplateDto templateDto = db.permissionTemplates().insertTemplate(org);
    PermissionTemplateDto otherTemplateDto = db.permissionTemplates().insertTemplate(otherOrg);
    logInAsAdministrator(org);

    sendRequest(org);

    verifyOrganizationDoesNotExist(org);
    assertThat(dbClient.groupDao().selectByIds(dbSession, of(group1.getId(), otherGroup1.getId(), group2.getId(), otherGroup2.getId())))
      .extracting(GroupDto::getId)
      .containsOnly(otherGroup1.getId(), otherGroup2.getId());
    assertThat(dbClient.permissionTemplateDao().selectByUuid(dbSession, templateDto.getUuid()))
      .isNull();
    assertThat(dbClient.permissionTemplateDao().selectByUuid(dbSession, otherTemplateDto.getUuid()))
      .isNotNull();
    assertThat(db.select("select role as \"role\" from USER_ROLES"))
      .extracting(row -> (String) row.get("role"))
      .doesNotContain("u2", "u6")
      .contains("not deleted u2", "not deleted u6");
    assertThat(db.select("select role as \"role\" from GROUP_ROLES"))
      .extracting(row -> (String) row.get("role"))
      .doesNotContain("u1", "u3", "u4", "u5")
      .contains("not deleted u1", "not deleted u3", "not deleted u4", "not deleted u5");
    verify(projectLifeCycleListeners).onProjectsDeleted(ImmutableSet.of(Project.from(projectDto)));
  }

  @Test
  public void delete_members_of_specified_organization() {
    OrganizationDto org = db.organizations().insert();
    OrganizationDto otherOrg = db.organizations().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.organizations().addMember(org, user1);
    db.organizations().addMember(otherOrg, user1);
    db.organizations().addMember(org, user2);
    userIndexer.commitAndIndex(db.getSession(), asList(user1, user2));
    logInAsAdministrator(org);

    sendRequest(org);

    verifyOrganizationDoesNotExist(org);
    assertThat(db.getDbClient().organizationMemberDao().select(db.getSession(), org.getUuid(), user1.getId())).isNotPresent();
    assertThat(db.getDbClient().organizationMemberDao().select(db.getSession(), org.getUuid(), user2.getId())).isNotPresent();
    assertThat(db.getDbClient().organizationMemberDao().select(db.getSession(), otherOrg.getUuid(), user1.getId())).isPresent();
    assertThat(userIndex.search(UserQuery.builder().setOrganizationUuid(org.getUuid()).build(), new SearchOptions()).getTotal()).isEqualTo(0);
    assertThat(userIndex.search(UserQuery.builder().setOrganizationUuid(otherOrg.getUuid()).build(), new SearchOptions()).getTotal()).isEqualTo(1);
    verify(projectLifeCycleListeners).onProjectsDeleted(emptySet());
  }

  @Test
  public void delete_quality_profiles_of_specified_organization() {
    OrganizationDto org = db.organizations().insert();
    OrganizationDto otherOrg = db.organizations().insert();
    QProfileDto profileInOrg = db.qualityProfiles().insert(org);
    QProfileDto profileInOtherOrg = db.qualityProfiles().insert(otherOrg);

    logInAsAdministrator(org);

    sendRequest(org);

    verifyOrganizationDoesNotExist(org);
    assertThat(db.select("select uuid as \"profileKey\" from org_qprofiles"))
      .extracting(row -> (String) row.get("profileKey"))
      .containsOnly(profileInOtherOrg.getKee());
  }

  @Test
  public void delete_quality_gates() {
    QualityGateDto builtInQualityGate = db.qualityGates().insertBuiltInQualityGate();
    OrganizationDto organization = db.organizations().insert();
    db.qualityGates().associateQualityGateToOrganization(builtInQualityGate, organization);
    OrganizationDto otherOrganization = db.organizations().insert();
    db.qualityGates().associateQualityGateToOrganization(builtInQualityGate, otherOrganization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    QGateWithOrgDto qualityGateInOtherOrg = db.qualityGates().insertQualityGate(otherOrganization);
    logInAsAdministrator(organization);

    sendRequest(organization);

    verifyOrganizationDoesNotExist(organization);
    assertThat(db.select("select uuid as \"uuid\" from quality_gates"))
      .extracting(row -> (String) row.get("uuid"))
      .containsExactlyInAnyOrder(qualityGateInOtherOrg.getUuid(), builtInQualityGate.getUuid());
    assertThat(db.select("select organization_uuid as \"organizationUuid\" from org_quality_gates"))
      .extracting(row -> (String) row.get("organizationUuid"))
      .containsOnly(otherOrganization.getUuid());

    // Check built-in quality gate is still available in other organization
    assertThat(db.getDbClient().qualityGateDao().selectByOrganizationAndName(db.getSession(), otherOrganization, "Sonar way")).isNotNull();
    verify(projectLifeCycleListeners).onProjectsDeleted(emptySet());
  }

  @Test
  @UseDataProvider("indexOfFailingProjectDeletion")
  public void projectLifeCycleListener_are_notified_even_if_deletion_of_a_project_throws_an_Exception(int failingProjectIndex) {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto[] projects = new ComponentDto[] {
      db.components().insertPrivateProject(organization),
      db.components().insertPrivateProject(organization),
      db.components().insertPrivateProject(organization)
    };
    logInAsAdministrator(organization);
    RuntimeException expectedException = new RuntimeException("Faking deletion of 2nd project throwing an exception");
    doThrow(expectedException)
      .when(spiedComponentCleanerService).delete(any(), eq(projects[failingProjectIndex]));

    try {
      sendRequest(organization);
      fail("A RuntimeException should have been thrown");
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(expectedException);
      verify(projectLifeCycleListeners).onProjectsDeleted(Arrays.stream(projects).map(Project::from).collect(toSet()));
    }
  }

  @Test
  public void call_billing_validation_on_delete() {
    OrganizationDto organization = db.organizations().insert();
    logInAsAdministrator(organization);

    sendRequest(organization);

    verify(billingValidationsProxy).onDelete(any(BillingValidations.Organization.class));
  }

  @Test
  public void delete_organization_alm_binding() {
    OrganizationDto organization = db.organizations().insert();
    db.alm().insertOrganizationAlmBinding(organization, db.alm().insertAlmAppInstall(), true);
    logInAsAdministrator(organization);

    sendRequest(organization);

    assertThat(db.getDbClient().organizationAlmBindingDao().selectByOrganization(db.getSession(), organization)).isNotPresent();
  }

  @DataProvider
  public static Object[][] indexOfFailingProjectDeletion() {
    return new Object[][] {
      {0},
      {1},
      {2}
    };
  }

  private void verifyOrganizationDoesNotExist(OrganizationDto organization) {
    assertThat(db.getDbClient().organizationDao().selectByKey(dbSession, organization.getKey())).isEmpty();
  }

  private void sendRequest(OrganizationDto organization) {
    sendRequest(organization.getKey());
  }

  private void sendRequest(String organizationKey) {
    wsTester.newRequest()
      .setParam(PARAM_ORGANIZATION, organizationKey)
      .execute();
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private void logInAsAdministrator(OrganizationDto organization) {
    userSession.logIn().addPermission(ADMINISTER, organization);
  }
}
