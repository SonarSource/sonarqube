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
package org.sonar.server.common;

import org.junit.jupiter.api.Test;
import org.sonar.db.alm.setting.ALM;
import org.sonarqube.ws.AlmSettings;

import static org.assertj.core.api.Assertions.assertThat;

class AlmSettingMapperTest {


  @Test
  void toResponseAlm_shouldCorrectlyMapAlms() {
    assertThat(AlmSettingMapper.toResponseAlm(ALM.GITHUB)).isEqualTo(AlmSettings.Alm.github);
    assertThat(AlmSettingMapper.toResponseAlm(ALM.BITBUCKET)).isEqualTo(AlmSettings.Alm.bitbucket);
    assertThat(AlmSettingMapper.toResponseAlm(ALM.BITBUCKET_CLOUD)).isEqualTo(AlmSettings.Alm.bitbucketcloud);
    assertThat(AlmSettingMapper.toResponseAlm(ALM.AZURE_DEVOPS)).isEqualTo(AlmSettings.Alm.azure);
    assertThat(AlmSettingMapper.toResponseAlm(ALM.GITLAB)).isEqualTo(AlmSettings.Alm.gitlab);
  }

}