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
package org.sonar.server.almsettings.ws;

import org.sonar.core.platform.Module;

public class AlmSettingsWsModule extends Module {
  @Override
  protected void configureModule() {
    add(
      AlmSettingsWs.class,
      AlmSettingsSupport.class,
      DeleteAction.class,
      ListAction.class,
      ListDefinitionsAction.class,
      ValidateAction.class,
      CountBindingAction.class,
      GetBindingAction.class,
      //Azure alm settings,
      CreateAzureAction.class,
      UpdateAzureAction.class,
      //Github alm settings
      CreateGithubAction.class,
      UpdateGithubAction.class,
      //Gitlab alm settings
      CreateGitlabAction.class,
      UpdateGitlabAction.class,
      //Bitbucket alm settings
      CreateBitBucketAction.class,
      UpdateBitbucketAction.class,
      //BitbucketCloud alm settings
      CreateBitbucketCloudAction.class,
      UpdateBitbucketCloudAction.class
    );
  }
}
