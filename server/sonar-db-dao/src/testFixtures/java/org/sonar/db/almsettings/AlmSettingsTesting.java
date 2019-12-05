/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.almsettings;

import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.ComponentDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

public class AlmSettingsTesting {

  public static AlmSettingDto newGithubAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(randomAlphanumeric(200))
      .setUrl(randomAlphanumeric(2000))
      .setAppId(randomAlphanumeric(80))
      .setPrivateKey(randomAlphanumeric(2000))
      .setAlm(ALM.GITHUB);
  }

  public static AlmSettingDto newAzureAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(randomAlphanumeric(200))
      .setPersonalAccessToken(randomAlphanumeric(2000))
      .setAlm(ALM.AZURE_DEVOPS);
  }

  public static AlmSettingDto newGitlabAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(randomAlphanumeric(200))
      .setPersonalAccessToken(randomAlphanumeric(2000))
      .setAlm(ALM.GITLAB);
  }

  public static AlmSettingDto newBitbucketAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(randomAlphanumeric(200))
      .setUrl(randomAlphanumeric(2000))
      .setPersonalAccessToken(randomAlphanumeric(2000))
      .setAlm(ALM.BITBUCKET);
  }

  public static ProjectAlmSettingDto newGithubProjectAlmSettingDto(AlmSettingDto githubAlmSetting, ComponentDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(githubAlmSetting.getUuid())
      .setProjectUuid(project.uuid())
      .setAlmRepo(randomAlphanumeric(256));
  }

  static ProjectAlmSettingDto newAzureProjectAlmSettingDto(AlmSettingDto azureAlmSetting, ComponentDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(azureAlmSetting.getUuid())
      .setProjectUuid(project.uuid());
  }

  static ProjectAlmSettingDto newGitlabProjectAlmSettingDto(AlmSettingDto gitlabAlmSetting, ComponentDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(gitlabAlmSetting.getUuid())
      .setProjectUuid(project.uuid());
  }

  public static ProjectAlmSettingDto newBitbucketProjectAlmSettingDto(AlmSettingDto githubAlmSetting, ComponentDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(githubAlmSetting.getUuid())
      .setProjectUuid(project.uuid())
      .setAlmRepo(randomAlphanumeric(256))
      .setAlmSlug(randomAlphanumeric(256));
  }
}
