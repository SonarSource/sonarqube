/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.io.IOException;
import org.sonar.api.config.Settings;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;

import static com.google.common.collect.Sets.newHashSet;

public class FakeBaseIdProvider implements BaseIdentityProvider {

  private static final String ENABLED = "sonar.auth.fake-base-id-provider.enabled";
  private static final String ALLOWS_USERS_TO_SIGN_UP = "sonar.auth.fake-base-id-provider.allowsUsersToSignUp";
  private static final String ENABLED_GROUPS_SYNC = "sonar.auth.fake-base-id-provider.enabledGroupsSync";
  private static final String GROUPS = "sonar.auth.fake-base-id-provider.groups";

  private static final String USER_INFO = "sonar.auth.fake-base-id-provider.user";

  private static final String THROW_UNAUTHORIZED_EXCEPTION = "sonar.auth.fake-base-id-provider.throwUnauthorizedMessage";

  private final Settings settings;

  public FakeBaseIdProvider(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void init(Context context) {
    String userInfoProperty = settings.getString(USER_INFO);
    if (userInfoProperty == null) {
      throw new IllegalStateException(String.format("The property %s is required", USER_INFO));
    }
    boolean throwUnauthorizedException = settings.getBoolean(THROW_UNAUTHORIZED_EXCEPTION);
    if (throwUnauthorizedException) {
      throw new UnauthorizedException("A functional error has happened");
    }

    String[] userInfos = userInfoProperty.split(",");
    UserIdentity.Builder builder = UserIdentity.builder()
      .setLogin(userInfos[0])
      .setProviderLogin(userInfos[1])
      .setName(userInfos[2])
      .setEmail(userInfos[3]);

    if (settings.getBoolean(ENABLED_GROUPS_SYNC)) {
      builder.setGroups(newHashSet(settings.getStringArray(GROUPS)));
    }

    context.authenticate(builder.build());
    try {
      context.getResponse().sendRedirect("/");
    } catch (IOException e) {
      throw new IllegalStateException("Fail to redirect to home", e);
    }
  }

  @Override
  public String getKey() {
    return "fake-base-id-provider";
  }

  @Override
  public String getName() {
    return "Fake base identity provider";
  }

  @Override
  public Display getDisplay() {
    return Display.builder()
      .setIconPath("/static/baseauthplugin/base.png")
      .setBackgroundColor("#205081")
      .build();
  }

  @Override
  public boolean isEnabled() {
    return settings.getBoolean(ENABLED);
  }

  @Override
  public boolean allowsUsersToSignUp() {
    if (settings.hasKey(ALLOWS_USERS_TO_SIGN_UP)) {
      return settings.getBoolean(ALLOWS_USERS_TO_SIGN_UP);
    }
    // If property is not defined, default behaviour is not always allow users to sign up
    return true;
  }
}
