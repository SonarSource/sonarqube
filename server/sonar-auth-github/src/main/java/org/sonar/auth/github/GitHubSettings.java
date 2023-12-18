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
package org.sonar.auth.github;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.PropertyType;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.DevOpsPlatformSettings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.server.property.InternalProperties;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.api.PropertyType.BOOLEAN;
import static org.sonar.api.PropertyType.PASSWORD;
import static org.sonar.api.PropertyType.STRING;
import static org.sonar.api.utils.Preconditions.checkState;

@ServerSide
@ComputeEngineSide
public class GitHubSettings implements DevOpsPlatformSettings {

  public static final String CLIENT_ID = "sonar.auth.github.clientId.secured";
  public static final String CLIENT_SECRET = "sonar.auth.github.clientSecret.secured";
  public static final String APP_ID = "sonar.auth.github.appId";
  public static final String PRIVATE_KEY = "sonar.auth.github.privateKey.secured";
  public static final String ENABLED = "sonar.auth.github.enabled";
  public static final String ALLOW_USERS_TO_SIGN_UP = "sonar.auth.github.allowUsersToSignUp";
  public static final String GROUPS_SYNC = "sonar.auth.github.groupsSync";
  public static final String API_URL = "sonar.auth.github.apiUrl";
  public static final String DEFAULT_API_URL = "https://api.github.com/";
  public static final String WEB_URL = "sonar.auth.github.webUrl";
  public static final String DEFAULT_WEB_URL = "https://github.com/";
  public static final String ORGANIZATIONS = "sonar.auth.github.organizations";
  @VisibleForTesting
  static final String PROVISIONING = "provisioning.github.enabled";
  @VisibleForTesting
  static final String PROVISION_VISIBILITY = "provisioning.github.project.visibility.enabled";
  @VisibleForTesting
  static final String USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE = "sonar.auth.github.userConsentForPermissionProvisioningRequired";

  private static final String CATEGORY = "authentication";
  private static final String SUBCATEGORY = "github";

  private final Configuration configuration;

  private final InternalProperties internalProperties;
  private final DbClient dbClient;

  public GitHubSettings(Configuration configuration, InternalProperties internalProperties, DbClient dbClient) {
    this.configuration = configuration;
    this.internalProperties = internalProperties;
    this.dbClient = dbClient;
  }

  String clientId() {
    return configuration.get(CLIENT_ID).orElse("");
  }

  String clientSecret() {
    return configuration.get(CLIENT_SECRET).orElse("");
  }

  public String appId() {
    return configuration.get(APP_ID).orElse("");
  }

  public String privateKey() {
    return configuration.get(PRIVATE_KEY).orElse("");
  }

  public boolean isEnabled() {
    return configuration.getBoolean(ENABLED).orElse(false) && !clientId().isEmpty() && !clientSecret().isEmpty();
  }

  boolean allowUsersToSignUp() {
    return configuration.getBoolean(ALLOW_USERS_TO_SIGN_UP).orElse(false);
  }

  boolean syncGroups() {
    return configuration.getBoolean(GROUPS_SYNC).orElse(false);
  }

  @CheckForNull
  String webURL() {
    return urlWithEndingSlash(configuration.get(WEB_URL).orElse(""));
  }

  @CheckForNull
  public String apiURL() {
    return urlWithEndingSlash(configuration.get(API_URL).orElse(""));
  }

  public String apiURLOrDefault() {
    return configuration.get(API_URL).map(GitHubSettings::urlWithEndingSlash).orElse(DEFAULT_API_URL);
  }

  public Set<String> getOrganizations() {
    return Set.of(configuration.getStringArray(ORGANIZATIONS));
  }

  @CheckForNull
  private static String urlWithEndingSlash(@Nullable String url) {
    if (url != null && !url.endsWith("/")) {
      return url + "/";
    }
    return url;
  }

  public void setProvisioning(boolean enableProvisioning) {
    if (enableProvisioning) {
      checkGithubConfigIsCompleteForProvisioning();
    } else {
      removeExternalGroupsForGithub();
    }
    internalProperties.write(PROVISIONING, String.valueOf(enableProvisioning));
  }

  private void removeExternalGroupsForGithub() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.externalGroupDao().deleteByExternalIdentityProvider(dbSession, GitHubIdentityProvider.KEY);
      dbSession.commit();
    }
  }

  private void checkGithubConfigIsCompleteForProvisioning() {
    checkState(isEnabled(), getErrorMessage("GitHub authentication must be enabled"));
    checkState(isNotBlank(appId()), getErrorMessage("Application ID must be provided"));
    checkState(isNotBlank(privateKey()), getErrorMessage("Private key must be provided"));
  }

  private static String getErrorMessage(String prefix) {
    return format("%s to enable GitHub provisioning.", prefix);
  }

  @Override
  public String getDevOpsPlatform() {
    return ALM.GITHUB.getId();
  }

  @Override
  public boolean isProvisioningEnabled() {
    return isEnabled() && internalProperties.read(PROVISIONING).map(Boolean::parseBoolean).orElse(false);
  }

  public boolean isUserConsentRequiredAfterUpgrade() {
    return configuration.get(USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE).isPresent();
  }

  @Override
  public boolean isProjectVisibilitySynchronizationActivated() {
    return configuration.getBoolean(PROVISION_VISIBILITY).orElse(true);
  }

  public static List<PropertyDefinition> definitions() {
    int index = 1;
    return Arrays.asList(
      PropertyDefinition.builder(ENABLED)
        .name("Enabled")
        .description("Enable GitHub users to login. Value is ignored if client ID and secret are not defined.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(false))
        .index(index++)
        .build(),
      PropertyDefinition.builder(CLIENT_ID)
        .name("Client ID")
        .description("Client ID provided by GitHub when registering the application.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(index++)
        .build(),
      PropertyDefinition.builder(CLIENT_SECRET)
        .name("Client Secret")
        .description("Client password provided by GitHub when registering the application.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(PASSWORD)
        .index(index++)
        .build(),
      PropertyDefinition.builder(APP_ID)
        .name("App ID")
        .description("The App ID is found on your GitHub App's page on GitHub at Settings > Developer Settings > GitHub Apps.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(STRING)
        .index(index++)
        .build(),
      PropertyDefinition.builder(PRIVATE_KEY)
        .name("Private Key")
        .description("""
          Your GitHub App's private key. You can generate a .pem file from your GitHub App's page under Private keys.
          Copy and paste the whole contents of the file here.""")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(PropertyType.TEXT)
        .index(index++)
        .build(),
      PropertyDefinition.builder(ALLOW_USERS_TO_SIGN_UP)
        .name("Allow users to sign up")
        .description("Allow new users to authenticate. When set to disabled, only existing users will be able to authenticate to the server.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(true))
        .index(index++)
        .build(),
      PropertyDefinition.builder(GROUPS_SYNC)
        .name("Synchronize teams as groups")
        .description("Synchronize GitHub team with SonarQube group memberships when users log in to SonarQube."
          + " For each GitHub team they belong to, users will be associated to a group of the same name if it exists in SonarQube.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(false))
        .index(index++)
        .build(),
      PropertyDefinition.builder(API_URL)
        .name("The API url for a GitHub instance.")
        .description(String.format("The API url for a GitHub instance. %s for Github.com, https://github.company.com/api/v3/ when using Github Enterprise", DEFAULT_API_URL))
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(STRING)
        .defaultValue(DEFAULT_API_URL)
        .index(index++)
        .build(),
      PropertyDefinition.builder(WEB_URL)
        .name("The WEB url for a GitHub instance.")
        .description(String.format("The WEB url for a GitHub instance. %s for Github.com, https://github.company.com/ when using GitHub Enterprise.", DEFAULT_WEB_URL))
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(STRING)
        .defaultValue(DEFAULT_WEB_URL)
        .index(index++)
        .build(),
      PropertyDefinition.builder(ORGANIZATIONS)
        .name("Organizations")
        .description("Only members of these organizations will be able to authenticate to the server. "
          + "⚠ if not set, users from any organization where the GitHub App is installed will be able to login to this SonarQube instance.")
        .multiValues(true)
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(index)
        .build(),
      PropertyDefinition.builder(PROVISION_VISIBILITY)
        .name("Provision project visibility")
        .description("Change project visibility based on GitHub repository visibility. If disabled, every provisioned project will be private in SonarQube and visible only"
          + " to users with explicit GitHub permissions for the corresponding repository. Changes take effect at the next synchronization.")
        .type(BOOLEAN)
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .defaultValue(valueOf(true))
        .index(index)
        .build());
  }
}
