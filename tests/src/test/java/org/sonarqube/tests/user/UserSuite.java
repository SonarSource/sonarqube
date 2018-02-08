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

package org.sonarqube.tests.user;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  BaseIdentityProviderTest.class,
  FavoritesWsTest.class,
  ForceAuthenticationTest.class,
  LocalAuthenticationTest.class,
  MyAccountPageTest.class,
  NotificationsWsTest.class,
  OAuth2IdentityProviderTest.class,
  RootUserInStandaloneModeTest.class,
  UsersPageTest.class
})
public class UserSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())

    // Used in BaseIdentityProviderTest
    .addPlugin(pluginArtifact("base-auth-plugin"))

    // Used in OAuth2IdentityProviderTest
    .addPlugin(pluginArtifact("oauth2-auth-plugin"))

    // reduce memory for Elasticsearch
    .setServerProperty("sonar.search.javaOpts", "-Xms128m -Xmx128m")

    .build();

}
