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
package org.sonar.auth.bitbucket;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.server.ServerSide;

@ServerSide
public class BitbucketSettings {

  private static final Supplier<? extends IllegalStateException> DEFAULT_VALUE_MISSING = () -> new IllegalStateException("Should have a default value");
  public static final String CONSUMER_KEY = "sonar.auth.bitbucket.clientId.secured";
  public static final String CONSUMER_SECRET = "sonar.auth.bitbucket.clientSecret.secured";
  public static final String ENABLED = "sonar.auth.bitbucket.enabled";
  public static final String ALLOW_USERS_TO_SIGN_UP = "sonar.auth.bitbucket.allowUsersToSignUp";
  public static final String WORKSPACE_ALLOWED_LIST = "sonar.auth.bitbucket.workspaces";
  public static final String DEFAULT_API_URL = "https://api.bitbucket.org/";
  public static final String DEFAULT_WEB_URL = "https://bitbucket.org/";
  public static final String SUBCATEGORY = "bitbucket";

  private final Configuration config;

  public BitbucketSettings(Configuration config) {
    this.config = config;
  }

  @CheckForNull
  public String clientId() {
    return config.get(CONSUMER_KEY).orElse(null);
  }

  @CheckForNull
  public String clientSecret() {
    return config.get(CONSUMER_SECRET).orElse(null);
  }

  public boolean isEnabled() {
    return config.getBoolean(ENABLED).orElseThrow(DEFAULT_VALUE_MISSING) && clientId() != null && clientSecret() != null;
  }

  public boolean allowUsersToSignUp() {
    return config.getBoolean(ALLOW_USERS_TO_SIGN_UP).orElseThrow(DEFAULT_VALUE_MISSING);
  }

  public String[] workspaceAllowedList() {
    return config.getStringArray(WORKSPACE_ALLOWED_LIST);
  }

  public String webURL() {
    return DEFAULT_WEB_URL;
  }

  public String apiURL() {
    return DEFAULT_API_URL;
  }

  public static List<PropertyDefinition> definitions() {
    return Arrays.asList(
      PropertyDefinition.builder(ENABLED)
        .name("Enabled")
        .description("Enable Bitbucket users to login. Value is ignored if consumer key and secret are not defined.")
        .category(CoreProperties.CATEGORY_ALM_INTEGRATION)
        .subCategory(SUBCATEGORY)
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .index(1)
        .build(),
      PropertyDefinition.builder(CONSUMER_KEY)
        .name("OAuth consumer key")
        .description("Consumer key provided by Bitbucket when registering the consumer.")
        .category(CoreProperties.CATEGORY_ALM_INTEGRATION)
        .subCategory(SUBCATEGORY)
        .index(2)
        .build(),
      PropertyDefinition.builder(CONSUMER_SECRET)
        .name("OAuth consumer secret")
        .description("Consumer secret provided by Bitbucket when registering the consumer.")
        .category(CoreProperties.CATEGORY_ALM_INTEGRATION)
        .subCategory(SUBCATEGORY)
        .index(3)
        .build(),
      PropertyDefinition.builder(ALLOW_USERS_TO_SIGN_UP)
        .name("Allow users to sign-up")
        .description("Allow new users to authenticate. When set to 'false', only existing users will be able to authenticate.")
        .category(CoreProperties.CATEGORY_ALM_INTEGRATION)
        .subCategory(SUBCATEGORY)
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(true))
        .index(4)
        .build(),
      PropertyDefinition.builder(WORKSPACE_ALLOWED_LIST)
        .name("Workspaces")
        .description("Only members of at least one of these workspace will be able to authenticate. Keep empty to disable workspace restriction. You can use either the workspace name, or the workspace slug (ex: https://bitbucket.org/{workspace-slug}).")
        .category(CoreProperties.CATEGORY_ALM_INTEGRATION)
        .subCategory(SUBCATEGORY)
        .multiValues(true)
        .index(5)
        .build()
    );
  }

}
