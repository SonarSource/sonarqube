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
package org.sonar.db.almsettings;

import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.RandomStringUtils.randomNumeric;

public class AlmSettingsTesting {

  private AlmSettingsTesting() {

  }

  public static AlmSettingDto newGithubAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(randomAlphanumeric(200))
      .setUrl(randomAlphanumeric(2000))
      .setAppId(randomNumeric(8))
      .setClientId(randomNumeric(8))
      .setClientSecret(randomAlphanumeric(80))
      .setPrivateKey(randomAlphanumeric(2000))
      .setAlm(ALM.GITHUB);
  }

  public static AlmSettingDto newAzureAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(randomAlphanumeric(200))
      .setPersonalAccessToken(randomAlphanumeric(2000))
      .setUrl(randomAlphanumeric(2000))
      .setAlm(ALM.AZURE_DEVOPS);
  }

  public static AlmSettingDto newGitlabAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(randomAlphanumeric(200))
      .setPersonalAccessToken(randomAlphanumeric(2000))
      .setUrl(randomAlphanumeric(2000))
      .setAlm(ALM.GITLAB);
  }

  public static AlmSettingDto newBitbucketAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(randomAlphanumeric(200))
      .setUrl(randomAlphanumeric(2000))
      .setPersonalAccessToken(randomAlphanumeric(2000))
      .setAlm(ALM.BITBUCKET);
  }

  public static AlmSettingDto newBitbucketCloudAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(randomAlphanumeric(200))
      .setClientId(randomAlphanumeric(50))
      .setAppId(randomAlphanumeric(80))
      .setClientSecret(randomAlphanumeric(50))
      .setAlm(ALM.BITBUCKET_CLOUD);
  }

  public static ProjectAlmSettingDto newGithubProjectAlmSettingDto(AlmSettingDto githubAlmSetting, ProjectDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(githubAlmSetting.getUuid())
      .setProjectUuid(project.getUuid())
      .setAlmRepo(randomAlphanumeric(256))
      .setSummaryCommentEnabled(true)
      .setMonorepo(false);
  }

  public static ProjectAlmSettingDto newGitlabProjectAlmSettingDto(AlmSettingDto gitlabAlmSetting, ProjectDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(gitlabAlmSetting.getUuid())
      .setProjectUuid(project.getUuid())
      .setMonorepo(false);
  }

  static ProjectAlmSettingDto newAzureProjectAlmSettingDto(AlmSettingDto azureAlmSetting, ProjectDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(azureAlmSetting.getUuid())
      .setProjectUuid(project.getUuid())
      .setAlmSlug(randomAlphanumeric(256))
      .setAlmRepo(randomAlphanumeric(256))
      .setMonorepo(false);
  }

  public static ProjectAlmSettingDto newBitbucketProjectAlmSettingDto(AlmSettingDto bitbucketAlmSetting, ProjectDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(bitbucketAlmSetting.getUuid())
      .setProjectUuid(project.getUuid())
      .setAlmRepo(randomAlphanumeric(256))
      .setAlmSlug(randomAlphanumeric(256))
      .setMonorepo(false);
  }

  public static ProjectAlmSettingDto newBitbucketCloudProjectAlmSettingDto(AlmSettingDto bitbucketCloudAlmSetting, ProjectDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(bitbucketCloudAlmSetting.getUuid())
      .setProjectUuid(project.getUuid())
      .setAlmRepo(randomAlphanumeric(256))
      .setMonorepo(false);
  }
}
