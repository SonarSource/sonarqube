/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;

import static java.lang.String.valueOf;
import static org.sonar.api.PropertyType.BOOLEAN;
import static org.sonar.api.PropertyType.PASSWORD;
import static org.sonar.api.PropertyType.STRING;

public class GitHubSettings {

  public static final String CLIENT_ID = "sonar.auth.github.clientId.secured";
  public static final String CLIENT_SECRET = "sonar.auth.github.clientSecret.secured";
  public static final String ENABLED = "sonar.auth.github.enabled";
  public static final String ALLOW_USERS_TO_SIGN_UP = "sonar.auth.github.allowUsersToSignUp";
  public static final String GROUPS_SYNC = "sonar.auth.github.groupsSync";
  public static final String API_URL = "sonar.auth.github.apiUrl";
  public static final String WEB_URL = "sonar.auth.github.webUrl";
  public static final String ORGANIZATIONS = "sonar.auth.github.organizations";

  private static final String CATEGORY = CoreProperties.CATEGORY_ALM_INTEGRATION;
  private static final String SUBCATEGORY = "github";

  private final Configuration configuration;

  public GitHubSettings(Configuration configuration) {
    this.configuration = configuration;
  }

  String clientId() {
    return configuration.get(CLIENT_ID).orElse("");
  }

  String clientSecret() {
    return configuration.get(CLIENT_SECRET).orElse("");
  }

  boolean isEnabled() {
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
  String apiURL() {
    return urlWithEndingSlash(configuration.get(API_URL).orElse(""));
  }

  String[] organizations() {
    return configuration.getStringArray(ORGANIZATIONS);
  }

  @CheckForNull
  private static String urlWithEndingSlash(@Nullable String url) {
    if (url != null && !url.endsWith("/")) {
      return url + "/";
    }
    return url;
  }

  public static List<PropertyDefinition> definitions() {
    return Arrays.asList(
      PropertyDefinition.builder(ENABLED)
        .name("Enabled")
        .description("Enable GitHub users to login. Value is ignored if client ID and secret are not defined.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(false))
        .index(1)
        .build(),
      PropertyDefinition.builder(CLIENT_ID)
        .name("Client ID")
        .description("Client ID provided by GitHub when registering the application.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(2)
        .build(),
      PropertyDefinition.builder(CLIENT_SECRET)
        .name("Client Secret")
        .description("Client password provided by GitHub when registering the application.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(PASSWORD)
        .index(3)
        .build(),
      PropertyDefinition.builder(ALLOW_USERS_TO_SIGN_UP)
        .name("Allow users to sign-up")
        .description("Allow new users to authenticate. When set to 'false', only existing users will be able to authenticate to the server.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(true))
        .index(4)
        .build(),
      PropertyDefinition.builder(GROUPS_SYNC)
        .name("Synchronize teams as groups")
        .description("For each team they belong to, the user will be associated to a group named 'Organisation/Team' (if it exists) in SonarQube.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(false))
        .index(6)
        .build(),
      PropertyDefinition.builder(API_URL)
        .name("The API url for a GitHub instance.")
        .description("The API url for a GitHub instance. https://api.github.com/ for Github.com, https://github.company.com/api/v3/ when using Github Enterprise")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(STRING)
        .defaultValue("https://api.github.com/")
        .index(7)
        .build(),
      PropertyDefinition.builder(WEB_URL)
        .name("The WEB url for a GitHub instance.")
        .description("The WEB url for a GitHub instance. " +
          "https://github.com/ for Github.com, https://github.company.com/ when using GitHub Enterprise.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(STRING)
        .defaultValue("https://github.com/")
        .index(8)
        .build(),
      PropertyDefinition.builder(ORGANIZATIONS)
        .name("Organizations")
        .description("Only members of these organizations will be able to authenticate to the server. " +
          "If a user is a member of any of the organizations listed they will be authenticated.")
        .multiValues(true)
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(9)
        .build());
  }
}
