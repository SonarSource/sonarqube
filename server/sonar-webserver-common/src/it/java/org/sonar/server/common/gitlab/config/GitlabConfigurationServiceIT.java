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
package org.sonar.server.common.gitlab.config;

import com.google.common.base.Strings;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.auth.gitlab.GitLabIdentityProvider;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.ExternalGroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.management.ManagedInstanceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_APPLICATION_ID;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_TOKEN;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SECRET;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SYNC_USER_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_URL;
import static org.sonar.server.common.NonNullUpdatedValue.withValueOrThrow;
import static org.sonar.server.common.UpdatedValue.withValue;
import static org.sonar.server.common.gitlab.config.GitlabConfigurationService.UNIQUE_GITLAB_CONFIGURATION_ID;
import static org.sonar.server.common.gitlab.config.UpdateGitlabConfigurationRequest.builder;

@RunWith(MockitoJUnitRunner.class)
public class GitlabConfigurationServiceIT {

  @Rule
  public DbTester dbTester = DbTester.create();

  @Mock
  private ManagedInstanceService managedInstanceService;

  private GitlabConfigurationService gitlabConfigurationService;

  @Before
  public void setUp() {
    when(managedInstanceService.getProviderName()).thenReturn("gitlab");
    gitlabConfigurationService = new GitlabConfigurationService(
      managedInstanceService,
      dbTester.getDbClient());
  }

  @Test
  public void getConfiguration_whenIdIsNotGitlabConfiguration_throwsException() {
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> gitlabConfigurationService.getConfiguration("not-gitlab-configuration"))
      .withMessage("Gitlab configuration with id not-gitlab-configuration not found");
  }

  @Test
  public void getConfiguration_whenNoConfiguration_throwsNotFoundException() {
    assertThatThrownBy(() -> gitlabConfigurationService.getConfiguration("gitlab-configuration"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("GitLab configuration doesn't exist.");

  }

  @Test
  public void getConfiguration_whenConfigurationSet_returnsConfig() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(SynchronizationType.AUTO_PROVISIONING));

    GitlabConfiguration configuration = gitlabConfigurationService.getConfiguration("gitlab-configuration");

    assertConfigurationFields(configuration);
  }

  @Test
  public void getConfiguration_whenConfigurationSetAndEmpty_returnsConfig() {
    dbTester.properties().insertProperty(GITLAB_AUTH_ENABLED, "true", null);
    dbTester.properties().insertProperty(GITLAB_AUTH_PROVISIONING_GROUPS, "", null);

    GitlabConfiguration configuration = gitlabConfigurationService.getConfiguration("gitlab-configuration");

    assertThat(configuration.id()).isEqualTo("gitlab-configuration");
    assertThat(configuration.enabled()).isTrue();
    assertThat(configuration.applicationId()).isEmpty();
    assertThat(configuration.url()).isEmpty();
    assertThat(configuration.secret()).isEmpty();
    assertThat(configuration.synchronizeGroups()).isFalse();
    assertThat(configuration.synchronizationType()).isEqualTo(SynchronizationType.JIT);
    assertThat(configuration.allowUsersToSignUp()).isFalse();
    assertThat(configuration.provisioningToken()).isNull();
    assertThat(configuration.provisioningGroups()).isEmpty();
  }

  @Test
  public void updateConfiguration_whenIdIsNotGitlabConfiguration_throwsException() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(SynchronizationType.AUTO_PROVISIONING));
    UpdateGitlabConfigurationRequest updateGitlabConfigurationRequest = builder().gitlabConfigurationId("not-gitlab-configuration").build();
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> gitlabConfigurationService.updateConfiguration(updateGitlabConfigurationRequest))
      .withMessage("Gitlab configuration with id not-gitlab-configuration not found");
  }

  @Test
  public void updateConfiguration_whenConfigurationDoesntExist_throwsException() {
    UpdateGitlabConfigurationRequest updateGitlabConfigurationRequest = builder().gitlabConfigurationId("gitlab-configuration").build();
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> gitlabConfigurationService.updateConfiguration(updateGitlabConfigurationRequest))
      .withMessage("GitLab configuration doesn't exist.");
  }

  @Test
  public void updateConfiguration_whenAllUpdateFieldDefined_updatesEverything() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(SynchronizationType.JIT));

    UpdateGitlabConfigurationRequest updateRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .enabled(withValueOrThrow(true))
      .applicationId(withValueOrThrow("applicationId"))
      .url(withValueOrThrow("url"))
      .secret(withValueOrThrow("secret"))
      .synchronizeGroups(withValueOrThrow(true))
      .synchronizationType(withValueOrThrow(SynchronizationType.AUTO_PROVISIONING))
      .allowUserToSignUp(withValueOrThrow(true))
      .provisioningToken(withValueOrThrow("provisioningToken"))
      .provisioningGroups(withValueOrThrow(new LinkedHashSet<>(List.of("group1", "group2", "group3"))))
      .build();

    GitlabConfiguration gitlabConfiguration = gitlabConfigurationService.updateConfiguration(updateRequest);

    verifySettingWasSet(GITLAB_AUTH_ENABLED, "true");
    verifySettingWasSet(GITLAB_AUTH_APPLICATION_ID, "applicationId");
    verifySettingWasSet(GITLAB_AUTH_URL, "url");
    verifySettingWasSet(GITLAB_AUTH_SECRET, "secret");
    verifySettingWasSet(GITLAB_AUTH_SYNC_USER_GROUPS, "true");
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_ENABLED, "true");
    verifySettingWasSet(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP, "true");
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_TOKEN, "provisioningToken");
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_GROUPS, "group1,group2,group3");
    verify(managedInstanceService).queueSynchronisationTask();

    assertConfigurationFields(gitlabConfiguration);
  }

  @Test
  public void updateConfiguration_whenAllUpdateFieldDefinedAndSetToFalse_updatesEverything() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(SynchronizationType.AUTO_PROVISIONING));
    verify(managedInstanceService).queueSynchronisationTask();
    clearInvocations(managedInstanceService);

    UpdateGitlabConfigurationRequest updateRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .enabled(withValueOrThrow(false))
      .synchronizeGroups(withValueOrThrow(false))
      .synchronizationType(withValueOrThrow(SynchronizationType.JIT))
      .allowUserToSignUp(withValueOrThrow(false))
      .build();

    gitlabConfigurationService.updateConfiguration(updateRequest);

    verifySettingWasSet(GITLAB_AUTH_ENABLED, "false");
    verifySettingWasSet(GITLAB_AUTH_SYNC_USER_GROUPS, "false");
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_ENABLED, "false");
    verifySettingWasSet(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP, "false");
    verifyNoMoreInteractions(managedInstanceService);

  }

  @Test
  public void updateConfiguration_whenSwitchingFromAutoToJit_shouldNotScheduleSyncAndCallManagedInstanceChecker() {
    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("12", "12", GitLabIdentityProvider.KEY));
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("34", "34", GitLabIdentityProvider.KEY));
    dbSession.commit();

    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(SynchronizationType.AUTO_PROVISIONING));
    verify(managedInstanceService).queueSynchronisationTask();
    reset(managedInstanceService);

    UpdateGitlabConfigurationRequest updateRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .provisioningToken(withValue(null))
      .synchronizationType(withValueOrThrow(SynchronizationType.JIT))
      .build();

    gitlabConfigurationService.updateConfiguration(updateRequest);

    verifyNoMoreInteractions(managedInstanceService);
    assertThat(dbTester.getDbClient().externalGroupDao().selectByIdentityProvider(dbTester.getSession(), GitLabIdentityProvider.KEY)).isEmpty();
  }

  @Test
  public void updateConfiguration_whenSwitchingToAutoProvisioningAndTheConfigIsNotEnabled_shouldThrow() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(SynchronizationType.JIT));

    UpdateGitlabConfigurationRequest disableRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .enabled(withValueOrThrow(false))
      .build();

    gitlabConfigurationService.updateConfiguration(disableRequest);

    UpdateGitlabConfigurationRequest updateRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .synchronizationType(withValueOrThrow(SynchronizationType.AUTO_PROVISIONING))
      .build();

    assertThatThrownBy(() -> gitlabConfigurationService.updateConfiguration(updateRequest))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("GitLab authentication must be turned on to enable GitLab provisioning.");
    verify(managedInstanceService, times(0)).queueSynchronisationTask();
  }

  @Test
  public void updateConfiguration_whenSwitchingToAutoProvisioningAndProvisioningTokenIsNotDefined_shouldThrow() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(SynchronizationType.JIT));

    UpdateGitlabConfigurationRequest removeTokenRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .provisioningToken(withValue(null))
      .build();

    gitlabConfigurationService.updateConfiguration(removeTokenRequest);

    UpdateGitlabConfigurationRequest updateRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .synchronizationType(withValueOrThrow(SynchronizationType.AUTO_PROVISIONING))
      .build();

    assertThatThrownBy(() -> gitlabConfigurationService.updateConfiguration(updateRequest))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Provisioning token must be set to enable GitLab provisioning.");
    verify(managedInstanceService, times(0)).queueSynchronisationTask();
  }

  private static void assertConfigurationFields(GitlabConfiguration configuration) {
    assertThat(configuration).isNotNull();
    assertThat(configuration.id()).isEqualTo("gitlab-configuration");
    assertThat(configuration.enabled()).isTrue();
    assertThat(configuration.applicationId()).isEqualTo("applicationId");
    assertThat(configuration.url()).isEqualTo("url");
    assertThat(configuration.secret()).isEqualTo("secret");
    assertThat(configuration.synchronizeGroups()).isTrue();
    assertThat(configuration.synchronizationType()).isEqualTo(SynchronizationType.AUTO_PROVISIONING);
    assertThat(configuration.allowUsersToSignUp()).isTrue();
    assertThat(configuration.provisioningToken()).isEqualTo("provisioningToken");
    assertThat(configuration.provisioningGroups()).containsExactlyInAnyOrder("group1", "group2", "group3");
  }

  @Test
  public void createConfiguration_whenConfigurationAlreadyExists_shouldThrow() {
    GitlabConfiguration gitlabConfiguration = buildGitlabConfiguration(SynchronizationType.AUTO_PROVISIONING);
    gitlabConfigurationService.createConfiguration(gitlabConfiguration);

    assertThatThrownBy(() -> gitlabConfigurationService.createConfiguration(gitlabConfiguration))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("GitLab configuration already exists. Only one Gitlab configuration is supported.");
  }

  @Test
  public void createConfiguration_whenAutoProvisioning_shouldCreateCorrectConfigurationAndScheduleSync() {
    GitlabConfiguration configuration = buildGitlabConfiguration(SynchronizationType.AUTO_PROVISIONING);

    GitlabConfiguration createdConfiguration = gitlabConfigurationService.createConfiguration(configuration);

    assertThat(createdConfiguration).isEqualTo(configuration);

    verifyCommonSettings(configuration);

    verify(managedInstanceService).queueSynchronisationTask();

  }

  @Test
  public void createConfiguration_whenAutoProvisioningConfigIsIncorrect_shouldThrow() {
    GitlabConfiguration configuration = new GitlabConfiguration(
      UNIQUE_GITLAB_CONFIGURATION_ID,
      true,
      "applicationId",
      "url",
      "secret",
      true,
      SynchronizationType.AUTO_PROVISIONING,
      true,
      null,
      Set.of("group1", "group2", "group3"));

    assertThatThrownBy(() -> gitlabConfigurationService.createConfiguration(configuration))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Provisioning token must be set to enable GitLab provisioning.");

  }

  @Test
  public void createConfiguration_whenInstanceIsExternallyManaged_shouldThrow() {
    GitlabConfiguration configuration = buildGitlabConfiguration(SynchronizationType.AUTO_PROVISIONING);

    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);
    when(managedInstanceService.getProviderName()).thenReturn("not-gitlab");

    assertThatIllegalStateException()
      .isThrownBy(() -> gitlabConfigurationService.createConfiguration(configuration))
      .withMessage("It is not possible to synchronize SonarQube using GitLab, as it is already managed by not-gitlab.");

  }

  @Test
  public void createConfiguration_whenJitProvisioning_shouldCreateCorrectConfiguration() {
    GitlabConfiguration configuration = buildGitlabConfiguration(SynchronizationType.JIT);

    GitlabConfiguration createdConfiguration = gitlabConfigurationService.createConfiguration(configuration);

    assertThat(createdConfiguration).isEqualTo(configuration);

    verifyCommonSettings(configuration);
    verifyNoInteractions(managedInstanceService);

  }

  @Test
  public void createConfiguration_whenJitProvisioningAndProvisioningTokenNotSet_shouldCreateCorrectConfiguration() {
    GitlabConfiguration configuration = new GitlabConfiguration(
      UNIQUE_GITLAB_CONFIGURATION_ID,
      true,
      "applicationId",
      "url",
      "secret",
      true,
      SynchronizationType.JIT,
      true,
      null,
      Set.of("group1", "group2", "group3"));

    GitlabConfiguration createdConfiguration = gitlabConfigurationService.createConfiguration(configuration);

    assertThat(createdConfiguration).isEqualTo(configuration);

    verifyCommonSettings(configuration);
    verifyNoInteractions(managedInstanceService);

  }

  private void verifyCommonSettings(GitlabConfiguration configuration) {
    verifySettingWasSet(GITLAB_AUTH_ENABLED, String.valueOf(configuration.enabled()));
    verifySettingWasSet(GITLAB_AUTH_APPLICATION_ID, configuration.applicationId());
    verifySettingWasSet(GITLAB_AUTH_URL, configuration.url());
    verifySettingWasSet(GITLAB_AUTH_SECRET, configuration.secret());
    verifySettingWasSet(GITLAB_AUTH_SYNC_USER_GROUPS, String.valueOf(configuration.synchronizeGroups()));
    verifySettingWasSet(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP, String.valueOf(configuration.allowUsersToSignUp()));
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_TOKEN, Strings.nullToEmpty(configuration.provisioningToken()));
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_GROUPS, String.join(",", configuration.provisioningGroups()));
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_ENABLED,
      String.valueOf(configuration.synchronizationType().equals(SynchronizationType.AUTO_PROVISIONING)));
  }

  private void verifySettingWasSet(String setting, @Nullable String value) {
    assertThat(dbTester.getDbClient().propertiesDao().selectGlobalProperty(setting).getValue()).isEqualTo(value);
  }

  @Test
  public void deleteConfiguration_whenIdIsNotGitlabConfiguration_throwsException() {
    assertThatThrownBy(() -> gitlabConfigurationService.deleteConfiguration("not-gitlab-configuration"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Gitlab configuration with id not-gitlab-configuration not found");
  }

  @Test
  public void deleteConfiguration_whenConfigurationDoesntExist_throwsException() {
    assertThatThrownBy(() -> gitlabConfigurationService.deleteConfiguration("gitlab-configuration"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("GitLab configuration doesn't exist.");
  }

  @Test
  public void deleteConfiguration_whenConfigurationExists_shouldDeleteConfiguration() {
    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("12", "12", GitLabIdentityProvider.KEY));
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("34", "34", GitLabIdentityProvider.KEY));
    dbSession.commit();
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(SynchronizationType.AUTO_PROVISIONING));
    gitlabConfigurationService.deleteConfiguration("gitlab-configuration");

    assertPropertyIsDeleted(GITLAB_AUTH_ENABLED);
    assertPropertyIsDeleted(GITLAB_AUTH_APPLICATION_ID);
    assertPropertyIsDeleted(GITLAB_AUTH_URL);
    assertPropertyIsDeleted(GITLAB_AUTH_SECRET);
    assertPropertyIsDeleted(GITLAB_AUTH_SYNC_USER_GROUPS);
    assertPropertyIsDeleted(GITLAB_AUTH_PROVISIONING_ENABLED);
    assertPropertyIsDeleted(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP);
    assertPropertyIsDeleted(GITLAB_AUTH_PROVISIONING_TOKEN);
    assertPropertyIsDeleted(GITLAB_AUTH_PROVISIONING_GROUPS);

    assertThat(dbTester.getDbClient().externalGroupDao().selectByIdentityProvider(dbTester.getSession(), GitLabIdentityProvider.KEY)).isEmpty();
  }

  private void assertPropertyIsDeleted(String property) {
    assertThat(dbTester.getDbClient().propertiesDao().selectGlobalProperty(property)).isNull();
  }

  @Test
  public void triggerRun_whenConfigIsCorrect_shouldTriggerSync() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(SynchronizationType.AUTO_PROVISIONING));
    reset(managedInstanceService);

    gitlabConfigurationService.triggerRun();

    verify(managedInstanceService).queueSynchronisationTask();
  }

  @Test
  public void triggerRun_whenConfigIsForJit_shouldThrow() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(SynchronizationType.JIT));

    assertThatIllegalStateException()
      .isThrownBy(() -> gitlabConfigurationService.triggerRun())
      .withMessage("Auto provisioning must be activated");
  }

  @Test
  public void triggerRun_whenConfigIsDisabled_shouldThrow() {
    GitlabConfiguration gitlabConfiguration = buildGitlabConfiguration(SynchronizationType.AUTO_PROVISIONING);
    gitlabConfigurationService.createConfiguration(gitlabConfiguration);
    gitlabConfigurationService.updateConfiguration(builder().gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID).enabled(withValueOrThrow(false)).build());

    assertThatIllegalStateException()
      .isThrownBy(() -> gitlabConfigurationService.triggerRun())
      .withMessage("GitLab authentication must be turned on to enable GitLab provisioning.");
  }

  @Test
  public void triggerRun_whenProvisioningTokenIsNotSet_shouldThrow() {
    GitlabConfiguration gitlabConfiguration = buildGitlabConfiguration(SynchronizationType.AUTO_PROVISIONING);
    gitlabConfigurationService.createConfiguration(gitlabConfiguration);
    gitlabConfigurationService.updateConfiguration(builder().gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID).provisioningToken(withValue(null)).build());

    assertThatIllegalStateException()
      .isThrownBy(() -> gitlabConfigurationService.triggerRun())
      .withMessage("Provisioning token must be set to enable GitLab provisioning.");
  }

  private static GitlabConfiguration buildGitlabConfiguration(SynchronizationType synchronizationType) {
    return new GitlabConfiguration(
      UNIQUE_GITLAB_CONFIGURATION_ID,
      true,
      "applicationId",
      "url",
      "secret",
      true,
      synchronizationType,
      true,
      "provisioningToken",
      Set.of("group1", "group2", "group3"));
  }
}
