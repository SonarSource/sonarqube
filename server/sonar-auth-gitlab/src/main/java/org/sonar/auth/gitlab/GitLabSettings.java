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
package org.sonar.auth.gitlab;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.sonar.api.PropertyType;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;

import static java.lang.String.valueOf;
import static org.sonar.api.PropertyType.BOOLEAN;
import static org.sonar.api.PropertyType.PASSWORD;

@ComputeEngineSide
public class GitLabSettings {

  public static final String GITLAB_AUTH_ENABLED = "sonar.auth.gitlab.enabled";
  public static final String GITLAB_AUTH_URL = "sonar.auth.gitlab.url";
  public static final String GITLAB_AUTH_APPLICATION_ID = "sonar.auth.gitlab.applicationId.secured";
  public static final String GITLAB_AUTH_SECRET = "sonar.auth.gitlab.secret.secured";
  public static final String GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP = "sonar.auth.gitlab.allowUsersToSignUp";
  public static final String GITLAB_AUTH_SYNC_USER_GROUPS = "sonar.auth.gitlab.groupsSync";
  public static final String GITLAB_AUTH_PROVISIONING_TOKEN = "provisioning.gitlab.token.secured";
  public static final String GITLAB_AUTH_PROVISIONING_GROUPS = "provisioning.gitlab.groups";
  public static final String GITLAB_AUTH_PROVISIONING_ENABLED = "provisioning.gitlab.enabled";

  private static final String CATEGORY = "authentication";
  private static final String SUBCATEGORY = "gitlab";

  private final Configuration configuration;

  public GitLabSettings(Configuration configuration) {
    this.configuration = configuration;
  }

  public String url() {
    String url = configuration.get(GITLAB_AUTH_URL).orElse(null);
    if (url != null && url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }

  public String applicationId() {
    return configuration.get(GITLAB_AUTH_APPLICATION_ID).orElse(null);
  }

  public String secret() {
    return configuration.get(GITLAB_AUTH_SECRET).orElse(null);
  }

  public boolean isEnabled() {
    return configuration.getBoolean(GITLAB_AUTH_ENABLED).orElse(false) && applicationId() != null && secret() != null;
  }

  public boolean allowUsersToSignUp() {
    return configuration.getBoolean(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP).orElse(false);
  }

  public boolean syncUserGroups() {
    return configuration.getBoolean(GITLAB_AUTH_SYNC_USER_GROUPS).orElse(false);
  }

  public String provisioningToken() {
    return configuration.get(GITLAB_AUTH_PROVISIONING_TOKEN).orElse(null);
  }

  public Set<String> provisioningGroups() {
    return Set.of(configuration.getStringArray(GITLAB_AUTH_PROVISIONING_GROUPS));
  }

  public boolean isProvisioningEnabled() {
    return isEnabled() && configuration.getBoolean(GITLAB_AUTH_PROVISIONING_ENABLED).orElse(false);
  }

  static List<PropertyDefinition> definitions() {
    return Arrays.asList(
      PropertyDefinition.builder(GITLAB_AUTH_ENABLED)
        .name("Enabled")
        .description("Enable Gitlab users to login. Value is ignored if URL, Application ID, and Secret are not set.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(false))
        .index(1)
        .build(),
      PropertyDefinition.builder(GITLAB_AUTH_URL)
        .name("GitLab URL")
        .description("URL to access GitLab.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .defaultValue("https://gitlab.com")
        .index(2)
        .build(),
      PropertyDefinition.builder(GITLAB_AUTH_APPLICATION_ID)
        .name("Application ID")
        .description("Application ID provided by GitLab when registering the application.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(3)
        .build(),
      PropertyDefinition.builder(GITLAB_AUTH_SECRET)
        .name("Secret")
        .description("Secret provided by GitLab when registering the application.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(PASSWORD)
        .index(4)
        .build(),
      PropertyDefinition.builder(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP)
        .name("Allow users to sign up")
        .description("Allow new users to authenticate. When set to 'false', only existing users will be able to authenticate to the server.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(true))
        .index(5)
        .build(),
      PropertyDefinition.builder(GITLAB_AUTH_SYNC_USER_GROUPS)
        .deprecatedKey("sonar.auth.gitlab.sync_user_groups")
        .name("Synchronize user groups")
        .description("For each GitLab group they belong to, the user will be associated to a group with the same name (if it exists) in SonarQube." +
          " If enabled, the GitLab Oauth2 application will need to provide the api scope.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(PropertyType.BOOLEAN)
        .defaultValue(valueOf(false))
        .index(6)
        .build(),
      PropertyDefinition.builder(GITLAB_AUTH_PROVISIONING_TOKEN)
        .name("Provisioning token")
        .description("Token used for provisioning users. Both a group or a personal access token can be used as soon as it has visibility on desired groups.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(PASSWORD)
        .index(7)
        .build(),
      PropertyDefinition.builder(GITLAB_AUTH_PROVISIONING_GROUPS)
        .name("Groups")
        .description("Only members of these groups (and sub-groups) will be provisioned. Please enter the group slug as it appears in GitLab URL, for instance `my-gitlab-group`.")
        .multiValues(true)
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(8)
        .build(),
      PropertyDefinition.builder(GITLAB_AUTH_PROVISIONING_ENABLED)
        .name("Provisioning enabled")
        .description("Enable Gitlab provisioning for users.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(false))
        .index(9)
        .build());

  }
}
