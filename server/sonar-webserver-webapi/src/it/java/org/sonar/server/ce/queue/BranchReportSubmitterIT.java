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
package org.sonar.server.ce.queue;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import org.sonar.db.component.ProjectData;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.almsettings.DevOpsProjectCreatorFactory;
import org.sonar.server.common.almsettings.github.GithubProjectCreatorFactory;
import org.sonar.server.common.component.ComponentCreationParameters;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.permission.PermissionTemplateService;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH_TYPE;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.permission.ProjectPermission.SCAN;

/**
 * Tests of {@link ReportSubmitter} when branch support is installed.
 */
@RunWith(DataProviderRunner.class)
public class BranchReportSubmitterIT {

  private static final String PROJECT_UUID = "PROJECT_UUID";
  private static final Map<String, String> CHARACTERISTICS = Map.of(
    BRANCH, "branch_name",
    BRANCH_TYPE, "branch"
  );
  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);

  private final CeQueue queue = mock(CeQueueImpl.class);
  private final ComponentUpdater componentUpdater = mock(ComponentUpdater.class);
  private final PermissionTemplateService permissionTemplateService = mock(PermissionTemplateService.class);
  private final FavoriteUpdater favoriteUpdater = mock(FavoriteUpdater.class);
  private final BranchSupportDelegate branchSupportDelegate = mock(BranchSupportDelegate.class);
  private final BranchSupport branchSupport = spy(new BranchSupport(branchSupportDelegate));

  private final DevOpsProjectCreatorFactory devOpsProjectCreatorFactory = new GithubProjectCreatorFactory(db.getDbClient(), null,
    null, null, null, null, null, null, null, null, null);

  private final ManagedInstanceService managedInstanceService = mock();
  private final ProjectCreator projectCreator = new ProjectCreator(userSession, projectDefaultVisibility, componentUpdater);
  private final ReportSubmitter underTest = new ReportSubmitter(queue, userSession, projectCreator, componentUpdater, permissionTemplateService, db.getDbClient(), branchSupport,
    devOpsProjectCreatorFactory, managedInstanceService);

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PUBLIC);
  }

  @Test
  public void submit_does_not_use_delegate_if_characteristics_are_empty() {
    ProjectData projectData = db.components().insertPublicProject();
    ProjectDto project = projectData.getProjectDto();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ProjectPermission.SCAN, project)
      .registerBranches(projectData.getMainBranchDto());
    mockSuccessfulPrepareSubmitCall();
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);

    underTest.submit(project.getKey(), project.getName(), emptyMap(), reportInput);

    verifyNoInteractions(branchSupportDelegate);
  }

  @Test
  public void submit_a_report_on_existing_branch() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey("branch1"));
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ProjectPermission.SCAN, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto())
      .addProjectBranchMapping(projectData.projectUuid(), branch);
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(mainBranch.getKey(), "branch1");
    when(branchSupportDelegate.createComponentKey(mainBranch.getKey(), CHARACTERISTICS)).thenReturn(componentKey);
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);
    String taskUuid = mockSuccessfulPrepareSubmitCall();

    underTest.submit(mainBranch.getKey(), mainBranch.name(), CHARACTERISTICS, reportInput);

    verifyNoInteractions(permissionTemplateService);
    verifyNoInteractions(favoriteUpdater);
    verify(branchSupport, times(0)).createBranchComponent(any(), any(), any(), any());
    verify(branchSupportDelegate).createComponentKey(mainBranch.getKey(), CHARACTERISTICS);
    verify(branchSupportDelegate, times(0)).createBranchComponent(any(), any(), any(), any());
    verifyNoMoreInteractions(branchSupportDelegate);
    verifyQueueSubmit(mainBranch, branch, user, CHARACTERISTICS, taskUuid);

    ProjectDto projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), componentKey.getKey()).orElseThrow();
    assertThat(projectDto.getCreationMethod()).isEqualTo(CreationMethod.LOCAL_API);
  }

  @Test
  public void submit_a_report_on_missing_branch_but_existing_project() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    BranchDto exitingProjectMainBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), mainBranch.uuid()).get();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ProjectPermission.SCAN, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    ComponentDto createdBranch = createButDoNotInsertBranch(mainBranch, projectData.projectUuid());
    userSession.addProjectBranchMapping(projectData.projectUuid(), createdBranch);
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(mainBranch.getKey(), "branch1");
    when(branchSupportDelegate.createComponentKey(mainBranch.getKey(), CHARACTERISTICS)).thenReturn(componentKey);
    when(branchSupportDelegate.createBranchComponent(any(DbSession.class), same(componentKey), eq(mainBranch), eq(exitingProjectMainBranch))).thenReturn(createdBranch);
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);
    String taskUuid = mockSuccessfulPrepareSubmitCall();

    underTest.submit(mainBranch.getKey(), mainBranch.name(), CHARACTERISTICS, reportInput);

    verifyNoInteractions(permissionTemplateService);
    verifyNoInteractions(favoriteUpdater);
    verify(branchSupport).createBranchComponent(any(DbSession.class), same(componentKey), eq(mainBranch), eq(exitingProjectMainBranch));
    verify(branchSupportDelegate).createComponentKey(mainBranch.getKey(), CHARACTERISTICS);
    verify(branchSupportDelegate).createBranchComponent(any(DbSession.class), same(componentKey), eq(mainBranch), eq(exitingProjectMainBranch));
    verifyNoMoreInteractions(branchSupportDelegate);
    verify(componentUpdater, times(0)).commitAndIndex(any(), any());
    verifyQueueSubmit(mainBranch, createdBranch, user, CHARACTERISTICS, taskUuid);
  }

  @Test
  public void submit_report_on_missing_branch_of_missing_project_provisions_project_when_PROVISION_PROJECT_perm() {
    ComponentDto nonExistingBranch = newPrivateProjectDto();
    UserDto user = db.users().insertUser();
    userSession.logIn(user)
      .addPermission(PROVISION_PROJECTS)
      .addPermission(GlobalPermission.SCAN);

    ComponentDto createdBranch = createButDoNotInsertBranch(nonExistingBranch, PROJECT_UUID);
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(nonExistingBranch.getKey());
    when(branchSupportDelegate.createComponentKey(nonExistingBranch.getKey(), CHARACTERISTICS)).thenReturn(componentKey);
    ComponentCreationData componentCreationData = mock(ComponentCreationData.class);
    when(componentCreationData.mainBranchComponent())
      .thenAnswer((Answer<ComponentDto>) invocation -> db.components().insertPrivateProject(PROJECT_UUID, nonExistingBranch).getMainBranchComponent());
    when(componentUpdater.createWithoutCommit(any(), any())).thenReturn(componentCreationData);
    when(branchSupportDelegate.createBranchComponent(any(DbSession.class), same(componentKey), any(), any())).thenReturn(createdBranch);
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), any(), eq(nonExistingBranch.getKey()))).thenReturn(true);
    String taskUuid = mockSuccessfulPrepareSubmitCall();
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);

    underTest.submit(nonExistingBranch.getKey(), nonExistingBranch.name(), CHARACTERISTICS, reportInput);

    BranchDto existingProjectMainBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), nonExistingBranch.uuid()).get();
    verify(branchSupport).createBranchComponent(any(DbSession.class), same(componentKey), eq(nonExistingBranch), eq(existingProjectMainBranch));
    verify(branchSupportDelegate).createComponentKey(nonExistingBranch.getKey(), CHARACTERISTICS);
    verify(branchSupportDelegate).createBranchComponent(any(DbSession.class), same(componentKey), eq(nonExistingBranch), eq(existingProjectMainBranch));
    verifyNoMoreInteractions(branchSupportDelegate);
    verifyQueueSubmit(nonExistingBranch, createdBranch, user, CHARACTERISTICS, taskUuid);
    verify(componentUpdater).commitAndIndex(any(DbSession.class), eq(componentCreationData));
    assertProjectCreatedWithCreationMethodEqualsScanner();
  }

  private void assertProjectCreatedWithCreationMethodEqualsScanner() {
    ArgumentCaptor<ComponentCreationParameters> componentCreationParametersCaptor = ArgumentCaptor.forClass(ComponentCreationParameters.class);
    verify(componentUpdater).createWithoutCommit(any(), componentCreationParametersCaptor.capture());
    assertThat(componentCreationParametersCaptor.getValue().creationMethod()).isEqualTo(CreationMethod.SCANNER_API);
  }

  @Test
  public void submit_fails_if_branch_support_delegate_createComponentKey_throws_an_exception() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(ProjectPermission.SCAN, projectData.getProjectDto());
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);
    RuntimeException expected = new RuntimeException("Faking an exception thrown by branchSupportDelegate");
    when(branchSupportDelegate.createComponentKey(any(), any())).thenThrow(expected);

    try {
      underTest.submit(project.getKey(), project.name(), CHARACTERISTICS, reportInput);
      fail("exception should have been thrown");
    } catch (Exception e) {
      assertThat(e).isSameAs(expected);
    }
  }

  @Test
  public void submit_report_on_missing_branch_of_missing_project_fails_with_ForbiddenException_if_only_scan_permission() {
    ComponentDto nonExistingBranch = newPrivateProjectDto();
    UserDto user = db.users().insertUser();

    ComponentDto createdBranch = createButDoNotInsertBranch(nonExistingBranch, PROJECT_UUID);
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(nonExistingBranch.getKey());
    String nonExistingProjectDbKey = nonExistingBranch.getKey();
    when(branchSupportDelegate.createComponentKey(nonExistingProjectDbKey, CHARACTERISTICS)).thenReturn(componentKey);
    when(branchSupportDelegate.createBranchComponent(any(DbSession.class), same(componentKey), any(), any())).thenReturn(createdBranch);
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);

    String name = nonExistingBranch.name();
    assertThatThrownBy(() -> underTest.submit(nonExistingProjectDbKey, name, CHARACTERISTICS, reportInput))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  private static ComponentDto createButDoNotInsertBranch(ComponentDto mainBranch, String projectUuid) {
    BranchType randomBranchType = BranchType.values()[new Random().nextInt(BranchType.values().length)];
    BranchDto branchDto = newBranchDto(projectUuid, randomBranchType);
    return ComponentTesting.newBranchComponent(mainBranch, branchDto);
  }

  private String mockSuccessfulPrepareSubmitCall() {
    String taskUuid = secure().nextAlphabetic(12);
    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(taskUuid));
    return taskUuid;
  }

  private void verifyQueueSubmit(ComponentDto project, ComponentDto branch, UserDto user, Map<String, String> characteristics, String taskUuid) {
    ArgumentCaptor<CeTaskSubmit> captor = ArgumentCaptor.forClass(CeTaskSubmit.class);
    verify(queue).submit(captor.capture());
    CeTaskSubmit ceTask = captor.getValue();
    assertThat(ceTask.getUuid()).isEqualTo(taskUuid);
    assertThat(ceTask.getSubmitterUuid()).isEqualTo(user.getUuid());
    assertThat(ceTask.getCharacteristics()).isEqualTo(characteristics);
    assertThat(ceTask.getType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(ceTask.getComponent()).isPresent();
    assertThat(ceTask.getComponent().get().getUuid()).isEqualTo(branch.uuid());
    BranchDto branchDto = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.uuid()).get();
    assertThat(ceTask.getComponent().get().getEntityUuid()).isEqualTo(branchDto.getProjectUuid());
  }

  private static BranchSupport.ComponentKey createComponentKeyOfBranch(String projectKey) {
    return createComponentKeyOfBranch(projectKey, secure().nextAlphabetic(5));
  }

  private static BranchSupport.ComponentKey createComponentKeyOfBranch(String projectKey, String branchKey) {
    BranchSupport.ComponentKey componentKey = mockComponentKey(projectKey);
    when(componentKey.getBranchName()).thenReturn(Optional.of(branchKey));
    return componentKey;
  }

  private static BranchSupport.ComponentKey mockComponentKey(String key) {
    BranchSupport.ComponentKey componentKey = mock(BranchSupport.ComponentKey.class);
    when(componentKey.getKey()).thenReturn(key);
    return componentKey;
  }

}
