/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.monitoring;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class AlmConfigurationSectionTest {

  @Rule
  public DbTester db = DbTester.create();

  private AlmConfigurationSection underTest = new AlmConfigurationSection(db.getDbClient());

  @Test
  public void alm_are_listed() {
    AlmSettingDto azure = db.almSettings().insertAzureAlmSetting();
    AlmSettingDto github = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto gitlab = db.almSettings().insertGitlabAlmSetting();
    AlmSettingDto bitbucket = db.almSettings().insertBitbucketAlmSetting();
    AlmSettingDto bitbucketCloud = db.almSettings().insertBitbucketCloudAlmSetting();

    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThat(section.getAttributesList()).hasSize(5);
    assertThat(section.getAttributesList())
      .extracting(Attribute::getKey, Attribute::getStringValue)
      .containsExactlyInAnyOrder(
        tuple(azure.getKey(), String.format("Alm:%s, Url:%s", azure.getRawAlm(), azure.getUrl())),
        tuple(github.getKey(), String.format("Alm:%s, Url:%s, App Id:%s, Client Id:%s", github.getRawAlm(), github.getUrl(), github.getAppId(), github.getClientId())),
        tuple(gitlab.getKey(), String.format("Alm:%s, Url:%s", gitlab.getRawAlm(), gitlab.getUrl())),
        tuple(bitbucket.getKey(), String.format("Alm:%s, Url:%s", bitbucket.getRawAlm(), bitbucket.getUrl())),
        tuple(bitbucketCloud.getKey(), String.format("Alm:%s, Workspace Id:%s, OAuth Key:%s", bitbucketCloud.getRawAlm(), bitbucketCloud.getAppId(), bitbucketCloud.getClientId())));
  }

  @Test
  public void several_alm_same_type() {
    AlmSettingDto gitlab1 = db.almSettings().insertGitlabAlmSetting();
    AlmSettingDto gitlab2 = db.almSettings().insertGitlabAlmSetting();

    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThat(section.getAttributesList()).hasSize(2);
    assertThat(section.getAttributesList())
      .extracting(Attribute::getKey, Attribute::getStringValue)
      .containsExactlyInAnyOrder(
        tuple(gitlab1.getKey(), String.format("Alm:%s, Url:%s", gitlab1.getRawAlm(), gitlab1.getUrl())),
        tuple(gitlab2.getKey(), String.format("Alm:%s, Url:%s", gitlab2.getRawAlm(), gitlab2.getUrl())));
  }

  @Test
  public void null_url_are_ignored() {
    AlmSettingDto azure = db.almSettings().insertAzureAlmSetting(a -> a.setUrl(null));

    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThat(section.getAttributesList()).hasSize(1);
    assertThat(section.getAttributesList())
      .extracting(Attribute::getKey, Attribute::getStringValue)
      .containsExactlyInAnyOrder(
        tuple(azure.getKey(), String.format("Alm:%s", azure.getRawAlm())));
  }

}
