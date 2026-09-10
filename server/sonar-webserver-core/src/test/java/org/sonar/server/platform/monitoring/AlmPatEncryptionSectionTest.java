/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class AlmPatEncryptionSectionTest {

  private static final String CLEAR_TEXT_TOKENS = "Stored As Clear Text";

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final AlmPatEncryptionSection underTest = new AlmPatEncryptionSection(db.getDbClient());

  @Test
  void toProtobuf_shouldReportHowManyTokensAreStoredAsClearText() {
    AlmSettingDto github = db.almSettings().insertGitHubAlmSetting();
    db.almPats().insert(p -> p.setAlmSettingUuid(github.getUuid()));
    db.almPats().insert(p -> p.setAlmSettingUuid(github.getUuid()));

    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThat(section.getAttributesList())
      .extracting(Attribute::getKey, Attribute::getLongValue)
      .containsExactly(tuple(CLEAR_TEXT_TOKENS, 2L));
  }

  @Test
  void toProtobuf_whenNoTokenIsStoredAsClearText_shouldStillReportAZero() {
    // a healthy instance has to show a zero, otherwise nothing tells an administrator this was even looked at
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThat(section.getAttributesList())
      .extracting(Attribute::getKey, Attribute::getLongValue)
      .containsExactly(tuple(CLEAR_TEXT_TOKENS, 0L));
  }

  @Test
  void toProtobuf_whenADevOpsPlatformConfigurationIsNamedAfterTheAttribute_shouldStillReportBoth() {
    // the attributes of the ALMs section are keyed by names an administrator picked, which is why the count is not
    // one of them: two attributes with the same key in one section collapse into one, and either value is then lost
    db.almSettings().insertGitHubAlmSetting(almSetting -> almSetting.setKey(CLEAR_TEXT_TOKENS));
    db.almPats().insert();

    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    ProtobufSystemInfo.Section almConfigurationSection = new AlmConfigurationSection(db.getDbClient()).toProtobuf();

    assertThat(section.getName()).isNotEqualTo(almConfigurationSection.getName());
    assertThat(section.getAttributesList())
      .extracting(Attribute::getKey, Attribute::getLongValue)
      .containsExactly(tuple(CLEAR_TEXT_TOKENS, 1L));
    assertThat(almConfigurationSection.getAttributesList())
      .extracting(Attribute::getKey)
      .containsExactly(CLEAR_TEXT_TOKENS);
  }
}
