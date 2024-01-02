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
package org.sonar.server.ce.queue;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
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
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
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
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;

/**
 * Tests of {@link ReportSubmitter} when branch support is installed.
 */
@RunWith(DataProviderRunner.class)
public class BranchReportSubmitterTest {

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

  private final ReportSubmitter underTest = new ReportSubmitter(queue, userSession, componentUpdater, permissionTemplateService, db.getDbClient(), branchSupport,
    projectDefaultVisibility);

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PUBLIC);
  }

  @Test
  public void submit_does_not_use_delegate_if_characteristics_are_empty() {
    ComponentDto project = db.components().insertPublicProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, project);
    mockSuccessfulPrepareSubmitCall();
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);

    underTest.submit(project.getKey(), project.name(), emptyMap(), reportInput);

    verifyNoInteractions(branchSupportDelegate);
  }

  @Test
  public void submit_a_report_on_existing_branch() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("branch1"));
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, project);
    Map<String, String> randomCharacteristics = randomNonEmptyMap();
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(project.getKey(), "branch1");
    when(branchSupportDelegate.createComponentKey(project.getKey(), randomCharacteristics)).thenReturn(componentKey);
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);
    String taskUuid = mockSuccessfulPrepareSubmitCall();

    underTest.submit(project.getKey(), project.name(), randomCharacteristics, reportInput);

    verifyNoInteractions(permissionTemplateService);
    verifyNoInteractions(favoriteUpdater);
    verify(branchSupport, times(0)).createBranchComponent(any(), any(), any(), any());
    verify(branchSupportDelegate).createComponentKey(project.getKey(), randomCharacteristics);
    verify(branchSupportDelegate, times(0)).createBranchComponent(any(), any(), any(), any());
    verifyNoMoreInteractions(branchSupportDelegate);
    verifyQueueSubmit(project, branch, user, randomCharacteristics, taskUuid);
  }

  @Test
  public void submit_a_report_on_missing_branch_but_existing_project() {
    ComponentDto existingProject = db.components().insertPublicProject();
    BranchDto exitingProjectMainBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), existingProject.uuid()).get();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, existingProject);
    Map<String, String> randomCharacteristics = randomNonEmptyMap();
    ComponentDto createdBranch = createButDoNotInsertBranch(existingProject);
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(existingProject.getKey(), "branch1");
    when(branchSupportDelegate.createComponentKey(existingProject.getKey(), randomCharacteristics)).thenReturn(componentKey);
    when(branchSupportDelegate.createBranchComponent(any(DbSession.class), same(componentKey), eq(existingProject), eq(exitingProjectMainBranch))).thenReturn(createdBranch);
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);
    String taskUuid = mockSuccessfulPrepareSubmitCall();

    underTest.submit(existingProject.getKey(), existingProject.name(), randomCharacteristics, reportInput);

    verifyNoInteractions(permissionTemplateService);
    verifyNoInteractions(favoriteUpdater);
    verify(branchSupport).createBranchComponent(any(DbSession.class), same(componentKey), eq(existingProject), eq(exitingProjectMainBranch));
    verify(branchSupportDelegate).createComponentKey(existingProject.getKey(), randomCharacteristics);
    verify(branchSupportDelegate).createBranchComponent(any(DbSession.class), same(componentKey), eq(existingProject), eq(exitingProjectMainBranch));
    verifyNoMoreInteractions(branchSupportDelegate);
    verify(componentUpdater, times(0)).commitAndIndex(any(), any());
    verifyQueueSubmit(existingProject, createdBranch, user, randomCharacteristics, taskUuid);
  }

  @Test
  public void submit_report_on_missing_branch_of_missing_project_provisions_project_when_PROVISION_PROJECT_perm() {
    ComponentDto nonExistingProject = newPrivateProjectDto();
    UserDto user = db.users().insertUser();
    userSession.logIn(user)
      .addPermission(PROVISION_PROJECTS)
      .addPermission(SCAN);

    Map<String, String> randomCharacteristics = randomNonEmptyMap();
    ComponentDto createdBranch = createButDoNotInsertBranch(nonExistingProject);
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(nonExistingProject.getKey());
    when(branchSupportDelegate.createComponentKey(nonExistingProject.getKey(), randomCharacteristics)).thenReturn(componentKey);
    when(componentUpdater.createWithoutCommit(any(), any(), eq(user.getUuid()), eq(user.getLogin()), any()))
      .thenAnswer((Answer<ComponentDto>) invocation -> db.components().insertPrivateProject(nonExistingProject));
    when(branchSupportDelegate.createBranchComponent(any(DbSession.class), same(componentKey), eq(nonExistingProject), any())).thenReturn(createdBranch);
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), any(), eq(nonExistingProject.getKey()))).thenReturn(true);
    String taskUuid = mockSuccessfulPrepareSubmitCall();
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);

    underTest.submit(nonExistingProject.getKey(), nonExistingProject.name(), randomCharacteristics, reportInput);

    BranchDto exitingProjectMainBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), nonExistingProject.uuid()).get();
    verify(branchSupport).createBranchComponent(any(DbSession.class), same(componentKey), eq(nonExistingProject), eq(exitingProjectMainBranch));
    verify(branchSupportDelegate).createComponentKey(nonExistingProject.getKey(), randomCharacteristics);
    verify(branchSupportDelegate).createBranchComponent(any(DbSession.class), same(componentKey), eq(nonExistingProject), eq(exitingProjectMainBranch));
    verifyNoMoreInteractions(branchSupportDelegate);
    verifyQueueSubmit(nonExistingProject, createdBranch, user, randomCharacteristics, taskUuid);
    verify(componentUpdater).commitAndIndex(any(DbSession.class), eq(nonExistingProject));
  }

  @Test
  public void submit_fails_if_branch_support_delegate_createComponentKey_throws_an_exception() {
    ComponentDto project = db.components().insertPublicProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, project);
    Map<String, String> randomCharacteristics = randomNonEmptyMap();
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);
    RuntimeException expected = new RuntimeException("Faking an exception thrown by branchSupportDelegate");
    when(branchSupportDelegate.createComponentKey(any(), any())).thenThrow(expected);

    try {
      underTest.submit(project.getKey(), project.name(), randomCharacteristics, reportInput);
      fail("exception should have been thrown");
    } catch (Exception e) {
      assertThat(e).isSameAs(expected);
    }
  }

  @Test
  public void submit_report_on_missing_branch_of_missing_project_fails_with_ForbiddenException_if_only_scan_permission() {
    ComponentDto nonExistingProject = newPrivateProjectDto();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, nonExistingProject);
    Map<String, String> randomCharacteristics = randomNonEmptyMap();
    ComponentDto createdBranch = createButDoNotInsertBranch(nonExistingProject);
    BranchSupport.ComponentKey componentKey = createComponentKeyOfBranch(nonExistingProject.getKey());
    String nonExistingProjectDbKey = nonExistingProject.getKey();
    when(branchSupportDelegate.createComponentKey(nonExistingProjectDbKey, randomCharacteristics)).thenReturn(componentKey);
    when(branchSupportDelegate.createBranchComponent(any(DbSession.class), same(componentKey), eq(nonExistingProject), any())).thenReturn(createdBranch);
    InputStream reportInput = IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8);

    String name = nonExistingProject.name();
    assertThatThrownBy(() -> underTest.submit(nonExistingProjectDbKey, name, randomCharacteristics, reportInput))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  private static ComponentDto createButDoNotInsertBranch(ComponentDto project) {
    BranchType randomBranchType = BranchType.values()[new Random().nextInt(BranchType.values().length)];
    BranchDto branchDto = newBranchDto(project.branchUuid(), randomBranchType);
    return ComponentTesting.newBranchComponent(project, branchDto);
  }

  private String mockSuccessfulPrepareSubmitCall() {
    String taskUuid = randomAlphabetic(12);
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
    assertThat(ceTask.getComponent().get().getMainComponentUuid()).isEqualTo(project.uuid());
  }

  private static BranchSupport.ComponentKey createComponentKeyOfBranch(String projectKey) {
    return createComponentKeyOfBranch(projectKey, randomAlphabetic(5));
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

  private static ImmutableMap<String, String> randomNonEmptyMap() {
    return IntStream.range(0, 1 + new Random().nextInt(5))
      .boxed()
      .collect(uniqueIndex(i -> "key_" + i, i -> "val_" + i));
  }

}
