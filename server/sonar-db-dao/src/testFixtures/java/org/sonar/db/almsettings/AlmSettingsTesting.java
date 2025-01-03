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

import static org.apache.commons.lang3.RandomStringUtils.secure;


public class AlmSettingsTesting {

  private AlmSettingsTesting() {

  }

  public static AlmSettingDto newGithubAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(secure().nextAlphanumeric(200))
      .setUrl(secure().nextAlphanumeric(2000))
      .setAppId(secure().nextNumeric(8))
      .setClientId(secure().nextNumeric(8))
      .setClientSecret(secure().nextAlphanumeric(80))
      .setPrivateKey(secure().nextAlphanumeric(2000))
      .setAlm(ALM.GITHUB);
  }

  public static AlmSettingDto newAzureAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(secure().nextAlphanumeric(200))
      .setPersonalAccessToken(secure().nextAlphanumeric(2000))
      .setUrl(secure().nextAlphanumeric(2000))
      .setAlm(ALM.AZURE_DEVOPS);
  }

  public static AlmSettingDto newGitlabAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(secure().nextAlphanumeric(200))
      .setPersonalAccessToken(secure().nextAlphanumeric(2000))
      .setUrl(secure().nextAlphanumeric(2000))
      .setAlm(ALM.GITLAB);
  }

  public static AlmSettingDto newBitbucketAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(secure().nextAlphanumeric(200))
      .setUrl(secure().nextAlphanumeric(2000))
      .setPersonalAccessToken(secure().nextAlphanumeric(2000))
      .setAlm(ALM.BITBUCKET);
  }

  public static AlmSettingDto newBitbucketCloudAlmSettingDto() {
    return new AlmSettingDto()
      .setKey(secure().nextAlphanumeric(200))
      .setClientId(secure().nextAlphanumeric(50))
      .setAppId(secure().nextAlphanumeric(80))
      .setClientSecret(secure().nextAlphanumeric(50))
      .setAlm(ALM.BITBUCKET_CLOUD);
  }

  public static ProjectAlmSettingDto newGithubProjectAlmSettingDto(AlmSettingDto githubAlmSetting, ProjectDto project) {
    return newGithubProjectAlmSettingDto(githubAlmSetting, project, false);
  }

  public static ProjectAlmSettingDto newGithubProjectAlmSettingDto(AlmSettingDto githubAlmSetting, ProjectDto project, boolean monorepo) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(githubAlmSetting.getUuid())
      .setProjectUuid(project.getUuid())
      .setAlmRepo(secure().nextAlphanumeric(256))
      .setSummaryCommentEnabled(true)
      .setMonorepo(monorepo);
  }

  public static ProjectAlmSettingDto newGitlabProjectAlmSettingDto(AlmSettingDto gitlabAlmSetting, ProjectDto project) {
    return newGitlabProjectAlmSettingDto(gitlabAlmSetting, project, false);
  }

  public static ProjectAlmSettingDto newGitlabProjectAlmSettingDto(AlmSettingDto gitlabAlmSetting, ProjectDto project, boolean monorepo) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(gitlabAlmSetting.getUuid())
      .setProjectUuid(project.getUuid())
      .setMonorepo(monorepo);
  }

  public static ProjectAlmSettingDto newAzureProjectAlmSettingDto(AlmSettingDto azureAlmSetting, ProjectDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(azureAlmSetting.getUuid())
      .setProjectUuid(project.getUuid())
      .setAlmSlug(secure().nextAlphanumeric(256))
      .setAlmRepo(secure().nextAlphanumeric(256))
      .setInlineAnnotationsEnabled(true)
      .setMonorepo(false);
  }

  public static ProjectAlmSettingDto newBitbucketProjectAlmSettingDto(AlmSettingDto bitbucketAlmSetting, ProjectDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(bitbucketAlmSetting.getUuid())
      .setProjectUuid(project.getUuid())
      .setAlmRepo(secure().nextAlphanumeric(256))
      .setAlmSlug(secure().nextAlphanumeric(256))
      .setMonorepo(false);
  }

  public static ProjectAlmSettingDto newBitbucketCloudProjectAlmSettingDto(AlmSettingDto bitbucketCloudAlmSetting, ProjectDto project) {
    return new ProjectAlmSettingDto()
      .setAlmSettingUuid(bitbucketCloudAlmSetting.getUuid())
      .setProjectUuid(project.getUuid())
      .setAlmRepo(secure().nextAlphanumeric(256))
      .setMonorepo(false);
  }
}
