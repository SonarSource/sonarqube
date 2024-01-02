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
package org.sonar.auth.github;

import java.util.Optional;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.provisioning.GithubOrganizationGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.property.InternalProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.auth.github.GitHubSettings.PROVISION_VISIBILITY;
import static org.sonar.auth.github.GitHubSettings.USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE;

public class GitHubSettingsIT {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final MapSettings settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, GitHubSettings.definitions()));
  private final InternalProperties internalProperties = mock(InternalProperties.class);

  private final GitHubSettings underTest = new GitHubSettings(settings.asConfig(), internalProperties, db.getDbClient());

  @Test
  public void is_enabled() {
    enableGithubAuthentication();

    assertThat(underTest.isEnabled()).isTrue();

    settings.setProperty("sonar.auth.github.enabled", false);
    assertThat(underTest.isEnabled()).isFalse();
  }

  @Test
  public void is_enabled_always_return_false_when_client_id_is_null() {
    settings.setProperty("sonar.auth.github.enabled", true);
    settings.setProperty("sonar.auth.github.clientId.secured", (String) null);
    settings.setProperty("sonar.auth.github.clientSecret.secured", "secret");

    assertThat(underTest.isEnabled()).isFalse();
  }

  @Test
  public void is_enabled_always_return_false_when_client_secret_is_null() {
    settings.setProperty("sonar.auth.github.enabled", true);
    settings.setProperty("sonar.auth.github.clientId.secured", "id");
    settings.setProperty("sonar.auth.github.clientSecret.secured", (String) null);

    assertThat(underTest.isEnabled()).isFalse();
  }

  @Test
  public void isProvisioningEnabled_returnsFalseByDefault() {
    enableGithubAuthentication();
    when(internalProperties.read(GitHubSettings.PROVISIONING)).thenReturn(Optional.empty());
    assertThat(underTest.isProvisioningEnabled()).isFalse();
  }

  @Test
  public void isProvisioningEnabled_ifProvisioningEnabledButGithubAuthNotSet_returnsFalse() {
    enableGithubAuthentication();
    when(internalProperties.read(GitHubSettings.PROVISIONING)).thenReturn(Optional.of(Boolean.FALSE.toString()));
    assertThat(underTest.isProvisioningEnabled()).isFalse();
  }

  @Test
  public void isProvisioningEnabled_ifProvisioningEnabledButGithubAuthDisabled_returnsFalse() {
    when(internalProperties.read(GitHubSettings.PROVISIONING)).thenReturn(Optional.of(Boolean.TRUE.toString()));
    assertThat(underTest.isProvisioningEnabled()).isFalse();
  }

  @Test
  public void isProvisioningEnabled_ifProvisioningEnabledAndGithubAuthEnabled_returnsTrue() {
    enableGithubAuthenticationWithGithubApp();
    when(internalProperties.read(GitHubSettings.PROVISIONING)).thenReturn(Optional.of(Boolean.TRUE.toString()));
    assertThat(underTest.isProvisioningEnabled()).isTrue();
  }

  @Test
  public void isUserConsentRequiredAfterUpgrade_returnsFalseByDefault() {
    assertThat(underTest.isUserConsentRequiredAfterUpgrade()).isFalse();
  }

  @Test
  public void isUserConsentRequiredAfterUpgrade_returnsTrueIfPropertyPresent() {
    settings.setProperty(USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE, "");
    assertThat(underTest.isUserConsentRequiredAfterUpgrade()).isTrue();
  }

  @Test
  public void isProjectVisibilitySynchronizationActivated_whenPropertyNotSet_returnsTrueByDefault() {
    assertThat(underTest.isProjectVisibilitySynchronizationActivated()).isTrue();
  }

  @Test
  public void isProjectVisibilitySynchronizationActivated_whenPropertyIsSetToFalse_returnsFalse() {
    settings.setProperty(PROVISION_VISIBILITY, "false");
    assertThat(underTest.isProjectVisibilitySynchronizationActivated()).isFalse();
  }

  @Test
  public void isProjectVisibilitySynchronizationActivated_whenPropertyIsSetToTrue_returnsTrue() {
    settings.setProperty(PROVISION_VISIBILITY, "true");
    assertThat(underTest.isProjectVisibilitySynchronizationActivated()).isTrue();
  }

  @Test
  public void setProvisioning_whenGitHubAuthDisabled_shouldThrow() {
    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.setProvisioning(true))
      .withMessage("GitHub authentication must be enabled to enable GitHub provisioning.");
    assertThat(underTest.isProvisioningEnabled()).isFalse();
  }

  @Test
  public void setProvisioning_whenPrivateKeyMissing_shouldThrow() {
    enableGithubAuthenticationWithGithubApp();
    settings.setProperty("sonar.auth.github.privateKey.secured", "");

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.setProvisioning(true))
      .withMessage("Private key must be provided to enable GitHub provisioning.");
    assertThat(underTest.isProvisioningEnabled()).isFalse();
  }

  @Test
  public void setProvisioning_whenAppIdMissing_shouldThrow() {
    enableGithubAuthenticationWithGithubApp();
    settings.setProperty("sonar.auth.github.appId", "");

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.setProvisioning(true))
      .withMessage("Application ID must be provided to enable GitHub provisioning.");
    assertThat(underTest.isProvisioningEnabled()).isFalse();
  }

  @Test
  public void setProvisioning_whenPassedTrue_delegatesToInternalPropertiesWrite() {
    enableGithubAuthenticationWithGithubApp();
    underTest.setProvisioning(true);
    verify(internalProperties).write(GitHubSettings.PROVISIONING, Boolean.TRUE.toString());
  }

  @Test
  public void setProvisioning_whenPassedFalse_delegatesToInternalPropertiesWriteAndCleansUpExternalGroups() {
    createGithubManagedGroup();
    createGitHubOrganizationGroup();

    underTest.setProvisioning(false);

    verify(internalProperties).write(GitHubSettings.PROVISIONING, Boolean.FALSE.toString());
    assertThat(db.getDbClient().externalGroupDao().selectByIdentityProvider(db.getSession(), GitHubIdentityProvider.KEY)).isEmpty();
    assertThat(db.getDbClient().githubOrganizationGroupDao().findAll(db.getSession())).isEmpty();
  }

  private void createGithubManagedGroup() {
    GroupDto groupDto = db.users().insertGroup();
    db.users().markGroupAsGithubManaged(groupDto.getUuid());
  }

  private void createGitHubOrganizationGroup() {
    GroupDto groupDto = db.users().insertGroup();
    db.getDbClient().githubOrganizationGroupDao().insert(db.getSession(), new GithubOrganizationGroupDto(groupDto.getUuid(), "org1"));
    db.commit();
  }

  @Test
  public void return_client_id() {
    settings.setProperty("sonar.auth.github.clientId.secured", "id");
    assertThat(underTest.clientId()).isEqualTo("id");
  }

  @Test
  public void return_client_secret() {
    settings.setProperty("sonar.auth.github.clientSecret.secured", "secret");
    assertThat(underTest.clientSecret()).isEqualTo("secret");
  }

  @Test
  public void return_app_id() {
    settings.setProperty("sonar.auth.github.appId", "secret");
    assertThat(underTest.appId()).isEqualTo("secret");
  }

  @Test
  public void return_private_key() {
    settings.setProperty("sonar.auth.github.privateKey.secured", "secret");
    assertThat(underTest.privateKey()).isEqualTo("secret");
  }

  @Test
  public void allow_users_to_sign_up() {
    settings.setProperty("sonar.auth.github.allowUsersToSignUp", "true");
    assertThat(underTest.allowUsersToSignUp()).isTrue();

    settings.setProperty("sonar.auth.github.allowUsersToSignUp", "false");
    assertThat(underTest.allowUsersToSignUp()).isFalse();

    // default value
    settings.setProperty("sonar.auth.github.allowUsersToSignUp", (String) null);
    assertThat(underTest.allowUsersToSignUp()).isTrue();
  }

  @Test
  public void sync_groups() {
    settings.setProperty("sonar.auth.github.groupsSync", "true");
    assertThat(underTest.syncGroups()).isTrue();

    settings.setProperty("sonar.auth.github.groupsSync", "false");
    assertThat(underTest.syncGroups()).isFalse();

    // default value
    settings.setProperty("sonar.auth.github.groupsSync", (String) null);
    assertThat(underTest.syncGroups()).isFalse();
  }

  @Test
  public void apiUrl_must_have_ending_slash() {
    settings.setProperty("sonar.auth.github.apiUrl", "https://github.com");
    assertThat(underTest.apiURL()).isEqualTo("https://github.com/");

    settings.setProperty("sonar.auth.github.apiUrl", "https://github.com/");
    assertThat(underTest.apiURL()).isEqualTo("https://github.com/");
  }

  @Test
  public void webUrl_must_have_ending_slash() {
    settings.setProperty("sonar.auth.github.webUrl", "https://github.com");
    assertThat(underTest.webURL()).isEqualTo("https://github.com/");

    settings.setProperty("sonar.auth.github.webUrl", "https://github.com/");
    assertThat(underTest.webURL()).isEqualTo("https://github.com/");
  }

  @Test
  public void return_organizations_single() {
    String setting = "example";
    settings.setProperty("sonar.auth.github.organizations", setting);
    Set<String> actual = underTest.getOrganizations();
    assertThat(actual).containsOnly(setting);
  }

  @Test
  public void return_organizations_multiple() {
    String setting = "example0,example1";
    settings.setProperty("sonar.auth.github.organizations", setting);
    Set<String> actual = underTest.getOrganizations();
    assertThat(actual).containsOnly("example0", "example1");
  }

  @Test
  public void return_organizations_empty_list() {
    String[] setting = null;
    settings.setProperty("sonar.auth.github.organizations", setting);
    Set<String> actual = underTest.getOrganizations();
    assertThat(actual).isEmpty();
  }

  @Test
  public void definitions() {
    assertThat(GitHubSettings.definitions().stream()
      .map(PropertyDefinition::name))
      .containsExactly(
        "Enabled",
        "Client ID",
        "Client Secret",
        "App ID",
        "Private Key",
        "Allow users to sign up",
        "Synchronize teams as groups",
        "The API url for a GitHub instance.",
        "The WEB url for a GitHub instance.",
        "Organizations",
        "Provision project visibility");
  }

  private void enableGithubAuthentication() {
    settings.setProperty("sonar.auth.github.clientId.secured", "id");
    settings.setProperty("sonar.auth.github.clientSecret.secured", "secret");
    settings.setProperty("sonar.auth.github.enabled", true);
  }

  private void enableGithubAuthenticationWithGithubApp() {
    enableGithubAuthentication();
    settings.setProperty("sonar.auth.github.appId", "id");
    settings.setProperty("sonar.auth.github.privateKey.secured", "secret");
  }
}
