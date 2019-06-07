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
package org.sonar.server.ce.queue;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.sonar.api.utils.System2;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeQueueImpl;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;

/**
 * Tests of {@link ReportSubmitter} when branch support is installed.
 */
@RunWith(DataProviderRunner.class)
public class BranchReportSubmitterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private CeQueue queue = mock(CeQueueImpl.class);
  private ComponentUpdater componentUpdater = mock(ComponentUpdater.class);
  private PermissionTemplateService permissionTemplateService = mock(PermissionTemplateService.class);
  private FavoriteUpdater favoriteUpdater = mock(FavoriteUpdater.class);
  private BranchSupportDelegate branchSupportDelegate = mock(BranchSupportDelegate.class);
  private BranchSupport branchSupport = spy(new BranchSupport(branchSupportDelegate));

  private ReportSubmitter underTest = new ReportSubmitter(queue, userSession, componentUpdater, permissionTemplateService, db.getDbClient(), branchSupport);

  @Test
  public void submit_does_not_use_delegate_if_characteristics_are_empty() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, project);
    mockSuccessfulPrepareSubmitCall();
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);

    underTest.submit(organization.getKey(), project.getDbKey(), project.name(), emptyMap(), reportInput);

    verifyZeroInteractions(branchSupportDelegate);
  }

  @Test
  public void submit_a_report_on_existing_branch() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    ComponentDto branch = db.components().insertProjectBranch(project);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, project);
    Map<String, String> randomCharacteristics = randomNonEmptyMap();
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(branch);
    when(branchSupportDelegate.createComponentKey(project.getDbKey(), randomCharacteristics))
      .thenReturn(componentKey);
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);
    String taskUuid = mockSuccessfulPrepareSubmitCall();

    underTest.submit(organization.getKey(), project.getDbKey(), project.name(), randomCharacteristics, reportInput);

    verifyZeroInteractions(permissionTemplateService);
    verifyZeroInteractions(favoriteUpdater);
    verify(branchSupport, times(0)).createBranchComponent(any(), any(), any(), any(), any());
    verify(branchSupportDelegate).createComponentKey(project.getDbKey(), randomCharacteristics);
    verify(branchSupportDelegate, times(0)).createBranchComponent(any(), any(), any(), any(), any());
    verifyNoMoreInteractions(branchSupportDelegate);
    verifyQueueSubmit(project, branch, user, randomCharacteristics, taskUuid);
  }

  @Test
  public void submit_a_report_on_missing_branch_but_existing_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto existingProject = db.components().insertMainBranch(organization);
    BranchDto exitingProjectMainBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), existingProject.uuid()).get();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, existingProject);
    Map<String, String> randomCharacteristics = randomNonEmptyMap();
    ComponentDto createdBranch = createButDoNotInsertBranch(existingProject);
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(createdBranch);
    when(branchSupportDelegate.createComponentKey(existingProject.getDbKey(), randomCharacteristics))
      .thenReturn(componentKey);
    when(branchSupportDelegate.createBranchComponent(any(DbSession.class), same(componentKey), eq(organization), eq(existingProject), eq(exitingProjectMainBranch)))
      .thenReturn(createdBranch);
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);
    String taskUuid = mockSuccessfulPrepareSubmitCall();

    underTest.submit(organization.getKey(), existingProject.getDbKey(), existingProject.name(), randomCharacteristics, reportInput);

    verifyZeroInteractions(permissionTemplateService);
    verifyZeroInteractions(favoriteUpdater);
    verify(branchSupport).createBranchComponent(any(DbSession.class), same(componentKey), eq(organization), eq(existingProject), eq(exitingProjectMainBranch));
    verify(branchSupportDelegate).createComponentKey(existingProject.getDbKey(), randomCharacteristics);
    verify(branchSupportDelegate).createBranchComponent(any(DbSession.class), same(componentKey), eq(organization), eq(existingProject), eq(exitingProjectMainBranch));
    verifyNoMoreInteractions(branchSupportDelegate);
    verify(componentUpdater, times(0)).commitAndIndex(any(), any());
    verifyQueueSubmit(existingProject, createdBranch, user, randomCharacteristics, taskUuid);
  }

  @Test
  public void submit_report_on_missing_branch_of_missing_project_provisions_project_when_org_PROVISION_PROJECT_perm() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto nonExistingProject = newPrivateProjectDto(organization);
    UserDto user = db.users().insertUser();
    userSession.logIn(user)
      .addPermission(PROVISION_PROJECTS, organization)
      .addPermission(SCAN, organization);

    Map<String, String> randomCharacteristics = randomNonEmptyMap();
    ComponentDto createdBranch = createButDoNotInsertBranch(nonExistingProject);
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(createdBranch);
    when(branchSupportDelegate.createComponentKey(nonExistingProject.getDbKey(), randomCharacteristics))
      .thenReturn(componentKey);
    when(componentUpdater.createWithoutCommit(any(), any(), eq(user.getId())))
      .thenAnswer((Answer<ComponentDto>) invocation -> db.components().insertMainBranch(nonExistingProject));
    when(branchSupportDelegate.createBranchComponent(any(DbSession.class), same(componentKey), eq(organization), eq(nonExistingProject), any()))
      .thenReturn(createdBranch);
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), eq(organization.getUuid()), any(), eq(nonExistingProject.getKey())))
      .thenReturn(true);
    String taskUuid = mockSuccessfulPrepareSubmitCall();
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);

    underTest.submit(organization.getKey(), nonExistingProject.getDbKey(), nonExistingProject.name(), randomCharacteristics, reportInput);

    BranchDto exitingProjectMainBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), nonExistingProject.uuid()).get();
    verify(branchSupport).createBranchComponent(any(DbSession.class), same(componentKey), eq(organization), eq(nonExistingProject), eq(exitingProjectMainBranch));
    verify(branchSupportDelegate).createComponentKey(nonExistingProject.getDbKey(), randomCharacteristics);
    verify(branchSupportDelegate).createBranchComponent(any(DbSession.class), same(componentKey), eq(organization), eq(nonExistingProject), eq(exitingProjectMainBranch));
    verifyNoMoreInteractions(branchSupportDelegate);
    verifyQueueSubmit(nonExistingProject, createdBranch, user, randomCharacteristics, taskUuid);
    verify(componentUpdater).commitAndIndex(any(DbSession.class), eq(nonExistingProject));
  }

  @Test
  public void submit_fails_if_branch_support_delegate_createComponentKey_throws_an_exception() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, project);
    Map<String, String> randomCharacteristics = randomNonEmptyMap();
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);
    RuntimeException expected = new RuntimeException("Faking an exception thrown by branchSupportDelegate");
    when(branchSupportDelegate.createComponentKey(any(), any())).thenThrow(expected);

    try {
      underTest.submit(organization.getKey(), project.getDbKey(), project.name(), randomCharacteristics, reportInput);
      fail("exception should have been thrown");
    } catch (Exception e) {
      assertThat(e).isSameAs(expected);
    }
  }

  @DataProvider
  public static Object[][] permissionsAllowingProjectProvisioning() {
    BiConsumer<ComponentDto, UserSessionRule> noProjectPerm = (cpt, userSession) -> {
    };
    BiConsumer<OrganizationDto, UserSessionRule> noOrgPerm = (cpt, userSession) -> {
    };
    BiConsumer<ComponentDto, UserSessionRule> provisionOnProject = (cpt, userSession) -> userSession.addProjectPermission(PROVISIONING, cpt);
    BiConsumer<OrganizationDto, UserSessionRule> provisionOnOrganization = (cpt, userSession) -> userSession.addPermission(PROVISION_PROJECTS, cpt);
    return new Object[][] {
      {provisionOnProject, noOrgPerm},
      {noProjectPerm, provisionOnOrganization},
      {provisionOnProject, provisionOnOrganization}
    };
  }

  @Test
  public void submit_report_on_missing_branch_of_missing_project_fails_with_ForbiddenException_if_only_scan_permission() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto nonExistingProject = newPrivateProjectDto(organization);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, nonExistingProject);
    Map<String, String> randomCharacteristics = randomNonEmptyMap();
    ComponentDto createdBranch = createButDoNotInsertBranch(nonExistingProject);
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(createdBranch);
    when(branchSupportDelegate.createComponentKey(nonExistingProject.getDbKey(), randomCharacteristics))
      .thenReturn(componentKey);
    when(branchSupportDelegate.createBranchComponent(any(DbSession.class), same(componentKey), eq(organization), eq(nonExistingProject), any()))
      .thenReturn(createdBranch);
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    underTest.submit(organization.getKey(), nonExistingProject.getDbKey(), nonExistingProject.name(), randomCharacteristics, reportInput);
  }

  private static ComponentDto createButDoNotInsertBranch(ComponentDto project) {
    BranchType randomBranchType = BranchType.values()[new Random().nextInt(BranchType.values().length)];
    BranchDto branchDto = newBranchDto(project.projectUuid(), randomBranchType);
    return ComponentTesting.newProjectBranch(project, branchDto);
  }

  private String mockSuccessfulPrepareSubmitCall() {
    String taskUuid = randomAlphabetic(12);
    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(taskUuid));
    return taskUuid;
  }

  private void verifyQueueSubmit(ComponentDto project, ComponentDto branch, UserDto user, Map<String, String> characteristics, String taskUuid) {
    verify(queue).submit(argThat(submit -> submit.getType().equals(CeTaskTypes.REPORT)
      && submit.getComponent().filter(cpt -> cpt.getUuid().equals(branch.uuid()) && cpt.getMainComponentUuid().equals(project.uuid())).isPresent()
      && submit.getSubmitterUuid().equals(user.getUuid())
      && submit.getCharacteristics().equals(characteristics)
      && submit.getUuid().equals(taskUuid)));
  }

  private static BranchSupport.ComponentKey createComponentKeyOfBranch(ComponentDto branch) {
    BranchSupport.ComponentKey mainComponentKey = mockComponentKey(branch.getKey(), branch.getKey());
    when(mainComponentKey.getMainBranchComponentKey()).thenReturn(mainComponentKey);

    BranchSupport.ComponentKey componentKey = mockComponentKey(branch.getKey(), branch.getDbKey());
    when(componentKey.getBranch()).thenReturn(Optional.ofNullable(branch).map(b -> new BranchSupport.Branch(b.name(), BranchType.LONG)));
    when(componentKey.getMainBranchComponentKey()).thenReturn(mainComponentKey);

    return componentKey;
  }

  private static BranchSupport.ComponentKey mockComponentKey(String key, String dbKey) {
    BranchSupport.ComponentKey componentKey = mock(BranchSupport.ComponentKey.class);
    when(componentKey.getKey()).thenReturn(key);
    when(componentKey.getDbKey()).thenReturn(dbKey);
    return componentKey;
  }

  private static ImmutableMap<String, String> randomNonEmptyMap() {
    return IntStream.range(0, 1 + new Random().nextInt(5))
      .boxed()
      .collect(uniqueIndex(i -> "key_" + i, i -> "val_" + i));
  }

}
